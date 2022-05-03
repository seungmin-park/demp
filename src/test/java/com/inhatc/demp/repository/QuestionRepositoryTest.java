package com.inhatc.demp.repository;

import com.inhatc.demp.domain.Question;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
class QuestionRepositoryTest {

    @Autowired
    QuestionRepository questionRepository;

    @Test
    @DisplayName("questionList")
    void questionList() throws Exception {
        //given
        List<Question> questions = questionRepository.findAll();
        //when

        //then
        assertThat(questions.size()).isEqualTo(2);
        assertThat(questions.get(0).getTitle()).isEqualTo("접근 제어자가 헷갈려요");
        assertThat(questions.get(0).getHits()).isEqualTo(11);
        assertThat(questions.get(0).getRecomend()).isEqualTo(23);
        assertThat(questions.get(0).getMember().getUsername()).isEqualTo("testMemberA");
        assertThat(questions.get(0).getAnswers().get(0).getMember().getUsername()).isEqualTo("testMemberB");

        assertThat(questions.get(1).getTitle()).isEqualTo("Java8에서 뭐가 달라진건가요?");
        assertThat(questions.get(1).getHits()).isEqualTo(110);
        assertThat(questions.get(1).getRecomend()).isEqualTo(20);
        assertThat(questions.get(1).getMember().getUsername()).isEqualTo("testMemberB");
        assertThat(questions.get(1).getAnswers().get(0).getMember().getUsername()).isEqualTo("testMemberA");
    }
}