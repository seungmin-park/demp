package com.inhatc.demp.service;

import com.inhatc.demp.dto.member.MemberLoginForm;
import com.inhatc.demp.dto.member.MemberSaveForm;
import com.inhatc.demp.error.ApiException;
import com.inhatc.demp.repository.MemberRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import static org.mockito.Mockito.when;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@AutoConfigureMockMvc
class MemberLoginProtectionTest {
    @Autowired MemberService service;
    @Autowired MockMvc mvc;
    @Autowired MemberRepository members;
    @MockitoBean Clock clock;
    private final AtomicReference<Instant> now = new AtomicReference<>();
    private static final Instant START = Instant.parse("2026-10-06T00:00:00Z");

    @BeforeEach
    void prepareTime() {
        now.set(START);
        when(clock.instant()).thenAnswer(invocation -> now.get());
        when(clock.getZone()).thenReturn(ZoneOffset.UTC);
    }

    @AfterEach
    void cleanUp() {
        members.deleteAll();
    }

    @Test
    @DisplayName("오류 응답 이후에도 실패가 누적되어 다섯 번째 실패부터 올바른 비밀번호도 제한한다")
    void commitsFailuresBeforeReturningError() {
        service.registerMember(new MemberSaveForm("protected-member", "password"));
        for (int i = 0; i < 4; i++) {
            assertFailure("wrong", HttpStatus.UNAUTHORIZED);
        }
        assertFailure("wrong", HttpStatus.TOO_MANY_REQUESTS);
        assertFailure("password", HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("제한은 만료 직전까지 유지되고 정확히 15분이 지나면 정상 로그인된다")
    void allowsLoginAtExactBlockExpiry() {
        service.registerMember(new MemberSaveForm("protected-member", "password"));
        failFiveTimes();
        now.set(START.plusSeconds(899));
        assertFailure("password", HttpStatus.TOO_MANY_REQUESTS);
        now.set(START.plusSeconds(900));
        assertThat(service.login(form("password")).getUsername()).isEqualTo("protected-member");
        assertFailure("wrong", HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("실패 창이 정확히 15분이면 이전 실패를 초기화하고 새 창을 시작한다")
    void startsNewWindowAtExactExpiry() {
        service.registerMember(new MemberSaveForm("protected-member", "password"));
        for (int i = 0; i < 4; i++) assertFailure("wrong", HttpStatus.UNAUTHORIZED);
        now.set(START.plusSeconds(900));
        for (int i = 0; i < 4; i++) assertFailure("wrong", HttpStatus.UNAUTHORIZED);
        assertFailure("wrong", HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("실패 창의 만료 직전 다섯 번째 실패는 그 시각부터 15분간 제한한다")
    void blocksFifthFailureJustBeforeWindowExpiry() {
        service.registerMember(new MemberSaveForm("protected-member", "password"));
        for (int i = 0; i < 4; i++) assertFailure("wrong", HttpStatus.UNAUTHORIZED);
        now.set(START.plusSeconds(899));
        assertFailure("wrong", HttpStatus.TOO_MANY_REQUESTS);
        now.set(START.plusSeconds(900));
        assertFailure("password", HttpStatus.TOO_MANY_REQUESTS);
        now.set(START.plusSeconds(1799));
        assertThat(service.login(form("password")).getUsername()).isEqualTo("protected-member");
    }

    @Test
    @DisplayName("정상 로그인은 이전 네 번의 실패를 초기화한다")
    void successResetsPreviousFailures() {
        service.registerMember(new MemberSaveForm("protected-member", "password"));
        for (int i = 0; i < 4; i++) assertFailure("wrong", HttpStatus.UNAUTHORIZED);
        assertThat(service.login(form("password")).getUsername()).isEqualTo("protected-member");
        for (int i = 0; i < 4; i++) assertFailure("wrong", HttpStatus.UNAUTHORIZED);
        assertFailure("wrong", HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("같은 회원의 동시 실패 여덟 건은 네 건의 401과 네 건의 429를 반환한다")
    void concurrentFailuresCannotOverwriteEachOther() throws Exception {
        service.registerMember(new MemberSaveForm("protected-member", "password"));
        var barrier = new CyclicBarrier(8);
        try (var executor = Executors.newFixedThreadPool(8)) {
            var tasks = java.util.stream.IntStream.range(0, 8)
                    .<java.util.concurrent.Callable<Integer>>mapToObj(i -> () -> {
                        barrier.await(10, TimeUnit.SECONDS);
                        try { service.login(form("wrong")); return 200; }
                        catch (ApiException error) { return error.getStatus().value(); }
                    }).toList();
            var results = executor.invokeAll(tasks, 20, TimeUnit.SECONDS);
            var statuses = new java.util.ArrayList<Integer>();
            for (var result : results) statuses.add(result.get());
            assertThat(statuses).containsExactlyInAnyOrder(401, 401, 401, 401, 429, 429, 429, 429);
        }
        assertFailure("password", HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("한 회원의 제한은 다른 회원의 정상 로그인을 막지 않는다")
    void keepsAccountsIndependent() {
        service.registerMember(new MemberSaveForm("protected-member", "password"));
        service.registerMember(new MemberSaveForm("other-member", "password"));
        failFiveTimes();
        var other = form("password"); other.setUsername("other-member");
        assertThat(service.login(other).getUsername()).isEqualTo("other-member");
    }

    @Test
    @DisplayName("없는 회원과 틀린 비밀번호는 모두 401이며 없는 회원의 상태를 생성하지 않는다")
    void unknownAccountRemainsGeneric() {
        for (int i = 0; i < 6; i++) assertFailure("wrong", HttpStatus.UNAUTHORIZED);
        assertThat(members.count()).isZero();
    }

    @Test
    @DisplayName("비밀번호 누락도 기존 회원의 로그인 실패로 누적한다")
    void missingPasswordCountsAsFailure() {
        service.registerMember(new MemberSaveForm("protected-member", "password"));
        for (int i = 0; i < 4; i++) assertFailure(null, HttpStatus.UNAUTHORIZED);
        assertFailure(null, HttpStatus.TOO_MANY_REQUESTS);
    }

    @Test
    @DisplayName("로그인 HTTP 제한은 429와 남은 초를 Retry-After로 보내며 교차 출처에서도 읽을 수 있다")
    void exposesRetryAfterOnRateLimitedLogin() throws Exception {
        service.registerMember(new MemberSaveForm("protected-member", "password"));
        for (int i = 0; i < 4; i++) assertFailure("wrong", HttpStatus.UNAUTHORIZED);
        mvc.perform(post("/api/member/login").param("username", "protected-member").param("password", "wrong")
                        .header("Origin", "http://localhost:5050"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "900"))
                .andExpect(header().string("Access-Control-Expose-Headers", "Retry-After"))
                .andExpect(jsonPath("$.errorCode").value(429))
                .andExpect(jsonPath("$.errorMessage").value("Too many requests"))
                .andExpect(jsonPath("$.instance").value("/api/member/login"));
        now.set(START.plusSeconds(899).plusNanos(1));
        mvc.perform(post("/api/member/login").param("username", "protected-member").param("password", "password"))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "1"));
    }

    private void failFiveTimes() {
        for (int i = 0; i < 4; i++) assertFailure("wrong", HttpStatus.UNAUTHORIZED);
        assertFailure("wrong", HttpStatus.TOO_MANY_REQUESTS);
    }

    private MemberLoginForm form(String password) {
        MemberLoginForm form = new MemberLoginForm();
        form.setUsername("protected-member");
        form.setPassword(password);
        return form;
    }

    private void assertFailure(String password, HttpStatus expected) {
        assertThatThrownBy(() -> service.login(form(password))).isInstanceOfSatisfying(ApiException.class,
                error -> assertThat(error.getStatus()).isEqualTo(expected));
    }
}
