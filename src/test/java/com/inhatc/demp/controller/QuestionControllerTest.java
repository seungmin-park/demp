package com.inhatc.demp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inhatc.demp.dto.question.QuestionDetail;
import com.inhatc.demp.dto.question.QuestionUpdateForm;
import com.inhatc.demp.service.QuestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

@Transactional
@SpringBootTest
class QuestionControllerTest {

    @Autowired
    QuestionController questionController;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private ObjectMapper objectMapper;
    private MockMvc mockMvc;

    @BeforeEach
    void beforeEach() {
        mockMvc = MockMvcBuilders.standaloneSetup(questionController).build();
    }

    @Test
    @DisplayName("deleteQuestion")
    void deleteQuestion() throws Exception {
        //given
        mockMvc.perform(delete("/api/question/delete").param("questionId","1"));
        //when

        //then
        assertThatThrownBy(() -> questionService.findById(1L))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    @DisplayName("updateQuestion")
    void updateQuestion() throws Exception {
        //given
        QuestionUpdateForm questionUpdateForm = new QuestionUpdateForm(1L, "제목 수정 테스트용 텍스트", "내용 수정 테스트용 텍스트", new ArrayList<>(Arrays.asList("1", "2")));
        String content = objectMapper.writeValueAsString(questionUpdateForm);
        //when
        mockMvc.perform(patch("/api/question/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content));
        //then
        QuestionDetail questionDetail = questionService.findById(questionUpdateForm.getQuestionId());
        List<String> hashtags = questionService.findAllHashtags();

        assertThat(questionDetail.getTitle()).isEqualTo("제목 수정 테스트용 텍스트");
        assertThat(questionDetail.getContent()).isEqualTo("내용 수정 테스트용 텍스트");
        assertThat(hashtags.size()).isEqualTo(6);
        assertThat(hashtags).contains("java", "jpa", "cs", "thread", "1", "2");
    }

    @Test
    @DisplayName("updateQuestionFail")
    void updateQuestionFail() throws Exception {
        //given
        QuestionUpdateForm questionUpdateForm = new QuestionUpdateForm(200L, "제목 수정 테스트용 텍스트", "내용 수정 테스트용 텍스트", new ArrayList<>(Arrays.asList("1", "2")));
        String content = objectMapper.writeValueAsString(questionUpdateForm);
        //when

        //then
        assertThatThrownBy(() -> mockMvc.perform(patch("/api/question/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)));
    }
}