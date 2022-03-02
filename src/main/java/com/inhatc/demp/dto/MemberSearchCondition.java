package com.inhatc.demp.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberSearchCondition {

    private String username;
    private Integer ageGoe;
    private Integer ageLoe;
}
