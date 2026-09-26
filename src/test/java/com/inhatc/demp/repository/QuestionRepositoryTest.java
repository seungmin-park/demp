package com.inhatc.demp.repository;

import com.inhatc.demp.domain.Member;
import com.inhatc.demp.domain.Question;
import com.inhatc.demp.repository.question.QuestionRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class QuestionRepositoryTest {

    @Autowired
    MemberRepository memberRepository;

    @Autowired
    QuestionRepository questionRepository;

    @Test
    @DisplayName("저장한 질문 목록을 조회한다")
    void findsSavedQuestions() {
        //given
        Member memberA = memberRepository.save(new Member("testMemberA", "password", List.of("ROLE_USER")));
        Member memberB = memberRepository.save(new Member("testMemberB", "password", List.of("ROLE_USER")));
        Question questionA = new Question("접근 제어자가 헷갈려요", "CS 내용", 11, 23, 1);
        questionA.assignMember(memberA);
        Question questionB = new Question("Java8에서 뭐가 달라진건가요?", "Java 내용", 110, 20, 10);
        questionB.assignMember(memberB);
        questionRepository.saveAll(List.of(questionA, questionB));
        //when
        List<Question> questions = questionRepository.findAll();

        //then
        assertThat(questions).extracting(Question::getTitle, Question::getHits, Question::getRecommend,
                        question -> question.getMember().getUsername())
                .containsExactlyInAnyOrder(
                        org.assertj.core.api.Assertions.tuple("접근 제어자가 헷갈려요", 11, 23, "testMemberA"),
                        org.assertj.core.api.Assertions.tuple("Java8에서 뭐가 달라진건가요?", 110, 20, "testMemberB"));
    }
}
