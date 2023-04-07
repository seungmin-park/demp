package com.inhatc.demp.repository;

import static com.inhatc.demp.domain.announcemnet.Language.React;
import static com.inhatc.demp.domain.announcemnet.Language.SPRING;
import static org.assertj.core.api.Assertions.assertThat;

import com.inhatc.demp.domain.announcemnet.Announcement;
import com.inhatc.demp.domain.announcemnet.AnnouncementType;
import com.inhatc.demp.domain.announcemnet.JobPosition;
import com.inhatc.demp.domain.announcemnet.Language;
import com.inhatc.demp.dto.announcement.AnnouncementResponse;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.inhatc.demp.repository.announcement.AnnouncementQueryRepository;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

@SpringBootTest
class AnnouncementQueryRepositoryTest {

    @Autowired
    AnnouncementQueryRepository announcementQueryRepository;

    @ParameterizedTest
    @MethodSource("generateTypeTestData")
    void test_announcement_type(AnnouncementType type, int announcementCnt, String... expected) {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.setAnnouncementType(type);

        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(
                announcementSearchCondition);

        //then
        assertThat(result.size()).isEqualTo(announcementCnt);
        assertThat(result).extracting("title").contains(expected);
    }

    static Stream<Arguments> generateTypeTestData() {
        return Stream.of(Arguments.of(AnnouncementType.EMP, 10, new String[]{"2021라인 공채1"}),
                Arguments.of(AnnouncementType.EDU, 10, new String[]{"2021 우아한 테크코스1"}),
                Arguments.of(null, 20, new String[]{"2021라인 공채1", "2021 우아한 테크코스1"}));
    }

    @ParameterizedTest
    @MethodSource("generatePositionTestData")
    void test_announcement_position(List<JobPosition> positions, int announcementCnt, String... expected) {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        for (JobPosition position : positions) {
            announcementSearchCondition.getPositions().add(position);
        }
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(
                announcementSearchCondition);

        //then
        assertThat(result.size()).isEqualTo(announcementCnt);
        assertThat(result).extracting("title").contains(expected);
    }

    static Stream<Arguments> generatePositionTestData() {
        return Stream.of(Arguments.of(List.of(JobPosition.FRONTEND), 10, new String[]{"2021라인 공채1"}),
                Arguments.of(List.of(JobPosition.BACKEND), 10, new String[]{"2021 우아한 테크코스1"}),
                Arguments.of(List.of(JobPosition.FRONTEND, JobPosition.BACKEND), 20,
                        new String[]{"2021라인 공채1", "2021 우아한 테크코스1"}),
                Arguments.of(List.of(), 20, new String[]{"2021라인 공채1", "2021 우아한 테크코스1"}));
    }

    @ParameterizedTest
    @MethodSource("generateTitleTestData")
    void test_announcement_title(String title, int announcementCnt, String... expected) {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.setTitle(title);
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(
                announcementSearchCondition);

        //then
        assertThat(result.size()).isEqualTo(announcementCnt);
        assertThat(result).extracting("title").contains(expected);
    }

    static Stream<Arguments> generateTitleTestData() {
        return Stream.of(Arguments.of("공채", 10, new String[]{"2021라인 공채1"}),
                Arguments.of("테크", 10, new String[]{"2021 우아한 테크코스1"}));
    }

    @ParameterizedTest
    @MethodSource("generateTypeAndPositionData")
    void test_announcement_type_and_position(AnnouncementType type, JobPosition position, int expectedCnt) {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.setAnnouncementType(type);
        announcementSearchCondition.getPositions().add(position);

        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(
                announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(expectedCnt);
    }

    static Stream<Arguments> generateTypeAndPositionData() {
        return Stream.of(Arguments.of(AnnouncementType.EMP, JobPosition.BACKEND, 0),
                Arguments.of(AnnouncementType.EMP, JobPosition.FRONTEND, 10),
                Arguments.of(AnnouncementType.EDU, JobPosition.FRONTEND, 0),
                Arguments.of(AnnouncementType.EDU, JobPosition.BACKEND, 10));
    }

    @ParameterizedTest
    @MethodSource("generateLanguageData")
    void test_announcement_language(Language language, int expectedCnt, String... expectedTitle) {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.setLanguage(language);
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(
                announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(expectedCnt);
        assertThat(result).extracting("title").contains(expectedTitle);
    }

    static Stream<Arguments> generateLanguageData() {
        return Stream.of(Arguments.of(React, 10, new String[]{"2021라인 공채1"}),
                Arguments.of(SPRING, 10, new String[]{"2021 우아한 테크코스1"}),
                Arguments.of(null, 20, new String[]{"2021라인 공채1", "2021 우아한 테크코스1"}));
    }

    @ParameterizedTest
    @MethodSource("generatePaymentData")
    void test_announcement_payment(int payment, int expectedCnt, String... expectedTitle) {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.setPayment(payment);
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(
                announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(expectedCnt);
        assertThat(result).extracting("title").contains(expectedTitle);
    }

    static Stream<Arguments> generatePaymentData() {
        return Stream.of(Arguments.of(0, 20, new String[]{"2021라인 공채1", "2021 우아한 테크코스1"}),
                Arguments.of(5000, 6, new String[]{"2021라인 공채6"}));
    }

    @Test
    @DisplayName("pagingTest")
    void pagingTest() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        PageRequest pageRequest = PageRequest.of(0, 3);
        //when
        Page<Announcement> result = announcementQueryRepository.pagingTest(announcementSearchCondition, pageRequest);

        //then
        assertThat(result.getSize()).isEqualTo(3);
        assertThat(result).extracting("title").contains("2021라인 공채1", "2021라인 공채2", "2021라인 공채3");
    }

    @Test
    @DisplayName("scrollTest")
    void scrollTest() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        PageRequest pageRequest = PageRequest.of(0, 3);

        //when
        Slice<AnnouncementResponse> announceScroll = announcementQueryRepository.getAnnounceScroll(
                announcementSearchCondition, pageRequest);
        //then
        assertThat(announceScroll.getSize()).isEqualTo(3);
        assertThat(announceScroll).extracting("title").contains("2021라인 공채1", "2021라인 공채2", "2021라인 공채3");
    }
}