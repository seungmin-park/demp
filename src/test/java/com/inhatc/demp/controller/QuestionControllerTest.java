package com.inhatc.demp.controller;

import tools.jackson.databind.ObjectMapper;
import com.inhatc.demp.config.SecurityConfiguration;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.controller.ExController;
import com.inhatc.demp.dto.question.QuestionForm;
import com.inhatc.demp.dto.question.QuestionList;
import com.inhatc.demp.dto.question.QuestionSearchCondition;
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
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import com.inhatc.demp.support.WithMember;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@MockitoBean(types = JwtTokenProvider.class)
@WebMvcTest(QuestionController.class)
@ContextConfiguration(classes = {QuestionController.class, ExController.class, SecurityConfiguration.class})
@WithMember
class QuestionControllerTest {
    @MockitoBean
    private QuestionService questionService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("상세 조회는 기본적으로 조회수를 집계하는 유스케이스를 호출한다")
    void recordsViewByDefault() throws Exception {
        mockMvc.perform(get("/api/question/detail/51")).andExpect(status().isOk());
        verify(questionService).recordViewAndGetDetail(51L, 41L);
        verify(questionService, never()).findById(51L, 41L);
    }

    @Test
    @DisplayName("편집 상세 조회는 집계하지 않는 순수 조회를 호출한다")
    void editingReadDoesNotRecordView() throws Exception {
        mockMvc.perform(get("/api/question/detail/51").param("recordView", "false"))
                .andExpect(status().isOk());
        verify(questionService).findById(51L, 41L);
        verify(questionService, never()).recordViewAndGetDetail(51L, 41L);
    }

    @Test
    @DisplayName("잘못된 조회수 집계 조건은 서비스 호출 전에 400으로 거절한다")
    void rejectsInvalidRecordView() throws Exception {
        mockMvc.perform(get("/api/question/detail/51").param("recordView", "invalid"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(questionService);
    }

    @Test
    @DisplayName("질문 목록은 내용과 마지막 여부 및 페이지 번호를 반환한다")
    void returnsQuestionSlice() throws Exception {
        QuestionList question = new QuestionList();
        ReflectionTestUtils.setField(question, "id", 51L);
        ReflectionTestUtils.setField(question, "title", "페이지 질문");
        when(questionService.findSliceBySearchCondition(any(), eq(PageRequest.of(0, 2))))
                .thenReturn(new SliceImpl<>(List.of(question), PageRequest.of(0, 2), false));

        mockMvc.perform(get("/api/question").param("page", "0").param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(51))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.number").value(0));
    }

    @ParameterizedTest
    @DisplayName("질문 페이지 크기는 1부터 100까지만 허용한다")
    @ValueSource(strings = {"0", "-1", "101"})
    void rejectsInvalidQuestionPageSize(String size) throws Exception {
        mockMvc.perform(get("/api/question").param("size", size))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(400));
        verifyNoInteractions(questionService);
    }

    @Test
    @DisplayName("음수 질문 페이지 번호를 거절한다")
    void rejectsNegativeQuestionPage() throws Exception {
        mockMvc.perform(get("/api/question").param("page", "-1"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(questionService);
    }

    @Test
    @DisplayName("프런트 Axios 배열 쿼리의 태그를 검색 조건에 바인딩한다")
    void bindsArrayStyleHashtagQuery() throws Exception {
        when(questionService.findSliceBySearchCondition(any(), any()))
                .thenReturn(new SliceImpl<>(List.of(), PageRequest.of(0, 20), false));

        mockMvc.perform(get("/api/question").param("hashtags[]", "JAVA", "SPRING"))
                .andExpect(status().isOk());

        ArgumentCaptor<QuestionSearchCondition> condition = ArgumentCaptor.forClass(QuestionSearchCondition.class);
        verify(questionService).findSliceBySearchCondition(condition.capture(), eq(PageRequest.of(0, 20)));
        assertThat(condition.getValue().getHashtags()).containsExactly("JAVA", "SPRING");
    }

    @Test
    @DisplayName("없는 질문의 상세 조회는 공통 오류 본문과 404를 반환한다")
    void missingQuestionDetail() throws Exception {
        when(questionService.recordViewAndGetDetail(999L, 41L)).thenThrow(new com.inhatc.demp.error.ResourceNotFoundException());

        mockMvc.perform(get("/api/question/detail/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value(404))
                .andExpect(jsonPath("$.errorMessage").value("Resource not found"));
    }

    @Test
    @DisplayName("깨진 JSON 요청은 안전한 400으로 거절하고 서비스를 호출하지 않는다")
    void rejectsMalformedJson() throws Exception {
        mockMvc.perform(patch("/api/question/update")
                        .contentType(MediaType.APPLICATION_JSON).content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(400))
                .andExpect(jsonPath("$.errorMessage").value("Invalid request"));
        verifyNoInteractions(questionService);
    }

    @Test
    @DisplayName("질문 삭제 요청의 ID를 서비스에 전달한다")
    void deleteQuestion() throws Exception {
        mockMvc.perform(delete("/api/question/delete").param("questionId", "41"))
                .andExpect(status().isOk());
        verify(questionService).deleteQuestion(41L, 41L);
    }

    @Test
    @DisplayName("질문 수정 요청을 DTO로 변환해 서비스에 전달한다")
    void updateQuestion() throws Exception {
        mockMvc.perform(patch("/api/question/update")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new QuestionUpdateForm(41L, "수정 제목", "수정 내용", new ArrayList<>(List.of("java", "jpa"))))))
                .andExpect(status().isOk());
        ArgumentCaptor<QuestionUpdateForm> form = ArgumentCaptor.forClass(QuestionUpdateForm.class);
        verify(questionService).updateQuestion(eq(41L), form.capture());
        assertThat(form.getValue().getQuestionId()).isEqualTo(41L);
        assertThat(form.getValue().getTitle()).isEqualTo("수정 제목");
        assertThat(form.getValue().getContent()).isEqualTo("수정 내용");
        assertThat(form.getValue().getHashtags()).containsExactly("java", "jpa");
    }

    @Test
    @DisplayName("질문 수정 중 발생한 현재 예외가 전파된다")
    void updateQuestionFail() throws Exception {
        doThrow(new com.inhatc.demp.error.ResourceNotFoundException()).when(questionService).updateQuestion(eq(41L), any());
        QuestionUpdateForm request = new QuestionUpdateForm(200L, "title", "content", new ArrayList<>());
        mockMvc.perform(patch("/api/question/update").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.errorCode").value(404));
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
        verify(questionService).createQuestion(eq(41L), form.capture());
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
