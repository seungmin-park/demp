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

    private String tagName;

    public void addQuestionHashtag(QuestionHashtag questionHashtag) {
        questionHashtags.add(questionHashtag);
        questionHashtag.setHashtag(this);
    }

    public Hashtag(String tagName) {
        this.tagName = tagName;
    }
}
