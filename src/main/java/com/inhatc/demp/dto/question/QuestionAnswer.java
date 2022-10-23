package com.inhatc.demp.dto.question;

import com.inhatc.demp.domain.Answer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionAnswer {

    private Long answerId;
    private String username;
    private String content;
    private int recommend;
    private int dislike;

    public QuestionAnswer(Answer answer) {
        this.answerId = answer.getId();
        this.username = answer.getMember().getUsername();
        this.content = answer.getContent();
        this.recommend = answer.getRecommend();
        this.dislike = answer.getDislike();
    }
}
