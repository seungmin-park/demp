package com.inhatc.demp.service;

import com.inhatc.demp.domain.*;
import com.inhatc.demp.dto.answer.*;
import com.inhatc.demp.dto.question.QuestionAnswer;
import com.inhatc.demp.error.ResourceNotFoundException;
import com.inhatc.demp.repository.*;
import com.inhatc.demp.repository.question.QuestionRepository;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class AnswerServiceTest {
    @Autowired AnswerService service;
    @Autowired MemberRepository members;
    @Autowired QuestionRepository questions;
    @Autowired AnswerRepository answers;
    @AfterEach
    void cleanup() {
        answers.deleteAllInBatch(); questions.deleteAll(); members.deleteAll();
    }
    @Test
    @DisplayName("회원이 없으면 답변을 저장하지 않고 기존 답변을 유지한다")
    void rejectsMissingAuthor() {
        Member member = members.save(Member.builder().username("answer-member").password("hash").roles(List.of("ROLE_USER")).build());
        Question question = Question.builder().title("질문").content("본문").hits(0).recommend(0).dislike(0).build();
        question.assignMember(member); questions.save(question);

        assertThatThrownBy(() -> service.createAnswerAndList(-1L,
                new AnswerForm("missing", question.getId(), "답변")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(answers.findAll()).isEmpty();
    }

    @Test
    @DisplayName("답변 저장 결과는 같은 질문의 기존 답변과 새 답변을 반환한다")
    void returnsAnswersAfterSave() {
        Member member = members.save(Member.builder().username("answer-member").password("hash").roles(List.of("ROLE_USER")).build());
        Question question = Question.builder().title("질문").content("본문").hits(0).recommend(0).dislike(0).build();
        question.assignMember(member); questions.save(question);
        Answer existing = Answer.builder().content("기존").recommend(2).dislike(1).build();
        existing.assignMember(member); existing.assignQuestion(question); answers.save(existing);

        List<QuestionAnswer> result = service.createAnswerAndList(member.getId(),
                new AnswerForm(member.getUsername(), question.getId(), "새 답변"));

        assertThat(result).extracting(QuestionAnswer::getContent)
                .containsExactlyInAnyOrder("기존", "새 답변");
        assertThat(answers.findByQuestion_Id(question.getId())).extracting(Answer::getContent)
                .containsExactlyInAnyOrder("기존", "새 답변");
    }

    @Test
    @DisplayName("질문이 없으면 답변을 저장하지 않는다")
    void rejectsMissingQuestion() {
        Member member = members.save(Member.builder().username("answer-member").password("hash").roles(List.of("ROLE_USER")).build());

        assertThatThrownBy(() -> service.createAnswerAndList(member.getId(),
                new AnswerForm(member.getUsername(), -1L, "답변")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThat(answers.findAll()).isEmpty();
    }
    @Test
    @DisplayName("답변 작성자는 인증 회원이며 정제된 내용을 실제 저장한다")
    void savesSanitizedAnswerWithAuthenticatedAuthor() {
        Member actor = members.save(Member.builder().username("answer-actor").password("hash").roles(List.of("ROLE_USER")).build());
        Question question = Question.builder().title("title").content("body").hits(0).recommend(0).dislike(0).build();
        question.assignMember(actor); questions.save(question);

        List<QuestionAnswer> response = service.createAnswerAndList(actor.getId(), new AnswerForm("forged", question.getId(), "<b>safe</b><script>bad()</script>"));

        assertThat(response).extracting(QuestionAnswer::getUsername).containsExactly("answer-actor");
        assertThat(answers.findByQuestion_Id(question.getId())).extracting(Answer::getContent).containsExactly("<b>safe</b>");
    }
    @Test
    @DisplayName("답변 수정은 정제된 내용을 커밋하여 별도 조회에도 반영한다")
    void commitsSanitizedUpdate() {
        Member actor = members.save(Member.builder().username("answer-actor").password("hash").roles(List.of("ROLE_USER")).build());
        Question question = Question.builder().title("title").content("body").hits(0).recommend(0).dislike(0).build();
        question.assignMember(actor); questions.save(question);
        Answer answer = Answer.builder().content("original").recommend(0).dislike(0).build();
        answer.assignMember(actor); answer.assignQuestion(question); answers.save(answer);

        service.update(actor.getId(), new UpdateAnswerForm(answer.getId(), "<p onclick='bad()'>changed</p>"));

        assertThat(answers.findById(answer.getId()).orElseThrow().getContent()).isEqualTo("<p>changed</p>");
    }
    @Test
    @DisplayName("다른 회원의 답변 수정은 거절되고 저장된 내용은 유지된다")
    void rejectsOtherMembersUpdateWithoutChangingAnswer() {
        Member owner = members.save(Member.builder().username("answer-owner").password("hash").roles(List.of("ROLE_USER")).build());
        Member other = members.save(Member.builder().username("answer-other").password("hash").roles(List.of("ROLE_USER")).build());
        Question question = Question.builder().title("title").content("body").hits(0).recommend(0).dislike(0).build();
        question.assignMember(owner); questions.save(question);
        Answer answer = Answer.builder().content("original").recommend(0).dislike(0).build();
        answer.assignMember(owner); answer.assignQuestion(question); answers.save(answer);

        assertThatThrownBy(() -> service.update(other.getId(), new UpdateAnswerForm(answer.getId(), "changed")))
                .isInstanceOf(AccessDeniedException.class);
        assertThat(answers.findById(answer.getId()).orElseThrow().getContent()).isEqualTo("original");
    }
    @Test
    @DisplayName("없는 답변의 수정과 삭제는 자원 없음 예외를 반환한다")
    void rejectsMissingAnswer() {
        assertThatThrownBy(() -> service.update(1L, new UpdateAnswerForm(-1L, "body")))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.delete(1L, -1L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
