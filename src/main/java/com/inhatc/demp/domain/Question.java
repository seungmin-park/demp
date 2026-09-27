package com.inhatc.demp.domain;

import lombok.Builder;
import jdk.jfr.Timestamp;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
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

    @OneToMany(mappedBy = "question", cascade = CascadeType.REMOVE, orphanRemoval = true)
    private List<Answer> answers = new ArrayList<>();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionHashtag> questionHashtags = new ArrayList<>();

    //연관 관계 편의 메소드
    public void assignMember(Member member) {
        if (this.member == member) return;
        if (this.member != null) this.member.getQuestions().remove(this);
        this.member = member;
        if (member != null) member.getQuestions().add(this);
    }

    private String title;
    @Lob
    private String content;

    private int hits;
    private int recommend;
    private int dislike;

    @Timestamp
    private LocalDateTime createdDate = LocalDateTime.now();

    public void addHashtag(Hashtag hashtag) {
        QuestionHashtag relation = new QuestionHashtag();
        relation.setQuestion(this);
        questionHashtags.add(relation);
        hashtag.attachRelation(relation);
    }

    public void replaceHashtags(List<Hashtag> hashtags) {
        for (QuestionHashtag relation : new ArrayList<>(questionHashtags)) {
            relation.getHashtag().removeQuestionHashtag(relation);
            questionHashtags.remove(relation);
            relation.setQuestion(null);
        }
        for (Hashtag hashtag : hashtags) {
            addHashtag(hashtag);
        }
    }

    @Builder
    private Question(String title, String content, int hits, int recommend, int dislike) {
        this.title = title;
        this.content = content;
        this.hits = hits;
        this.recommend = recommend;
        this.dislike = dislike;
    }

    public void updateQuestion(String title, String content) {
        this.title = title;
        this.content = content;
    }
}
