package com.inhatc.demp.controller;

import com.inhatc.demp.config.*;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.service.MemberService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberController.class)
@ContextConfiguration(classes = {MemberController.class, ExController.class, SecurityConfiguration.class})
@MockitoBean(types = JwtTokenProvider.class)
class ExControllerTest {
    @Autowired MockMvc mvc;
    @MockitoBean MemberService members;
    @Test
    @DisplayName("필드 오류가 없는 검증 실패도 안전한 400으로 반환한다")
    void handlesGlobalValidationError() throws Exception {
        BeanPropertyBindingResult errors = new BeanPropertyBindingResult(new Object(), "request");
        errors.reject("invalid", "SQL password token");
        doAnswer(invocation -> { throw new BindException(errors); }).when(members).findById(99L);
        mvc.perform(get("/api/member/99")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(400))
                .andExpect(jsonPath("$.errorMessage").value("Invalid request"));
    }
    @Test
    @DisplayName("내부 예외의 SQL과 자격 증명은 오류 응답에 노출하지 않는다")
    void hidesInternalDetails() throws Exception {
        when(members.findById(99L)).thenThrow(new IllegalStateException("SQL password private-token com.private.Secret"));
        mvc.perform(get("/api/member/99")).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value(500))
                .andExpect(jsonPath("$.errorMessage").value("Internal server error"));
    }
}
