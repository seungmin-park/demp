package com.inhatc.demp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inhatc.demp.domain.Answer;
import com.inhatc.demp.dto.answer.AnswerForm;
import com.inhatc.demp.dto.answer.UpdateAnswerForm;
import com.inhatc.demp.repository.AnswerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@SpringBootTest
class AnswerControllerTest {

    @Autowired
    AnswerController answerController;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private AnswerRepository answerRepository;
    private MockMvc mockMvc;

    @BeforeEach
    void beforeEach() {
        mockMvc = MockMvcBuilders.standaloneSetup(answerController).build();
    }

    @Test
    @DisplayName("controllerAnswerSave")
    void controllerAnswerSave() throws Exception {
        //given
        String content = objectMapper.writeValueAsString(new AnswerForm("memberA@memberA", 1L, "댓글 테스트"));
        //when
        //then
        mockMvc.perform(post("/api/answer/save")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andDo(print());
    }

    @Test
    @DisplayName("updateAnswer")
    void updateAnswer() throws Exception {
        //given
        String content = objectMapper.writeValueAsString(new UpdateAnswerForm(7L, "댓글 수정 테스트를 위한 텍스트"));
        //when
        mockMvc.perform(patch("/api/answer/update")
                .contentType(MediaType.APPLICATION_JSON)
                .content(content)
                .accept(MediaType.APPLICATION_JSON));
        Answer answer = answerRepository.findById(7L).orElseThrow(() -> new NoSuchElementException("해당 데이터 존재x"));
        //then
        assertThat(answer.getContent()).isEqualTo("댓글 수정 테스트를 위한 텍스트");
    }

    @Test
    @DisplayName("deleteAnswer")
    void deleteAnswer() throws Exception {
        //given
        mockMvc.perform(delete("/api/answer/delete")
                .param("answerId", "7"));
        //when
        Answer answer = answerRepository.findById(7L).orElse(null);
        //then
        assertThat(answer).isNull();
    }
}