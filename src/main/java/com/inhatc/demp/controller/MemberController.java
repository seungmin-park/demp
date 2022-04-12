package com.inhatc.demp.controller;

import com.inhatc.demp.domain.Member;
import com.inhatc.demp.dto.member.MemberForm;
import com.inhatc.demp.service.MemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("")
    public ResponseEntity<Member> Member() {
        log.info("MemberController.Member");
        Optional<Member> findMember = memberService.findById(1L);
        return new ResponseEntity<>(findMember.get(), HttpStatus.OK);
    }

    @PostMapping("/save")
    public ResponseEntity<Member> saveMember(@RequestBody MemberForm memberForm) {
        Member member = new Member(memberForm);
        memberService.join(member);
        Optional<Member> findMember = memberService.findById(member.getId());
        return new ResponseEntity<>(findMember.get(), HttpStatus.OK);
    }
}
