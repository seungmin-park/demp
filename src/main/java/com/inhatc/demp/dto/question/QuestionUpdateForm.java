package com.inhatc.demp.dto.question;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QuestionUpdateForm {

    @NotNull
    private Long questionId;
    @NotBlank @Size(max = QuestionLimits.TITLE_LENGTH)
    private String title;
    @NotBlank @Size(max = QuestionLimits.CONTENT_LENGTH)
    private String content;
    @Size(max = QuestionLimits.HASHTAG_COUNT)
    private ArrayList<@Size(max = QuestionLimits.HASHTAG_LENGTH) String> hashtags = new ArrayList<>();
}
