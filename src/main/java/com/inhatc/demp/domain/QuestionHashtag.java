package com.inhatc.demp.domain;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;

@Entity
@Getter
@Setter
public class QuestionHashtag {

    @Id
    @GeneratedValue
    private Long id;

    @JoinColumn(name = "question_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Question question;

    @JoinColumn(name = "hashtag_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Hashtag hashtag;
}
