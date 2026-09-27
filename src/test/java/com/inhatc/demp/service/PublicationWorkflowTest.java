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

    @Test
    @DisplayName("제목이 같아도 다른 기관이나 기수이면 등록하고 같은 원문 기관 기수는 거절한다")
    void duplicatesUseSourceInstitutionAndCohort() throws Exception {
        var first = new AnnouncementCreateRequest(); fill(first); first.setType(AnnouncementType.EDU); first.setCohort("1기"); service.createAnnouncement(first);
        var other = new AnnouncementCreateRequest(); fill(other); other.setCompany("다른 기관"); service.createAnnouncement(other);
        var next = new AnnouncementCreateRequest(); fill(next); next.setType(AnnouncementType.EDU); next.setCohort("2기"); service.createAnnouncement(next);
        var duplicate = new AnnouncementCreateRequest(); fill(duplicate); duplicate.setTitle("다른 제목"); duplicate.setType(AnnouncementType.EDU); duplicate.setCohort(" 1기 "); duplicate.setAccessUrl("https://EXAMPLE.com:443/jobs/1?utm_source=test#top");
        assertThatThrownBy(() -> service.createAnnouncement(duplicate)).isInstanceOfSatisfying(com.inhatc.demp.error.ApiException.class, e -> assertThat(e.getStatus()).isEqualTo(org.springframework.http.HttpStatus.CONFLICT));
        assertThat(repository.count()).isEqualTo(3);
    }

    @Test
    @DisplayName("명시적 신입 구분과 교육 기수 지원금은 저장하고 공고 종류 전환 때 섞이지 않는다")
    void audienceAndEducationFunding() throws Exception {
        var request = new AnnouncementCreateRequest(); fill(request); request.setRecruitmentAudience(RecruitmentAudience.NEW);
        service.createAnnouncement(request); long id = repository.findByTitle(request.getTitle()).orElseThrow().getId();
        assertThat(service.findAdminDetailResponse(id).orElseThrow().getRecruitmentAudience()).isEqualTo(RecruitmentAudience.NEW);
        var update = new AnnouncementUpdateRequest(); fill(update); update.setType(AnnouncementType.EDU); update.setCohort("3기"); update.setStipendAmount(30); update.setStipendNote("월 최대, 출석 요건 충족 시");
        admin.update(id, update);
        var detail = service.findAdminDetailResponse(id).orElseThrow();
        assertThat(detail.getRecruitmentAudience()).isNull(); assertThat(detail.getCohort()).isEqualTo("3기");
        assertThat(detail.getStipendAmount()).isEqualTo(30); assertThat(detail.getPayment()).isNull();
        update.setType(AnnouncementType.EMP); admin.update(id, update);
        assertThat(service.findAdminDetailResponse(id).orElseThrow().getCohort()).isNull();
        assertThat(service.findAdminDetailResponse(id).orElseThrow().getStipendAmount()).isNull();
    }

    @Test
    @DisplayName("운영자 수동 마감은 공개 상태를 유지하면서 모집 중 검색에서 제외한다")
    void manualCloseIsSeparateFromPublication() throws Exception {
        var request = new AnnouncementCreateRequest(); fill(request); request.setPublicationStatus(PublicationStatus.PUBLISHED);
        request.setStartedDate(LocalDateTime.now().minusDays(1)); request.setDeadLineDate(LocalDateTime.now().plusDays(10));
        request.setRecruitmentClosed(true); service.createAnnouncement(request);
        long id = repository.findByTitle(request.getTitle()).orElseThrow().getId();
        var search = new AnnouncementSearchCondition(); search.setRecruitmentStatus(RecruitmentStatus.OPEN);
        assertThat(service.findAnnouncementSlice(search, PageRequest.of(0,8)).getContent()).isEmpty();
        search.setRecruitmentStatus(RecruitmentStatus.CLOSED);
        assertThat(service.findAnnouncementSlice(search, PageRequest.of(0,8)).getContent()).hasSize(1);
        assertThat(service.findDetailResponse(id)).isPresent();
    }

    private void fill(AnnouncementFields request) {
        request.setTitle("게시 상태 검증"); request.setCompany("DEMP"); request.setType(AnnouncementType.EMP);
        request.setPosition(JobPosition.BACKEND); request.setLanguage(Set.of(Language.JAVA));
        request.setContent("운영자가 확인한 내용"); request.setAccessUrl("https://example.com/jobs/1");
        request.setStartedDate(LocalDateTime.of(2026,9,1,0,0)); request.setDeadLineDate(LocalDateTime.of(2026,12,31,0,0));
    }
}
