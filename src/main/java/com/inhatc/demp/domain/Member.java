package com.inhatc.demp.domain;

import com.inhatc.demp.dto.member.MemberForm;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SequenceGenerator(name = "member_id_generator",
sequenceName = "member_sequence",allocationSize = 1)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member {

    @Id
    @GeneratedValue(generator = "member_id_generator")
    @Column(name = "member_id")
    private Long id;

    private String email;
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
        this.email = memberForm.getEmail();
        this.age = memberForm.getAge();
        this.password = memberForm.getPassword();
    }
}
