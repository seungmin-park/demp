package com.inhatc.demp.docs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inhatc.demp.config.SecurityConfiguration;
import com.inhatc.demp.config.WebConfig;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.controller.ExController;
import com.inhatc.demp.controller.QuestionController;
import com.inhatc.demp.domain.Hashtag;
import com.inhatc.demp.domain.Member;
import com.inhatc.demp.domain.Question;
import com.inhatc.demp.domain.QuestionHashtag;
import com.inhatc.demp.dto.question.QuestionDetail;
import com.inhatc.demp.dto.question.QuestionForm;
import com.inhatc.demp.dto.question.QuestionList;
import com.inhatc.demp.dto.question.QuestionSearchCondition;
import com.inhatc.demp.dto.question.QuestionUpdateForm;
import com.inhatc.demp.service.QuestionService;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.SliceImpl;
import com.inhatc.demp.support.WithMember;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.delete;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.patch;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.post;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MockBean(JwtTokenProvider.class)
@WebMvcTest(QuestionController.class)
@ContextConfiguration(classes = {QuestionController.class, ExController.class, SecurityConfiguration.class, WebConfig.class})
@WithMember
@AutoConfigureRestDocs
class QuestionRestDocsTest {

    private static final String DOCS_TOKEN = "docs-only-jwt-token";

    @MockBean
    private QuestionService questionService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("질문 목록 검색과 응답을 문서화한다")
    void documentsQuestionList() throws Exception {
        QuestionSearchCondition request = new QuestionSearchCondition();
        request.setTitle("docs-title");
        request.setContent("docs-content");
        request.setOrderBy("recommend");
        request.setHashtags(List.of("spring", "jpa"));
        QuestionList response = new QuestionList();
        ReflectionTestUtils.setField(response, "id", 51L);
        ReflectionTestUtils.setField(response, "title", "docs-question");
        ReflectionTestUtils.setField(response, "hits", 7);
        ReflectionTestUtils.setField(response, "recommend", 3);
        when(questionService.findSliceBySearchCondition(refEq(request), eq(PageRequest.of(0, 20))))
                .thenReturn(new SliceImpl<>(List.of(response), PageRequest.of(0, 20), false));

        mockMvc.perform(get("/api/question")
                        .param("title", request.getTitle())
                        .param("content", request.getContent())
                        .param("orderBy", request.getOrderBy())
                        .param("hashtags", request.getHashtags().toArray(new String[0]))
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(51))
                .andExpect(jsonPath("$.content[0].title").value("docs-question"))
                .andExpect(jsonPath("$.last").value(true))
                .andExpect(jsonPath("$.number").value(0))
                .andDo(document("question-list",
                        requestParameters(
                                parameterWithName("title").description("제목 검색어"),
                                parameterWithName("content").description("본문 검색어"),
                                parameterWithName("orderBy").description("정렬 기준"),
                                parameterWithName("hashtags").description("하나 이상 일치할 태그 목록"),
                                parameterWithName("page").description("0부터 시작하는 페이지 번호"),
                                parameterWithName("size").description("페이지 크기 1~100, 기본값 20")),
                        responseFields(
                                fieldWithPath("content[].id").description("질문 ID"),
                                fieldWithPath("content[].title").description("질문 제목"),
                                fieldWithPath("content[].hits").description("조회 수"),
                                fieldWithPath("content[].recommend").description("추천 수"),
                                fieldWithPath("last").description("마지막 페이지 여부"),
                                fieldWithPath("number").description("현재 페이지 번호"))));
    }

    @Test
    @DisplayName("질문 상세 조회 응답을 문서화한다")
    void documentsQuestionDetail() throws Exception {
        Member member = new Member("docs-member", "docs-password-hash", List.of("ROLE_USER"));
        Question question = new Question("docs-question", "docs-question-content", 7, 3, 1);
        ReflectionTestUtils.setField(question, "id", 51L);
        question.assignMember(member);
        Hashtag hashtag = new Hashtag("spring");
        question.addHashtag(hashtag);
        QuestionDetail response = new QuestionDetail(question);
        when(questionService.findById(51L)).thenReturn(response);

        mockMvc.perform(get("/api/question/detail/{questionId}", 51L)
                        .header("X-AUTH-TOKEN", DOCS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(51))
                .andExpect(jsonPath("$.content").value("docs-question-content"))
                .andExpect(jsonPath("$.hashtags[0]").value("spring"))
                .andDo(document("question-detail",
                        requestHeaders(headerWithName("X-AUTH-TOKEN").description("로그인 시 발급된 JWT")),
                        pathParameters(parameterWithName("questionId").description("질문 ID")),
                        responseFields(
                                fieldWithPath("id").description("질문 ID"),
                                fieldWithPath("title").description("질문 제목"),
                                fieldWithPath("content").description("질문 본문"),
                                fieldWithPath("hits").description("조회 수"),
                                fieldWithPath("recommend").description("추천 수"),
                                fieldWithPath("dislike").description("비추천 수"),
                                fieldWithPath("username").description("작성자 이름"),
                                fieldWithPath("hashtags").description("태그 목록"))));
    }

    @Test
    @DisplayName("질문 해시태그 목록을 문서화한다")
    void documentsquestionHashtags() throws Exception {
        List<String> response = List.of("spring", "jpa");
        when(questionService.findAllHashtags()).thenReturn(response);

        mockMvc.perform(get("/api/question/hashtags"))
                .andExpect(status().isOk())
                .andExpect(content().json("[\"spring\",\"jpa\"]", true))
                .andDo(document("question-hashtags",
                        responseFields(fieldWithPath("[]").description("등록된 태그 이름"))));
    }

    @Test
    @DisplayName("질문 등록 요청과 응답을 문서화한다")
    void documentsQuestionAdd() throws Exception {
        QuestionForm request = new QuestionForm(
                "docs-question", "docs-question-content", "docs-member",
                new ArrayList<>(List.of("spring", "jpa")));

        mockMvc.perform(post("/api/question/add")
                        .header("X-AUTH-TOKEN", DOCS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"))
                .andDo(document("question-add",
                        requestHeaders(
                                headerWithName("X-AUTH-TOKEN").description("로그인 시 발급된 JWT"),
                                headerWithName("Content-Type").description("JSON 요청 형식")),
                        requestFields(
                                fieldWithPath("title").description("질문 제목"),
                                fieldWithPath("content").description("질문 본문"),
                                fieldWithPath("username").description("호환 입력이며 작성자는 인증 회원으로 결정한다"),
                                fieldWithPath("hashtags").description("태그 목록"))));
        verify(questionService).join(eq(41L), refEq(request));
    }

    @Test
    @DisplayName("질문 수정 요청과 응답을 문서화한다")
    void documentsQuestionUpdate() throws Exception {
        QuestionUpdateForm request = new QuestionUpdateForm(
                51L, "docs-updated-question", "docs-updated-content",
                new ArrayList<>(List.of("spring")));

        mockMvc.perform(patch("/api/question/update")
                        .header("X-AUTH-TOKEN", DOCS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(""))
                .andDo(document("question-update",
                        requestHeaders(
                                headerWithName("X-AUTH-TOKEN").description("로그인 시 발급된 JWT"),
                                headerWithName("Content-Type").description("JSON 요청 형식")),
                        requestFields(
                                fieldWithPath("questionId").description("수정할 질문 ID"),
                                fieldWithPath("title").description("수정할 제목"),
                                fieldWithPath("content").description("수정할 본문"),
                                fieldWithPath("hashtags").description("기존 태그를 대체할 목록"))));
        verify(questionService).updateQuestion(eq(41L), refEq(request));
    }

    @Test
    @DisplayName("질문 삭제 요청과 응답을 문서화한다")
    void documentsQuestionDelete() throws Exception {
        Long questionId = 51L;

        mockMvc.perform(delete("/api/question/delete")
                        .header("X-AUTH-TOKEN", DOCS_TOKEN)
                        .param("questionId", questionId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(""))
                .andDo(document("question-delete",
                        requestHeaders(headerWithName("X-AUTH-TOKEN").description("로그인 시 발급된 JWT")),
                        requestParameters(parameterWithName("questionId").description("삭제할 질문 ID"))));
        verify(questionService).deleteQuestion(41L, questionId);
    }
}
