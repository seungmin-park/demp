package com.inhatc.demp.controller;

import com.inhatc.demp.config.SecurityConfiguration;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.service.AnnouncementService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verifyNoInteractions;

@WebMvcTest(AnnouncementController.class)
@ContextConfiguration(classes = {AnnouncementController.class, ExController.class, SecurityConfiguration.class})
class AnnouncementPublishingAuthorizationTest {
    @Autowired MockMvc mvc;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean AnnouncementService announcements;
    @Test
    @DisplayName("일반 회원은 과거 공고 등록 API에서도 등록할 수 없다")
    void blocksOrdinaryPublisher() throws Exception {
        mvc.perform(multipart("/api/announce/add").with(user("member").roles("USER"))).andExpect(status().isForbidden());
        verifyNoInteractions(announcements);
    }
    @Test
    @DisplayName("익명 등록은 인증을 요구하고 관리자는 요청 검증 단계까지 진입한다")
    void authenticationAndAdminBoundary() throws Exception {
        mvc.perform(multipart("/api/announce/add")).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/announce/add").with(user("operator").roles("ADMIN"))).andExpect(status().isBadRequest());
    }
}
