package com.inhatc.demp.service;

import com.inhatc.demp.domain.Member;
import com.inhatc.demp.repository.MemberRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class MemberServiceTest {

    @Autowired
    private MemberService memberService;
    @Autowired
    private MemberRepository memberRepository;

    @AfterEach
    void cleanUp() {
        memberRepository.findByUsername("member-a").ifPresent(memberRepository::delete);
    }

    @Test
    @DisplayName("가입한 회원과 초기 회원이 함께 조회된다")
    void memberSave() {
        Member member = new Member("member-a", "password", List.of("ROLE_USER"));
        memberService.join(member);

        Member saved = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(saved.getUsername()).isEqualTo("member-a");
        assertThat(saved.getPassword()).isEqualTo("password");
        assertThat(saved.getRoles()).containsExactly("ROLE_USER");
        assertThat(memberService.findAll()).extracting(Member::getUsername).containsExactlyInAnyOrder("testMemberA", "testMemberB", "member-a");
    }

    @Test
    @DisplayName("사용하지 않는 회원 이름은 가입할 수 있다")
    void validDuplicateUsernameTrue() {
        assertThat(memberService.validationDuplicateUsername("new-member")).isTrue();
    }

    @Test
    @DisplayName("이미 사용 중인 회원 이름은 가입할 수 없다")
    void validDuplicateUsernameFalse() {
        memberService.join(new Member("member-a", "password", List.of("ROLE_USER")));
        assertThat(memberService.validationDuplicateUsername("member-a")).isFalse();
    }
    @ParameterizedTest
    @DisplayName("비어 있거나 공백인 회원 이름은 가입할 수 없다")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void rejectsBlankUsername(String username) {
        assertThat(memberService.validationDuplicateUsername(username)).isFalse();
    }

}
