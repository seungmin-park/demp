package com.inhatc.demp.service;

import com.inhatc.demp.domain.Member;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.dto.member.MemberDto;
import com.inhatc.demp.dto.member.MemberInfo;
import com.inhatc.demp.dto.member.MemberSaveForm;
import com.inhatc.demp.dto.member.MemberLoginForm;
import com.inhatc.demp.error.ApiException;
import com.inhatc.demp.error.ResourceNotFoundException;
import com.inhatc.demp.repository.MemberRepository;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import com.inhatc.demp.error.LoginRateLimitException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final PlatformTransactionManager transactionManager;
    private final Clock clock;

    @Transactional
    public MemberDto registerMember(MemberSaveForm form) {
        Member member = form.toEntity();
        if (!StringUtils.hasText(member.getUsername()) || !StringUtils.hasText(member.getPassword())
                || member.getPassword().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new ApiException(HttpStatus.BAD_REQUEST);
        }
        if (memberRepository.findByUsername(member.getUsername()).isPresent()) {
            throw new ApiException(HttpStatus.CONFLICT);
        }
        member.encodePassword(passwordEncoder.encode(member.getPassword()));
        try {
            memberRepository.saveAndFlush(member);
        } catch (DataIntegrityViolationException ex) {
            throw new ApiException(HttpStatus.CONFLICT);
        }
        return new MemberDto(member.getId(), member.getUsername());
    }

    public Member findById(Long id) {
        return memberRepository.findById(id).orElseThrow(ResourceNotFoundException::new);
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public MemberInfo login(MemberLoginForm form) {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        LoginAttempt attempt = transaction.execute(status -> authenticateUnderLock(form));
        if (attempt.retryAfterSeconds() > 0) throw new LoginRateLimitException(attempt.retryAfterSeconds());
        if (attempt.memberInfo() == null) throw new ApiException(HttpStatus.UNAUTHORIZED);
        return attempt.memberInfo();
    }

    private LoginAttempt authenticateUnderLock(MemberLoginForm form) {
        Member member = memberRepository.findByUsernameForLogin(form.getUsername()).orElse(null);
        if (member == null) return new LoginAttempt(null, 0);
        // Read time after acquiring the row: a queued request must use its own decision time.
        Instant now = clock.instant();
        member.expireLoginRestriction(now);
        long retryAfter = member.loginRetryAfterSeconds(now);
        if (retryAfter > 0) return new LoginAttempt(null, retryAfter);
        if (form.getPassword() == null || !passwordEncoder.matches(form.getPassword(), member.getPassword())) {
            member.recordLoginFailure(now);
            return new LoginAttempt(null, member.loginRetryAfterSeconds(now));
        }
        member.resetLoginFailures();
        MemberInfo result = new MemberInfo();
        result.setUsername(member.getUsername());
        result.setJwt(jwtTokenProvider.createToken(member.getId().toString(), member.getRoles()));
        return new LoginAttempt(result, 0);
    }

    private record LoginAttempt(MemberInfo memberInfo, long retryAfterSeconds) {}

    public Member findByUsername(String username) {
        return memberRepository.findByUsername(username).orElseThrow(ResourceNotFoundException::new);
    }

    public Boolean isUsernameAvailable(String username) {
        return StringUtils.hasText(username) && memberRepository.findByUsername(username).isEmpty();
    }
}
