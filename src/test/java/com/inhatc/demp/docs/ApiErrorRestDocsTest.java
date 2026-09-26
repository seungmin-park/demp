package com.inhatc.demp.docs;

import com.inhatc.demp.config.*;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.controller.*;
import com.inhatc.demp.error.ApiException;
import com.inhatc.demp.service.MemberService;
import com.inhatc.demp.service.QuestionService;
import com.inhatc.demp.support.WithMember;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({MemberController.class, QuestionController.class})
@ContextConfiguration(classes = {MemberController.class, QuestionController.class, ExController.class, SecurityConfiguration.class, WebConfig.class})
@AutoConfigureRestDocs
@WithMember
@MockBean(JwtTokenProvider.class)
class ApiErrorRestDocsTest {
    @Autowired MockMvc mvc;
    @MockBean MemberService members;
    @MockBean QuestionService questions;
    @Test
    @DisplayName("가입 입력 오류의 400 응답을 문서화한다")
    void documentsBadRequest() throws Exception {
        mvc.perform(post("/api/member/save").param("username", " ").param("password", "secret"))
                .andExpect(status().isBadRequest()).andDo(document("error-400", fields()));
        verifyNoInteractions(members);
    }
    @Test
    @DisplayName("로그인 실패의 401 응답을 문서화한다")
    void documentsUnauthorized() throws Exception {
        when(members.login(any())).thenThrow(new ApiException(HttpStatus.UNAUTHORIZED));
        mvc.perform(post("/api/member/login").param("username", "docs-member").param("password", "wrong"))
                .andExpect(status().isUnauthorized()).andDo(document("error-401", fields()));
    }
    @Test
    @DisplayName("중복 가입의 409 응답을 문서화한다")
    void documentsConflict() throws Exception {
        doThrow(new ApiException(HttpStatus.CONFLICT)).when(members).registerMember(any());
        mvc.perform(post("/api/member/save").param("username", "docs-member").param("password", "secret"))
                .andExpect(status().isConflict()).andDo(document("error-409", fields()));
    }
    @ParameterizedTest
    @DisplayName("질문 변경의 권한 및 자원 오류를 문서화한다")
    @ValueSource(ints = {403, 404, 500})
    void documentsQuestionErrors(int status) throws Exception {
        doThrow(new ApiException(HttpStatus.valueOf(status))).when(questions).deleteQuestion(41L, 99L);
        mvc.perform(delete("/api/question/delete").param("questionId", "99"))
                .andExpect(status().is(status)).andExpect(jsonPath("$.errorCode").value(status))
                .andDo(document("error-" + status, fields()));
    }
    private org.springframework.restdocs.payload.ResponseFieldsSnippet fields() {
        return responseFields(fieldWithPath("errorCode").description("HTTP 상태와 같은 오류 코드"),
                fieldWithPath("errorMessage").description("내부 정보를 포함하지 않는 오류 안내"),
                fieldWithPath("instance").description("요청 경로"));
    }
}
