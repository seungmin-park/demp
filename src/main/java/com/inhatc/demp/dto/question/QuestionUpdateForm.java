package com.inhatc.demp.dto.question;

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

    private Long questionId;
    private String title;
    private String content;
    private ArrayList<String> hashtags = new ArrayList<>();
}
