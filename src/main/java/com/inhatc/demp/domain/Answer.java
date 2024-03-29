package com.inhatc.demp.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer {

    @Id
    @GeneratedValue
    @Column(name = "answer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;
    @Lob
    private String content;
    private int recommend;
    private int dislike;

    //연관 관계 편의 메소드
    public void settingMember(Member member) {
        this.member = member;
        member.getAnswers().add(this);
    }

    public void settingQuestion(Question question) {
        this.question = question;
        question.getAnswers().add(this);
    }

    public void updateAnswer(String content) {
        this.content = content;
    }

    public Answer(String content, int recommend, int dislike) {
        this.content = content;
        this.recommend = recommend;
        this.dislike = dislike;
    }
}
