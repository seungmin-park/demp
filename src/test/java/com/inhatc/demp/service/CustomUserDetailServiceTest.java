package com.inhatc.demp.service;

import com.inhatc.demp.domain.Member;
import com.inhatc.demp.repository.MemberRepository;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class CustomUserDetailServiceTest {
    @Autowired CustomUserDetailService service;
    @Autowired MemberRepository members;
    private Long memberId;

    @AfterEach
    void cleanup() { if (memberId != null) members.deleteById(memberId); }

    @Test
    @DisplayName("인증 정보는 회원 엔티티가 아닌 독립 객체이며 이름과 권한을 유지한다")
    void returnsIndependentAuthenticationDetails() {
        Member member = members.save(Member.builder()
                .username("principal-test")
                .password("password-hash")
                .roles(List.of("ROLE_USER"))
                .build());
        memberId = member.getId();

        UserDetails details = service.loadUserByUsername(memberId.toString());

        assertThat(details.getUsername()).isEqualTo("principal-test");
        assertThat(details.getPassword()).isEqualTo("password-hash");
        assertThat(details.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
        assertThat(details).isNotInstanceOf(Member.class);
    }
}
