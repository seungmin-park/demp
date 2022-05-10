package com.inhatc.demp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnswerForm {

    private String memberEmail;
    private Long questionId;
    private String answerContent;
}
