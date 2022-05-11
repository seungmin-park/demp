package com.inhatc.demp.repository;

import com.inhatc.demp.domain.Question;
import com.inhatc.demp.repository.question.QuestionRepository;
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
        assertThat(questions).extracting("title").containsExactly("접근 제어자가 헷갈려요", "Java8에서 뭐가 달라진건가요?");
        assertThat(questions).extracting("hits").containsExactly(11, 110);
        assertThat(questions).extracting("recomend").containsExactly(23, 20);
        assertThat(questions).extracting("member.username").containsExactly("testMemberA","testMemberB");
    }
}