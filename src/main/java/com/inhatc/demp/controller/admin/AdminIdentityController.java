package com.inhatc.demp.controller.admin;

import com.inhatc.demp.config.security.MemberPrincipal;
import com.inhatc.demp.dto.member.MemberDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AdminIdentityController {
    @GetMapping("/api/admin/me")
    public MemberDto currentAdmin(@AuthenticationPrincipal MemberPrincipal principal) {
        return new MemberDto(principal.getMemberId(), principal.getUsername());
    }
}
