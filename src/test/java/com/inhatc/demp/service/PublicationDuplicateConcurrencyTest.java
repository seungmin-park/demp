package com.inhatc.demp.service;
import com.inhatc.demp.domain.announcement.*;
import com.inhatc.demp.dto.announcement.*;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import com.inhatc.demp.error.ApiException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
@SpringBootTest
class PublicationDuplicateConcurrencyTest {
    @Autowired AnnouncementService service;
    @MockitoSpyBean AnnouncementRepository repository;
    @AfterEach void cleanup() { repository.deleteAll(); }
    @Test
    @DisplayName("서로 다른 제목으로 동시에 등록해도 동일 출처는 하나만 저장하고 나머지는 409이다")
    void simultaneousDuplicatesAreRejected() throws Exception {
        var barrier = new CyclicBarrier(2);
        var delegate = mockingDetails(repository).getMockCreationSettings().getDefaultAnswer();
        doAnswer(invocation -> { barrier.await(10, TimeUnit.SECONDS); return delegate.answer(invocation); }).when(repository).saveAndFlush(any(Announcement.class));
        try (var executor = Executors.newFixedThreadPool(2)) {
            var results = executor.invokeAll(List.of(attempt("동시 등록 A"), attempt("동시 등록 B")), 15, TimeUnit.SECONDS);
            assertThat(List.of(results.get(0).get(), results.get(1).get())).containsExactlyInAnyOrder(201,409);
            assertThat(repository.count()).isEqualTo(1);
        }
    }
    private Callable<Integer> attempt(String title) {
        return () -> {
            var request = new AnnouncementCreateRequest(); request.setTitle(title); request.setCompany("DEMP"); request.setType(AnnouncementType.EMP);
            request.setPosition(JobPosition.BACKEND); request.setLanguage(Set.of(Language.JAVA)); request.setContent("동시 등록 본문"); request.setAccessUrl("https://example.com/race");
            request.setStartedDate(LocalDateTime.of(2026,9,1,0,0)); request.setDeadLineDate(LocalDateTime.of(2026,12,31,0,0));
            try { service.createAnnouncement(request); return 201; } catch (ApiException e) { return e.getStatus().value(); }
        };
    }
}
