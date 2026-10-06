package com.inhatc.demp.dto.question;

import com.inhatc.demp.domain.Answer;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.ToStringSerializer;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class QuestionAnswer {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long answerId;
    private String username;
    private String content;
    private int recommend;
    private int dislike;
    private com.inhatc.demp.domain.ReactionType myReaction = com.inhatc.demp.domain.ReactionType.NONE;

    public QuestionAnswer(Long answerId, String username, String content, int recommend, int dislike) {
        this.answerId = answerId;
        this.username = username;
        this.content = content;
        this.recommend = recommend;
        this.dislike = dislike;
    }

    public QuestionAnswer(Answer answer) {
        this.answerId = answer.getId();
        this.username = answer.getMember().getUsername();
        this.content = answer.getContent();
        this.recommend = answer.getRecommend();
        this.dislike = answer.getDislike();
    }
}
