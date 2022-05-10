package com.inhatc.demp.repository;

import com.inhatc.demp.domain.Answer;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Transactional
@SpringBootTest
class AnswerRepositoryTest {

    @Autowired
    AnswerRepository answerRepository;

    @Test
    @DisplayName("selectAnswer")
    void selectAnswer() throws Exception {
        //given
        List<Answer> answers = answerRepository.findByQuestion_Id(1L);
        //when

        //then
        Assertions.assertThat(answers.size()).isEqualTo(1);
        Assertions.assertThat(answers.get(0).getContent()).isEqualTo("질문\\n 답변\\n 테스트");
        Assertions.assertThat(answers.get(0).getRecomend()).isEqualTo(22);
        Assertions.assertThat(answers.get(0).getDislike()).isEqualTo(11);
    }
}