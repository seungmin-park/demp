package com.inhatc.demp.service;

import com.inhatc.demp.domain.announcemnet.Announcement;
import com.inhatc.demp.domain.announcemnet.Description;
import com.inhatc.demp.domain.announcemnet.Career;
import com.inhatc.demp.dto.announcement.AnnouncementCreateRequest;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import java.io.IOException;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AnnouncementServiceTest {

    @Autowired
    private AnnouncementService announcementService;
    @Autowired
    private AnnouncementRepository announcementRepository;
    @MockBean
    private FileService fileService;

    @AfterEach
    void cleanUp() {
        announcementRepository.deleteAll();
    }

    @Test
    @DisplayName("공고 생성 요청의 본문은 정제해서 저장하고 입력 설명은 보존한다")
    void sanitizeCreateRequest() throws IOException {
        Description description = description();
        AnnouncementCreateRequest request = new AnnouncementCreateRequest();
        request.setTitle("본문 정제 공고");
        request.setDescription(description);
        request.setCareer(new Career(0, 1));

        announcementService.save(request);

        assertSanitized("본문 정제 공고");
        assertThat(description.getContent()).contains("<script>", "onclick");
    }

    @Test
    @DisplayName("공고 엔티티 저장 경로도 본문을 정제한 새 설명을 저장한다")
    void sanitizeJoin() {
        Description description = description();
        Announcement announcement = Announcement.builder().title("엔티티 정제 공고")
                .description(description).career(new Career(0, 1)).build();

        announcementService.join(announcement);

        assertSanitized("엔티티 정제 공고");
        assertThat(description.getContent()).contains("<script>", "onclick");
    }

    private Description description() {
        return new Description("<p onclick=\"alert(1)\"><strong>채용</strong></p><script>alert(1)</script>",
                "https://example.com/jobs", 3000, Set.of());
    }

    private void assertSanitized(String title) {
        Announcement saved = announcementRepository.findByTitle(title).orElseThrow();
        assertThat(saved.getDescription().getContent()).isEqualTo("<p><strong>채용</strong></p>");
        assertThat(saved.getDescription().getAccessUrl()).isEqualTo("https://example.com/jobs");
        assertThat(saved.getDescription().getPayment()).isEqualTo(3000);
        assertThat(announcementService.findById(saved.getId()).orElseThrow().getDescription().getContent())
                .isEqualTo("<p><strong>채용</strong></p>");
    }
}
