package com.inhatc.demp.docs;

import com.inhatc.demp.config.SecurityConfiguration;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.controller.ExController;
import com.inhatc.demp.controller.admin.AdminIdentityController;
import com.inhatc.demp.controller.admin.AdminOperationsController;
import com.inhatc.demp.dto.admin.*;
import com.inhatc.demp.dto.announcement.*;
import com.inhatc.demp.service.AnnouncementService;
import com.inhatc.demp.service.admin.*;
import java.util.*;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import org.springframework.data.domain.*;
import org.springframework.mock.web.MockMultipartFile;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.*;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

@WebMvcTest(AdminOperationsController.class)
@ContextConfiguration(classes = {AdminIdentityController.class, AdminOperationsController.class, ExController.class, SecurityConfiguration.class})
@AutoConfigureRestDocs
@WithMockUser(roles = "ADMIN")
class AdminRestDocsTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean JwtTokenProvider tokens;
    @MockitoBean AdminOverviewService overview;
    @MockitoBean AdminCommunityService community;
    @MockitoBean AdminAnnouncementService management;
    @MockitoBean AnnouncementService announcements;

    @Test
    @DisplayName("관리자 집계 응답을 실제 필드별로 문서화한다")
    void overview() throws Exception {
        when(overview.overview()).thenReturn(new AdminOverview(3,2,7,9,4));
        mvc.perform(get("/api/admin/overview")).andExpect(status().isOk())
                .andExpect(jsonPath("$.announcements").value(3)).andExpect(jsonPath("$.members").value(4))
                .andDo(document("admin-overview", responseFields(fieldWithPath("announcements").description("채용 공고 수"),
                        fieldWithPath("bootcamps").description("교육 공고 수"), fieldWithPath("questions").description("질문 수"),
                        fieldWithPath("answers").description("답변 수"), fieldWithPath("members").description("회원 수"))));
    }

    @Test
    @DisplayName("관리자 공고 생성과 선택 이미지 없는 수정의 multipart 계약을 문서화한다")
    void announcementWrites() throws Exception {
        var create = multipart("/api/admin/announcements").file(new MockMultipartFile("image","image.png","image/png",new byte[]{1}));
        fields(create);
        mvc.perform(create).andExpect(status().isCreated()).andDo(document("admin-announcement-create"));
        var capture = org.mockito.ArgumentCaptor.forClass(AnnouncementCreateRequest.class);
        verify(announcements).createAnnouncement(capture.capture());
        assertThat(capture.getValue().getTitle()).isEqualTo("관리 공고");
        when(management.update(eq(8L), any())).thenReturn(new AdminMutationResult(false));
        var update = multipart("/api/admin/announcements/{id}",8L);
        fields(update);
        mvc.perform(update.with(request -> { request.setMethod("PATCH"); return request; })).andExpect(status().isOk()).andExpect(jsonPath("$.cleanupPending").value(false))
                .andDo(document("admin-announcement-update"));
        var changed = org.mockito.ArgumentCaptor.forClass(AnnouncementUpdateRequest.class);
        verify(management).update(eq(8L),changed.capture());
        assertThat(changed.getValue().getImage()).isNull();
        assertThat(changed.getValue().getLanguage()).containsExactly(com.inhatc.demp.domain.announcement.Language.JAVA);
    }

    @Test
    @DisplayName("관리자 공고 조회와 삭제 결과를 문서화한다")
    void announcementReadsAndDelete() throws Exception {
        when(announcements.findAnnouncementSlice(any(), any())).thenReturn(new SliceImpl<>(List.of(),PageRequest.of(0,20),false));
        mvc.perform(get("/api/admin/announcements").param("title","Spring").param("announcementType","EDU"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content").isEmpty()).andDo(document("admin-announcements"));
        var filter = org.mockito.ArgumentCaptor.forClass(AnnouncementSearchCondition.class);
        verify(announcements).findAnnouncementSlice(filter.capture(), any());
        assertThat(filter.getValue().getTitle()).isEqualTo("Spring");
        when(announcements.findDetailResponse(8L)).thenReturn(Optional.of(AnnouncementDetailResponse.builder().title("상세").language(Set.of()).build()));
        mvc.perform(get("/api/admin/announcements/{id}",8L)).andExpect(status().isOk()).andExpect(jsonPath("$.title").value("상세"))
                .andDo(document("admin-announcement-detail"));
        when(management.delete(8L)).thenReturn(new AdminMutationResult(true));
        mvc.perform(delete("/api/admin/announcements/{id}",8L)).andExpect(status().isOk()).andExpect(jsonPath("$.cleanupPending").value(true))
                .andDo(document("admin-announcement-delete"));
        verify(management).delete(8L);
    }

    @ParameterizedTest
    @DisplayName("질문과 답변 목록 상세 수정 삭제 계약을 검증하고 문서화한다")
    @ValueSource(strings = {"questions", "answers"})
    void communityCrud(String kind) throws Exception {
        AdminPost item = new AdminPost(7L,3L,"제목","<h2>본문</h2>","author",List.of("JAVA"));
        when(community.questions("Java",0)).thenReturn(new PageImpl<>(List.of(item)));
        when(community.answers("Java",0)).thenReturn(new PageImpl<>(List.of(item)));
        when(community.question(7L)).thenReturn(item); when(community.answer(7L)).thenReturn(item);
        mvc.perform(get("/api/admin/"+kind).param("q","Java"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].username").value("author"))
                .andExpect(jsonPath("$.totalElements").value(1)).andDo(document("admin-"+kind));
        mvc.perform(get("/api/admin/"+kind+"/{id}",7L)).andExpect(status().isOk()).andExpect(jsonPath("$.content").value("<h2>본문</h2>"))
                .andDo(document("admin-"+kind+"-detail"));
        Map<String,String> payload = kind.equals("questions") ? Map.of("title","수정", "content","<h2>수정</h2>") : Map.of("content","<h2>수정</h2>");
        mvc.perform(patch("/api/admin/"+kind+"/{id}",7L).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(payload)))
                .andExpect(status().isNoContent()).andDo(document("admin-"+kind+"-update"));
        mvc.perform(delete("/api/admin/"+kind+"/{id}",7L)).andExpect(status().isNoContent()).andDo(document("admin-"+kind+"-delete"));
        if (kind.equals("questions")) { verify(community).updateQuestion(7L,"수정","<h2>수정</h2>"); verify(community).deleteQuestion(7L); }
        else { verify(community).updateAnswer(7L,"<h2>수정</h2>"); verify(community).deleteAnswer(7L); }
    }

    @Test
    @DisplayName("잘못된 관리자 입력과 음수 페이지는 서비스 호출 없이 400이다")
    void rejectsInvalidFields() throws Exception {
        mvc.perform(patch("/api/admin/questions/7").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("title","","content",""))))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/admin/announcements").param("page","-1")).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/admin/announcements")).andExpect(status().isBadRequest());
        verifyNoInteractions(announcements,management,community);
    }

    private void fields(org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder request) {
        request.param("title","관리 공고").param("company","DEMP").param("position","BACKEND").param("type","EDU")
                .param("startedDate","2026-01-01T00:00").param("deadLineDate","2026-12-31T00:00")
                .param("content","<h2>본문</h2>").param("accessUrl","https://example.test").param("language","JAVA");
    }
}
