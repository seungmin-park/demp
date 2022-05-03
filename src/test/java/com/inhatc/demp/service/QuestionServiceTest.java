package com.inhatc.demp.service;

import com.inhatc.demp.domain.Question;
import com.inhatc.demp.dto.question.QuestionForm;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@SpringBootTest
class QuestionServiceTest {

    @Autowired
    QuestionService questionService;

    @Test
    @DisplayName("saveQuestion")
    void saveQuestion() throws Exception {
        //given
        QuestionForm questionForm = new QuestionForm("제목테스트", "내용테스트", new ArrayList<>(Arrays.asList("java", "jpa")));
        //when
        questionService.join(questionForm);
        List<Question> result = questionService.findAll();
        //then
        assertThat(result.size()).isEqualTo(3);
        assertThat(result.get(2).getTitle()).isEqualTo(questionForm.getTitle());
        assertThat(result.get(2).getContent()).isEqualTo(questionForm.getContent());
    }

    @Test
    @DisplayName("findALlByHashtag")
    void findALlByHashtag() throws Exception {
        //given
        QuestionForm questionForm = new QuestionForm("제목테스트", "내용테스트", new ArrayList<>(Arrays.asList("java", "jpa")));
        //when
        questionService.join(questionForm);
        //then
        ArrayList<String> condition = new ArrayList<>(Arrays.asList("java"));
        List<Question> result = questionService.findAllByHashtags(condition);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result).extracting("title").containsExactly("제목테스트");
        assertThat(result).extracting("content").containsExactly("내용테스트");
    }

    @Test
    @DisplayName("findAllHashtags")
    void findAllHashtags() throws Exception {
        //given
        QuestionForm questionForm1 = new QuestionForm("제목테스트1", "내용테스트1", new ArrayList<>(Arrays.asList("java", "jpa")));
        QuestionForm questionForm2 = new QuestionForm("제목테스트2", "내용테스트2", new ArrayList<>(Arrays.asList("spring", "jpa")));
        QuestionForm questionForm3 = new QuestionForm("제목테스트3", "내용테스트3", new ArrayList<>(Arrays.asList("html", "css")));
        //when
        questionService.join(questionForm1);
        questionService.join(questionForm2);
        questionService.join(questionForm3);

        List<String> result = questionService.findAllHashtags();
        //then
        assertThat(result.size()).isEqualTo(5);
        assertThat(result).contains("java", "jpa", "spring", "html", "css");
    }
}