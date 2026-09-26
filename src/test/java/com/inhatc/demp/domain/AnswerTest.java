package com.inhatc.demp.domain;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AnswerTest {
    @Test
    @DisplayName("답변 질문을 바꾸면 이전 질문의 답변 목록에서 제거한다")
    void movesAnswerBetweenQuestions() {
        Question first = new Question("첫 질문", "본문", 0, 0, 0);
        Question second = new Question("둘째 질문", "본문", 0, 0, 0);
        Answer answer = new Answer("답변", 0, 0);
        answer.assignQuestion(first);

        answer.assignQuestion(second);

        assertThat(answer.getQuestion()).isSameAs(second);
        assertThat(first.getAnswers()).isEmpty();
        assertThat(second.getAnswers()).containsExactly(answer);
    }

    @Test
    @DisplayName("답변 작성자를 바꾸면 이전 회원의 답변 목록에서 제거한다")
    void movesAnswerBetweenMembers() {
        Member first = new Member("first", "secret", List.of("ROLE_USER"));
        Member second = new Member("second", "secret", List.of("ROLE_USER"));
        Answer answer = new Answer("답변", 0, 0);
        answer.assignMember(first);

        answer.assignMember(second);

        assertThat(answer.getMember()).isSameAs(second);
        assertThat(first.getAnswers()).isEmpty();
        assertThat(second.getAnswers()).containsExactly(answer);
    }
}
