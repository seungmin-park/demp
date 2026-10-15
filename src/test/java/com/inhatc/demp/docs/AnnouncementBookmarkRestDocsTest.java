package com.inhatc.demp.docs;

import com.inhatc.demp.config.SecurityConfiguration;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.controller.*;
import com.inhatc.demp.dto.announcement.*;
import com.inhatc.demp.service.AnnouncementBookmarkService;
import com.inhatc.demp.support.WithMember;
import java.util.*;
import org.junit.jupiter.api.*;
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

@WebMvcTest(AnnouncementBookmarkController.class)
@ContextConfiguration(classes={AnnouncementBookmarkController.class,ExController.class,SecurityConfiguration.class})
@WithMember
@AutoConfigureRestDocs
class AnnouncementBookmarkRestDocsTest {
    @MockitoBean AnnouncementBookmarkService bookmarks;
    @MockitoBean JwtTokenProvider tokens;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Test
    @DisplayName("저장 요청은 인증 회원 ID와 명시적 최종 상태를 전달하고 응답을 문서화한다")
    void saves() throws Exception {
        when(bookmarks.setSaved(41,71,true)).thenReturn(new BookmarkState(71,true));
        mvc.perform(put("/api/member/me/bookmarks/71").contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("saved",true,"memberId",99))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.saved").value(true))
                .andExpect(jsonPath("$.announcementId").value(71))
                .andDo(document("announcement-bookmark-save",relaxedRequestFields(fieldWithPath("saved").description("원하는 최종 저장 상태, true 저장/false 해제. 재전송 멱등")),
                        responseFields(fieldWithPath("announcementId").description("공고 ID"),fieldWithPath("saved").description("확정된 저장 상태"))));
        verify(bookmarks).setSaved(41,71,true);
    }
    @Test
    @DisplayName("일괄 저장 상태 조회는 요청한 공고 중 본인이 저장한 ID만 반환한다")
    void readsSavedStates() throws Exception {
        when(bookmarks.savedIds(41,List.of(71L,72L))).thenReturn(List.of(71L));
        mvc.perform(get("/api/member/me/bookmarks/status").param("announcementIds","71,72"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.savedIds.length()").value(1)).andExpect(jsonPath("$.savedIds[0]").value(71))
                .andDo(document("announcement-bookmark-status",responseFields(fieldWithPath("savedIds").description("요청한 공고 중 본인이 저장한 ID 목록; 최대 요청 100건"))));
        verify(bookmarks).savedIds(41,List.of(71L,72L));
    }
    @Test
    @DisplayName("내 관심 목록은 비공개 공고의 내용을 노출하지 않는 페이지 계약을 문서화한다")
    void list() throws Exception {
        when(bookmarks.list(41,0,20)).thenReturn(new BookmarkPage(List.of(new BookmarkEntry(71,java.time.LocalDateTime.of(2026,10,15,12,0),false,null)),false,0));
        mvc.perform(get("/api/member/me/bookmarks")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].available").value(false)).andExpect(jsonPath("$.content[0].announcement").doesNotExist())
                .andExpect(jsonPath("$.hasNext").value(false))
                .andDo(document("announcement-bookmark-list",responseFields(fieldWithPath("content").description("저장순 항목; page 기본 0, size 기본/최대 20"),
                        fieldWithPath("content[].announcementId").description("저장 대상 공고 ID"),fieldWithPath("content[].savedAt").description("저장 시각, 한국 시각"),
                        fieldWithPath("content[].available").description("현재 공개 조회 가능 여부"),subsectionWithPath("content[].announcement").optional().description("공개 공고 요약. 삭제/비공개는 null"),
                        fieldWithPath("hasNext").description("다음 페이지 존재"),fieldWithPath("number").description("현재 페이지 번호"))));
        verify(bookmarks).list(41,0,20);
    }
    @Test
    @DisplayName("저장 상태 누락은 서비스 호출 없이 400으로 거절한다")
    void rejectsMissingState() throws Exception {
        mvc.perform(put("/api/member/me/bookmarks/71").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest());verifyNoInteractions(bookmarks);
    }
    @Test
    @DisplayName("비로그인 저장 조회와 변경은 401이며 서비스로 전달하지 않는다")
    @WithAnonymousUser
    void rejectsAnonymous() throws Exception {
        mvc.perform(get("/api/member/me/bookmarks")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/member/me/bookmarks/status").param("announcementIds","71")).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/member/me/bookmarks/71").contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(Map.of("saved",true))))
                .andExpect(status().isUnauthorized());verifyNoInteractions(bookmarks);
    }
}
