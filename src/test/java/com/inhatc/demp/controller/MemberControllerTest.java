package com.inhatc.demp.controller;

import com.inhatc.demp.config.SecurityConfiguration;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.controller.ExController;
import com.inhatc.demp.service.MemberService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import com.inhatc.demp.support.WithMember;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@MockitoBean(types = JwtTokenProvider.class)
@WebMvcTest(MemberController.class)
@ContextConfiguration(classes = {MemberController.class, ExController.class, SecurityConfiguration.class})
@WithMember
class MemberControllerTest {
    @MockitoBean
    private MemberService memberService;
    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @DisplayName("회원 이름 중복 확인 결과를 응답 본문으로 반환한다")
    @ValueSource(booleans = {true, false})
    void returnsUsernameAvailability(boolean available) throws Exception {
        when(memberService.isUsernameAvailable("member-a")).thenReturn(available);

        mockMvc.perform(get("/api/member/validUsername").param("username", "member-a"))
                .andExpect(status().isOk())
                .andExpect(content().string(Boolean.toString(available)));
        verify(memberService).isUsernameAvailable("member-a");
    }

    @Test
    @DisplayName("내부 오류의 상세 정보를 응답에 노출하지 않는다")
    void hidesInternalErrorDetails() throws Exception {
        when(memberService.isUsernameAvailable("member-a")).thenThrow(new IllegalStateException("SQL secret-password private-token"));
        mockMvc.perform(get("/api/member/validUsername").param("username", "member-a"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.errorCode").value(500))
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("secret-password"))));
    }

}
