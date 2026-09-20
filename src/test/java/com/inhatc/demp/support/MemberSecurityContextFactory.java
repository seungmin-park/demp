package com.inhatc.demp.support;

import com.inhatc.demp.config.security.MemberPrincipal;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithSecurityContextFactory;
public class MemberSecurityContextFactory implements WithSecurityContextFactory<WithMember> {
    public SecurityContext createSecurityContext(WithMember annotation) {
        MemberPrincipal principal = new MemberPrincipal(annotation.id(), annotation.username(), "test-hash", List.of("ROLE_USER"));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(principal, "", principal.getAuthorities()));
        return context;
    }
}
