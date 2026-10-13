package com.inhatc.demp.service;

import com.inhatc.demp.domain.Member;
import com.inhatc.demp.dto.member.MemberProfile;
import com.inhatc.demp.dto.member.PasswordChange;
import com.inhatc.demp.error.ApiException;
import com.inhatc.demp.error.LoginRateLimitException;
import com.inhatc.demp.error.ResourceNotFoundException;
import com.inhatc.demp.repository.MemberRepository;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class MemberAccountService {
    private final MemberRepository members;
    private final PasswordEncoder passwords;
    private final PlatformTransactionManager transactionManager;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MemberProfile profile(Long memberId) {
        return MemberProfile.from(members.findById(memberId).orElseThrow(ResourceNotFoundException::new));
    }

    @Transactional
    public MemberProfile updateNickname(Long memberId, String nickname) {
        Member member = members.findByIdForAccountUpdate(memberId).orElseThrow(ResourceNotFoundException::new);
        member.changeNickname(nickname);
        return MemberProfile.from(member);
    }

    public void changePassword(Long memberId, long credentialVersion, PasswordChange form) {
        String next = form.newPassword();
        if (next == null || next.isBlank() || next.length() < 8 || next.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST);
        }
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        PasswordAttempt result = transaction.execute(status -> {
            Member member = members.findByIdForAccountUpdate(memberId).orElseThrow(ResourceNotFoundException::new);
            if (member.getCredentialVersion() != credentialVersion) throw new ApiException(HttpStatus.UNAUTHORIZED);
            Instant now = clock.instant();
            member.expireLoginRestriction(now);
            if (member.loginRetryAfterSeconds(now) > 0) return new PasswordAttempt(false, member.loginRetryAfterSeconds(now));
            if (form.currentPassword() == null || !passwords.matches(form.currentPassword(), member.getPassword())) {
                member.recordLoginFailure(now);
                return new PasswordAttempt(false, member.loginRetryAfterSeconds(now));
            }
            member.changePassword(passwords.encode(next));
            return new PasswordAttempt(true, 0);
        });
        if (result.retryAfterSeconds() > 0) throw new LoginRateLimitException(result.retryAfterSeconds());
        if (!result.changed()) throw new ApiException(HttpStatus.BAD_REQUEST);
    }

    private record PasswordAttempt(boolean changed, long retryAfterSeconds) {}
}
