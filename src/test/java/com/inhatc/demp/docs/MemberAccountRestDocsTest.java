package com.inhatc.demp.docs;

import com.inhatc.demp.config.SecurityConfiguration;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.controller.ExController;
import com.inhatc.demp.controller.MemberAccountController;
import com.inhatc.demp.dto.member.MemberProfile;
import com.inhatc.demp.dto.member.PasswordChange;
import com.inhatc.demp.service.MemberAccountService;
import com.inhatc.demp.support.WithMember;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;
import static org.mockito.Mockito.*;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(MemberAccountController.class)
@ContextConfiguration(classes = {MemberAccountController.class, ExController.class, SecurityConfiguration.class})
@WithMember
@AutoConfigureRestDocs
class MemberAccountRestDocsTest {
    @MockitoBean MemberAccountService accounts;
    @MockitoBean JwtTokenProvider tokens;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    @Test
    @DisplayName("본인 정보는 비밀과 내부 권한 목록 없이 응답하며 서버 인증 ID로 조회한다")
    void profile() throws Exception {
        when(accounts.profile(41L)).thenReturn(new MemberProfile(41L, "member-a", "내 이름", false));
        mvc.perform(get("/api/member/me")).andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("member-a"))
                .andExpect(jsonPath("$.nickname").value("내 이름"))
                .andExpect(jsonPath("$.admin").value(false))
                .andDo(document("member-profile", responseFields(
                        fieldWithPath("id").description("본인 회원 ID"), fieldWithPath("username").description("변경할 수 없는 로그인 아이디"),
                        fieldWithPath("nickname").optional().description("닉네임, 미설정이면 null"), fieldWithPath("admin").description("관리자 여부"))));
        verify(accounts).profile(41L);
    }

    @Test
    @DisplayName("닉네임 변경은 본인의 인증 ID와 닉네임만 서비스로 전달한다")
    void nickname() throws Exception {
        when(accounts.updateNickname(41L, "새 이름")).thenReturn(new MemberProfile(41L, "member-a", "새 이름", false));
        mvc.perform(patch("/api/member/me").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("nickname", "새 이름", "id", 99, "admin", true))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(41)).andExpect(jsonPath("$.admin").value(false))
                .andDo(document("member-nickname", relaxedRequestFields(fieldWithPath("nickname").description("앞뒤 공백 제거 후 1~30자"))));
        verify(accounts).updateNickname(41L, "새 이름");
    }

    @Test
    @DisplayName("현재 비밀번호를 확인하는 변경 요청은 인증 버전을 전달하고 204를 반환한다")
    void password() throws Exception {
        PasswordChange body = new PasswordChange("old-password", "new-password");
        mvc.perform(put("/api/member/me/password").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isNoContent()).andExpect(content().string(""))
                .andDo(document("member-password", requestFields(
                        fieldWithPath("currentPassword").description("현재 비밀번호"),
                        fieldWithPath("newPassword").description("새 비밀번호: 8자 이상, UTF-8 72바이트 이하"))));
        verify(accounts).changePassword(41L, 0L, body);
    }

    @Test
    @DisplayName("빈 닉네임과 짧은 새 비밀번호는 서비스 호출 없이 400으로 거절한다")
    void invalidInput() throws Exception {
        mvc.perform(patch("/api/member/me").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("nickname", " "))))
                .andExpect(status().isBadRequest());
        mvc.perform(put("/api/member/me/password").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(new PasswordChange("old-password", "short"))))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(accounts);
    }

    @Test
    @DisplayName("비로그인 회원 정보 조회와 변경은 401로 거절한다")
    @WithAnonymousUser
    void unauthenticated() throws Exception {
        mvc.perform(get("/api/member/me")).andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/member/me").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("nickname", "name"))))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/member/me/password").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(new PasswordChange("old-password", "new-password"))))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(accounts);
    }
}
