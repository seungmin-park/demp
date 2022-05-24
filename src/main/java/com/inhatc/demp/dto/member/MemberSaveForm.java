package com.inhatc.demp.dto.member;

import com.inhatc.demp.domain.Member;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.Collections;

@Setter
@Getter
public class MemberSaveForm {

    private String username;
    private String password;

    private BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();

    public MemberSaveForm(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public Member toEntity() {
        return Member.builder()
                .username(username)
                .password(bCryptPasswordEncoder.encode(password))
                .roles(Collections.singletonList("ROLE_USER"))
                .build();
    }
}
