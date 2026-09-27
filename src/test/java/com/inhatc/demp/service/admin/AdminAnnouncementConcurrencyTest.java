package com.inhatc.demp.service.admin;

import com.inhatc.demp.domain.announcement.*;
import com.inhatc.demp.dto.announcement.AnnouncementUpdateRequest;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import com.inhatc.demp.service.FileService;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class AdminAnnouncementConcurrencyTest {
    @Autowired AdminAnnouncementService service;
    @MockitoSpyBean AnnouncementRepository repository;
    @MockitoBean FileService files;

    @AfterEach
    void cleanup() { repository.deleteAll(); }

    @ParameterizedTest
    @DisplayName("이미지 없는 수정과 교체 또는 삭제가 겹쳐도 삭제한 이미지 참조가 되살아나지 않는다")
    @ValueSource(booleans = {false, true})
    void concurrentMutationsPreserveCommittedImage(boolean delete) throws Exception {
        Announcement original = repository.save(Announcement.builder().title("동시 수정 원본")
                .company(Company.builder().name("DEMP").build()).career(Career.builder().minCareer(0).maxCareer(0).build())
                .description(Description.builder()
                        .content("본문")
                        .accessUrl("https://example.test")
                        .payment(0)
                        .languages(Set.of(Language.JAVA))
                        .build())
                .image(UploadFile.builder().uploadFileName("old.png").saveFileName("old.png").build())
                .recruitPeriod(RecruitPeriod.builder()
                        .startedDate(LocalDateTime.of(2026,1,1,0,0))
                        .deadLineDate(LocalDateTime.of(2026,12,31,0,0))
                        .build())
                .announcementType(AnnouncementType.EMP).jobPosition(JobPosition.BACKEND).build());
        CountDownLatch firstRead = new CountDownLatch(1), releaseFirst = new CountDownLatch(1), secondStarted = new CountDownLatch(1);
        var repositoryDelegate = mockingDetails(repository).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocation -> {
            if (Thread.currentThread().getName().equals("admin-text-update")) {
                firstRead.countDown();
                assertThat(releaseFirst.await(10, TimeUnit.SECONDS)).isTrue();
            }
            return repositoryDelegate.answer(invocation);
        }).when(repository).saveAndFlush(org.mockito.ArgumentMatchers.any(Announcement.class));
        when(files.save(org.mockito.ArgumentMatchers.any())).thenReturn(UploadFile.builder()
                .uploadFileName("new.png")
                .saveFileName("new.png")
                .build());
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> first = executor.submit(() -> {
                Thread.currentThread().setName("admin-text-update");
                return service.update(original.getId(), request());
            });
            assertThat(firstRead.await(5, TimeUnit.SECONDS)).isTrue();
            Future<?> second = executor.submit(() -> {
                secondStarted.countDown();
                if (delete) return service.delete(original.getId());
                AnnouncementUpdateRequest replacement = request();
                replacement.setImage(new MockMultipartFile("image", "new.png", "image/png", new byte[]{1}));
                return service.update(original.getId(), replacement);
            });
            assertThat(secondStarted.await(5, TimeUnit.SECONDS)).isTrue();
            // Without row locking the second mutation commits here; with locking it waits.
            try { second.get(1, TimeUnit.SECONDS); } catch (TimeoutException expectedLockWait) { }
            finally { releaseFirst.countDown(); }
            first.get(5, TimeUnit.SECONDS);
            second.get(5, TimeUnit.SECONDS);
            if (delete) assertThat(repository.findById(original.getId())).isEmpty();
            else assertThat(repository.findById(original.getId()).orElseThrow().getImage().getSaveFileName()).isEqualTo("new.png");
            verify(files, times(1)).delete("old.png");
            verify(files, never()).delete("new.png");
        } finally {
            releaseFirst.countDown(); executor.shutdownNow();
            assertThat(executor.awaitTermination(5, TimeUnit.SECONDS)).isTrue();
        }
    }

    private AnnouncementUpdateRequest request() {
        AnnouncementUpdateRequest request = new AnnouncementUpdateRequest();
        request.setTitle("동시 수정 결과"); request.setCompany("DEMP"); request.setType(AnnouncementType.EMP);
        request.setPosition(JobPosition.BACKEND); request.setLanguage(Set.of(Language.JAVA));
        request.setStartedDate(LocalDateTime.of(2026,1,1,0,0)); request.setDeadLineDate(LocalDateTime.of(2026,12,31,0,0));
        request.setContent("수정 본문"); request.setAccessUrl("https://example.test");
        return request;
    }
}
