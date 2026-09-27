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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.inhatc.demp.error.ApiException;
import org.springframework.http.HttpStatus;

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
        MemberDto saved = memberService.registerMember(new MemberSaveForm("login-member", "password"));
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
        MemberDto member = memberService.registerMember(new MemberSaveForm("member-a", "password"));

        Member saved = memberRepository.findById(member.getId()).orElseThrow();
        assertThat(saved.getUsername()).isEqualTo("member-a");
        assertThat(new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().matches("password", saved.getPassword())).isTrue();
        assertThat(saved.getRoles()).containsExactly("ROLE_USER");
        assertThat(memberService.findAll()).extracting(Member::getUsername).containsExactlyInAnyOrder("member-a");
    }

    @Test
    @DisplayName("사용하지 않는 회원 이름은 가입할 수 있다")
    void validDuplicateUsernameTrue() {
        assertThat(memberService.isUsernameAvailable("new-member")).isTrue();
    }

    @Test
    @DisplayName("이미 사용 중인 회원 이름은 가입할 수 없다")
    void validDuplicateUsernameFalse() {
        memberService.registerMember(new MemberSaveForm("member-a", "password"));
        assertThat(memberService.isUsernameAvailable("member-a")).isFalse();
    }
    @ParameterizedTest
    @DisplayName("비어 있거나 공백인 회원 이름은 가입할 수 없다")
    @NullAndEmptySource
    @ValueSource(strings = {" ", "   "})
    void rejectsBlankUsername(String username) {
        assertThat(memberService.isUsernameAvailable(username)).isFalse();
    }

    @ParameterizedTest
    @DisplayName("72 UTF-8 바이트를 넘는 신규 비밀번호는 저장 없이 400으로 거절한다")
    @ValueSource(strings = {"aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa", "가나다라마바사아자차카타파하가나다라마바사아자차카"})
    void rejectsOverlongPassword(String password) {
        assertThatThrownBy(() -> memberService.registerMember(new MemberSaveForm("long-password", password)))
                .isInstanceOfSatisfying(ApiException.class,
                        ex -> assertThat(ex.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST));
        assertThat(memberRepository.findByUsername("long-password")).isEmpty();
    }

    @Test
    @DisplayName("72 UTF-8 바이트 경계의 한글 비밀번호로 가입하고 로그인한다")
    void acceptsPasswordAtByteLimit() {
        String password = "가".repeat(24);
        MemberDto saved = memberService.registerMember(new MemberSaveForm("password-boundary", password));
        MemberLoginForm form = new MemberLoginForm();
        form.setUsername("password-boundary");
        form.setPassword(password);

        MemberInfo result = memberService.login(form);

        assertThat(jwtTokenProvider.getUserPk(result.getJwt())).isEqualTo(saved.getId().toString());
    }

    @Test
    @DisplayName("기존 BCrypt에 저장된 긴 비밀번호의 로그인은 계속 허용한다")
    void acceptsExistingLongPasswordAtLogin() {
        // 이전 BCrypt가 긴 ASCII 비밀번호에 사용한 첫 72바이트의 해시와 같은 저장값이다.
        String hash = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder().encode("a".repeat(72));
        Member member = memberRepository.save(Member.builder().username("legacy-long-password")
                .password(hash).roles(List.of("ROLE_USER")).build());
        MemberLoginForm form = new MemberLoginForm();
        form.setUsername("legacy-long-password");
        form.setPassword("a".repeat(73));

        MemberInfo result = memberService.login(form);

        assertThat(jwtTokenProvider.getUserPk(result.getJwt())).isEqualTo(member.getId().toString());
    }

}
