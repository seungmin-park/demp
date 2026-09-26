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
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public MemberDto registerMember(MemberSaveForm form) {
        Member member = form.toEntity();
        if (!StringUtils.hasText(member.getUsername()) || !StringUtils.hasText(member.getPassword())) {
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

    public MemberInfo login(MemberLoginForm form) {
        Member member = memberRepository.findByUsername(form.getUsername())
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED));
        if (form.getPassword() == null || !passwordEncoder.matches(form.getPassword(), member.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED);
        }
        MemberInfo result = new MemberInfo();
        result.setUsername(member.getUsername());
        result.setJwt(jwtTokenProvider.createToken(member.getId().toString(), member.getRoles()));
        return result;
    }

    public Member findByUsername(String username) {
        return memberRepository.findByUsername(username).orElseThrow(ResourceNotFoundException::new);
    }

    public Boolean isUsernameAvailable(String username) {
        return StringUtils.hasText(username) && memberRepository.findByUsername(username).isEmpty();
    }
}
