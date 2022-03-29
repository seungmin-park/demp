package com.inhatc.demp.repository;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.domain.AnnouncementType;
import com.inhatc.demp.dto.AnnouncementSearchCondition;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

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
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementType(announcementSearchCondition);

        //then
        Assertions.assertThat(result.size()).isEqualTo(1);
        Assertions.assertThat(result.get(0).getCompany().getName()).isEqualTo("LINE");
    }
    
    @Test
    @DisplayName("typeNameEdu")
    void typeNameEdu() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();
        announcementSearchCondition.setTypeName("edu");
        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementType(announcementSearchCondition);

        //then
        Assertions.assertThat(result.size()).isEqualTo(1);
        Assertions.assertThat(result.get(0).getCompany().getName()).isEqualTo("우아한 형제들");
    }
    
    @Test
    @DisplayName("typeNameNull")
    void typeNameNull() throws Exception {
        //given
        AnnouncementSearchCondition announcementSearchCondition = new AnnouncementSearchCondition();

        //when
        List<Announcement> result = announcementQueryRepository.findAllByAnnouncementType(announcementSearchCondition);

        //then
        Assertions.assertThat(result.size()).isEqualTo(2);
        Assertions.assertThat(result.get(0).getCompany().getName()).isEqualTo("LINE");
        Assertions.assertThat(result.get(1).getCompany().getName()).isEqualTo("우아한 형제들");
    }
}