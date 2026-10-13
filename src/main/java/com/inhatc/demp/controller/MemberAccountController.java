package com.inhatc.demp.controller;

import com.inhatc.demp.config.security.MemberPrincipal;
import com.inhatc.demp.dto.member.MemberProfile;
import com.inhatc.demp.dto.member.NicknameUpdate;
import com.inhatc.demp.dto.member.PasswordChange;
import com.inhatc.demp.service.MemberAccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/me")
@RequiredArgsConstructor
public class MemberAccountController {
    private final MemberAccountService accounts;

    @GetMapping
    public MemberProfile profile(@AuthenticationPrincipal MemberPrincipal member) {
        return accounts.profile(member.getMemberId());
    }

    @PatchMapping
    public MemberProfile updateNickname(@AuthenticationPrincipal MemberPrincipal member, @Valid @RequestBody NicknameUpdate form) {
        return accounts.updateNickname(member.getMemberId(), form.nickname());
    }

    @PutMapping("/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal MemberPrincipal member, @Valid @RequestBody PasswordChange form) {
        accounts.changePassword(member.getMemberId(), member.getCredentialVersion(), form);
    }
}
