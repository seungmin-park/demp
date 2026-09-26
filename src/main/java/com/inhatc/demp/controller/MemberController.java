package com.inhatc.demp.controller;

import com.inhatc.demp.domain.Member;
import com.inhatc.demp.dto.member.MemberDto;
import com.inhatc.demp.dto.member.MemberInfo;
import com.inhatc.demp.dto.member.MemberLoginForm;
import com.inhatc.demp.dto.member.MemberSaveForm;
import com.inhatc.demp.service.MemberService;
import lombok.RequiredArgsConstructor;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/{memberId}")
    public ResponseEntity<MemberDto> getMember(@PathVariable Long memberId) {
        Member findMember = memberService.findById(memberId);
        MemberDto memberDto = new MemberDto(findMember.getId(), findMember.getUsername());
        return new ResponseEntity<>(memberDto, HttpStatus.OK);
    }


    @PostMapping("/login")
    public ResponseEntity<MemberInfo> signIn(@ModelAttribute MemberLoginForm memberLoginForm) {
        return ResponseEntity.ok(memberService.login(memberLoginForm));
    }


    @PostMapping("/save")
    public ResponseEntity<MemberDto> saveMember(@Valid @ModelAttribute MemberSaveForm memberSaveForm) {
        return ResponseEntity.ok(memberService.registerMember(memberSaveForm));
    }

    @GetMapping("/validUsername")
    public boolean isUsernameAvailable(@RequestParam String username) {
        return memberService.isUsernameAvailable(username);
    }
}
