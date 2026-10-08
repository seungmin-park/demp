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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import com.inhatc.demp.domain.announcement.EmploymentType;
import com.inhatc.demp.domain.announcement.RecruitmentAudience;
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
    @Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;
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
                .career(Career.builder()
                        .minCareer(0)
                        .maxCareer(1)
                        .build()).description(Description.builder().content("본문").accessUrl("https://example.test/job").payment(0).languages(Set.of()).build())
                .company(Company.builder()
                        .name("DEMP")
                        .build()).image(UploadFile.builder().uploadFileName("image.png").saveFileName("saved.png").build())
                .recruitPeriod(RecruitPeriod.builder()
                        .startedDate(LocalDateTime.of(2026, 9, 1, 0, 0))
                        .deadLineDate(LocalDateTime.of(2026, 9, 30, 0, 0))
                        .build())
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
        when(fileService.save(request.getImage())).thenReturn(UploadFile.builder()
                .uploadFileName("image.png")
                .saveFileName("saved.png")
                .build());

        announcementService.createAnnouncement(request);

        assertSanitized("본문 정제 공고");
        assertThat(description.getContent()).contains("<script>", "onclick");
    }

    @Test
    @DisplayName("중복 공고는 파일을 업로드하기 전에 409로 거절한다")
    void rejectsDuplicateBeforeUpload() {
        announcementService.saveAnnouncementEntity(Announcement.builder()
                .title("중복 공고")
                .career(Career.builder().minCareer(0).maxCareer(1).build())
                .description(description())
                .company(Company.builder().name("DEMP").build())
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
                .description(description).career(Career.builder().minCareer(0).maxCareer(1).build()).build();

        announcementService.saveAnnouncementEntity(announcement);

        assertSanitized("엔티티 정제 공고");
        assertThat(description.getContent()).contains("<script>", "onclick");
    }

    @Test
    @DisplayName("대표 이미지 없이 생성한 공고도 상세·검색·관련 목록에서 원문과 본문을 조회한다")
    void createsWithoutCoverImage() throws IOException {
        AnnouncementCreateRequest request = request("텍스트 원문 공고");
        request.setImage(null);
        request.setPublicationStatus(com.inhatc.demp.domain.announcement.PublicationStatus.PUBLISHED);
        announcementService.createAnnouncement(request);
        Announcement saved = announcementRepository.findByTitle("텍스트 원문 공고").orElseThrow();
        var detail = announcementService.findDetailResponse(saved.getId()).orElseThrow();
        assertThat(detail.getImage()).isEmpty();
        assertThat(detail.getAccessUrl()).isEqualTo("https://example.com/jobs");
        assertThat(detail.getContent()).contains("채용");
        assertThat(announcementService.findScrollResponses()).extracting(item -> item.getImage()).containsExactly("");
        assertThat(announcementService.findAnnouncementSlice(new AnnouncementSearchCondition(), PageRequest.of(0, 8))
                .getContent()).extracting(item -> item.getImage()).containsExactly("");
        verifyNoInteractions(fileService);
    }

    private Description description() {
        return Description.builder()
                .content("<p onclick=\"alert(1)\"><strong>채용</strong></p><script>alert(1)</script>")
                .accessUrl("https://example.com/jobs")
                .payment(3000)
                .languages(Set.of())
                .build();
    }

    @ParameterizedTest
    @DisplayName("각 고용 형태는 신입 대상과 독립적으로 커밋되고 상세·목록·관련 응답에 반환된다")
    @EnumSource(EmploymentType.class)
    void persistsEmploymentTypeAcrossResponses(EmploymentType employmentType) throws IOException {
        AnnouncementCreateRequest request = request("고용 형태 공고");
        request.setImage(null);
        request.setPublicationStatus(com.inhatc.demp.domain.announcement.PublicationStatus.PUBLISHED);
        request.setRecruitmentAudience(RecruitmentAudience.NEW);
        request.setEmploymentType(employmentType);
        announcementService.createAnnouncement(request);

        Announcement saved = announcementRepository.findByTitle("고용 형태 공고").orElseThrow();
        assertThat(saved.getRecruitmentAudience()).isEqualTo(RecruitmentAudience.NEW);
        assertThat(saved.getEmploymentType()).isEqualTo(employmentType);
        assertThat(announcementService.findDetailResponse(saved.getId()).orElseThrow().getEmploymentType()).isEqualTo(employmentType);
        var listing = announcementService.findAnnouncementSlice(new AnnouncementSearchCondition(), PageRequest.of(0, 8));
        assertThat(listing.getContent()).hasSize(1);
        assertThat(listing.hasNext()).isFalse();
        assertThat(listing.getContent().getFirst().getEmploymentType()).isEqualTo(employmentType);
        var related = announcementService.findScrollResponses();
        assertThat(related).hasSize(1);
        assertThat(related.getFirst().getEmploymentType()).isEqualTo(employmentType);
        verifyNoInteractions(fileService);
    }

    @Test
    @DisplayName("고용 형태를 지정하지 않은 공고는 저장·재조회 후에도 미확인이다")
    void preservesUnknownEmploymentType() throws IOException {
        AnnouncementCreateRequest request = request("미확인 고용 형태");
        request.setImage(null);
        request.setPublicationStatus(com.inhatc.demp.domain.announcement.PublicationStatus.PUBLISHED);
        announcementService.createAnnouncement(request);
        Announcement saved = announcementRepository.findByTitle("미확인 고용 형태").orElseThrow();
        assertThat(saved.getEmploymentType()).isNull();
        assertThat(announcementService.findDetailResponse(saved.getId()).orElseThrow().getEmploymentType()).isNull();
    }

    @ParameterizedTest
    @DisplayName("새 직무와 기술 조합은 커밋 후 상세·검색에서 기존 문자열과 함께 보존된다")
    @CsvSource({"SRE,KOTLIN|SPRING_BOOT|KUBERNETES|React", "AI_RESEARCH,PYTHON|PYTORCH|RAG", "GAME_ENGINE,CPP|CSHARP|UNITY"})
    void persistsExpandedCatalog(String position, String technologies) throws IOException {
        assertThat(JobPosition.values()).extracting(Enum::name).contains(position);
        var values = java.util.Arrays.stream(technologies.split("\\|")).map(Language::valueOf).collect(java.util.stream.Collectors.toSet());
        AnnouncementCreateRequest request = request("확장 스택 " + position);
        request.setImage(null); request.setPublicationStatus(com.inhatc.demp.domain.announcement.PublicationStatus.PUBLISHED);
        request.setPosition(JobPosition.valueOf(position)); request.setLanguage(values);
        announcementService.createAnnouncement(request);

        var saved = announcementRepository.findByTitle(request.getTitle()).orElseThrow();
        assertThat(saved.getJobPosition().name()).isEqualTo(position);
        assertThat(jdbc.queryForList("SELECT languages FROM language WHERE announcement_id = ?", String.class, saved.getId()))
                .containsExactlyInAnyOrderElementsOf(values.stream().map(Enum::name).toList());
        var detail = announcementService.findDetailResponse(saved.getId()).orElseThrow();
        assertThat(detail.getPosition().name()).isEqualTo(position);
        assertThat(detail.getLanguage()).containsExactlyInAnyOrderElementsOf(values);
        var condition = new AnnouncementSearchCondition(); condition.setPositions(java.util.List.of(JobPosition.valueOf(position))); condition.setLanguages(java.util.List.of(values.iterator().next()));
        var page = announcementService.findAnnouncementSlice(condition, PageRequest.of(0, 8));
        assertThat(page.getContent()).extracting(item -> item.getId()).containsExactly(saved.getId());
        assertThat(page.hasNext()).isFalse();
        assertThat(page.getContent().getFirst().getLanguage()).containsExactlyInAnyOrderElementsOf(values);
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
