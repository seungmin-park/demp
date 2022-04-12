package com.inhatc.demp.service;

import com.inhatc.demp.domain.Member;
import com.inhatc.demp.dto.member.MemberSearchCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@Transactional
@SpringBootTest
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @BeforeEach
    void beforeEach() {
        for (int i = 0; i < 10; i++) {
            memberService.join(new Member("member"+i, i));
        }
    }


    @Test
    @DisplayName("MemberSave")
    void memberSave() throws Exception {
        //given
        List<Member> members = memberService.findAll();
        //when

        //then
        assertThat(members.size()).isEqualTo(10);
    }

    @Test
    @DisplayName("findMemberByQuerydslUsername")
    void findMemberByQuerydslUsername() throws Exception {
        //given
        MemberSearchCondition memberSearchCondition = new MemberSearchCondition();
        memberSearchCondition.setUsername("member2");
        //when
        List<Member> members = memberService.findByUsernameOrAge(memberSearchCondition);
        //then
        assertThat(members.size()).isEqualTo(1);
        assertThat(members.get(0).getUsername()).isEqualTo("member2");
        assertThat(members.get(0).getAge()).isEqualTo(2);
    }

    @Test
    @DisplayName("findMemberByQuerydslAge")
    void findMemberByQuerydslAge() throws Exception {
        //given
        MemberSearchCondition memberSearchCondition = new MemberSearchCondition();
        memberSearchCondition.setAgeGoe(7);
        //when
        List<Member> members = memberService.findByUsernameOrAge(memberSearchCondition);
        //then
        assertThat(members.size()).isEqualTo(3);
        assertThat(members).extracting("username").containsExactly("member7", "member8", "member9");
        assertThat(members).extracting("age").containsExactly(7,8,9);

    }
   @Test
    @DisplayName("findMemberByQuerydslUsernameAndAge")
    void findMemberByQuerydslUsernameAndAge() throws Exception {
        //given
        MemberSearchCondition memberSearchCondition = new MemberSearchCondition();
        memberSearchCondition.setUsername("member2");
        memberSearchCondition.setAgeLoe(7);
        //when
        List<Member> members = memberService.findByUsernameOrAge(memberSearchCondition);
        //then
        assertThat(members.size()).isEqualTo(1);
       assertThat(members).extracting("username").containsExactly("member2");
       assertThat(members).extracting("age").containsExactly(2);
    }

}