package com.inhatc.demp.docs;

import tools.jackson.databind.ObjectMapper;
import com.inhatc.demp.config.SecurityConfiguration;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.controller.AnswerController;
import com.inhatc.demp.controller.ExController;
import com.inhatc.demp.domain.Answer;
import com.inhatc.demp.domain.Member;
import com.inhatc.demp.dto.answer.AnswerForm;
import com.inhatc.demp.dto.answer.AnswerPage;
import com.inhatc.demp.dto.answer.UpdateAnswerForm;
import com.inhatc.demp.dto.question.QuestionAnswer;
import com.inhatc.demp.repository.AnswerRepository;
import com.inhatc.demp.service.AnswerService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
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
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MockitoBean(types = JwtTokenProvider.class)
@WebMvcTest(AnswerController.class)
@ContextConfiguration(classes = {AnswerController.class, ExController.class, SecurityConfiguration.class})
@WithMember
@AutoConfigureRestDocs
class AnswerRestDocsTest {

    private static final String DOCS_TOKEN = "docs-only-jwt-token";

    @MockitoBean
    private AnswerService answerService;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("답변 목록 조회 응답을 문서화한다")
    void documentsAnswerList() throws Exception {
        Member member = Member.builder().username("docs-member").password("docs-password-hash").roles(List.of("ROLE_USER")).build();
        Answer answer = Answer.builder().content("docs-answer").recommend(2).dislike(1).build();
        ReflectionTestUtils.setField(answer, "id", 61L);
        answer.assignMember(member);
        List<Answer> response = List.of(answer);
        when(answerService.findAnswerPage(51L, 41L, 70L)).thenReturn(new AnswerPage(List.of(new QuestionAnswer(answer)), null, false));

        mockMvc.perform(get("/api/answer/{questionId}", 51L).queryParam("before", "70")
                        .header("X-AUTH-TOKEN", DOCS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].answerId").isString())
                .andExpect(jsonPath("$.content[0].answerId").value("61"))
                .andExpect(jsonPath("$.content[0].content").value("docs-answer"))
                .andDo(document("answer-list",
                        requestHeaders(headerWithName("X-AUTH-TOKEN").description("로그인 시 발급된 JWT")),
                        pathParameters(parameterWithName("questionId").description("답변을 조회할 질문 ID")),
                        queryParameters(parameterWithName("before").description("이 ID 미만의 답변 조회, 생략 시 최신 20건").optional()),
                        responseFields(
                                fieldWithPath("content").description("최대 20건의 답변, ID 내림차순"),
                                fieldWithPath("hasNext").description("다음 페이지 존재 여부"),
                                fieldWithPath("nextCursor").type(org.springframework.restdocs.payload.JsonFieldType.VARIES).description("다음 페이지의 before 문자열, 마지막 페이지는 null"),
                                fieldWithPath("content[].answerId").description("정밀도를 보존하는 답변 ID 문자열"),
                                fieldWithPath("content[].username").description("답변 작성자 이름"),
                                fieldWithPath("content[].content").description("답변 본문"),
                                fieldWithPath("content[].recommend").description("추천 수"),
                                fieldWithPath("content[].myReaction").description("로그인 회원의 반응 NONE/RECOMMEND/DISLIKE"),
                                fieldWithPath("content[].dislike").description("비추천 수"))));
        verify(answerService).findAnswerPage(51L, 41L, 70L);
    }

    @Test
    @DisplayName("답변 등록 요청과 응답을 문서화한다")
    void documentsAnswerSave() throws Exception {
        AnswerForm request = new AnswerForm("docs-member", 51L, "docs-answer");
        Member member = Member.builder().username("docs-member").password("docs-password-hash").roles(List.of("ROLE_USER")).build();
        Answer savedAnswer = Answer.builder().content("docs-answer").recommend(0).dislike(0).build();
        ReflectionTestUtils.setField(savedAnswer, "id", 61L);
        savedAnswer.assignMember(member);
        QuestionAnswer response = new QuestionAnswer(savedAnswer);
        when(answerService.createAnswer(eq(41L), refEq(request))).thenReturn(response);

        mockMvc.perform(post("/api/answer/save")
                        .header("X-AUTH-TOKEN", DOCS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isMap())
                .andExpect(jsonPath("$.answerId").isString())
                .andExpect(jsonPath("$.answerId").value("61"))
                .andExpect(jsonPath("$.content").value("docs-answer"))
                .andDo(document("answer-save",
                        requestHeaders(
                                headerWithName("X-AUTH-TOKEN").description("로그인 시 발급된 JWT"),
                                headerWithName("Content-Type").description("JSON 요청 형식")),
                        requestFields(
                                fieldWithPath("username").description("호환 입력이며 작성자는 인증 회원으로 결정한다"),
                                fieldWithPath("questionId").description("답변을 추가할 질문 ID"),
                                fieldWithPath("answerContent").description("답변 본문")),
                        responseFields(
                                fieldWithPath("answerId").description("정밀도를 보존하는 답변 ID 문자열"),
                                fieldWithPath("username").description("답변 작성자 이름"),
                                fieldWithPath("content").description("답변 본문"),
                                fieldWithPath("recommend").description("추천 수"),
                                fieldWithPath("myReaction").description("로그인 회원의 반응 NONE/RECOMMEND/DISLIKE"),
                                fieldWithPath("dislike").description("비추천 수"))));
    }

    @Test
    @DisplayName("답변 수정 요청과 응답을 문서화한다")
    void documentsAnswerUpdate() throws Exception {
        UpdateAnswerForm request = new UpdateAnswerForm(61L, "docs-updated-answer");
        Answer answer = Answer.builder().content("docs-answer").recommend(0).dislike(0).build();

        mockMvc.perform(patch("/api/answer/update")
                        .header("X-AUTH-TOKEN", DOCS_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(content().string(""))
                .andDo(document("answer-update",
                        requestHeaders(
                                headerWithName("X-AUTH-TOKEN").description("로그인 시 발급된 JWT"),
                                headerWithName("Content-Type").description("JSON 요청 형식")),
                        requestFields(
                                fieldWithPath("answerId").description("수정할 답변 ID"),
                                fieldWithPath("answerContent").description("수정할 답변 본문"))));
        verify(answerService).update(eq(41L), refEq(request));
    }

    @Test
    @DisplayName("답변 삭제 요청과 응답을 문서화한다")
    void documentsAnswerDelete() throws Exception {
        Long answerId = 61L;

        mockMvc.perform(delete("/api/answer/delete")
                        .header("X-AUTH-TOKEN", DOCS_TOKEN)
                        .queryParam("answerId", answerId.toString()))
                .andExpect(status().isOk())
                .andExpect(content().string(""))
                .andDo(document("answer-delete",
                        requestHeaders(headerWithName("X-AUTH-TOKEN").description("로그인 시 발급된 JWT")),
                        queryParameters(parameterWithName("answerId").description("삭제할 답변 ID"))));
        verify(answerService).delete(41L, answerId);
    }
}
