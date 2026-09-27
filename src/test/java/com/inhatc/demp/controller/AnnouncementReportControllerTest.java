package com.inhatc.demp.controller;
import com.inhatc.demp.config.SecurityConfiguration;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.service.AnnouncementReportService;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.*;
@WebMvcTest(AnnouncementReportController.class)
@ContextConfiguration(classes = {AnnouncementReportController.class, ExController.class, SecurityConfiguration.class})
class AnnouncementReportControllerTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean AnnouncementReportService reports;
    @Test
    @DisplayName("제보 작성자는 인증 계정에서 가져오고 일반 회원은 처리 API에 접근할 수 없다")
    void reportIdentityAndOperatorBoundary() throws Exception {
        mvc.perform(post("/api/announce/1/reports").with(user("member").roles("USER")).contentType("application/json")
            .content(mapper.writeValueAsString(Map.of("message","원문 변경","reporter","forged")))).andExpect(status().isCreated());
        verify(reports).submit(1,"원문 변경","member");
        mvc.perform(get("/api/admin/announcement-reports").with(user("member").roles("USER"))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/announcement-reports/1").with(user("member").roles("USER")).contentType("application/json").content(mapper.writeValueAsString(Map.of("note","처리")))).andExpect(status().isForbidden());
        mvc.perform(patch("/api/admin/announcement-reports/1").with(user("operator").roles("ADMIN")).contentType("application/json").content(mapper.writeValueAsString(Map.of("note","처리")))).andExpect(status().isNoContent());
        verify(reports).resolve(1,"처리","operator");
    }
    @Test
    @DisplayName("익명 요청과 빈 제보 내용은 서비스 실행 전에 거절한다")
    void invalidAndAnonymousRequests() throws Exception {
        mvc.perform(post("/api/announce/1/reports").contentType("application/json").content(mapper.writeValueAsString(Map.of("message","제보")))).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/announce/1/reports").with(user("member").roles("USER")).contentType("application/json").content(mapper.writeValueAsString(Map.of("message"," ")))).andExpect(status().isBadRequest());
        verifyNoInteractions(reports);
    }
}
