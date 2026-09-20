package com.inhatc.demp.domain;

import lombok.Builder;
import lombok.Getter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Getter
@SequenceGenerator(name = "member_id_generator",
sequenceName = "member_sequence",allocationSize = 1)
public class Member {

    @Id
    @GeneratedValue(generator = "member_id_generator")
    @Column(name = "member_id")
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;
    private String password;

    @OneToMany(mappedBy = "member")
    private List<Question> questions = new ArrayList<>();

    @OneToMany(mappedBy = "member")
    private List<Answer> answers = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    private List<String> roles = new ArrayList<>();

    public Member() {
    }

    @Builder
    public Member(String username, String password, List<String> roles) {
        this.username = username;
        this.password = password;
        this.roles = roles;
    }

    public void encodePassword(String encodedPassword) {
        this.password = encodedPassword;
    }
}
