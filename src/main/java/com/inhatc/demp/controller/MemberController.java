package com.inhatc.demp.controller;

import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.domain.Member;
import com.inhatc.demp.dto.member.MemberInfo;
import com.inhatc.demp.dto.member.MemberLoginForm;
import com.inhatc.demp.dto.member.MemberSaveForm;
import com.inhatc.demp.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final JwtTokenProvider jwtTokenProvider;

    @GetMapping("")
    public ResponseEntity<Member> Member() {
        log.info("MemberController.Member");
        Member findMember = memberService.findById(1L);
        return new ResponseEntity<>(findMember, HttpStatus.OK);
    }


    @PostMapping("/login")
    public ResponseEntity<MemberInfo> signIn(@ModelAttribute MemberLoginForm memberLoginForm) {
        Member findMember = memberService.login(memberLoginForm);
        String jwt = jwtTokenProvider.createToken(String.valueOf(findMember.getId()), findMember.getRoles());
        MemberInfo memberInfo = new MemberInfo();
        memberInfo.setUsername(findMember.getUsername());
        memberInfo.setJwt(jwt);
        return new ResponseEntity<>(memberInfo, HttpStatus.OK);
    }


    @PostMapping("/save")
    public ResponseEntity<Member> saveMember(@ModelAttribute MemberSaveForm memberSaveForm) {
        Member member = memberSaveForm.toEntity();
        memberService.join(member);
        Member findMember = memberService.findById(member.getId());
        return new ResponseEntity<>(findMember, HttpStatus.OK);
    }

    @GetMapping("/validUsername")
    public boolean validUsername(@RequestParam String username) {
        return memberService.validationDuplicateUsername(username);
    }
}
