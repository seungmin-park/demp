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
class EducationDiscoveryTest {
    @Autowired AnnouncementService service;
    @Autowired AdminAnnouncementService admin;
    @Autowired AnnouncementRepository repository;
    @Autowired AnnouncementQueryRepository queries;
    @AfterEach void cleanup() { repository.deleteAll(); }

    @Test
    @DisplayName("교육 상세 조건의 교집합은 목록과 페이지 건수에 동일하게 적용된다")
    void filtersPersistedEducation() throws Exception {
        var match = create("일치", DeliveryMode.ONLINE);
        create("오프라인", DeliveryMode.OFFLINE);
        var unknown = request("미확인"); service.createAnnouncement(unknown);
        var filters = filters();
        var slice = queries.findAnnouncementSlice(filters, PageRequest.of(0, 8));
        assertThat(slice.getContent()).extracting(Announcement::getId).containsExactly(match);
        assertThat(slice.hasNext()).isFalse();
        var page = queries.findAnnouncementPage(filters, PageRequest.of(0, 8));
        assertThat(page.getContent()).extracting(Announcement::getId).containsExactly(match);
        assertThat(page.getTotalElements()).isEqualTo(1);
        assertThat(queries.findAllByAnnouncementCondition(filters)).extracting(Announcement::getId).containsExactly(match);
    }

    @Test
    @DisplayName("각 교육 조건은 미확인 데이터를 제외하고 기간 경계와 종료 페이지를 지킨다")
    void eachFilterAndDateBoundary() throws Exception {
        var match = create("확인된 과정", DeliveryMode.ONLINE);
        service.createAnnouncement(request("정보 없음"));
        var choices = List.<java.util.function.Consumer<AnnouncementSearchCondition>>of(
            f -> f.setDeliveryMode(DeliveryMode.ONLINE), f -> f.setRegion(EducationRegion.SEOUL),
            f -> f.setCommitment(Commitment.PART_TIME), f -> f.setFundingType(FundingType.CARD_REQUIRED),
            f -> f.setSelectionProcess(SelectionProcess.NO_CODING), f -> f.setLearningLevel(LearningLevel.BEGINNER),
            f -> f.setDuration(EducationDuration.LONG), f -> f.setStartAfter(LocalDate.of(2026,10,1)),
            f -> f.setStartBefore(LocalDate.of(2026,10,1)));
        for (var choice : choices) {
            var filter = new AnnouncementSearchCondition(); choice.accept(filter);
            assertThat(queries.findAnnouncementSlice(filter, PageRequest.of(0,1)).getContent())
                    .extracting(Announcement::getId).containsExactly(match);
            assertThat(queries.findAnnouncementSlice(filter, PageRequest.of(0,1)).hasNext()).isFalse();
        }
        var filter = filters(); filter.setStartAfter(LocalDate.of(2026,10,2));
        assertThat(queries.findAnnouncementPage(filter, PageRequest.of(0,8)).getTotalElements()).isZero();
        filter = filters(); filter.setDuration(EducationDuration.MEDIUM);
        assertThat(queries.findAnnouncementSlice(filter, PageRequest.of(0,8)).getContent()).isEmpty();
    }

    @Test
    @DisplayName("교육 정보 수정은 재조회에 반영되고 채용 전환 시 교육 조건이 남지 않는다")
    void updatesAndClearsEducation() throws Exception {
        var id = create("수정 대상", DeliveryMode.ONLINE);
        var update = new AnnouncementUpdateRequest(); fill(update, "수정 대상");
        update.setDeliveryMode(DeliveryMode.HYBRID); update.setFundingType(FundingType.SELF_FUNDED);
        admin.update(id, update);
        var detail = service.findDetailResponse(id).orElseThrow();
        assertThat(detail.getEducation().getDeliveryMode()).isEqualTo(DeliveryMode.HYBRID);
        assertThat(detail.getEducation().getFundingType()).isEqualTo(FundingType.SELF_FUNDED);
        update.setType(AnnouncementType.EMP); admin.update(id, update);
        assertThat(service.findDetailResponse(id).orElseThrow().getEducation()).isNull();
    }

    @Test
    @DisplayName("채용을 교육으로 전환하면 입력한 교육 정보를 처음부터 보존한다")
    void convertsEmploymentToEducation() throws Exception {
        var request = request("채용에서 교육"); request.setType(AnnouncementType.EMP); service.createAnnouncement(request);
        var id = repository.findByTitle(request.getTitle()).orElseThrow().getId();
        var update = new AnnouncementUpdateRequest(); fill(update, request.getTitle()); update.setDeliveryMode(DeliveryMode.ONLINE);
        admin.update(id, update);
        assertThat(service.findDetailResponse(id).orElseThrow().getEducation()).isNotNull();
        assertThat(service.findDetailResponse(id).orElseThrow().getEducation().getDeliveryMode()).isEqualTo(DeliveryMode.ONLINE);
    }

    private Long create(String title, DeliveryMode mode) throws Exception {
        var request = request(title); request.setDeliveryMode(mode); request.setRegion(EducationRegion.SEOUL);
        request.setCommitment(Commitment.PART_TIME); request.setFundingType(FundingType.CARD_REQUIRED);
        request.setSelectionProcess(SelectionProcess.NO_CODING); request.setLearningLevel(LearningLevel.BEGINNER);
        request.setLearningStartDate(LocalDate.of(2026,10,1)); request.setLearningEndDate(LocalDate.of(2026,12,31));
        service.createAnnouncement(request); return repository.findByTitle(title).orElseThrow().getId();
    }
    private AnnouncementSearchCondition filters() {
        var f = new AnnouncementSearchCondition(); f.setAnnouncementType(AnnouncementType.EDU);
        f.setDeliveryMode(DeliveryMode.ONLINE); f.setRegion(EducationRegion.SEOUL); f.setCommitment(Commitment.PART_TIME);
        f.setFundingType(FundingType.CARD_REQUIRED); f.setSelectionProcess(SelectionProcess.NO_CODING);
        f.setLearningLevel(LearningLevel.BEGINNER); f.setDuration(EducationDuration.LONG);
        f.setStartAfter(LocalDate.of(2026,10,1)); f.setStartBefore(LocalDate.of(2026,10,1)); return f;
    }
    private AnnouncementCreateRequest request(String title) { var request = new AnnouncementCreateRequest(); fill(request,title); return request; }
    private void fill(AnnouncementFields request, String title) {
        request.setPublicationStatus(PublicationStatus.PUBLISHED); request.setTitle(title); request.setCompany("DEMP 교육"); request.setType(AnnouncementType.EDU);
        request.setPosition(JobPosition.BACKEND); request.setLanguage(Set.of(Language.JAVA)); request.setContent("교육 소개");
        request.setAccessUrl("https://example.com/camp"); request.setStartedDate(LocalDateTime.of(2026,9,1,0,0));
        request.setDeadLineDate(LocalDateTime.of(2026,9,30,18,0));
    }
}
