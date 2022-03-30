package com.inhatc.demp.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.inhatc.demp.dto.MemberForm;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue
    @Column(name = "member_id")
    private Long id;

    private String username;
    private Integer age;
    private int password;

    @OneToMany(mappedBy = "member")
    private List<Question> questions = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<Answer> answers = new ArrayList<>();

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
