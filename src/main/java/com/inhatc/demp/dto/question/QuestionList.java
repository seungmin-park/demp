package com.inhatc.demp.dto.question;

import com.inhatc.demp.domain.Question;
import lombok.Data;

@Data
public class QuestionList {

    private Long id;
    private String title;
    private int hits;
    private int recomend;

    public QuestionList(Question question) {
        this.id = question.getId();
        this.title = question.getTitle();
        this.hits = question.getHits();
        this.recomend = question.getRecomend();
    }
}
