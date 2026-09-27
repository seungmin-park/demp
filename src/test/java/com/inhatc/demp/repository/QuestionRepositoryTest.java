package com.inhatc.demp.repository;

import com.inhatc.demp.domain.Member;
import com.inhatc.demp.domain.Question;
import com.inhatc.demp.repository.question.QuestionRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

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
        Member memberA = memberRepository.save(Member.builder()
                .username("testMemberA")
                .password("password")
                .roles(List.of("ROLE_USER"))
                .build());
        Member memberB = memberRepository.save(Member.builder()
                .username("testMemberB")
                .password("password")
                .roles(List.of("ROLE_USER"))
                .build());
        Question questionA = Question.builder().title("접근 제어자가 헷갈려요").content("CS 내용").hits(11).recommend(23).dislike(1).build();
        questionA.assignMember(memberA);
        Question questionB = Question.builder().title("Java8에서 뭐가 달라진건가요?").content("Java 내용").hits(110).recommend(20).dislike(10).build();
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
