package com.inhatc.demp.docs;

import com.inhatc.demp.config.SecurityConfiguration;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.controller.*;
import com.inhatc.demp.domain.ReactionType;
import com.inhatc.demp.dto.reaction.ReactionResponse;
import com.inhatc.demp.service.ContentReactionService;
import com.inhatc.demp.support.WithMember;
import tools.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.http.MediaType;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.put;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.payload.PayloadDocumentation.*;
import static org.springframework.restdocs.request.RequestDocumentation.*;

@WebMvcTest(ContentReactionController.class)
@ContextConfiguration(classes = {ContentReactionController.class, ExController.class, SecurityConfiguration.class})
@MockitoBean(types = JwtTokenProvider.class)
@WithMember
@AutoConfigureRestDocs
class ContentReactionRestDocsTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @MockitoBean ContentReactionService service;

    @ParameterizedTest
    @DisplayName("반응 API는 로그인 회원 ID와 선택을 전달하고 확정 집계를 반환한다")
    @ValueSource(strings = {"question", "answer"})
    void saveReaction(String target) throws Exception {
        var result = new ReactionResponse(4, 1, ReactionType.RECOMMEND);
        if (target.equals("question")) when(service.setQuestionReaction(41L, 7L, ReactionType.RECOMMEND)).thenReturn(result);
        else when(service.setAnswerReaction(41L, 7L, ReactionType.RECOMMEND)).thenReturn(result);
        mvc.perform(put("/api/" + target + "/{id}/reaction", 7L).contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("reaction", "RECOMMEND"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.recommend").value(4))
                .andExpect(jsonPath("$.dislike").value(1)).andExpect(jsonPath("$.myReaction").value("RECOMMEND"))
                .andDo(document(target + "-reaction", pathParameters(parameterWithName("id").description("반응 대상 ID")),
                        requestFields(fieldWithPath("reaction").description("RECOMMEND 추천, DISLIKE 비추천, NONE 취소. 동일 요청 재전송은 멱등적")),
                        responseFields(fieldWithPath("recommend").description("추천 수"), fieldWithPath("dislike").description("비추천 수"),
                                fieldWithPath("myReaction").description("로그인 회원의 현재 선택"))));
        if (target.equals("question")) verify(service).setQuestionReaction(41L, 7L, ReactionType.RECOMMEND);
        else verify(service).setAnswerReaction(41L, 7L, ReactionType.RECOMMEND);
    }

    @ParameterizedTest
    @DisplayName("없는 값과 잘못된 반응 요청은 400으로 거절한다")
    @ValueSource(strings = {"", "LIKE", "null"})
    void invalidReaction(String value) throws Exception {
        Map<String, Object> body = new java.util.HashMap<>();
        if (!value.isEmpty()) body.put("reaction", value.equals("null") ? null : value);
        mvc.perform(put("/api/question/{id}/reaction", 7).contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body)))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(service);
    }
}
