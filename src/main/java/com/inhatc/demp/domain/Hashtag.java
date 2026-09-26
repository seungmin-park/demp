package com.inhatc.demp.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Hashtag {

    @Id
    @GeneratedValue
    @Column(name = "hashtag_id")
    private Long id;

    @OneToMany(mappedBy = "hashtag", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionHashtag> questionHashtags = new ArrayList<>();

    @Column(nullable = false, unique = true)
    private String tagName;

    void attachRelation(QuestionHashtag relation) {
        questionHashtags.add(relation);
        relation.setHashtag(this);
    }

    void removeQuestionHashtag(QuestionHashtag questionHashtag) {
        questionHashtags.remove(questionHashtag);
        questionHashtag.setHashtag(null);
    }

    public Hashtag(String tagName) {
        this.tagName = tagName;
    }
}
