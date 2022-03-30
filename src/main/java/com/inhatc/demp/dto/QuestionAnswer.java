package com.inhatc.demp.dto;

import com.inhatc.demp.domain.Answer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionAnswer {

    private String username;
    private String content;
    private int recomend;
    private int dislike;

    public QuestionAnswer(Answer answer) {
        this.username = answer.getMember().getUsername();
        this.content = answer.getContent();
        this.recomend = answer.getRecomend();
        this.dislike = answer.getDislike();
    }
}
