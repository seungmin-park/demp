package com.inhatc.demp.docs;

import com.inhatc.demp.config.SecurityConfiguration;
import com.inhatc.demp.config.WebConfig;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.controller.ExController;
import com.inhatc.demp.controller.MemberController;
import com.inhatc.demp.domain.Member;
import com.inhatc.demp.dto.member.MemberLoginForm;
import com.inhatc.demp.dto.member.MemberSaveForm;
import com.inhatc.demp.service.MemberService;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MemberController.class)
@ContextConfiguration(classes = {MemberController.class, ExController.class, SecurityConfiguration.class, WebConfig.class})
@WithMockUser(roles = "USER")
@AutoConfigureRestDocs
class MemberRestDocsTest {

    @MockBean
    private MemberService memberService;
    @MockBean
    private JwtTokenProvider jwtTokenProvider;
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("회원 단일 조회 응답을 문서화한다")
    void documentsMemberGet() throws Exception {
        Member member = new Member("docs-member", "docs-password", List.of("ROLE_USER"));
        ReflectionTestUtils.setField(member, "id", 41L);
        when(memberService.findById(41L)).thenReturn(member);

        mockMvc.perform(get("/api/member/{memberId}", 41L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(41))
                .andExpect(jsonPath("$.username").value("docs-member"))
                .andDo(document("member-get",
                        pathParameters(parameterWithName("memberId").description("회원 ID")),
                        responseFields(
                                fieldWithPath("id").description("회원 ID"),
                                fieldWithPath("username").description("로그인 이름"),
                                fieldWithPath("password").description("현재 응답에 포함되는 비밀번호 값(T11에서 제거 예정)"))));
    }

    @Test
    @DisplayName("로그인 요청과 토큰 응답을 문서화한다")
    void documentsMemberLogin() throws Exception {
        MemberLoginForm request = new MemberLoginForm();
        request.setUsername("docs-member");
        request.setPassword("docs-login-password");
        Member authenticatedMember = new Member("docs-member", "docs-password-hash", List.of("ROLE_USER"));
        ReflectionTestUtils.setField(authenticatedMember, "id", 42L);
        when(memberService.login(refEq(request))).thenReturn(authenticatedMember);
        when(jwtTokenProvider.createToken("42", List.of("ROLE_USER"))).thenReturn("docs-only-jwt-token");

        mockMvc.perform(post("/api/member/login")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", request.getUsername())
                        .param("password", request.getPassword()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("docs-member"))
                .andExpect(jsonPath("$.jwt").value("docs-only-jwt-token"))
                .andDo(document("member-login",
                        requestHeaders(headerWithName("Content-Type").description("폼 요청 형식")),
                        requestParameters(
                                parameterWithName("username").description("로그인 이름"),
                                parameterWithName("password").description("로그인 비밀번호")),
                        responseFields(
                                fieldWithPath("username").description("로그인한 회원 이름"),
                                fieldWithPath("jwt").description("테스트 예시 JWT"))));
    }

    @Test
    @DisplayName("회원 가입 요청과 현재 응답을 문서화한다")
    void documentsMemberSave() throws Exception {
        MemberSaveForm request = new MemberSaveForm("docs-new-member", "docs-signup-password");
        Member savedMember = new Member("docs-new-member", "docs-password-hash", List.of("ROLE_USER"));
        ReflectionTestUtils.setField(savedMember, "id", 43L);
        doAnswer(invocation -> {
            Member joiningMember = invocation.getArgument(0);
            ReflectionTestUtils.setField(joiningMember, "id", 43L);
            return null;
        }).when(memberService).join(any(Member.class));
        when(memberService.findById(43L)).thenReturn(savedMember);

        mockMvc.perform(post("/api/member/save")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("username", request.getUsername())
                        .param("password", request.getPassword()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(43))
                .andExpect(jsonPath("$.username").value("docs-new-member"))
                .andDo(document("member-save",
                        requestHeaders(headerWithName("Content-Type").description("폼 요청 형식")),
                        requestParameters(
                                parameterWithName("username").description("가입할 로그인 이름"),
                                parameterWithName("password").description("가입할 비밀번호")),
                        responseFields(
                                fieldWithPath("id").description("회원 ID"),
                                fieldWithPath("username").description("로그인 이름"),
                                fieldWithPath("password").description("현재 응답에 포함되는 비밀번호 해시(T11에서 제거 예정)"),
                                fieldWithPath("questions").description("작성 질문 목록"),
                                fieldWithPath("answers").description("작성 답변 목록"),
                                fieldWithPath("roles").description("권한 문자열 목록"),
                                fieldWithPath("authorities[].authority").description("Spring Security 권한"),
                                fieldWithPath("accountNonExpired").description("계정 만료 여부"),
                                fieldWithPath("accountNonLocked").description("계정 잠금 여부"),
                                fieldWithPath("credentialsNonExpired").description("자격 증명 만료 여부"),
                                fieldWithPath("enabled").description("계정 활성 여부"))));
    }

    @Test
    @DisplayName("회원 이름 중복 확인 요청을 문서화한다")
    void documentsUsernameValidation() throws Exception {
        String username = "docs-available-member";
        when(memberService.validationDuplicateUsername(username)).thenReturn(true);

        mockMvc.perform(get("/api/member/validUsername").param("username", username))
                .andExpect(status().isOk())
                .andExpect(content().string("true"))
                .andDo(document("member-valid-username",
                        requestParameters(parameterWithName("username").description("중복 확인할 로그인 이름"))));
    }
}
