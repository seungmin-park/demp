package com.inhatc.demp.repository;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.inhatc.demp.repository.announcement.AnnouncementQueryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static com.inhatc.demp.domain.Language.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AnnouncementQueryRepositoryTest {

    @Autowired
    AnnouncementQueryRepository announcementQueryRepository;

    @Test
    @DisplayName("typeNameEmp")
    void typeNameEmp() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.setTypeName("emp");
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);

        //then
        assertThat(result.size()).isEqualTo(10);
        assertThat(result).extracting("title").contains("2021라인 공채1");
    }
    
    @Test
    @DisplayName("typeNameEdu")
    void typeNameEdu() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.setTypeName("edu");
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);

        //then
        assertThat(result.size()).isEqualTo(10);
        assertThat(result).extracting("title").contains("2021 우아한 테크코스1");

    }
    
    @Test
    @DisplayName("typeNameNull")
    void typeNameNull() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();

        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);

        //then
        assertThat(result.size()).isEqualTo(20);
        assertThat(result).extracting("title").contains("2021라인 공채1","2021 우아한 테크코스1");
    }

    @Test
    @DisplayName("positionFrontend")
    void positionFrontend() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.getPositions().add("frontend");
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);

        //then
        assertThat(result.size()).isEqualTo(10);
        assertThat(result).extracting("title").contains("2021라인 공채1");
    }

    @Test
    @DisplayName("positionBackend")
    void positionBackend() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.getPositions().add("server/backend");
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);

        //then
        assertThat(result.size()).isEqualTo(10);
        assertThat(result).extracting("title").contains("2021 우아한 테크코스1");
    }

    @Test
    @DisplayName("positionBackAndFront")
    void positionBackAndFront() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.getPositions().add("frontend");
        announcementSearchCondition.getPositions().add("server/backend");

        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);

        //then
        assertThat(result.size()).isEqualTo(20);
        assertThat(result).extracting("title").contains("2021라인 공채1","2021 우아한 테크코스1");
    }

    @Test
    @DisplayName("titleContainLINE")
    void titleContainLINE() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.setTitle("공채");
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);

        //then
        assertThat(result.size()).isEqualTo(10);
        assertThat(result).extracting("title").contains("2021라인 공채1");
    }

    @Test
    @DisplayName("titleContainWOO")
    void titleContainWOO() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.setTitle("테크");
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);

        //then
        assertThat(result.size()).isEqualTo(10);
        assertThat(result).extracting("title").contains("2021 우아한 테크코스1");
    }

    @Test
    @DisplayName("typeAndPosition")
    void typeAndPosition() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.setTypeName("emp");
        announcementSearchCondition.getPositions().add("server/backend");

        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("typeAndPosition2")
    void typeAndPosition2() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.setTypeName("emp");
        announcementSearchCondition.getPositions().add("frontend");

        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(10);
        assertThat(result).extracting("title").contains("2021라인 공채1");
    }

    @Test
    @DisplayName("typeAndPosition3")
    void typeAndPosition3() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.setTypeName("edu");
        announcementSearchCondition.getPositions().add("frontend");

        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(0);
    }

    @Test
    @DisplayName("typeAndPosition4")
    void typeAndPosition4() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.setTypeName("edu");
        announcementSearchCondition.getPositions().add("server/backend");

        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(10);
        assertThat(result).extracting("title").contains("2021 우아한 테크코스1");
    }

    @Test
    @DisplayName("language")
    void language() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.getLanguages().add(JAVA);
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(10);
        assertThat(result).extracting("title").contains("2021 우아한 테크코스1");
    }

    @Test
    @DisplayName("language2")
    void language2() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.getLanguages().add(JAVA);
        announcementSearchCondition.getLanguages().add(SPRING);
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(10);
        assertThat(result).extracting("title").contains("2021 우아한 테크코스1");
    }

    @Test
    @DisplayName("language3")
    void language3() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.getLanguages().add(HTML);
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(10);
        assertThat(result).extracting("title").contains("2021라인 공채1");
    }

    @Test
    @DisplayName("language4")
    void language4() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.getLanguages().add(HTML);
        announcementSearchCondition.getLanguages().add(CSS);
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(10);
        assertThat(result).extracting("title").contains("2021라인 공채1");
    }

    @Test
    @DisplayName("language5")
    void language5() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.getLanguages().add(JAVA);
        announcementSearchCondition.getLanguages().add(HTML);
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(20);
        assertThat(result).extracting("title").contains("2021라인 공채1","2021 우아한 테크코스1");
    }

    @Test
    @DisplayName("paymentZero")
    void paymentZero() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.setPayment(0);
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(20);
        assertThat(result).extracting("title").contains("2021라인 공채1", "2021 우아한 테크코스1");
    }

    @Test
    @DisplayName("payment5000")
    void payment5000() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.setPayment(5000);
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(6);
        assertThat(result).extracting("title").contains("2021라인 공채6");
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
}