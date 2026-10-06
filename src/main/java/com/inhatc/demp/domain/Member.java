package com.inhatc.demp.domain;

import lombok.Builder;
import lombok.Getter;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;
import java.time.Duration;
import java.time.Instant;

@Entity
@Getter
@SequenceGenerator(name = "member_id_generator",
sequenceName = "member_sequence",allocationSize = 1)
public class Member {

    @Id
    @GeneratedValue(generator = "member_id_generator")
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;
    private String password;

    private static final int MAX_LOGIN_FAILURES = 5;
    private static final Duration LOGIN_FAILURE_WINDOW = Duration.ofMinutes(15);
    private static final Duration LOGIN_BLOCK_DURATION = Duration.ofMinutes(15);

    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("0")
    private int failedLoginCount;
    private Instant loginFailureWindowStartedAt;
    private Instant loginBlockedUntil;

    @OneToMany(mappedBy = "member")
    private List<Question> questions = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<Answer> answers = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> roles = new ArrayList<>();

    protected Member() {
    }

    @Builder
    private Member(String username, String password, List<String> roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
    }

    public void encodePassword(String encodedPassword) {
        this.password = encodedPassword;
    }

    public void expireLoginRestriction(Instant now) {
        if (loginBlockedUntil != null) {
            if (!now.isBefore(loginBlockedUntil)) resetLoginFailures();
        } else if (loginFailureWindowStartedAt != null
                && !now.isBefore(loginFailureWindowStartedAt.plus(LOGIN_FAILURE_WINDOW))) {
            resetLoginFailures();
        }
    }

    public long loginRetryAfterSeconds(Instant now) {
        if (loginBlockedUntil == null || !now.isBefore(loginBlockedUntil)) return 0;
        Duration remaining = Duration.between(now, loginBlockedUntil);
        return remaining.getSeconds() + (remaining.getNano() > 0 ? 1 : 0);
    }

    public void recordLoginFailure(Instant now) {
        expireLoginRestriction(now);
        if (loginRetryAfterSeconds(now) > 0) return;
        if (loginFailureWindowStartedAt == null) loginFailureWindowStartedAt = now;
        failedLoginCount++;
        if (failedLoginCount >= MAX_LOGIN_FAILURES) loginBlockedUntil = now.plus(LOGIN_BLOCK_DURATION);
    }

    public void resetLoginFailures() {
        failedLoginCount = 0;
        loginFailureWindowStartedAt = null;
        loginBlockedUntil = null;
    }
}
