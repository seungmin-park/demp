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
    private ArrayList<String> hashtags = new ArrayList<>();
}
