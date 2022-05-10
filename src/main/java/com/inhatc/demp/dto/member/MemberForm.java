package com.inhatc.demp.dto.member;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class MemberForm {

    private String username;
    private String email;
    private Integer age;
    private int password;
}
