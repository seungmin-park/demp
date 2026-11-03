package com.inhatc.demp.dto.question;

/** Request limits for question writes; TITLE_LENGTH matches the question.title column. */
public final class QuestionLimits {
    public static final int TITLE_LENGTH = 255;
    public static final int CONTENT_LENGTH = 50_000;
    public static final int HASHTAG_COUNT = 10;
    public static final int HASHTAG_LENGTH = 30;

    private QuestionLimits() {}
}
