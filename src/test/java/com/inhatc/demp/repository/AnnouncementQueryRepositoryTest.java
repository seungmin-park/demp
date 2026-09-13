package com.inhatc.demp.repository;

import com.inhatc.demp.domain.announcemnet.Announcement;
import com.inhatc.demp.domain.announcemnet.AnnouncementType;
import com.inhatc.demp.domain.announcemnet.Career;
import com.inhatc.demp.domain.announcemnet.Company;
import com.inhatc.demp.domain.announcemnet.Description;
import com.inhatc.demp.domain.announcemnet.JobPosition;
import com.inhatc.demp.domain.announcemnet.Language;
import com.inhatc.demp.domain.announcemnet.RecruitPeriod;
import com.inhatc.demp.domain.announcemnet.UploadFile;
import com.inhatc.demp.dto.announcement.AnnouncementResponse;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.inhatc.demp.repository.announcement.AnnouncementQueryRepository;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
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
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import static com.inhatc.demp.domain.announcemnet.Language.React;
import static com.inhatc.demp.domain.announcemnet.Language.SPRING;
import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
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

        Page<Announcement> result = announcementQueryRepository.pagingTest(
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

        Slice<AnnouncementResponse> result = announcementQueryRepository.getAnnounceScroll(
                new AnnouncementSearchCondition(), PageRequest.of(pageNumber, 3));

        assertThat(result.getContent()).hasSize(expectedCount);
        assertThat(result.hasNext()).isEqualTo(hasNext);
        assertThat(result.getNumber()).isEqualTo(pageNumber);
        assertThat(result.getContent()).extracting(AnnouncementResponse::getId).doesNotHaveDuplicates();
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

        Page<Announcement> result = announcementQueryRepository.pagingTest(condition, PageRequest.of(0, 3));

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

        Slice<AnnouncementResponse> result = announcementQueryRepository.getAnnounceScroll(condition, PageRequest.of(0, 3));

        assertThat(result.getContent()).extracting(AnnouncementResponse::getId).containsExactly(matching.getId());
        assertThat(result.hasNext()).isFalse();
    }

    private Announcement saveAnnouncement(String title, AnnouncementType type, JobPosition position,
                                          Set<Language> languages, int payment) {
        return announcementRepository.save(new Announcement(title, new Career(0, 3),
                new Description("description", "https://example.test/jobs", payment, new HashSet<>(languages)),
                new Company("company"), new UploadFile(),
                new RecruitPeriod(LocalDateTime.of(2021, 3, 4, 0, 0), LocalDateTime.of(2021, 3, 21, 0, 0)),
                type, position));
    }
}
