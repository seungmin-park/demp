package com.inhatc.demp.domain;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AnswerTest {
    @Test
    @DisplayName("답변 질문을 바꾸면 이전 질문의 답변 목록에서 제거한다")
    void movesAnswerBetweenQuestions() {
        Question first = Question.builder().title("첫 질문").content("본문").hits(0).recommend(0).dislike(0).build();
        Question second = Question.builder().title("둘째 질문").content("본문").hits(0).recommend(0).dislike(0).build();
        Answer answer = Answer.builder().content("답변").recommend(0).dislike(0).build();
        answer.assignQuestion(first);

        answer.assignQuestion(second);

        assertThat(answer.getQuestion()).isSameAs(second);
        assertThat(first.getAnswers()).isEmpty();
        assertThat(second.getAnswers()).containsExactly(answer);
    }

    @Test
    @DisplayName("답변 작성자를 바꾸면 이전 회원의 답변 목록에서 제거한다")
    void movesAnswerBetweenMembers() {
        Member first = Member.builder().username("first").password("secret").roles(List.of("ROLE_USER")).build();
        Member second = Member.builder().username("second").password("secret").roles(List.of("ROLE_USER")).build();
        Answer answer = Answer.builder().content("답변").recommend(0).dislike(0).build();
        answer.assignMember(first);

        answer.assignMember(second);

        assertThat(answer.getMember()).isSameAs(second);
        assertThat(first.getAnswers()).isEmpty();
        assertThat(second.getAnswers()).containsExactly(answer);
    }
}
