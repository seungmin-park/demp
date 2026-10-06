package com.inhatc.demp.service;

import com.inhatc.demp.domain.Answer;
import com.inhatc.demp.domain.Member;
import com.inhatc.demp.domain.Question;
import com.inhatc.demp.dto.answer.AnswerPage;
import com.inhatc.demp.dto.question.QuestionAnswer;
import com.inhatc.demp.error.ResourceNotFoundException;
import com.inhatc.demp.repository.AnswerRepository;
import com.inhatc.demp.repository.MemberRepository;
import com.inhatc.demp.repository.question.QuestionRepository;
import java.util.List;
import java.util.ArrayList;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class AnswerPaginationTest {
    @Autowired AnswerService service;
    @Autowired AnswerRepository answers;
    @Autowired MemberRepository members;
    @Autowired QuestionRepository questions;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void cleanup() { answers.deleteAllInBatch(); questions.deleteAll(); members.deleteAll(); }

    @Test
    @DisplayName("답변 21개가 있어도 조회 응답은 20개 이하로 제한한다")
    void returnsAtMostTwentyAnswers() {
        Member member = members.save(Member.builder().username("page-reader").password("unused").roles(List.of("ROLE_USER")).build());
        Question question = Question.builder().title("질문").content("본문").build();
        question.assignMember(member);
        questions.save(question);
        answers.saveAll(IntStream.range(0, 21).mapToObj(i -> Answer.createFor(member, question, "답변-" + i)).toList());

        assertThat(service.findAnswerPage(question.getId(), member.getId(), null).content()).hasSize(20);
    }

    @ParameterizedTest
    @DisplayName("빈 결과와 20개 경계의 모든 페이지는 내림차순으로 중복과 누락 없이 읽는다")
    @ValueSource(ints = {0, 20, 21, 40, 41})
    void pagesAllAnswersInDescendingIdOrder(int count) {
        Member member = member();
        Question question = question(member);
        List<Answer> saved = answers.saveAll(IntStream.range(0, count)
                .mapToObj(i -> Answer.createFor(member, question, "답변-" + i)).toList());
        List<Long> expected = saved.reversed().stream().map(Answer::getId).toList();
        List<Long> observed = new ArrayList<>();
        Long before = null;
        int pageCount = 0;
        do {
            AnswerPage page = service.findAnswerPage(question.getId(), member.getId(), before);
            assertThat(page.content()).hasSize(Math.min(20, count - observed.size()));
            assertThat(page.content()).allSatisfy(answer -> assertThat(answer.getUsername()).isEqualTo("page-reader"));
            observed.addAll(page.content().stream().map(QuestionAnswer::getAnswerId).toList());
            assertThat(page.hasNext()).isEqualTo(observed.size() < count);
            if (page.hasNext()) {
                assertThat(page.nextCursor()).isEqualTo(page.content().getLast().getAnswerId().toString());
                before = Long.valueOf(page.nextCursor());
            } else {
                assertThat(page.nextCursor()).isNull();
                break;
            }
            assertThat(++pageCount).isLessThan(4);
        } while (true);
        assertThat(observed).containsExactlyElementsOf(expected);
    }

    @Test
    @DisplayName("커서 답변이 삭제돼도 다음 답변을 읽고 다른 질문은 섞지 않는다")
    void keepsQuestionScopeWhenCursorWasDeleted() {
        Member member = member();
        Question question = question(member);
        List<Answer> saved = answers.saveAll(IntStream.range(0, 22)
                .mapToObj(i -> Answer.createFor(member, question, "답변-" + i)).toList());
        Question other = question(member);
        Answer foreignCursor = answers.save(Answer.createFor(member, other, "다른 질문 답변"));
        Long deletedId = saved.get(2).getId();
        answers.deleteById(deletedId);

        var next = service.findAnswerPage(question.getId(), member.getId(), deletedId);
        assertThat(next.content()).extracting(QuestionAnswer::getAnswerId)
                .containsExactly(saved.get(1).getId(), saved.get(0).getId());
        assertThat(next.hasNext()).isFalse();
        assertThat(next.nextCursor()).isNull();
        assertThat(service.findAnswerPage(question.getId(), member.getId(), foreignCursor.getId()).content())
                .extracting(QuestionAnswer::getAnswerId).doesNotContain(foreignCursor.getId()).hasSize(20);
    }

    @Test
    @DisplayName("음수 local 답변 ID와 Long 경계 커서도 숫자 경계로 조회한다")
    void acceptsNegativeLocalCursor() {
        Member member = member();
        Question question = question(member);
        for (long id : new long[] {-1L, -2L}) {
            jdbc.update("insert into answer(answer_id,member_id,question_id,content,recommend,dislike) values(?,?,?,?,0,0)",
                    id, member.getId(), question.getId(), "local-" + id);
        }
        assertThat(service.findAnswerPage(question.getId(), member.getId(), Long.MAX_VALUE).content())
                .extracting(QuestionAnswer::getAnswerId).containsExactly(-1L, -2L);
        assertThat(service.findAnswerPage(question.getId(), member.getId(), -1L).content())
                .extracting(QuestionAnswer::getAnswerId).containsExactly(-2L);
        assertThat(service.findAnswerPage(question.getId(), member.getId(), Long.MIN_VALUE).content()).isEmpty();
    }

    @Test
    @DisplayName("없는 질문은 빈 페이지로 숨기지 않고 404 계약의 예외를 반환한다")
    void rejectsMissingQuestion() {
        assertThatThrownBy(() -> service.findAnswerPage(-123L, 41L, null)).isInstanceOf(ResourceNotFoundException.class);
    }

    private Member member() {
        return members.save(Member.builder().username("page-reader").password("unused").roles(List.of("ROLE_USER")).build());
    }
    private Question question(Member member) {
        Question question = Question.builder().title("질문").content("본문").build();
        question.assignMember(member);
        return questions.save(question);
    }
}
