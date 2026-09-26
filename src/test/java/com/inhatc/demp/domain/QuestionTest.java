package com.inhatc.demp.domain;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionTest {
    @Test
    @DisplayName("질문 작성자를 바꾸면 이전 회원 목록에서 질문을 제거한다")
    void movesQuestionBetweenMembers() {
        Member first = new Member("first", "secret", List.of("ROLE_USER"));
        Member second = new Member("second", "secret", List.of("ROLE_USER"));
        Question question = new Question("제목", "본문", 0, 0, 0);
        question.assignMember(first);

        question.assignMember(second);

        assertThat(question.getMember()).isSameAs(second);
        assertThat(first.getQuestions()).isEmpty();
        assertThat(second.getQuestions()).containsExactly(question);
    }

    @Test
    @DisplayName("태그 교체는 이전 양방향 연결을 끊고 새 연결을 맺는다")
    void replacesBothSidesOfHashtagRelation() {
        Question question = new Question("질문", "내용", 0, 0, 0);
        Hashtag java = new Hashtag("JAVA");
        Hashtag spring = new Hashtag("SPRING");
        question.replaceHashtags(List.of(java));
        QuestionHashtag oldRelation = question.getQuestionHashtags().get(0);

        question.replaceHashtags(List.of(spring));

        assertThat(question.getQuestionHashtags()).hasSize(1);
        QuestionHashtag newRelation = question.getQuestionHashtags().get(0);
        assertThat(newRelation.getQuestion()).isSameAs(question);
        assertThat(newRelation.getHashtag()).isSameAs(spring);
        assertThat(spring.getQuestionHashtags()).containsExactly(newRelation);
        assertThat(java.getQuestionHashtags()).isEmpty();
        assertThat(oldRelation.getQuestion()).isNull();
        assertThat(oldRelation.getHashtag()).isNull();
    }
}
