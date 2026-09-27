package com.inhatc.demp.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Answer {

    @Id
    @GeneratedValue(generator = "answer_legacy_id")
    @SequenceGenerator(name = "answer_legacy_id", sequenceName = "hibernate_sequence", allocationSize = 1)
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
    public void assignMember(Member member) {
        if (this.member == member) return;
        if (this.member != null) this.member.getAnswers().remove(this);
        this.member = member;
        if (member != null) member.getAnswers().add(this);
    }

    public void assignQuestion(Question question) {
        if (this.question == question) return;
        if (this.question != null) this.question.getAnswers().remove(this);
        this.question = question;
        if (question != null) question.getAnswers().add(this);
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
