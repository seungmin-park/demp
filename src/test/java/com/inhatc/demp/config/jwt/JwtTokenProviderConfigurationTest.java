package com.inhatc.demp.config.jwt;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.security.core.userdetails.UserDetailsService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class JwtTokenProviderConfigurationTest {
    @Test
    @DisplayName("짧은 JWT 키는 비밀값을 노출하지 않고 설정 이름과 바이트 조건을 안내한다")
    void rejectsShortKeyWithConfigurationMessage() {
        try (var context = contextWithSecret("short-test-key")) {
            assertThatThrownBy(context::refresh)
                    .hasRootCauseMessage("JWT_SECRET must contain at least 32 UTF-8 bytes");
        }
    }

    @Test
    @DisplayName("32 UTF-8 바이트 경계의 키로 토큰을 발급하고 검증한다")
    void acceptsKeyAtByteBoundary() {
        try (var context = contextWithSecret("가".repeat(10) + "aa")) {
            context.refresh();
            JwtTokenProvider provider = context.getBean(JwtTokenProvider.class);
            String token = provider.createToken("7", List.of("ROLE_USER"));
            assertThat(provider.validateToken(token)).isTrue();
            assertThat(provider.getUserPk(token)).isEqualTo("7");
        }
    }

    private AnnotationConfigApplicationContext contextWithSecret(String secret) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().getPropertySources().addFirst(
                new MapPropertySource("jwt-test", Map.of("spring.jwt.secret", secret)));
        context.registerBean(UserDetailsService.class, () -> mock(UserDetailsService.class));
        context.registerBean(JwtTokenProvider.class);
        return context;
    }
}
