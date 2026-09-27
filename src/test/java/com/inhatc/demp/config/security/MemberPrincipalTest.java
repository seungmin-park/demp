package com.inhatc.demp.config.security;

import com.inhatc.demp.domain.Member;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.test.util.ReflectionTestUtils;
import static org.assertj.core.api.Assertions.assertThat;

class MemberPrincipalTest {
    @Test
    @DisplayName("회원 ID와 인증 값을 복사하여 엔티티 변경과 독립적으로 유지한다")
    void copiesAuthenticationValues() {
        Member member = Member.builder().username("principal").password("hash").roles(new ArrayList<>(List.of("ROLE_USER"))).build();
        ReflectionTestUtils.setField(member, "id", 7L);
        MemberPrincipal principal = MemberPrincipal.from(member);

        member.getRoles().clear();
        member.encodePassword("changed");

        assertThat(principal.getMemberId()).isEqualTo(7L);
        assertThat(principal.getUsername()).isEqualTo("principal");
        assertThat(principal.getPassword()).isEqualTo("hash");
        assertThat(principal.getAuthorities()).extracting("authority").containsExactly("ROLE_USER");
    }
}
