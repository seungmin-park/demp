package com.inhatc.demp.service;

import com.inhatc.demp.domain.announcement.Announcement;
import com.inhatc.demp.domain.announcement.Description;
import com.inhatc.demp.domain.announcement.AnnouncementType;
import com.inhatc.demp.domain.announcement.Career;
import com.inhatc.demp.domain.announcement.JobPosition;
import com.inhatc.demp.domain.announcement.Language;
import com.inhatc.demp.domain.announcement.Company;
import com.inhatc.demp.domain.announcement.RecruitPeriod;
import com.inhatc.demp.domain.announcement.UploadFile;
import com.inhatc.demp.dto.announcement.AnnouncementCreateRequest;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.inhatc.demp.error.ApiException;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@SpringBootTest
@TestPropertySource(properties = "cloud.aws.s3.public-base-url=https://cdn.example/images")
class AnnouncementServiceTest {

    @Autowired
    private AnnouncementService announcementService;
    @Autowired
    private AnnouncementRepository announcementRepository;
    @MockitoBean
    private FileService fileService;

    @AfterEach
    void cleanUp() {
        announcementRepository.deleteAll();
    }

    @Test
    @DisplayName("공고의 상세·스크롤·검색 이미지 주소는 설정한 공개 주소를 사용한다")
    void usesConfiguredImageUrlInResponses() {
        Announcement announcement = Announcement.builder().title("이미지 공고")
                .career(new Career(0, 1)).description(new Description("본문", "https://example.test/job", 0, Set.of()))
                .company(new Company("DEMP")).image(new UploadFile("image.png", "saved.png"))
                .recruitPeriod(new RecruitPeriod(LocalDateTime.of(2026, 9, 1, 0, 0),
                        LocalDateTime.of(2026, 9, 30, 0, 0)))
                .announcementType(AnnouncementType.EMP).jobPosition(JobPosition.BACKEND)
                .build();
        announcementService.saveAnnouncementEntity(announcement);

        assertThat(announcementService.findDetailResponse(announcement.getId()).orElseThrow().getImage())
                .isEqualTo("https://cdn.example/images/saved.png");
        assertThat(announcementService.findScrollResponses()).extracting(response -> response.getImage())
                .containsExactly("https://cdn.example/images/saved.png");
        assertThat(announcementService.findAnnouncementSlice(new AnnouncementSearchCondition(), PageRequest.of(0, 8))
                .getContent()).extracting(response -> response.getImage())
                .containsExactly("https://cdn.example/images/saved.png");
    }

    @Test
    @DisplayName("공고 생성 요청의 본문은 정제해서 저장하고 입력 설명은 보존한다")
    void sanitizeCreateRequest() throws IOException {
        Description description = description();
        AnnouncementCreateRequest request = request("본문 정제 공고");
        request.setContent(description.getContent());
        request.setAccessUrl(description.getAccessUrl());
        request.setPayment(description.getPayment());
        when(fileService.save(request.getImage())).thenReturn(new UploadFile("image.png", "saved.png"));

        announcementService.createAnnouncement(request);

        assertSanitized("본문 정제 공고");
        assertThat(description.getContent()).contains("<script>", "onclick");
    }

    @Test
    @DisplayName("중복 공고는 파일을 업로드하기 전에 409로 거절한다")
    void rejectsDuplicateBeforeUpload() {
        announcementService.saveAnnouncementEntity(Announcement.builder()
                .title("중복 공고")
                .career(new Career(0, 1))
                .description(description())
                .company(new Company("DEMP"))
                .build());

        assertThatThrownBy(() -> announcementService.createAnnouncement(request("중복 공고")))
                .isInstanceOfSatisfying(ApiException.class,
                        exception -> assertThat(exception.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        verifyNoInteractions(fileService);
    }

    @Test
    @DisplayName("공고 엔티티 저장 경로도 본문을 정제한 새 설명을 저장한다")
    void sanitizeJoin() {
        Description description = description();
        Announcement announcement = Announcement.builder().title("엔티티 정제 공고")
                .description(description).career(new Career(0, 1)).build();

        announcementService.saveAnnouncementEntity(announcement);

        assertSanitized("엔티티 정제 공고");
        assertThat(description.getContent()).contains("<script>", "onclick");
    }

    private Description description() {
        return new Description("<p onclick=\"alert(1)\"><strong>채용</strong></p><script>alert(1)</script>",
                "https://example.com/jobs", 3000, Set.of());
    }

    private AnnouncementCreateRequest request(String title) {
        AnnouncementCreateRequest request = new AnnouncementCreateRequest();
        request.setTitle(title);
        request.setCompany("DEMP");
        request.setType(AnnouncementType.EMP);
        request.setPosition(JobPosition.BACKEND);
        request.setMinCareer(0);
        request.setMaxCareer(1);
        request.setStartedDate(LocalDateTime.of(2026, 9, 1, 0, 0));
        request.setDeadLineDate(LocalDateTime.of(2026, 9, 30, 23, 59));
        request.setContent(description().getContent());
        request.setAccessUrl(description().getAccessUrl());
        request.setPayment(description().getPayment());
        request.setLanguage(Set.of(Language.JAVA));
        request.setImage(new MockMultipartFile("image", "image.png", "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A}));
        return request;
    }

    private void assertSanitized(String title) {
        Announcement saved = announcementRepository.findByTitle(title).orElseThrow();
        assertThat(saved.getDescription().getContent()).isEqualTo("<p><strong>채용</strong></p>");
        assertThat(saved.getDescription().getAccessUrl()).isEqualTo("https://example.com/jobs");
        assertThat(saved.getDescription().getPayment()).isEqualTo(3000);
        assertThat(announcementRepository.findById(saved.getId()).orElseThrow().getDescription().getContent())
                .isEqualTo("<p><strong>채용</strong></p>");
    }
}
