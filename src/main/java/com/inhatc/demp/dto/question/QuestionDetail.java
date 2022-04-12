package com.inhatc.demp.dto.question;

import com.inhatc.demp.domain.Question;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionDetail {

    private Long id;
    private String title;
    private String content;
    private int hits;
    private int recomend;
    private int dislike;
    private String username;

    public QuestionDetail(Question question) {
        this.id = question.getId();
        this.title = question.getTitle();
        this.content = question.getContent();
        this.hits = question.getHits();
        this.recomend = question.getRecomend();
        this.dislike = question.getDislike();
        this.username = question.getMember().getUsername();
    }
}
