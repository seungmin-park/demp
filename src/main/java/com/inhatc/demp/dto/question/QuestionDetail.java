package com.inhatc.demp.dto.question;

import com.inhatc.demp.domain.Question;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@ToString
public class QuestionDetail {

    private Long id;
    private String title;
    private String content;
    private int hits;
    private int recomend;
    private int dislike;
    private String username;

    private List<String> hashtags = new ArrayList<>();

    public QuestionDetail(Question question) {
        this.id = question.getId();
        this.title = question.getTitle();
        this.content = question.getContent();
        this.hits = question.getHits();
        this.recomend = question.getRecomend();
        this.dislike = question.getDislike();
        this.username = question.getMember().getUsername();
        hashtags.addAll(getTagNames(question));
    }

    private List<String> getTagNames(Question question) {
        return question.getQuestionHashtags().stream().map(qh -> qh.getHashtag().getTagName()).collect(Collectors.toList());
    }
}
