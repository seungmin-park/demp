package com.inhatc.demp.dto.question;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class QuestionSearchCondition {

    private String title;
    private String content;
    private List<String> hashtags = new ArrayList<>();
}
