package com.inhatc.demp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inhatc.demp.config.SecurityConfiguration;
import com.inhatc.demp.config.WebConfig;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.controller.ExController;
import com.inhatc.demp.domain.Answer;
import com.inhatc.demp.domain.Member;
import com.inhatc.demp.dto.answer.AnswerForm;
import com.inhatc.demp.dto.answer.UpdateAnswerForm;
import com.inhatc.demp.dto.question.QuestionAnswer;
import com.inhatc.demp.repository.AnswerRepository;
import com.inhatc.demp.service.QuestionService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@MockBean(JwtTokenProvider.class)
@WebMvcTest(AnswerController.class)
@ContextConfiguration(classes = {AnswerController.class, ExController.class, SecurityConfiguration.class, WebConfig.class})
@WithMockUser(roles = "USER")
class AnswerControllerTest {
    @MockBean
    private QuestionService questionService;
    @MockBean
    private AnswerRepository answerRepository;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("답변 등록 요청을 서비스에 전달하고 저장 결과를 반환한다")
    void controllerAnswerSave() throws Exception {
        Answer answer = new Answer("댓글 테스트", 0, 0);
        answer.settingMember(new Member("member-a", "password", List.of("ROLE_USER")));
        when(questionService.saveAnswer(any())).thenReturn(List.of(new QuestionAnswer(answer)));

        mockMvc.perform(post("/api/answer/save").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AnswerForm("member-a", 41L, "댓글 테스트"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].username").value("member-a"))
                .andExpect(jsonPath("$[0].content").value("댓글 테스트"))
                .andExpect(jsonPath("$[0].recommend").value(0))
                .andExpect(jsonPath("$[0].dislike").value(0));
        ArgumentCaptor<AnswerForm> form = ArgumentCaptor.forClass(AnswerForm.class);
        verify(questionService).saveAnswer(form.capture());
        assertThat(form.getValue().getUsername()).isEqualTo("member-a");
        assertThat(form.getValue().getQuestionId()).isEqualTo(41L);
        assertThat(form.getValue().getAnswerContent()).isEqualTo("댓글 테스트");
    }

    @Test
    @DisplayName("답변 수정 요청의 내용을 조회한 답변에 반영한다")
    void updateAnswer() throws Exception {
        Answer answer = new Answer("원래 댓글", 0, 0);
        when(answerRepository.findById(73L)).thenReturn(Optional.of(answer));

        mockMvc.perform(patch("/api/answer/update").contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateAnswerForm(73L, "수정 댓글"))))
                .andExpect(status().isOk());
        // This proves HTTP binding and the in-memory change, not a database commit.
        assertThat(answer.getContent()).isEqualTo("수정 댓글");
    }

    @Test
    @DisplayName("답변 삭제 요청의 ID를 저장소에 전달한다")
    void deleteAnswer() throws Exception {
        mockMvc.perform(delete("/api/answer/delete").param("answerId", "73"))
                .andExpect(status().isOk());
        verify(answerRepository).deleteById(73L);
    }
    @Test
    @DisplayName("답변이 없는 질문의 조회 응답은 빈 배열이다")
    void returnsEmptyAnswers() throws Exception {
        when(answerRepository.findByQuestion_Id(41L)).thenReturn(List.of());

        mockMvc.perform(get("/api/answer/41"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]", true));
    }

    @Test
    @DisplayName("답변 ID가 없는 삭제 요청은 저장소를 호출하지 않고 거절한다")
    void rejectsMissingAnswerId() throws Exception {
        mockMvc.perform(delete("/api/answer/delete"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(answerRepository, questionService);
    }

}
