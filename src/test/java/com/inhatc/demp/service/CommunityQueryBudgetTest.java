package com.inhatc.demp.service;

import com.inhatc.demp.domain.*;
import com.inhatc.demp.repository.*;
import com.inhatc.demp.repository.question.QuestionRepository;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {"spring.jpa.properties.hibernate.generate_statistics=true",
        "logging.level.org.hibernate.engine.internal.StatisticalLoggingSessionEventListener=OFF"})
class CommunityQueryBudgetTest {
    @Autowired AnswerService service;
    @Autowired QuestionService questionService;
    @Autowired HashtagRepository hashtags;
    @Autowired MemberRepository members;
    @Autowired QuestionRepository questions;
    @Autowired AnswerRepository answers;
    @Autowired EntityManagerFactory entityManagerFactory;

    @AfterEach
    void cleanup() { answers.deleteAllInBatch(); questions.deleteAll(); hashtags.deleteAll(); members.deleteAll(); }

    @Test
    @DisplayName("서로 다른 작성자의 답변을 조회해도 작성자 수만큼 SQL이 증가하지 않는다")
    void loadsAuthorsWithinQueryBudget() {
        Member reader = members.save(Member.builder().username("reader").password("unused").roles(List.of("ROLE_USER")).build());
        Question question = Question.builder().title("질문").content("본문").build();
        question.assignMember(reader); questions.save(question);
        for (int i = 0; i < 4; i++) {
            Member author = members.save(Member.builder().username("author-" + i).password("unused").roles(List.of("ROLE_USER")).build());
            Answer answer = Answer.builder().content("답변-" + i).build();
            answer.assignQuestion(question); answer.assignMember(author); answers.save(answer);
        }
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var result = service.findAnswerPage(question.getId(), reader.getId(), null).content();

        assertThat(result).extracting(item -> item.getUsername()).containsExactlyInAnyOrder("author-0", "author-1", "author-2", "author-3");
        assertThat(result).extracting(item -> item.getMyReaction()).containsOnly(ReactionType.NONE);
        assertThat(statistics.getPrepareStatementCount()).as("답변·작성자 일괄 조회와 내 반응 조회").isLessThanOrEqualTo(3);
    }
    @Test
    @DisplayName("질문 상세의 작성자와 태그는 개수에 비례한 추가 조회 없이 가져온다")
    void loadsQuestionDetailWithinQueryBudget() {
        Member reader = members.save(Member.builder().username("reader").password("unused").roles(List.of("ROLE_USER")).build());
        Question question = Question.builder().title("질문").content("본문").build();
        question.assignMember(reader);
        for (int i = 0; i < 4; i++) question.addHashtag(hashtags.save(Hashtag.builder().tagName("tag-" + i).build()));
        questions.save(question);
        var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        statistics.clear();

        var result = questionService.findById(question.getId(), reader.getId());

        assertThat(result.getUsername()).isEqualTo("reader");
        assertThat(result.getHashtags()).containsExactlyInAnyOrder("tag-0", "tag-1", "tag-2", "tag-3");
        assertThat(result.getMyReaction()).isEqualTo(ReactionType.NONE);
        assertThat(statistics.getPrepareStatementCount()).as("상세·작성자·태그 일괄 조회와 내 반응 조회").isLessThanOrEqualTo(3);
    }
}
