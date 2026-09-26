package com.inhatc.demp.dto.question;

import java.util.List;
import lombok.Getter;
import org.springframework.data.domain.Slice;

@Getter
public class QuestionSliceResponse {
    private final List<QuestionList> content;
    private final boolean last;
    private final int number;

    public QuestionSliceResponse(Slice<QuestionList> slice) {
        this.content = slice.getContent();
        this.last = slice.isLast();
        this.number = slice.getNumber();
    }
}
