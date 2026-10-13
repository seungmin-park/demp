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










    private AnnouncementCreateRequest draft() {
        var request = new AnnouncementCreateRequest(); request.setTitle("복원 중 공고");
        request.setCompany("하나금융융합기술원"); request.setType(AnnouncementType.EMP);
        request.setContent("기술과 일정 확인 중"); request.setAccessUrl("https://example.com/264146");
        return request;
    }
}
