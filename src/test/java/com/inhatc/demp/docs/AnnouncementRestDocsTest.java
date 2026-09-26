package com.inhatc.demp.docs;

import com.inhatc.demp.config.SecurityConfiguration;
import com.inhatc.demp.config.WebConfig;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.controller.AnnouncementController;
import com.inhatc.demp.controller.ExController;
import com.inhatc.demp.domain.announcement.Announcement;
import com.inhatc.demp.domain.announcement.AnnouncementType;
import com.inhatc.demp.domain.announcement.Career;
import com.inhatc.demp.domain.announcement.Company;
import com.inhatc.demp.domain.announcement.Description;
import com.inhatc.demp.domain.announcement.JobPosition;
import com.inhatc.demp.domain.announcement.Language;
import com.inhatc.demp.domain.announcement.RecruitPeriod;
import com.inhatc.demp.domain.announcement.UploadFile;
import com.inhatc.demp.dto.announcement.AnnouncementResponse;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.inhatc.demp.service.AnnouncementService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.restdocs.AutoConfigureRestDocs;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import com.inhatc.demp.support.WithMember;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders.get;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.partWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MockBean(JwtTokenProvider.class)
@WebMvcTest(AnnouncementController.class)
@ContextConfiguration(classes = {AnnouncementController.class, ExController.class, SecurityConfiguration.class, WebConfig.class})
@WithMember
@AutoConfigureRestDocs
class AnnouncementRestDocsTest {

    private static final String DOCS_TOKEN = "docs-only-jwt-token";

    @MockBean
    private AnnouncementService announcementService;
    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("공고의 평면 multipart 등록 계약을 문서화한다")
    void documentsAnnouncementCreate() throws Exception {
        org.springframework.mock.web.MockMultipartFile image = new org.springframework.mock.web.MockMultipartFile(
                "image", "docs-image.png", "image/png",
                new byte[] {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A});

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/announce/add")
                        .file(image)
                        .header("X-AUTH-TOKEN", DOCS_TOKEN)
                        .param("title", "docs-backend-job")
                        .param("company", "docs-company")
                        .param("position", "BACKEND")
                        .param("type", "EMP")
                        .param("minCareer", "0")
                        .param("maxCareer", "3")
                        .param("startedDate", "2026-09-01T00:00:00")
                        .param("deadLineDate", "2026-09-30T23:59:00")
                        .param("content", "docs-description")
                        .param("accessUrl", "https://example.com/jobs/71")
                        .param("payment", "3000")
                        .param("language", "JAVA", "SPRING"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"))
                .andDo(document("announcement-add",
                        requestHeaders(headerWithName("X-AUTH-TOKEN").description("테스트용 인증 헤더")),
                        requestParameters(
                                parameterWithName("title").description("공고 제목"),
                                parameterWithName("company").description("회사 또는 교육기관 이름"),
                                parameterWithName("type").description("공고 유형: EMP 또는 EDU"),
                                parameterWithName("position").description("직무 enum"),
                                parameterWithName("minCareer").description("최소 경력"),
                                parameterWithName("maxCareer").description("최대 경력, 0은 상한 없음"),
                                parameterWithName("startedDate").description("모집 시작 일시"),
                                parameterWithName("deadLineDate").description("모집 마감 일시"),
                                parameterWithName("content").description("공고 본문"),
                                parameterWithName("accessUrl").description("지원 URL"),
                                parameterWithName("payment").description("연봉 또는 교육비, 만원 단위"),
                                parameterWithName("language").description("기술 언어 목록")),
                        requestParts(partWithName("image").description("필수 JPEG 또는 PNG 이미지"))));
        verify(announcementService).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    @DisplayName("공고 상세의 평면 응답 계약을 문서화한다")
    void documentsAnnouncementDetail() throws Exception {
        Announcement announcement = Announcement.builder()
                .title("docs-backend-job")
                .career(new Career(0, 3))
                .description(new Description("docs-description", "https://example.com/jobs/71", 3000,
                        Set.of(Language.JAVA, Language.SPRING)))
                .company(new Company("docs-company"))
                .image(new UploadFile("docs-image.png", "docs-saved-image.png"))
                .recruitPeriod(new RecruitPeriod(
                        LocalDateTime.of(2026, 9, 1, 0, 0),
                        LocalDateTime.of(2026, 9, 30, 23, 59)))
                .announcementType(AnnouncementType.EMP)
                .jobPosition(JobPosition.BACKEND)
                .build();
        when(announcementService.findById(71L)).thenReturn(Optional.of(announcement));

        mockMvc.perform(get("/api/announce/detail/{AnnouncementId}", 71L)
                        .header("X-AUTH-TOKEN", DOCS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company.name").value("docs-company"))
                .andExpect(jsonPath("$.minCareer").value(0))
                .andExpect(jsonPath("$.maxCareer").value(3))
                .andDo(document("announcement-detail",
                        requestHeaders(headerWithName("X-AUTH-TOKEN").description("로그인 시 발급된 JWT")),
                        pathParameters(parameterWithName("AnnouncementId").description("조회할 공고 ID")),
                        responseFields(
                                fieldWithPath("image").description("공고 이미지 URL"),
                                fieldWithPath("company.name").description("회사 또는 교육기관 이름"),
                                fieldWithPath("title").description("공고 제목"),
                                fieldWithPath("minCareer").description("최소 경력"),
                                fieldWithPath("maxCareer").description("최대 경력, 0은 상한 없음"),
                                fieldWithPath("startedDate").description("모집 시작 일시"),
                                fieldWithPath("deadLineDate").description("모집 마감 일시"),
                                fieldWithPath("content").description("정제된 공고 본문"),
                                fieldWithPath("accessUrl").description("지원 URL"),
                                fieldWithPath("payment").description("연봉 또는 교육비, 만원 단위"),
                                fieldWithPath("language").description("기술 언어 목록"),
                                fieldWithPath("position").description("직무 enum"),
                                fieldWithPath("announcementType").description("공고 유형: EMP 또는 EDU"))));
    }

    @Test
    @DisplayName("공고 검색과 페이지 응답을 문서화한다")
    void documentsAnnouncementList() throws Exception {
        AnnouncementSearchCondition request = new AnnouncementSearchCondition();
        request.setAnnouncementType(AnnouncementType.EMP);
        request.setPositions(List.of(JobPosition.BACKEND));
        request.setLanguage(Language.JAVA);
        request.setCareer(1);
        request.setPayment(5000);
        request.setTitle("docs");
        Announcement announcement = Announcement.builder()
                .title("docs-backend-job")
                .career(new Career(1, 3))
                .description(new Description("docs-description", "https://docs.invalid/jobs/71", 5000,
                        Set.of(Language.JAVA)))
                .company(new Company("docs-company"))
                .image(new UploadFile("docs-image.png", "docs-saved-image.png"))
                .recruitPeriod(new RecruitPeriod(
                        LocalDateTime.of(2026, 9, 1, 0, 0),
                        LocalDateTime.of(2026, 9, 30, 23, 59)))
                .announcementType(AnnouncementType.EMP)
                .jobPosition(JobPosition.BACKEND)
                .build();
        ReflectionTestUtils.setField(announcement, "id", 71L);
        AnnouncementResponse responseItem = new AnnouncementResponse(announcement);
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("title").ascending());
        Slice<AnnouncementResponse> response = new SliceImpl<>(List.of(responseItem), pageRequest, false);
        when(announcementService.getAnnounceScroll(refEq(request), org.mockito.ArgumentMatchers.eq(pageRequest)))
                .thenReturn(response);

        mockMvc.perform(get("/api/announce")
                        .param("announcementType", request.getAnnouncementType().name())
                        .param("positions", request.getPositions().get(0).name())
                        .param("language", request.getLanguage().name())
                        .param("career", String.valueOf(request.getCareer()))
                        .param("payment", String.valueOf(request.getPayment()))
                        .param("title", request.getTitle())
                        .param("page", "0")
                        .param("size", "10")
                        .param("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(71))
                .andExpect(jsonPath("$.content[0].title").value("docs-backend-job"))
                .andExpect(jsonPath("$.last").value(true))
                .andDo(document("announcement-list",
                        requestParameters(
                                parameterWithName("announcementType").description("공고 유형: EMP 또는 EDU"),
                                parameterWithName("positions").description("직무 목록"),
                                parameterWithName("language").description("기술 언어"),
                                parameterWithName("career").description("최소 경력 조건, 0이면 미적용"),
                                parameterWithName("payment").description("최소 금액 조건"),
                                parameterWithName("title").description("제목 검색어"),
                                parameterWithName("page").description("0부터 시작하는 페이지 번호"),
                                parameterWithName("size").description("페이지 크기"),
                                parameterWithName("sort").description("수신하는 정렬 필드와 방향. 현재 조회 쿼리에 적용되지 않음(T32 예정)")),
                        responseFields(
                                fieldWithPath("content[].id").description("공고 ID"),
                                fieldWithPath("content[].title").description("공고 제목"),
                                fieldWithPath("content[].language").description("기술 언어 목록"),
                                fieldWithPath("content[].position").description("직무"),
                                fieldWithPath("content[].image").description("이미지 URL"),
                                fieldWithPath("pageable.sort.empty").description("페이지 정렬 조건 없음 여부"),
                                fieldWithPath("pageable.sort.sorted").description("Pageable 정렬 메타데이터 존재 여부. 실제 조회 순서를 보장하지 않음"),
                                fieldWithPath("pageable.sort.unsorted").description("Pageable 정렬 메타데이터 없음 여부"),
                                fieldWithPath("pageable.offset").description("현재 페이지 오프셋"),
                                fieldWithPath("pageable.pageNumber").description("현재 페이지 번호"),
                                fieldWithPath("pageable.pageSize").description("페이지 크기"),
                                fieldWithPath("pageable.paged").description("페이지 요청 여부"),
                                fieldWithPath("pageable.unpaged").description("비페이지 요청 여부"),
                                fieldWithPath("first").description("첫 페이지 여부"),
                                fieldWithPath("last").description("마지막 slice 여부"),
                                fieldWithPath("size").description("요청 페이지 크기"),
                                fieldWithPath("number").description("현재 페이지 번호"),
                                fieldWithPath("sort.empty").description("정렬 조건 없음 여부"),
                                fieldWithPath("sort.sorted").description("정렬 메타데이터 존재 여부. 실제 조회 순서를 보장하지 않음"),
                                fieldWithPath("sort.unsorted").description("정렬 메타데이터 없음 여부"),
                                fieldWithPath("numberOfElements").description("현재 slice 원소 수"),
                                fieldWithPath("empty").description("현재 slice 비어 있음 여부"))));
    }

    @Test
    @DisplayName("공고 스크롤 목록 응답을 문서화한다")
    void documentsAnnouncementScroll() throws Exception {
        Announcement announcement = Announcement.builder()
                .title("docs-backend-job")
                .company(new Company("docs-company"))
                .image(new UploadFile("docs-image.png", "docs-saved-image.png"))
                .build();
        ReflectionTestUtils.setField(announcement, "id", 71L);
        List<Announcement> response = List.of(announcement);
        when(announcementService.findAll()).thenReturn(response);

        mockMvc.perform(get("/api/announce/scroll")
                        .header("X-AUTH-TOKEN", DOCS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(71))
                .andExpect(jsonPath("$[0].title").value("docs-backend-job"))
                .andDo(document("announcement-scroll",
                        requestHeaders(headerWithName("X-AUTH-TOKEN").description("로그인 시 발급된 JWT")),
                        responseFields(
                                fieldWithPath("[].id").description("공고 ID"),
                                fieldWithPath("[].title").description("공고 제목"),
                                fieldWithPath("[].company.name").description("회사 또는 교육기관 이름"),
                                fieldWithPath("[].image").description("이미지 URL"))));
    }

    @Test
    @DisplayName("없는 공고의 404 응답을 문서화한다")
    void documentsMissingAnnouncementDetail() throws Exception {
        Long announcementId = 999L;
        when(announcementService.findById(announcementId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/announce/detail/{AnnouncementId}", announcementId)
                        .header("X-AUTH-TOKEN", DOCS_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""))
                .andDo(document("announcement-detail-not-found",
                        requestHeaders(headerWithName("X-AUTH-TOKEN").description("로그인 시 발급된 JWT")),
                        pathParameters(parameterWithName("AnnouncementId").description("조회할 공고 ID"))));
    }

}
