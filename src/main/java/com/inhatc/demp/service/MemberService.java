package com.inhatc.demp.service;

import com.inhatc.demp.domain.Member;
import com.inhatc.demp.dto.member.MemberLoginForm;
import com.inhatc.demp.dto.member.MemberSearchCondition;
import com.inhatc.demp.repository.MemberRepository;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.inhatc.demp.domain.QMember.member;
import static org.springframework.util.StringUtils.hasText;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberService {

    private final MemberRepository memberRepository;
    private final JPAQueryFactory queryFactory;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    @Transactional
    public void join(Member member) {
        memberRepository.save(member);
    }

    public Member findById(Long id) {
        return memberRepository.findById(id).orElseThrow();
    }

    public List<Member> findAll() {
        return memberRepository.findAll();
    }

    public Member login(MemberLoginForm memberLoginForm) {
        Member findMember = memberRepository.findByUsername(memberLoginForm.getUsername()).orElseThrow();
        if (!bCryptPasswordEncoder.matches(memberLoginForm.getPassword(), findMember.getPassword())) {
            throw new RuntimeException();
        }

        return findMember;
    }

    public List<Member> findByUsername(MemberSearchCondition condition) {

        return queryFactory
                .selectFrom(member)
                .where(usernameEq(condition.getUsername()))
                .fetch();
    }

    private BooleanExpression usernameEq(String username) {
        return hasText(username) ? member.username.eq(username) : null;
    }
}
