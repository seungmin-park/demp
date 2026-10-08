package com.inhatc.demp.repository;

import com.inhatc.demp.domain.announcement.*;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.inhatc.demp.repository.announcement.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.PageRequest;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.when;

@DataJpaTest(properties = "spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true")
@Import(AnnouncementQueryRepository.class)
class AnnouncementOrderingTest {
    @MockitoBean Clock recruitmentClock;
    @BeforeEach
    void fixOrderingTime() {
        when(recruitmentClock.instant()).thenReturn(Instant.parse("2026-10-08T03:00:00Z"));
        when(recruitmentClock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
    }
    @Autowired AnnouncementRepository repository;
    @Autowired AnnouncementQueryRepository queries;

    @Test
    @DisplayName("기본 최신순은 게시 공고만 ID 내림차순이며 페이지 사이 중복이 없다")
    void latestHasStablePages() {
        save("첫 등록", null, false); save("두 번째", null, false); save("세 번째", null, false);
        var hidden = save("비공개", null, false); hidden.changePublication(PublicationStatus.DRAFT);
        var filter = new AnnouncementSearchCondition();
        var first = queries.findAnnouncementSlice(filter, PageRequest.of(0,2));
        var last = queries.findAnnouncementSlice(filter, PageRequest.of(1,2));
        assertThat(first.getContent()).extracting(Announcement::getTitle).containsExactly("세 번째", "두 번째");
        assertThat(first.hasNext()).isTrue();
        assertThat(last.getContent()).extracting(Announcement::getTitle).containsExactly("첫 등록");
        assertThat(last.hasNext()).isFalse();
    }

    @Test
    @DisplayName("마감순은 유효한 마감 임박과 동률 최신을 먼저 두고 만료·수동 마감·미확인은 뒤에 둔다")
    void deadlinePrioritizesAvailableDates() {
        save("임박 기존", LocalDateTime.of(2026,10,8,12,0), false);
        save("여유", LocalDateTime.of(2026,10,20,18,0), false);
        save("만료", LocalDateTime.of(2026,10,8,11,59), false);
        save("임박 최신", LocalDateTime.of(2026,10,8,12,0), false);
        save("수동 마감", LocalDateTime.of(2026,10,9,18,0), true);
        save("마감 미확인", null, false);
        var filter = ordered("DEADLINE");
        assertThat(queries.findAnnouncementSlice(filter, PageRequest.of(0,2)).getContent())
                .extracting(Announcement::getTitle).containsExactly("임박 최신", "임박 기존");
        var middle = queries.findAnnouncementSlice(filter, PageRequest.of(1,2));
        assertThat(middle.getContent()).extracting(Announcement::getTitle).containsExactly("여유", "마감 미확인");
        assertThat(middle.hasNext()).isTrue();
        var last = queries.findAnnouncementSlice(filter, PageRequest.of(2,2));
        var all = queries.findAnnouncementPage(filter, PageRequest.of(0,8));
        assertThat(all.getTotalElements()).isEqualTo(6);
        assertThat(all.getContent()).extracting(Announcement::getTitle)
                .containsExactly("임박 최신", "임박 기존", "여유", "마감 미확인", "수동 마감", "만료");
        assertThat(last.getContent()).extracting(Announcement::getTitle).containsExactly("수동 마감", "만료");
        assertThat(last.hasNext()).isFalse();
    }

    @Test
    @DisplayName("조회순은 저장된 조회수와 최신 동률 기준을 적용하고 직무·기술 필터를 유지한다")
    void viewsRespectFiltersAndTieBreaker() {
        var popular = save("조회 3", null, false); var older = save("조회 1 기존", null, false);
        var newer = save("조회 1 최신", null, false); save("조회 0", null, false);
        var wrongRole = Announcement.builder().title("다른 직무").jobPosition(JobPosition.FRONTEND).announcementType(AnnouncementType.EMP)
                .description(Description.builder().languages(Set.of(Language.React)).build()).build();
        wrongRole = repository.save(wrongRole);
        for (int i = 0; i < 3; i++) repository.recordPublishedView(popular.getId());
        repository.recordPublishedView(older.getId()); repository.recordPublishedView(newer.getId());
        for (int i = 0; i < 4; i++) repository.recordPublishedView(wrongRole.getId());
        var filter = ordered("VIEWS"); filter.getPositions().add(JobPosition.BACKEND);
        filter.getLanguages().addAll(List.of(Language.JAVA, Language.KOTLIN));
        var first = queries.findAnnouncementSlice(filter, PageRequest.of(0,2));
        var last = queries.findAnnouncementSlice(filter, PageRequest.of(1,2));
        assertThat(first.getContent()).extracting(Announcement::getTitle).containsExactly("조회 3", "조회 1 최신");
        assertThat(first.hasNext()).isTrue();
        assertThat(last.getContent()).extracting(Announcement::getTitle).containsExactly("조회 1 기존", "조회 0");
        assertThat(last.hasNext()).isFalse();
    }

    private AnnouncementSearchCondition ordered(String value) {
        var filter = new AnnouncementSearchCondition(); var fields = new BeanWrapperImpl(filter);
        assertThat(fields.isWritableProperty("orderBy")).as("정렬 조건 소유자").isTrue();
        fields.setPropertyValue("orderBy", value); return filter;
    }
    private Announcement save(String title, LocalDateTime deadline, boolean closed) {
        var item = Announcement.builder().title(title).announcementType(AnnouncementType.EMP).jobPosition(JobPosition.BACKEND)
                .company(Company.builder().name("DEMP").build()).career(Career.builder().minCareer(0).maxCareer(0).build())
                .description(Description.builder().languages(Set.of(Language.JAVA, Language.KOTLIN)).content("본문").build())
                // Legacy rows may have no period; new writes retain RecruitPeriod's required-date invariant.
                .recruitPeriod(deadline == null ? null : RecruitPeriod.builder().startedDate(LocalDateTime.of(2026,10,1,0,0)).deadLineDate(deadline).build()).build();
        item.changeRecruitmentClosed(closed); return repository.save(item);
    }
}
