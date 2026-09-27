package com.inhatc.demp.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.AccessLevel;

import jakarta.persistence.*;

@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Setter(AccessLevel.PACKAGE)
public class QuestionHashtag {

    @Id
    @GeneratedValue(generator = "questionhashtag_legacy_id")
    @SequenceGenerator(name = "questionhashtag_legacy_id", sequenceName = "hibernate_sequence", allocationSize = 1)
    private Long id;

    @JoinColumn(name = "question_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Question question;

    @JoinColumn(name = "hashtag_id")
    @ManyToOne(fetch = FetchType.LAZY)
    private Hashtag hashtag;
}
