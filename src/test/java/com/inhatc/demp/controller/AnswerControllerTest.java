package com.inhatc.demp.controller;

import tools.jackson.databind.ObjectMapper;
import com.inhatc.demp.config.SecurityConfiguration;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.controller.ExController;
import com.inhatc.demp.domain.Answer;
import com.inhatc.demp.domain.Member;
import com.inhatc.demp.dto.answer.AnswerForm;
import com.inhatc.demp.dto.answer.AnswerPage;
import com.inhatc.demp.dto.answer.UpdateAnswerForm;
import com.inhatc.demp.dto.question.QuestionAnswer;
import com.inhatc.demp.repository.AnswerRepository;
import com.inhatc.demp.service.AnswerService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import com.inhatc.demp.support.WithMember;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@MockitoBean(types = JwtTokenProvider.class)
@WebMvcTest(AnswerController.class)
@ContextConfiguration(classes = {AnswerController.class, ExController.class, SecurityConfiguration.class})
@WithMember
class AnswerControllerTest {
    @MockitoBean
    private AnswerService answerService;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("답변 등록 요청을 서비스에 전달하고 저장 결과를 반환한다")
    void controllerAnswerSave() throws Exception {
        Answer answer = Answer.builder().content("댓글 테스트").recommend(0).dislike(0).build();
        answer.assignMember(Member.builder().username("member-a").password("password").roles(List.of("ROLE_USER")).build());
        when(answerService.createAnswer(eq(41L), any())).thenReturn(new QuestionAnswer(answer));

        mockMvc.perform(post("/api/answer/save").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnswerForm("member-a", 41L, "댓글 테스트"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$.username").value("member-a"))
                .andExpect(jsonPath("$.content").value("댓글 테스트"))
                .andExpect(jsonPath("$.recommend").value(0))
                .andExpect(jsonPath("$.dislike").value(0))
                .andExpect(jsonPath("$.myReaction").value("NONE"));
        ArgumentCaptor<AnswerForm> form = ArgumentCaptor.forClass(AnswerForm.class);
        verify(answerService).createAnswer(eq(41L), form.capture());
        assertThat(form.getValue().getUsername()).isEqualTo("member-a");
        assertThat(form.getValue().getQuestionId()).isEqualTo(41L);
        assertThat(form.getValue().getAnswerContent()).isEqualTo("댓글 테스트");
    }

    @Test
    @DisplayName("답변 수정 요청의 ID와 내용을 서비스에 전달한다")
    void updateAnswer() throws Exception {
        mockMvc.perform(patch("/api/answer/update").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAnswerForm(73L, "수정 댓글"))))
                .andExpect(status().isOk());
        ArgumentCaptor<UpdateAnswerForm> form = ArgumentCaptor.forClass(UpdateAnswerForm.class);
        verify(answerService).update(eq(41L), form.capture());
        assertThat(form.getValue().getAnswerId()).isEqualTo(73L);
        assertThat(form.getValue().getAnswerContent()).isEqualTo("수정 댓글");
    }

    @Test
    @DisplayName("답변 삭제 요청의 ID를 서비스에 전달한다")
    void deleteAnswer() throws Exception {
        mockMvc.perform(delete("/api/answer/delete").param("answerId", "73"))
                .andExpect(status().isOk());
        verify(answerService).delete(41L, 73L);
    }
    @Test
    @DisplayName("답변이 없는 질문의 조회 응답은 마지막 빈 페이지다")
    void returnsEmptyAnswers() throws Exception {
        when(answerService.findAnswerPage(41L, 41L, null)).thenReturn(new AnswerPage(List.of(), null, false));

        mockMvc.perform(get("/api/answer/41"))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"content\":[],\"nextCursor\":null,\"hasNext\":false}", true));
    }

    @ParameterizedTest
    @DisplayName("잘못된 정수와 Long 범위를 넘는 답변 커서는 400으로 거절한다")
    @ValueSource(strings = {"bad", "1.5", "9223372036854775808", "-9223372036854775809", "", " 1", "١"})
    void rejectsMalformedAndOverflowCursor(String cursor) throws Exception {
        mockMvc.perform(get("/api/answer/41").param("before", cursor))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(answerService);
    }

    @ParameterizedTest
    @DisplayName("유효한 Long 커서는 질문과 인증 ID와 함께 서비스에 전달한다")
    @ValueSource(strings = {"-1", "0", "9223372036854775807", "-9223372036854775808"})
    void forwardsCursorAndAuthenticatedActor(String cursor) throws Exception {
        when(answerService.findAnswerPage(41L, 41L, Long.valueOf(cursor)))
                .thenReturn(new AnswerPage(List.of(), null, false));
        mockMvc.perform(get("/api/answer/41").param("before", cursor)).andExpect(status().isOk());
        verify(answerService).findAnswerPage(41L, 41L, Long.valueOf(cursor));
    }

    @Test
    @DisplayName("답변 ID가 없는 삭제 요청은 저장소를 호출하지 않고 거절한다")
    void rejectsMissingAnswerId() throws Exception {
        mockMvc.perform(delete("/api/answer/delete"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(answerService);
    }

    @ParameterizedTest
    @DisplayName("조회 답변 ID는 JavaScript 안전 정수와 Long 경계에서도 정확한 문자열이다")
    @ValueSource(longs = {9007199254740993L, Long.MAX_VALUE, Long.MIN_VALUE, -1L})
    void returnsExactStringAnswerIdInPage(long id) throws Exception {
        when(answerService.findAnswerPage(41L, 41L, null)).thenReturn(
                new AnswerPage(List.of(new QuestionAnswer(id, "member-a", "본문", 0, 0)), null, false));
        mockMvc.perform(get("/api/answer/41"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].answerId").isString())
                .andExpect(jsonPath("$.content[0].answerId").value(Long.toString(id)));
    }

    @ParameterizedTest
    @DisplayName("저장 답변 ID도 JavaScript 안전 정수와 Long 경계에서 정확한 문자열이다")
    @ValueSource(longs = {9007199254740993L, Long.MAX_VALUE, Long.MIN_VALUE, -1L})
    void returnsExactStringAnswerIdAfterCreate(long id) throws Exception {
        when(answerService.createAnswer(eq(41L), any())).thenReturn(new QuestionAnswer(id, "member-a", "본문", 0, 0));
        mockMvc.perform(post("/api/answer/save").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnswerForm("member-a", 41L, "본문"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answerId").isString())
                .andExpect(jsonPath("$.answerId").value(Long.toString(id)));
    }

}
