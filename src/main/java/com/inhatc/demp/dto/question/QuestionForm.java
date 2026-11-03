package com.inhatc.demp.dto.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.*;

import java.util.ArrayList;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class QuestionForm {

    @NotBlank @Size(max = QuestionLimits.TITLE_LENGTH)
    private String title;
    @NotBlank @Size(max = QuestionLimits.CONTENT_LENGTH)
    private String content;
    private String username;
    @Size(max = QuestionLimits.HASHTAG_COUNT)
    private ArrayList<@Size(max = QuestionLimits.HASHTAG_LENGTH) String> hashtags = new ArrayList<>();

    public QuestionForm(String title, String content, ArrayList<String> hashtags) {
        this.title = title;
        this.content = content;
        this.hashtags.addAll(hashtags);
    }
}
