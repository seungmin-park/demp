package com.inhatc.demp.service;

import com.inhatc.demp.domain.announcement.*;
import com.inhatc.demp.dto.announcement.*;
import com.inhatc.demp.repository.announcement.*;
import com.inhatc.demp.service.admin.AdminAnnouncementService;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class PublicationWorkflowTest {
    @Autowired AnnouncementService service;
    @Autowired AdminAnnouncementService admin;
    @Autowired AnnouncementRepository repository;
    @AfterEach void cleanup() { repository.deleteAll(); }

    @Test
    @DisplayName("새 공고는 초안으로 저장되고 공개 목록 상세 관련 공고에서 모두 제외된다")
    void draftsArePrivate() throws Exception {
        var request = new AnnouncementCreateRequest(); fill(request); service.createAnnouncement(request);
        var item = repository.findByTitle(request.getTitle()).orElseThrow();
        assertThat(service.findDetailResponse(item.getId())).isEmpty();
        assertThat(service.findAnnouncementSlice(new AnnouncementSearchCondition(), PageRequest.of(0,8)).getContent()).isEmpty();
        assertThat(service.findScrollResponses()).isEmpty();
    }
    @Test
    @DisplayName("검토 공개 비공개 전환과 관리자 전체 조회를 저장 후 재조회한다")
    void publicationTransitions() throws Exception {
        var request = new AnnouncementCreateRequest(); fill(request); service.createAnnouncement(request);
        long id = repository.findByTitle(request.getTitle()).orElseThrow().getId();
        var update = new AnnouncementUpdateRequest(); fill(update);
        for (var state : List.of(PublicationStatus.REVIEW, PublicationStatus.PUBLISHED, PublicationStatus.HIDDEN)) {
            update.setPublicationStatus(state); admin.update(id, update);
            assertThat(service.findAdminDetailResponse(id).orElseThrow().getPublicationStatus()).isEqualTo(state);
            assertThat(service.findAdminAnnouncementSlice(new AnnouncementSearchCondition(), PageRequest.of(0,8)).getContent()).hasSize(1);
            assertThat(service.findDetailResponse(id).isPresent()).isEqualTo(state == PublicationStatus.PUBLISHED);
            assertThat(service.findScrollResponses()).hasSize(state == PublicationStatus.PUBLISHED ? 1 : 0);
        }
    }

    @Test
    @DisplayName("출처 확인 및 별도 지원 주소와 인증된 운영자의 변경 이력을 저장한다")
    void sourceAndAuditArePersisted() throws Exception {
        var request = new AnnouncementCreateRequest(); fill(request);
        request.setSourceName("회사 채용 홈페이지"); request.setSourceIdentifier("job-17");
        request.setApplicationUrl("https://example.com/apply/17"); request.setSourceVerified(true);
        service.createAnnouncement(request, "operator-a");
        long id = repository.findByTitle(request.getTitle()).orElseThrow().getId();
        var saved = service.findAdminDetailResponse(id).orElseThrow();
        assertThat(saved.getSourceName()).isEqualTo("회사 채용 홈페이지");
        assertThat(saved.getApplicationUrl()).isEqualTo("https://example.com/apply/17");
        assertThat(saved.getSourceVerifiedAt()).isNotNull();
        assertThat(service.findPublicationHistory(id)).extracting(PublicationRevision::getActor).containsExactly("operator-a");
        var update = new AnnouncementUpdateRequest(); fill(update); update.setPublicationStatus(PublicationStatus.REVIEW);
        update.setAccessUrl("https://example.com/jobs/changed"); admin.update(id, update, "operator-b");
        assertThat(service.findAdminDetailResponse(id).orElseThrow().getSourceVerifiedAt()).isNull();
        assertThat(service.findPublicationHistory(id)).extracting(PublicationRevision::getActor).containsExactly("operator-a", "operator-b");
    }

    private void fill(AnnouncementFields request) {
        request.setTitle("게시 상태 검증"); request.setCompany("DEMP"); request.setType(AnnouncementType.EMP);
        request.setPosition(JobPosition.BACKEND); request.setLanguage(Set.of(Language.JAVA));
        request.setContent("운영자가 확인한 내용"); request.setAccessUrl("https://example.com/jobs/1");
        request.setStartedDate(LocalDateTime.of(2026,9,1,0,0)); request.setDeadLineDate(LocalDateTime.of(2026,12,31,0,0));
    }
}
