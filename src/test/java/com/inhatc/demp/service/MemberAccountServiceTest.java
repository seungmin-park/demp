package com.inhatc.demp.service;

import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.domain.Member;
import com.inhatc.demp.dto.member.PasswordChange;
import com.inhatc.demp.error.ApiException;
import com.inhatc.demp.error.LoginRateLimitException;
import com.inhatc.demp.repository.MemberRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class MemberAccountServiceTest {
    @Autowired MemberAccountService accounts;
    @Autowired MemberRepository members;
    @Autowired PasswordEncoder passwords;
    @Autowired JwtTokenProvider tokens;
    @AfterEach void cleanup() { members.deleteAll(); }

    private Member member() {
        return members.save(Member.builder().username("account-owner").password(passwords.encode("old-password"))
                .roles(List.of("ROLE_USER")).build());
    }

    @Test
    @DisplayName("닉네임 변경은 커밋되어 재조회되며 아이디와 권한은 유지된다")
    void commitsNickname() {
        Member member = member();
        accounts.updateNickname(member.getId(), "  새 이름  ");
        var result = accounts.profile(member.getId());
        assertThat(result.nickname()).isEqualTo("새 이름");
        assertThat(result.username()).isEqualTo("account-owner");
        assertThat(result.admin()).isFalse();
    }

    @Test
    @DisplayName("비밀번호 오류는 기존 비밀번호와 세션을 보존하고 실패 횟수는 커밋한다")
    void preservesCredentialsOnFailure() {
        Member member = member();
        String token = tokens.createToken(member.getId().toString(), member.getRoles());
        assertThatThrownBy(() -> accounts.changePassword(member.getId(), 0, new PasswordChange("wrong", "new-password")))
                .isInstanceOf(ApiException.class);
        Member saved = members.findById(member.getId()).orElseThrow();
        assertThat(passwords.matches("old-password", saved.getPassword())).isTrue();
        assertThat(saved.getFailedLoginCount()).isEqualTo(1);
        assertThat(saved.getCredentialVersion()).isZero();
        assertThat(tokens.getAuthentication(token).isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("비밀번호 변경 후 이전 토큰과 이전 버전의 중복 변경은 거절한다")
    void revokesOldCredentials() {
        Member member = member();
        String token = tokens.createToken(member.getId().toString(), member.getRoles());
        accounts.changePassword(member.getId(), 0, new PasswordChange("old-password", "new-password"));
        Member saved = members.findById(member.getId()).orElseThrow();
        assertThat(passwords.matches("new-password", saved.getPassword())).isTrue();
        assertThat(saved.getCredentialVersion()).isEqualTo(1);
        assertThatThrownBy(() -> tokens.getAuthentication(token)).isInstanceOf(org.springframework.security.authentication.BadCredentialsException.class);
        assertThatThrownBy(() -> accounts.changePassword(member.getId(), 0, new PasswordChange("new-password", "other-password")))
                .isInstanceOf(ApiException.class);
        assertThat(passwords.matches("new-password", members.findById(member.getId()).orElseThrow().getPassword())).isTrue();
    }

    @Test
    @DisplayName("현재 비밀번호를 다섯 번 틀리면 올바른 비밀번호도 잠금 동안 거절한다")
    void limitsPasswordAttempts() {
        Member member = member();
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> accounts.changePassword(member.getId(), 0, new PasswordChange("wrong", "new-password")))
                    .isInstanceOf(ApiException.class);
        }
        assertThatThrownBy(() -> accounts.changePassword(member.getId(), 0, new PasswordChange("wrong", "new-password")))
                .isInstanceOf(LoginRateLimitException.class);
        assertThatThrownBy(() -> accounts.changePassword(member.getId(), 0, new PasswordChange("old-password", "new-password")))
                .isInstanceOf(LoginRateLimitException.class);
        assertThat(members.findById(member.getId()).orElseThrow().getCredentialVersion()).isZero();
    }
}
