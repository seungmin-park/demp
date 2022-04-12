package com.inhatc.demp.repository;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

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
        assertThat(result.size()).isEqualTo(1);
        assertThat(result).extracting("title").containsExactly("2021라인 공채");
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
        assertThat(result.size()).isEqualTo(1);
        assertThat(result).extracting("title").containsExactly("2021 우아한 테크코스");
    }
    
    @Test
    @DisplayName("typeNameNull")
    void typeNameNull() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();

        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);

        //then
        assertThat(result.size()).isEqualTo(2);
        assertThat(result).extracting("title").containsExactly("2021라인 공채","2021 우아한 테크코스");
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
        assertThat(result.size()).isEqualTo(1);
        assertThat(result).extracting("title").containsExactly("2021라인 공채");
    }

    @Test
    @DisplayName("positionBackend")
    void positionBackend() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.getPositions().add("backend");
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);

        //then
        assertThat(result.size()).isEqualTo(1);
        assertThat(result).extracting("title").containsExactly("2021 우아한 테크코스");
    }

    @Test
    @DisplayName("positionBackAndFront")
    void positionBackAndFront() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.getPositions().add("frontend");
        announcementSearchCondition.getPositions().add("backend");

        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);

        //then
        assertThat(result.size()).isEqualTo(2);
        assertThat(result).extracting("title").containsExactly("2021라인 공채","2021 우아한 테크코스");
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
        assertThat(result.size()).isEqualTo(1);
        assertThat(result).extracting("title").containsExactly("2021라인 공채");
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
        assertThat(result.size()).isEqualTo(1);
        assertThat(result).extracting("title").containsExactly("2021 우아한 테크코스");
    }

    @Test
    @DisplayName("typeAndPosition")
    void typeAndPosition() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.setTypeName("emp");
        announcementSearchCondition.getPositions().add("backend");

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
        assertThat(result.size()).isEqualTo(1);
        assertThat(result).extracting("title").containsExactly("2021라인 공채");
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
        announcementSearchCondition.getPositions().add("backend");

        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(1);
        assertThat(result).extracting("title").containsExactly("2021 우아한 테크코스");
    }

    @Test
    @DisplayName("language")
    void language() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.getLanguages().add("Java");
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(1);
        assertThat(result).extracting("title").containsExactly("2021 우아한 테크코스");
    }

    @Test
    @DisplayName("language2")
    void language2() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.getLanguages().add("Java");
        announcementSearchCondition.getLanguages().add("Spring");
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(1);
        assertThat(result).extracting("title").containsExactly("2021 우아한 테크코스");
    }

    @Test
    @DisplayName("language3")
    void language3() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.getLanguages().add("HTML");
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(1);
        assertThat(result).extracting("title").containsExactly("2021라인 공채");
    }

    @Test
    @DisplayName("language4")
    void language4() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.getLanguages().add("HTML");
        announcementSearchCondition.getLanguages().add("CSS");
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(1);
        assertThat(result).extracting("title").containsExactly("2021라인 공채");
    }

    @Test
    @DisplayName("language5")
    void language5() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.getLanguages().add("Java");
        announcementSearchCondition.getLanguages().add("HTML");
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementCondition(announcementSearchCondition);
        //then
        assertThat(result.size()).isEqualTo(2);
        assertThat(result).extracting("title").containsExactly("2021라인 공채","2021 우아한 테크코스");
    }
}