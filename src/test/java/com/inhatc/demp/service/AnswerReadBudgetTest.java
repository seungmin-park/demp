package com.inhatc.demp.service;

import com.inhatc.demp.domain.Answer;
import com.inhatc.demp.domain.Member;
import com.inhatc.demp.domain.Question;
import com.inhatc.demp.domain.ContentReaction;
import com.inhatc.demp.domain.ReactionType;
import com.inhatc.demp.dto.question.QuestionAnswer;
import com.inhatc.demp.dto.answer.AnswerForm;
import com.inhatc.demp.repository.AnswerRepository;
import com.inhatc.demp.repository.MemberRepository;
import com.inhatc.demp.repository.ContentReactionRepository;
import com.inhatc.demp.repository.question.QuestionRepository;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.Arrays;
import java.util.stream.IntStream;
import org.assertj.core.api.SoftAssertions;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"})
class AnswerReadBudgetTest {
    @Autowired AnswerService service;
    @Autowired AnswerRepository answers;
    @Autowired MemberRepository members;
    @Autowired QuestionRepository questions;
    @Autowired EntityManagerFactory entityManagerFactory;
    @Autowired ContentReactionRepository reactions;
    @Autowired ContentReactionService reactionService;

    @AfterEach
    void cleanup() { reactions.deleteAllInBatch(); answers.deleteAllInBatch(); questions.deleteAll(); members.deleteAll(); }

    @Test
    @DisplayName("새 답변 저장은 기존 답변 행과 부모 답변 컬렉션을 읽지 않는다")
    void doesNotLoadExistingAnswersWhenCreating() {
        Member member = members.save(Member.builder().username("budget-writer").password("unused").roles(List.of("ROLE_USER")).build());
        Question question = Question.builder().title("질문").content("본문").build();
        question.assignMember(member);
        questions.save(question);
        for (int index = 0; index < 3; index++) {
            Answer existing = Answer.builder().content("기존-" + index).build();
            existing.assignMember(member);
            existing.assignQuestion(question);
            answers.save(existing);
        }
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        service.createAnswer(member.getId(), new AnswerForm("forged", question.getId(), "새 답변"));

        assertThat(statistics.getEntityStatistics(Answer.class.getName()).getLoadCount()).as("기존 답변 로딩").isZero();
        assertThat(statistics.getCollectionStatistics(Member.class.getName() + ".answers").getLoadCount()).isZero();
        assertThat(statistics.getCollectionStatistics(Question.class.getName() + ".answers").getLoadCount()).isZero();
        assertThat(answers.findByQuestion_Id(question.getId())).extracting(Answer::getContent)
                .containsExactlyInAnyOrder("기존-0", "기존-1", "기존-2", "새 답변");
    }

    @Test
    @DisplayName("천 개 답변의 페이지는 본문 21행과 반환할 답변의 반응만 읽는다")
    void readsOnlyPageReactions() {
        Member member = members.save(Member.builder().username("budget-reader").password("unused").roles(List.of("ROLE_USER")).build());
        Question question = Question.builder().title("질문").content("본문").build();
        question.assignMember(member);
        questions.save(question);
        List<Answer> saved = answers.saveAll(IntStream.range(0, 1000)
                .mapToObj(i -> Answer.createFor(member, question, "본문-" + i)).toList());
        saved.reversed().subList(0, 25).forEach(answer ->
                reactionService.setAnswerReaction(member.getId(), answer.getId(), ReactionType.DISLIKE));
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var page = service.findAnswerPage(question.getId(), member.getId(), null);

        SoftAssertions.assertSoftly(assertions -> {
            assertions.assertThat(page.content()).hasSize(20);
            assertions.assertThat(page.content()).extracting(QuestionAnswer::getAnswerId)
                    .containsExactlyElementsOf(saved.reversed().subList(0, 20).stream().map(Answer::getId).toList());
            assertions.assertThat(page.content()).extracting(QuestionAnswer::getMyReaction).containsOnly(ReactionType.DISLIKE);
            assertions.assertThat(statistics.getEntityStatistics(Answer.class.getName()).getLoadCount()).isZero();
            assertions.assertThat(statistics.getEntityStatistics(ContentReaction.class.getName()).getLoadCount()).isEqualTo(20);
            assertions.assertThat(statistics.getCollectionStatistics(Member.class.getName() + ".roles").getLoadCount()).isZero();
            assertions.assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
            assertions.assertThat(Arrays.stream(statistics.getQueries())
                    .mapToLong(query -> statistics.getQueryStatistics(query).getExecutionRowCount()).max().orElse(0L))
                    .as("서비스가 실제 조회한 쿼리의 최대 행 수").isBetween(1L, 21L);
        });
    }
}
