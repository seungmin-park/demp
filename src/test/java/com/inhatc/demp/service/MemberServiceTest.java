package com.inhatc.demp.service;

import com.inhatc.demp.domain.Member;
import com.inhatc.demp.dto.member.MemberDto;
import com.inhatc.demp.dto.member.MemberInfo;
import com.inhatc.demp.dto.member.MemberLoginForm;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.dto.member.MemberSaveForm;
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

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @AfterEach
    void cleanUp() {
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("로그인은 인증한 회원의 이름과 유효한 회원 ID 토큰을 반환한다")
    void returnsAuthenticatedLoginResult() {
        MemberDto saved = memberService.join(new MemberSaveForm("login-member", "password"));
        MemberLoginForm form = new MemberLoginForm();
        form.setUsername("login-member");
        form.setPassword("password");

        MemberInfo result = memberService.login(form);

        assertThat(result.getUsername()).isEqualTo("login-member");
        assertThat(jwtTokenProvider.validateToken(result.getJwt())).isTrue();
        assertThat(jwtTokenProvider.getUserPk(result.getJwt())).isEqualTo(saved.getId().toString());
    }

    @Test
    @DisplayName("가입한 회원이 조회된다")
    void memberSave() {
        MemberDto member = memberService.join(new MemberSaveForm("member-a", "password"));

        Member saved = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(saved.getUsername()).isEqualTo("member-a");
        assertThat(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches("password", saved.getPassword())).isTrue();
        assertThat(saved.getRoles()).containsExactly("ROLE_USER");
        assertThat(memberService.findAll()).extracting(Member::getUsername).containsExactlyInAnyOrder("member-a");
    }

    @Test
    @DisplayName("사용하지 않는 회원 이름은 가입할 수 있다")
    void validDuplicateUsernameTrue() {
        assertThat(memberService.validationDuplicateUsername("new-member")).isTrue();
    }

    @Test
    @DisplayName("이미 사용 중인 회원 이름은 가입할 수 없다")
    void validDuplicateUsernameFalse() {
        memberService.join(new MemberSaveForm("member-a", "password"));
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
