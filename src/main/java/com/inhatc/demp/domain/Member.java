package com.inhatc.demp.domain;

import com.inhatc.demp.dto.MemberForm;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue
    private Long id;

    private String username;
    private Integer age;
    private int password;

    public Member(String username, int age) {
        this.username = username;
        this.age = age;
    }

    public Member(MemberForm memberForm) {
        this.username = memberForm.getUsername();
        this.age = memberForm.getAge();
        this.password = memberForm.getPassword();
    }
}
