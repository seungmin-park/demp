package com.inhatc.demp.repository;

import com.inhatc.demp.domain.announcement.Announcement;
import com.inhatc.demp.domain.announcement.AnnouncementType;
import com.inhatc.demp.domain.announcement.Career;
import com.inhatc.demp.domain.announcement.Company;
import com.inhatc.demp.domain.announcement.Description;
import com.inhatc.demp.domain.announcement.JobPosition;
import com.inhatc.demp.domain.announcement.Language;
import com.inhatc.demp.domain.announcement.RecruitPeriod;
import com.inhatc.demp.domain.announcement.UploadFile;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.inhatc.demp.repository.announcement.AnnouncementQueryRepository;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import com.inhatc.demp.support.SqlCaptureInspector;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import static com.inhatc.demp.domain.announcement.Language.React;
import static com.inhatc.demp.domain.announcement.Language.SPRING;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest(properties = {
        "spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch=true",
        "spring.jpa.properties.hibernate.session_factory.statement_inspector=com.inhatc.demp.support.SqlCaptureInspector"
})
@Import(AnnouncementQueryRepository.class)
class AnnouncementQueryRepositoryTest {

    @Autowired
    private AnnouncementRepository announcementRepository;
    @Autowired
    private AnnouncementQueryRepository announcementQueryRepository;

    @ParameterizedTest
    @DisplayName("공고 유형 조건에 맞는 공고를 조회한다")
    @MethodSource("typeTestCases")
    void findsByType(AnnouncementType type, List<String> expectedTitles) {
        saveAnnouncement("employment", AnnouncementType.EMP, JobPosition.FRONTEND, Set.of(Language.JAVA), 0);
        saveAnnouncement("education", AnnouncementType.EDU, JobPosition.FRONTEND, Set.of(Language.JAVA), 0);
        AnnouncementSearchCondition condition = new AnnouncementSearchCondition();
        condition.setAnnouncementType(type);

        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(condition);

        assertThat(result).extracting(Announcement::getTitle)
                .containsExactlyInAnyOrderElementsOf(expectedTitles);
    }

    static Stream<Arguments> typeTestCases() {
        return Stream.of(
                Arguments.of(AnnouncementType.EMP, List.of("employment")),
                Arguments.of(AnnouncementType.EDU, List.of("education")),
                Arguments.of(null, List.of("employment", "education")));
    }

    @ParameterizedTest
    @DisplayName("직무 조건에 맞는 공고를 조회한다")
    @MethodSource("positionTestCases")
    void findsByPosition(List<JobPosition> positions, List<String> expectedTitles) {
        saveAnnouncement("frontend", AnnouncementType.EMP, JobPosition.FRONTEND, Set.of(Language.JAVA), 0);
        saveAnnouncement("backend", AnnouncementType.EMP, JobPosition.BACKEND, Set.of(Language.JAVA), 0);
        AnnouncementSearchCondition condition = new AnnouncementSearchCondition();
        condition.getPositions().addAll(positions);

        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(condition);

        assertThat(result).extracting(Announcement::getTitle)
                .containsExactlyInAnyOrderElementsOf(expectedTitles);
    }

    static Stream<Arguments> positionTestCases() {
        return Stream.of(
                Arguments.of(List.of(JobPosition.FRONTEND), List.of("frontend")),
                Arguments.of(List.of(JobPosition.BACKEND), List.of("backend")),
                Arguments.of(List.of(JobPosition.FRONTEND, JobPosition.BACKEND), List.of("frontend", "backend")),
                Arguments.of(List.of(), List.of("frontend", "backend")));
    }

    @ParameterizedTest
    @DisplayName("제목 검색어를 포함하는 공고를 조회한다")
    @MethodSource("titleTestCases")
    void findsByTitle(String title, List<String> expectedTitles) {
        saveAnnouncement("java job", AnnouncementType.EMP, JobPosition.BACKEND, Set.of(Language.JAVA), 0);
        saveAnnouncement("spring job", AnnouncementType.EMP, JobPosition.BACKEND, Set.of(Language.JAVA), 0);
        AnnouncementSearchCondition condition = new AnnouncementSearchCondition();
        condition.setTitle(title);

        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(condition);

        assertThat(result).extracting(Announcement::getTitle)
                .containsExactlyInAnyOrderElementsOf(expectedTitles);
    }

    static Stream<Arguments> titleTestCases() {
        return Stream.of(
                Arguments.of("java", List.of("java job")),
                Arguments.of("job", List.of("java job", "spring job")),
                Arguments.of("missing", List.of()),
                Arguments.of(null, List.of("java job", "spring job")),
                Arguments.of("", List.of("java job", "spring job")),
                Arguments.of("  ", List.of("java job", "spring job")));
    }

    @ParameterizedTest
    @DisplayName("기술 조건에 맞는 공고를 조회한다")
    @MethodSource("languageTestCases")
    void findsByLanguage(Language language, List<String> expectedTitles) {
        saveAnnouncement("java", AnnouncementType.EMP, JobPosition.BACKEND, Set.of(Language.JAVA), 0);
        saveAnnouncement("spring", AnnouncementType.EMP, JobPosition.BACKEND, Set.of(Language.JAVA, SPRING), 0);
        AnnouncementSearchCondition condition = new AnnouncementSearchCondition();
        condition.setLanguage(language);

        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(condition);

        assertThat(result).extracting(Announcement::getTitle)
                .containsExactlyInAnyOrderElementsOf(expectedTitles);
    }

    static Stream<Arguments> languageTestCases() {
        return Stream.of(
                Arguments.of(Language.JAVA, List.of("java", "spring")),
                Arguments.of(SPRING, List.of("spring")),
                Arguments.of(React, List.of()),
                Arguments.of(null, List.of("java", "spring")));
    }

    @ParameterizedTest
    @DisplayName("최소 급여 조건을 만족하는 공고를 조회한다")
    @MethodSource("paymentTestCases")
    void findsByPayment(int payment, List<String> expectedTitles) {
        saveAnnouncement("below", AnnouncementType.EMP, JobPosition.BACKEND, Set.of(Language.JAVA), 4999);
        saveAnnouncement("equal", AnnouncementType.EMP, JobPosition.BACKEND, Set.of(Language.JAVA), 5000);
        saveAnnouncement("above", AnnouncementType.EMP, JobPosition.BACKEND, Set.of(Language.JAVA), 5001);
        AnnouncementSearchCondition condition = new AnnouncementSearchCondition();
        condition.setPayment(payment);

        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(condition);

        assertThat(result).extracting(Announcement::getTitle)
                .containsExactlyInAnyOrderElementsOf(expectedTitles);
    }

    static Stream<Arguments> paymentTestCases() {
        return Stream.of(
                Arguments.of(5000, List.of("equal", "above")),
                Arguments.of(5002, List.of()),
                Arguments.of(0, List.of("below", "equal", "above")),
                Arguments.of(-1, List.of("below", "equal", "above")));
    }

    @ParameterizedTest
    @DisplayName("유형과 직무 조건을 함께 만족하는 공고를 조회한다")
    @MethodSource("typeAndPositionTestCases")
    void findsByTypeAndPosition(AnnouncementType type, JobPosition position, List<String> expectedTitles) {
        saveAnnouncement("employment-frontend", AnnouncementType.EMP, JobPosition.FRONTEND, Set.of(Language.JAVA), 0);
        saveAnnouncement("employment-backend", AnnouncementType.EMP, JobPosition.BACKEND, Set.of(Language.JAVA), 0);
        saveAnnouncement("education-backend", AnnouncementType.EDU, JobPosition.BACKEND, Set.of(Language.JAVA), 0);
        AnnouncementSearchCondition condition = new AnnouncementSearchCondition();
        condition.setAnnouncementType(type);
        condition.getPositions().add(position);

        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(condition);

        assertThat(result).extracting(Announcement::getTitle)
                .containsExactlyInAnyOrderElementsOf(expectedTitles);
    }

    static Stream<Arguments> typeAndPositionTestCases() {
        return Stream.of(
                Arguments.of(AnnouncementType.EMP, JobPosition.FRONTEND, List.of("employment-frontend")),
                Arguments.of(AnnouncementType.EMP, JobPosition.BACKEND, List.of("employment-backend")),
                Arguments.of(AnnouncementType.EDU, JobPosition.BACKEND, List.of("education-backend")),
                Arguments.of(AnnouncementType.EDU, JobPosition.FRONTEND, List.of()));
    }

    @ParameterizedTest
    @DisplayName("공고 페이지의 실제 개수와 전체 개수를 검증한다")
    @MethodSource("paginationTestCases")
    void pageBoundaries(int total, int pageNumber, int expectedCount, boolean hasNext) {
        for (int i = 0; i < total; i++) {
            saveAnnouncement("job-" + i, AnnouncementType.EMP, JobPosition.BACKEND,
                    Set.of(Language.JAVA, SPRING), 0);
        }

        Page<Announcement> result = announcementQueryRepository.findAnnouncementPage(
                new AnnouncementSearchCondition(), PageRequest.of(pageNumber, 3));

        assertThat(result.getContent()).hasSize(expectedCount);
        assertThat(result.hasNext()).isEqualTo(hasNext);
        assertThat(result.getNumber()).isEqualTo(pageNumber);
        assertThat(result.getTotalElements()).isEqualTo(total);
        assertThat(result.getContent()).extracting(Announcement::getId).doesNotHaveDuplicates();
    }

    @ParameterizedTest
    @DisplayName("공고 슬라이스의 실제 개수와 다음 페이지 여부를 검증한다")
    @MethodSource("paginationTestCases")
    void sliceBoundaries(int total, int pageNumber, int expectedCount, boolean hasNext) {
        for (int i = 0; i < total; i++) {
            saveAnnouncement("job-" + i, AnnouncementType.EMP, JobPosition.BACKEND,
                    Set.of(Language.JAVA, SPRING), 0);
        }

        Slice<Announcement> result = announcementQueryRepository.findAnnouncementSlice(
                new AnnouncementSearchCondition(), PageRequest.of(pageNumber, 3));

        assertThat(result.getContent()).hasSize(expectedCount);
        assertThat(result.hasNext()).isEqualTo(hasNext);
        assertThat(result.getNumber()).isEqualTo(pageNumber);
        assertThat(result.getContent()).extracting(Announcement::getId).doesNotHaveDuplicates();
    }

    static Stream<Arguments> paginationTestCases() {
        return Stream.of(
                Arguments.of(0, 0, 0, false),
                Arguments.of(2, 0, 2, false),
                Arguments.of(3, 0, 3, false),
                Arguments.of(4, 0, 3, true),
                Arguments.of(4, 1, 1, false),
                Arguments.of(4, 2, 0, false));
    }


    @Test
    @DisplayName("페이지 조회에도 제목 조건을 적용한다")
    void filtersPageContent() {
        Announcement matching = saveAnnouncement("matching", AnnouncementType.EMP, JobPosition.BACKEND,
                Set.of(Language.JAVA, SPRING), 0);
        saveAnnouncement("other", AnnouncementType.EMP, JobPosition.BACKEND, Set.of(Language.JAVA), 0);
        AnnouncementSearchCondition condition = new AnnouncementSearchCondition();
        condition.setTitle("matching");

        Page<Announcement> result = announcementQueryRepository.findAnnouncementPage(condition, PageRequest.of(0, 3));

        assertThat(result.getContent()).extracting(Announcement::getId).containsExactly(matching.getId());
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("슬라이스 조회에도 제목 조건을 적용한다")
    void filtersSliceContent() {
        Announcement matching = saveAnnouncement("matching", AnnouncementType.EMP, JobPosition.BACKEND,
                Set.of(Language.JAVA, SPRING), 0);
        saveAnnouncement("other", AnnouncementType.EMP, JobPosition.BACKEND, Set.of(Language.JAVA), 0);
        AnnouncementSearchCondition condition = new AnnouncementSearchCondition();
        condition.setTitle("matching");

        Slice<Announcement> result = announcementQueryRepository.findAnnouncementSlice(condition, PageRequest.of(0, 3));

        assertThat(result.getContent()).extracting(Announcement::getId).containsExactly(matching.getId());
        assertThat(result.hasNext()).isFalse();
    }

    @Test
    @DisplayName("복수 언어 공고의 슬라이스는 ID 내림차순으로 겹치지 않고 다음 페이지를 찾는다")
    void slicesMultipleLanguagesWithoutOverlappingPages() {
        for (int i = 0; i < 5; i++) {
            saveAnnouncement("ordered-" + i, AnnouncementType.EMP, JobPosition.BACKEND,
                    Set.of(Language.JAVA, SPRING), 0);
        }
        AnnouncementSearchCondition condition = new AnnouncementSearchCondition();

        SqlCaptureInspector.clear();
        Slice<Announcement> first = announcementQueryRepository.findAnnouncementSlice(condition, PageRequest.of(0, 2));
        Slice<Announcement> second = announcementQueryRepository.findAnnouncementSlice(condition, PageRequest.of(1, 2));
        List<String> paginationSql = SqlCaptureInspector.statements();
        List<Long> ids = announcementRepository.findAll().stream().map(Announcement::getId)
                .sorted(java.util.Comparator.reverseOrder()).collect(java.util.stream.Collectors.toList());

        assertThat(first.getContent()).extracting(Announcement::getId)
                .containsExactly(ids.get(0), ids.get(1));
        assertThat(second.getContent()).extracting(Announcement::getId)
                .containsExactly(ids.get(2), ids.get(3));
        assertThat(first.hasNext()).isTrue();
        assertThat(second.hasNext()).isTrue();
        assertThat(paginationSql).anySatisfy(sql -> assertThat(sql.toLowerCase()).contains("fetch first ? rows only"));
        assertThat(paginationSql).noneMatch(sql -> sql.toLowerCase().contains("count("));
    }

    @Test
    @DisplayName("공고 슬라이스의 다음 페이지도 제목·직군·언어 조건을 유지한다")
    void keepsFiltersAcrossAnnouncementPages() {
        Announcement first = saveAnnouncement("Java backend one", AnnouncementType.EMP, JobPosition.BACKEND,
                Set.of(Language.JAVA, SPRING), 0);
        Announcement second = saveAnnouncement("Java backend two", AnnouncementType.EMP, JobPosition.BACKEND,
                Set.of(Language.JAVA, SPRING), 0);
        Announcement third = saveAnnouncement("Java backend three", AnnouncementType.EMP, JobPosition.BACKEND,
                Set.of(Language.JAVA, SPRING), 0);
        saveAnnouncement("Java frontend", AnnouncementType.EMP, JobPosition.FRONTEND,
                Set.of(Language.JAVA, SPRING), 0);
        saveAnnouncement("Kotlin backend", AnnouncementType.EMP, JobPosition.BACKEND,
                Set.of(Language.JAVA, SPRING), 0);
        AnnouncementSearchCondition condition = new AnnouncementSearchCondition();
        condition.setTitle("Java");
        condition.getPositions().add(JobPosition.BACKEND);
        condition.setLanguage(SPRING);

        Slice<Announcement> page0 = announcementQueryRepository.findAnnouncementSlice(condition, PageRequest.of(0, 2));
        Slice<Announcement> page1 = announcementQueryRepository.findAnnouncementSlice(condition, PageRequest.of(1, 2));

        assertThat(page0.getContent()).extracting(Announcement::getId)
                .containsExactly(third.getId(), second.getId());
        assertThat(page1.getContent()).extracting(Announcement::getId)
                .containsExactly(first.getId());
        assertThat(page0.hasNext()).isTrue();
        assertThat(page1.isLast()).isTrue();
    }

    @org.springframework.test.context.bean.override.mockito.MockitoBean
    private java.time.Clock clock;

    @Test
    @DisplayName("기술 다중 선택은 OR이며 회사 검색·직무와 AND로 조합하고 페이지가 중복되지 않는다")
    void combinesLanguagesCompanyAndPosition() {
        Announcement first = saveAnnouncement("first", AnnouncementType.EMP, JobPosition.BACKEND, Set.of(Language.JAVA, SPRING), 4000);
        Announcement second = saveAnnouncement("second", AnnouncementType.EMP, JobPosition.BACKEND, Set.of(SPRING), 4000);
        saveAnnouncement("other", AnnouncementType.EMP, JobPosition.FRONTEND, Set.of(Language.JAVA), 4000);
        saveAnnouncement("css", AnnouncementType.EMP, JobPosition.BACKEND, Set.of(Language.CSS), 4000);
        AnnouncementSearchCondition condition = new AnnouncementSearchCondition();
        condition.setTitle("COMPANY");
        condition.setPositions(List.of(JobPosition.BACKEND));
        condition.setLanguages(List.of(Language.JAVA, SPRING));

        Slice<Announcement> page0 = announcementQueryRepository.findAnnouncementSlice(condition, PageRequest.of(0, 1));
        Slice<Announcement> page1 = announcementQueryRepository.findAnnouncementSlice(condition, PageRequest.of(1, 1));

        assertThat(page0.getContent()).extracting(Announcement::getId).containsExactly(second.getId());
        assertThat(page0.hasNext()).isTrue();
        assertThat(page1.getContent()).extracting(Announcement::getId).containsExactly(first.getId());
        assertThat(page1.hasNext()).isFalse();
    }

    @ParameterizedTest
    @DisplayName("교육 비용 조건은 무료·유료를 구분하고 채용 급여와 섞이지 않는다")
    @MethodSource("tuitionCases")
    void filtersTuition(com.inhatc.demp.dto.announcement.Tuition tuition, String title) {
        saveAnnouncement("free", AnnouncementType.EDU, JobPosition.BACKEND, Set.of(Language.JAVA), 0);
        saveAnnouncement("paid", AnnouncementType.EDU, JobPosition.BACKEND, Set.of(Language.JAVA), 100);
        saveAnnouncement("employment", AnnouncementType.EMP, JobPosition.BACKEND, Set.of(Language.JAVA), 0);
        AnnouncementSearchCondition condition = new AnnouncementSearchCondition();
        condition.setTuition(tuition);

        Slice<Announcement> result = announcementQueryRepository.findAnnouncementSlice(condition, PageRequest.of(0, 10));
        assertThat(result.getContent()).extracting(Announcement::getTitle).containsExactly(title);
        assertThat(result.hasNext()).isFalse();
    }

    static Stream<Arguments> tuitionCases() {
        return Stream.of(Arguments.of(com.inhatc.demp.dto.announcement.Tuition.FREE, "free"),
                Arguments.of(com.inhatc.demp.dto.announcement.Tuition.PAID, "paid"));
    }

    @ParameterizedTest
    @DisplayName("모집 상태는 고정 시각과 시작·마감 경계를 포함해 구분한다")
    @MethodSource("recruitmentCases")
    void filtersRecruitment(com.inhatc.demp.dto.announcement.RecruitmentStatus status, List<String> titles) {
        java.time.Clock fixed = java.time.Clock.fixed(java.time.Instant.parse("2026-09-27T00:00:00Z"), java.time.ZoneId.of("Asia/Seoul"));
        org.mockito.Mockito.when(clock.instant()).thenReturn(fixed.instant());
        org.mockito.Mockito.when(clock.getZone()).thenReturn(fixed.getZone());
        LocalDateTime now = LocalDateTime.of(2026, 9, 27, 9, 0);
        saveAtPeriod("starting", now, now.plusDays(1));
        saveAtPeriod("ending", now.minusDays(1), now);
        saveAtPeriod("closed", now.minusDays(1), now.minusSeconds(1));
        saveAtPeriod("upcoming", now.plusSeconds(1), now.plusDays(1));
        AnnouncementSearchCondition condition = new AnnouncementSearchCondition();
        condition.setRecruitmentStatus(status);

        Slice<Announcement> result = announcementQueryRepository.findAnnouncementSlice(condition, PageRequest.of(0, 10));
        assertThat(result.getContent()).extracting(Announcement::getTitle).containsExactlyElementsOf(titles);
        assertThat(result.hasNext()).isFalse();
    }

    static Stream<Arguments> recruitmentCases() {
        return Stream.of(Arguments.of(com.inhatc.demp.dto.announcement.RecruitmentStatus.OPEN, List.of("ending", "starting")),
                Arguments.of(com.inhatc.demp.dto.announcement.RecruitmentStatus.CLOSED, List.of("closed")),
                Arguments.of(com.inhatc.demp.dto.announcement.RecruitmentStatus.UPCOMING, List.of("upcoming")));
    }

    private void saveAtPeriod(String title, LocalDateTime start, LocalDateTime end) {
        announcementRepository.save(Announcement.builder().title(title).career(Career.builder().minCareer(0).maxCareer(3).build())
                .description(Description.builder()
                        .content("body")
                        .accessUrl("https://example.test")
                        .payment(0)
                        .languages(Set.of(Language.JAVA))
                        .build())
                .company(Company.builder().name("company").build()).image(UploadFile.builder().build())
                .recruitPeriod(RecruitPeriod.builder().startedDate(start).deadLineDate(end).build()).announcementType(AnnouncementType.EMP)
                .jobPosition(JobPosition.BACKEND).build());
    }

    private Announcement saveAnnouncement(String title, AnnouncementType type, JobPosition position,
                                          Set<Language> languages, int payment) {
        return announcementRepository.save(Announcement.builder()
                .title(title).career(Career.builder().minCareer(0).maxCareer(3).build())
                .description(Description.builder()
                        .content("description")
                        .accessUrl("https://example.test/jobs")
                        .payment(payment)
                        .languages(new HashSet<>(languages))
                        .build())
                .company(Company.builder().name("company").build()).image(UploadFile.builder().build())
                .recruitPeriod(RecruitPeriod.builder()
                        .startedDate(LocalDateTime.of(2021, 3, 4, 0, 0))
                        .deadLineDate(LocalDateTime.of(2021, 3, 21, 0, 0))
                        .build())
                .announcementType(type).jobPosition(position).build());
    }
}
