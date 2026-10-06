package com.inhatc.demp.dto.answer;

import com.inhatc.demp.dto.question.QuestionAnswer;
import java.util.List;

public record AnswerPage(List<QuestionAnswer> content, String nextCursor, boolean hasNext) {}
