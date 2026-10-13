package com.inhatc.demp.service;

import com.inhatc.demp.domain.announcement.*;
import com.inhatc.demp.dto.announcement.*;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import com.inhatc.demp.service.admin.AdminAnnouncementService;
import jakarta.validation.Validator;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class AnnouncementDraftRecoveryTest {
    @Autowired AnnouncementService service;
    @Autowired AdminAnnouncementService admin;
    @Autowired AnnouncementRepository repository;
    @Autowired Validator validator;
    @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired tools.jackson.databind.ObjectMapper objectMapper;
    @AfterEach void cleanup() { repository.deleteAll(); }

    @Test
    @DisplayName("미확인 분야 기술 일정의 초안은 실제 commit 후 null 그대로 재조회하고 공개하지 않는다")
    void savesIncompleteDraft() throws Exception {
        var request = draft();
        assertThat(validator.validate(request)).isEmpty();
        service.createAnnouncement(request);
        var id = repository.findByTitle("복원 중 공고").orElseThrow().getId();
        var detail = service.findAdminDetailResponse(id).orElseThrow();
        assertThat(detail.getPosition()).isNull();
        assertThat(detail.getLanguage()).isEmpty();
        assertThat(detail.getStartedDate()).isNull();
        assertThat(detail.getDeadLineDate()).isNull();
        assertThat(service.findDetailResponse(id)).isEmpty();
        var update = new AnnouncementUpdateRequest();
        update.setTitle(request.getTitle()); update.setCompany(request.getCompany()); update.setType(request.getType());
        update.setContent(request.getContent()); update.setAccessUrl(request.getAccessUrl());
        update.setPublicationStatus(PublicationStatus.PUBLISHED);
        assertThat(validator.validate(update)).isNotEmpty();
        assertThatThrownBy(() -> admin.update(id, update)).isInstanceOf(RuntimeException.class);
        assertThat(service.findAdminDetailResponse(id).orElseThrow().getPublicationStatus()).isEqualTo(PublicationStatus.DRAFT);
    }


    @Test
    @DisplayName("원문 식별값이 같은 공고는 대체 게시처 URL을 사용해도 중복 저장되지 않는다")
    void rejectsSourceAlias() throws Exception {
        var first = draft(); first.setSourceIdentifier("264146");
        service.createAnnouncement(first);
        var alias = draft(); alias.setSourceIdentifier("264146"); alias.setAccessUrl("https://linkareer.com/activity/345218");
        assertThatThrownBy(() -> service.createAnnouncement(alias))
                .isInstanceOf(com.inhatc.demp.error.ApiException.class);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("같은 원문 식별값을 가진 다른 기업은 별도 공고로 저장한다")
    void scopesIdentifierToCompany() throws Exception {
        var first = draft(); first.setSourceIdentifier("264146"); service.createAnnouncement(first);
        var other = draft(); other.setCompany("다른 회사"); other.setSourceIdentifier("264146");
        other.setAccessUrl("https://another.example/jobs/264146"); service.createAnnouncement(other);
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    @DisplayName("월급 근무기간 전환조건 직무별 요건과 보조 출처를 실제 저장 후 구별해 재조회한다")
    void preservesRecruitmentFacts() throws Exception {
        var request = draft();
        var values = new org.springframework.beans.MutablePropertyValues();
        values.add("salaryPeriod", "MONTH").add("payment", "230").add("salaryStatus", "DISCLOSED");
        values.add("workPeriod", "2026년 10월~2027년 2월, 약 5개월").add("conversionNote", "계약 연장 및 정규직 전환 불가");
        values.add("roles[0].title", "AI-OCR").add("roles[0].position", "AI").add("roles[0].requiredSkills", "Python 또는 PyTorch")
                .add("roles[0].preferredSkills", "문서 인식 경험").add("roles[0].usedTechnologies", "팀 사용 기술 확인 중");
        values.add("roles[1].title", "플랫폼 개발").add("roles[1].position", "FULLSTACK").add("roles[1].location", "서울");
        values.add("referenceUrls[0]", "https://linkareer.com/activity/345218");
        var binder = new org.springframework.validation.DataBinder(request); binder.bind(values);
        assertThat(binder.getBindingResult().getAllErrors()).isEmpty();
        service.createAnnouncement(request);
        var id = repository.findByTitle(request.getTitle()).orElseThrow().getId();
        var json = objectMapper.valueToTree(service.findAdminDetailResponse(id).orElseThrow());
        assertThat(json.path("salaryPeriod").asText()).isEqualTo("MONTH");
        assertThat(json.path("payment").asInt()).isEqualTo(230);
        assertThat(json.path("workPeriod").asText()).isEqualTo("2026년 10월~2027년 2월, 약 5개월");
        assertThat(json.path("conversionNote").asText()).isEqualTo("계약 연장 및 정규직 전환 불가");
        assertThat(json.path("roles").size()).isEqualTo(2);
        assertThat(json.path("roles").get(0).path("requiredSkills").asText()).isEqualTo("Python 또는 PyTorch");
        assertThat(json.path("roles").get(1).path("location").asText()).isEqualTo("서울");
        assertThat(json.path("referenceUrls").get(0).asText()).isEqualTo("https://linkareer.com/activity/345218");
    }

    @Test
    @DisplayName("새 식별 키가 없는 기존 공고도 원문 식별값으로 대체 출처 중복을 거절한다")
    void checksLegacyIdentifiers() throws Exception {
        var request = draft(); request.setSourceIdentifier("264146"); service.createAnnouncement(request);
        jdbc.update("update announcement set source_identity_key = null");
        var alias = draft(); alias.setSourceIdentifier("264146"); alias.setAccessUrl("https://mirror.example.com/345218");
        assertThatThrownBy(() -> service.createAnnouncement(alias)).isInstanceOf(com.inhatc.demp.error.ApiException.class);
        assertThat(repository.count()).isEqualTo(1);
    }

    private AnnouncementCreateRequest draft() {
        var request = new AnnouncementCreateRequest(); request.setTitle("복원 중 공고");
        request.setCompany("하나금융융합기술원"); request.setType(AnnouncementType.EMP);
        request.setContent("기술과 일정 확인 중"); request.setAccessUrl("https://example.com/264146");
        return request;
    }
}
