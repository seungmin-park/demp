package com.inhatc.demp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inhatc.demp.config.SecurityConfiguration;
import com.inhatc.demp.config.WebConfig;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.controller.ExController;
import com.inhatc.demp.dto.question.QuestionForm;
import com.inhatc.demp.dto.question.QuestionUpdateForm;
import com.inhatc.demp.service.QuestionService;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.util.NestedServletException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@MockBean(JwtTokenProvider.class)
@WebMvcTest(QuestionController.class)
@ContextConfiguration(classes = {QuestionController.class, ExController.class, SecurityConfiguration.class, WebConfig.class})
@WithMockUser(roles = "USER")
class QuestionControllerTest {
    @MockBean
    private QuestionService questionService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("질문 삭제 요청의 ID를 서비스에 전달한다")
    void deleteQuestion() throws Exception {
        mockMvc.perform(delete("/api/question/delete").param("questionId", "41"))
                .andExpect(status().isOk());
        verify(questionService).deleteQuestion(41L);
    }

    @Test
    @DisplayName("질문 수정 요청을 DTO로 변환해 서비스에 전달한다")
    void updateQuestion() throws Exception {
        mockMvc.perform(patch("/api/question/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new QuestionUpdateForm(41L, "수정 제목", "수정 내용", new ArrayList<>(List.of("java", "jpa"))))))
                .andExpect(status().isOk());
        ArgumentCaptor<QuestionUpdateForm> form = ArgumentCaptor.forClass(QuestionUpdateForm.class);
        verify(questionService).updateQuestion(form.capture());
        assertThat(form.getValue().getQuestionId()).isEqualTo(41L);
        assertThat(form.getValue().getTitle()).isEqualTo("수정 제목");
        assertThat(form.getValue().getContent()).isEqualTo("수정 내용");
        assertThat(form.getValue().getHashtags()).containsExactly("java", "jpa");
    }

    @Test
    @DisplayName("질문 수정 중 발생한 현재 예외가 전파된다")
    void updateQuestionFail() {
        doThrow(new NoSuchElementException("데이터 존재X")).when(questionService).updateQuestion(any());
        assertThatThrownBy(() -> mockMvc.perform(patch("/api/question/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(new QuestionUpdateForm(200L, "수정 제목", "수정 내용", new ArrayList<>(List.of()))))))
                .isInstanceOf(NestedServletException.class)
                .hasRootCauseInstanceOf(NoSuchElementException.class);
    }
    @Test
    @DisplayName("질문 등록 요청의 모든 필드를 서비스로 전달한다")
    void createsQuestion() throws Exception {
        QuestionForm request = new QuestionForm("질문 제목", "질문 내용", "member-a",
                new ArrayList<>(List.of("java", "jpa")));

        mockMvc.perform(post("/api/question/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));

        ArgumentCaptor<QuestionForm> form = ArgumentCaptor.forClass(QuestionForm.class);
        verify(questionService).join(form.capture());
        assertThat(form.getValue()).usingRecursiveComparison().isEqualTo(request);
    }

    @ParameterizedTest
    @DisplayName("숫자로 변환할 수 없는 질문 ID는 서비스를 호출하지 않고 거절한다")
    @ValueSource(strings = {"abc", "9223372036854775808"})
    void rejectsInvalidQuestionId(String questionId) throws Exception {
        mockMvc.perform(delete("/api/question/delete").param("questionId", questionId))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(questionService);
    }

    @Test
    @DisplayName("질문 ID가 없는 삭제 요청은 서비스를 호출하지 않고 거절한다")
    void rejectsMissingQuestionId() throws Exception {
        mockMvc.perform(delete("/api/question/delete"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(questionService);
    }

}
