package com.inhatc.demp.service;

import com.inhatc.demp.repository.MemberRepository;
import com.inhatc.demp.config.security.MemberPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailService implements UserDetailsService {
    private final MemberRepository memberRepository;
    public UserDetails loadUserByUsername(String userPk) {
        final long id;
        try { id = Long.parseLong(userPk); }
        catch (NumberFormatException ex) { throw new UsernameNotFoundException("Invalid identity"); }
        return memberRepository.findById(id).map(MemberPrincipal::from)
                .orElseThrow(() -> new UsernameNotFoundException("Invalid identity"));
    }
}
