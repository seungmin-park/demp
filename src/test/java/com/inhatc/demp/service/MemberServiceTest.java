package com.inhatc.demp.service;

import com.inhatc.demp.domain.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
class MemberServiceTest {

    @Autowired
    private MemberService memberService;

    @BeforeEach
    void beforeEach() {
        for (int i = 0; i < 10; i++) {
            memberService.join(new Member("member" + i, String.valueOf(i), Collections.singletonList("ROLE_USER")));
        }
    }

    @Test
    @DisplayName("MemberSave")
    void memberSave() throws Exception {
        //given
        List<Member> members = memberService.findAll();
        //when

        //then
        assertThat(members.size()).isEqualTo(12);
    }

    @Test
    @DisplayName("validDuplicateUsernameTrue")
    void validDuplicateUsernameTrue() throws Exception {
        assertThat(memberService.validationDuplicateUsername("newMemberTestUsername")).isTrue();
    }

    @Test
    @DisplayName("validDuplicateUsernameFalse")
    void validDuplicateUsernameFalse() throws Exception {
        assertThat(memberService.validationDuplicateUsername("member0")).isFalse();
    }
}