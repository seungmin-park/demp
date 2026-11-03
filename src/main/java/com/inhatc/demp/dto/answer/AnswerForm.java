package com.inhatc.demp.dto.answer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnswerForm {

    private String username;
    @NotNull
    private Long questionId;
    @NotBlank @Size(max = AnswerLimits.CONTENT_LENGTH)
    private String answerContent;
}
