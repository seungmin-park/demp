package com.inhatc.demp.domain;

import jdk.jfr.Timestamp;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@SequenceGenerator(name = "que_id_generator",
        sequenceName = "que_sequence",allocationSize = 1)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Question {

    @Id
    @GeneratedValue(generator = "que_id_generator")
    @Column(name = "question_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @OneToMany(mappedBy = "question")
    private List<Answer> answers = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "question_id")
    private List<Hashtag> hashtags = new ArrayList<>();

    //연관 관계 편의 메소드
    public void settingMember(Member member) {
        this.member = member;
        member.getQuestions().add(this);
    }

    private String title;
    private String content;

    private int hits;
    private int recomend;
    private int dislike;

    @Timestamp
    private LocalDateTime createdDate = LocalDateTime.now();

    public Question(String title, String content, int hits, int recomend, int dislike) {
        this.title = title;
        this.content = content;
        this.hits = hits;
        this.recomend = recomend;
        this.dislike = dislike;
    }
}
