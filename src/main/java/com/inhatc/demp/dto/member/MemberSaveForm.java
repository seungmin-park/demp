package com.inhatc.demp.dto.member;

import com.inhatc.demp.domain.Member;
import lombok.Getter;
import lombok.Setter;
import javax.validation.constraints.NotBlank;

import java.util.Collections;

@Setter
@Getter
public class MemberSaveForm {

    @NotBlank
    private String username;
    @NotBlank
    private String password;


    public MemberSaveForm(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Member toEntity() {
        return Member.builder()
                .username(username)
                .password(password)
                .roles(Collections.singletonList("ROLE_USER"))
                .build();
    }
}
