package com.inhatc.demp.config.security;

import com.inhatc.demp.domain.Member;
import lombok.Getter;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public final class MemberPrincipal implements UserDetails {
    private final Long memberId;
    private final String username;
    private final String password;
    private final List<GrantedAuthority> authorities;

    public MemberPrincipal(Long memberId, String username, String password, List<String> roles) {
        this.memberId = memberId;
        this.username = username;
        this.password = password;
        this.authorities = List.copyOf(roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList()));
    }

    public static MemberPrincipal from(Member member) {
        return new MemberPrincipal(member.getId(), member.getUsername(), member.getPassword(), member.getRoles());
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
