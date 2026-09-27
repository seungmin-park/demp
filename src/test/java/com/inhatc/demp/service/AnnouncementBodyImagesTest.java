package com.inhatc.demp.service;

import com.inhatc.demp.domain.announcement.*;
import com.inhatc.demp.dto.announcement.*;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import com.inhatc.demp.service.admin.AdminAnnouncementService;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.mock.web.MockMultipartFile;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@TestPropertySource(properties = "cloud.aws.s3.public-base-url=https://cdn.example/images")
class AnnouncementBodyImagesTest {
    @Autowired AnnouncementService service;
    @Autowired AdminAnnouncementService admin;
    @Autowired AnnouncementRepository repository;
    @MockitoBean FileService files;
    @org.springframework.test.context.bean.override.mockito.MockitoSpyBean AnnouncementImageUrl urls;

    @AfterEach
    void cleanup() { repository.deleteAll(); }

    @Test
    @DisplayName("본문 이미지는 업로드한 파일 주소로 바뀌고 외부 이미지와 실행 속성은 저장하지 않는다")
    void storesOwnedImagesAndText() throws Exception {
        var request = request();
        request.setContent("<h2 style='color:red'>업무</h2><p><u>개발</u></p><img src='attachment:0' alt='교육 공간' onerror='bad()'><img src='https://foreign.test/a.png'><script>bad()</script>");
        service.createAnnouncement(request);
        var item = repository.findByTitle(request.getTitle()).orElseThrow();
        assertThat(service.findDetailResponse(item.getId()).orElseThrow().getContent())
                .contains("<h2>업무</h2>", "<u>개발</u>", "https://cdn.example/images/body.png", "alt=\"교육 공간\"")
                .doesNotContain("attachment:", "foreign.test", "onerror", "style=", "script");
        verify(files).save(request.getBodyImages().getFirst());
    }

    @Test
    @DisplayName("본문 이미지 수정은 기존 파일을 유지하고 제거할 때 DB 커밋 후 정리한다")
    void keepsAndRemovesImages() throws Exception {
        var request = request(); service.createAnnouncement(request);
        var item = repository.findByTitle(request.getTitle()).orElseThrow();
        var update = update();
        update.setContent("<p>수정</p><img src='https://cdn.example/images/body.png' alt='사진'>");
        admin.update(item.getId(), update);
        assertThat(service.findDetailResponse(item.getId()).orElseThrow().getContent()).contains("body.png");
        verify(files, never()).delete("body.png");
        update.setContent("<p>사진 제거</p>");
        doAnswer(call -> {
            assertThat(repository.findById(item.getId()).orElseThrow().getDescription().getContent()).doesNotContain("body.png");
            return null;
        }).when(files).delete("body.png");
        admin.update(item.getId(), update);
        verify(files).delete("body.png");
    }

    @Test
    @DisplayName("공고를 삭제하면 본문 첨부 파일도 정리한다")
    void deletesBodyImages() throws Exception {
        var request = request(); service.createAnnouncement(request);
        var item = repository.findByTitle(request.getTitle()).orElseThrow();
        doAnswer(call -> {
            assertThat(repository.existsById(item.getId())).isFalse();
            return null;
        }).when(files).delete("body.png");
        assertThat(admin.delete(item.getId()).cleanupPending()).isFalse();
        verify(files).delete("body.png");
    }

    @Test
    @DisplayName("두 번째 본문 파일 업로드 실패 시 먼저 저장한 파일을 보상하고 공고는 생성하지 않는다")
    void compensatesPartialUpload() throws Exception {
        var request = request();
        var second = new MockMultipartFile("bodyImages", "second.png", "image/png", new byte[]{2});
        request.setBodyImages(List.of(request.getBodyImages().getFirst(), second));
        request.setContent("<p>업무</p><img src='attachment:0'><img src='attachment:1'>");
        when(files.save(second)).thenThrow(new IOException("upload unavailable"));
        assertThatThrownBy(() -> service.createAnnouncement(request)).isInstanceOf(IOException.class);
        assertThat(repository.count()).isZero();
        verify(files).delete("body.png");
    }

    @Test
    @DisplayName("이미지 공개 주소가 변경되어도 기존 공고 수정은 첨부를 보존하고 현재 주소로 갱신한다")
    void preservesImagesWhenPublicBaseChanges() throws Exception {
        var request = request(); service.createAnnouncement(request);
        var item = repository.findByTitle(request.getTitle()).orElseThrow();
        var update = update(); update.setContent(item.getDescription().getContent());
        doReturn("https://new-cdn.example/assets/body.png").when(urls).forKey("body.png");
        admin.update(item.getId(), update);
        assertThat(service.findDetailResponse(item.getId()).orElseThrow().getContent())
                .contains("https://new-cdn.example/assets/body.png");
        verify(files, never()).delete("body.png");
    }

    private AnnouncementCreateRequest request() throws IOException {
        var request = new AnnouncementCreateRequest(); fill(request);
        var image = new MockMultipartFile("bodyImages", "body.png", "image/png", new byte[]{1});
        request.setBodyImages(List.of(image));
        request.setContent("<p>업무</p><img src='attachment:0' alt='사진'>");
        when(files.save(image)).thenReturn(UploadFile.builder().uploadFileName("body.png").saveFileName("body.png").build());
        return request;
    }
    private AnnouncementUpdateRequest update() { var request = new AnnouncementUpdateRequest(); fill(request); return request; }
    private void fill(AnnouncementFields request) {
        request.setPublicationStatus(PublicationStatus.PUBLISHED); request.setTitle("본문 이미지 공고"); request.setCompany("DEMP"); request.setType(AnnouncementType.EMP);
        request.setPosition(JobPosition.BACKEND); request.setLanguage(Set.of(Language.JAVA));
        request.setAccessUrl("https://employer.test/careers/1");
        request.setStartedDate(LocalDateTime.of(2026,9,1,0,0)); request.setDeadLineDate(LocalDateTime.of(2026,12,31,0,0));
    }
}
