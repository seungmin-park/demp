package com.inhatc.demp.dto.question;

import com.inhatc.demp.domain.Question;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class QuestionList {

    private Long id;
    private String title;
    private int hits;
    private int recomend;
}
