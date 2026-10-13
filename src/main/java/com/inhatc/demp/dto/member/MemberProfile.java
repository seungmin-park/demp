package com.inhatc.demp.dto.member;

import com.inhatc.demp.domain.Member;

public record MemberProfile(Long id, String username, String nickname, boolean admin) {
    public static MemberProfile from(Member member) {
        return new MemberProfile(member.getId(), member.getUsername(), member.getNickname(), member.getRoles().contains("ROLE_ADMIN"));
    }
}
