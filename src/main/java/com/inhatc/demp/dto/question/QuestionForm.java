package com.inhatc.demp.dto.question;

import lombok.*;

import java.util.ArrayList;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class QuestionForm {

    private String title;
    private String content;
    private String username;
    private ArrayList<String> hashtags = new ArrayList<>();

    public QuestionForm(String title, String content, ArrayList<String> hashtags) {
        this.title = title;
        this.content = content;
        this.hashtags.addAll(hashtags);
    }
}
