package com.inhatc.demp.docs;

import com.inhatc.demp.config.SecurityConfiguration;
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
import com.inhatc.demp.dto.announcement.AnnouncementDetailResponse;
import com.inhatc.demp.dto.announcement.AnnouncementScroll;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.inhatc.demp.service.AnnouncementService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.restdocs.test.autoconfigure.AutoConfigureRestDocs;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
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
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.springframework.restdocs.request.RequestDocumentation.requestParts;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MockitoBean(types = JwtTokenProvider.class)
@WebMvcTest(AnnouncementController.class)
@ContextConfiguration(classes = {AnnouncementController.class, ExController.class, SecurityConfiguration.class})
@org.springframework.security.test.context.support.WithMockUser(roles = {"ADMIN", "USER"})
@AutoConfigureRestDocs
class AnnouncementRestDocsTest {

    private static final String DOCS_TOKEN = "docs-only-jwt-token";

    @MockitoBean
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
                        .part(new org.springframework.mock.web.MockPart("title", "docs-backend-job".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .part(new org.springframework.mock.web.MockPart("company", "docs-company".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .part(new org.springframework.mock.web.MockPart("position", "BACKEND".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .part(new org.springframework.mock.web.MockPart("type", "EMP".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .part(new org.springframework.mock.web.MockPart("minCareer", "0".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .part(new org.springframework.mock.web.MockPart("maxCareer", "3".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .part(new org.springframework.mock.web.MockPart("startedDate", "2026-09-01T00:00:00".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .part(new org.springframework.mock.web.MockPart("deadLineDate", "2026-09-30T23:59:00".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .part(new org.springframework.mock.web.MockPart("content", "docs-description".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .part(new org.springframework.mock.web.MockPart("accessUrl", "https://example.com/jobs/71".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .part(new org.springframework.mock.web.MockPart("payment", "3000".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .part(new org.springframework.mock.web.MockPart("language", "JAVA".getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                        .part(new org.springframework.mock.web.MockPart("language", "SPRING".getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"))
                .andDo(document("announcement-add",
                        requestHeaders(headerWithName("X-AUTH-TOKEN").description("테스트용 인증 헤더")),
                        requestParts(
                                partWithName("title").description("공고 제목"),
                                partWithName("company").description("회사 또는 교육기관 이름"),
                                partWithName("type").description("공고 유형: EMP 또는 EDU"),
                                partWithName("position").description("직무 enum"),
                                partWithName("minCareer").description("최소 경력"),
                                partWithName("maxCareer").description("최대 경력, 0은 상한 없음"),
                                partWithName("startedDate").description("모집 시작 일시"),
                                partWithName("deadLineDate").description("모집 마감 일시"),
                                partWithName("content").description("공고 본문"),
                                partWithName("accessUrl").description("지원하기 버튼으로 이동할 원문 공고 URL"),
                                partWithName("payment").description("선택 연봉 또는 교육비, 만원 단위").optional(),
                                partWithName("salaryStatus").description("채용 연봉 공개 상태").optional(),
                                partWithName("salaryMax").description("선택 연봉 상한").optional(),
                                partWithName("language").description("기술 언어 목록"),
                                partWithName("image").description("선택 JPEG 또는 PNG 대표 이미지").optional(),
                                partWithName("bodyImages").description("선택 본문 이미지 목록, content의 attachment:0부터 순서대로 대응").optional())));
        verify(announcementService).createAnnouncement(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq("user"));
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
        when(announcementService.findDetailResponse(71L)).thenReturn(Optional.of(
                AnnouncementDetailResponse.from(announcement,
                        "https://inhatc-demp.s3.ap-northeast-2.amazonaws.com/docs-saved-image.png")));

        mockMvc.perform(get("/api/announce/detail/{AnnouncementId}", 71L)
                        .header("X-AUTH-TOKEN", DOCS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company.name").value("docs-company"))
                .andExpect(jsonPath("$.image").value("https://inhatc-demp.s3.ap-northeast-2.amazonaws.com/docs-saved-image.png"))
                .andExpect(jsonPath("$.minCareer").value(0))
                .andExpect(jsonPath("$.maxCareer").value(3))
                .andDo(document("announcement-detail",
                        requestHeaders(headerWithName("X-AUTH-TOKEN").description("로그인 시 발급된 JWT")),
                        pathParameters(parameterWithName("AnnouncementId").description("조회할 공고 ID")),
                        responseFields(
                                fieldWithPath("image").description("공고 이미지 URL"),
                                fieldWithPath("company.name").description("회사 또는 교육기관 이름"),
                                fieldWithPath("title").description("공고 제목"),
                                fieldWithPath("recruitmentAudience").optional().description("명시적 모집 대상"),
                        fieldWithPath("recruitmentClosed").description("운영자 수동 마감"),
                        fieldWithPath("minCareer").description("최소 경력"),
                                fieldWithPath("maxCareer").description("최대 경력, 0은 상한 없음"),
                                fieldWithPath("startedDate").description("모집 시작 일시"),
                                fieldWithPath("deadLineDate").description("모집 마감 일시"),
                                fieldWithPath("content").description("정제된 공고 본문"),
                                fieldWithPath("accessUrl").description("지원하기 버튼으로 이동할 원문 공고 URL"),
                                org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath("education").description("교육 정보, 미확인 항목은 null").optional(),
                                fieldWithPath("cohort").optional().description("교육 기수"),
                        fieldWithPath("stipendAmount").optional().description("지원금 만원"),
                        fieldWithPath("stipendNote").optional().description("지원금 조건"),
                        fieldWithPath("sourceName").optional().description("출처명"),
                        fieldWithPath("sourceIdentifier").optional().description("출처 식별자"),
                        fieldWithPath("applicationUrl").optional().description("별도 지원 주소"),
                        fieldWithPath("sourceVerifiedAt").optional().description("원문 확인 시각"),
                        fieldWithPath("publicationStatus").description("게시 상태"),
                                fieldWithPath("payment").description("연봉 또는 교육비, 만원 단위. null은 미확인").optional(),
                                fieldWithPath("salaryStatus").description("UNDISCLOSED / NEGOTIABLE / DISCLOSED"),
                                fieldWithPath("salaryMax").description("연봉 상한, 생략 시 단일 금액").optional(),
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
        AnnouncementResponse responseItem = new AnnouncementResponse(announcement, "https://inhatc-demp.s3.ap-northeast-2.amazonaws.com/docs-saved-image.png");
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("title").ascending());
        Slice<AnnouncementResponse> response = new SliceImpl<>(List.of(responseItem), pageRequest, false);
        when(announcementService.findAnnouncementSlice(refEq(request), org.mockito.ArgumentMatchers.eq(pageRequest)))
                .thenReturn(response);

        mockMvc.perform(get("/api/announce")
                        .queryParam("announcementType", request.getAnnouncementType().name())
                        .queryParam("positions", request.getPositions().get(0).name())
                        .queryParam("language", request.getLanguage().name())
                        .queryParam("career", String.valueOf(request.getCareer()))
                        .queryParam("payment", String.valueOf(request.getPayment()))
                        .queryParam("title", request.getTitle())
                        .queryParam("page", "0")
                        .queryParam("size", "10")
                        .queryParam("sort", "title,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(71))
                .andExpect(jsonPath("$.content[0].title").value("docs-backend-job"))
                .andExpect(jsonPath("$.content[0].company").value("docs-company"))
                .andExpect(jsonPath("$.content[0].announcementType").value("EMP"))
                .andExpect(jsonPath("$.content[0].payment").value(5000))
                .andExpect(jsonPath("$.content[0].deadLineDate").value("2026-09-30T23:59:00"))
                .andExpect(jsonPath("$.content[0].image").value("https://inhatc-demp.s3.ap-northeast-2.amazonaws.com/docs-saved-image.png"))
                .andExpect(jsonPath("$.last").value(true))
                .andDo(document("announcement-list",
                        queryParameters(
                                parameterWithName("announcementType").description("공고 유형: EMP 또는 EDU"),
                                parameterWithName("positions").description("직무 목록"),
                                parameterWithName("language").description("단일 기술 언어, 기존 호환"),
                                parameterWithName("languages").description("기술 다중 선택, 쉼표 구분, 그룹 내 OR").optional(),
                                parameterWithName("recruitmentStatus").description("OPEN 모집 중(시작/마감 포함), UPCOMING 모집 예정, CLOSED 마감. 한국 시각 기준").optional(),
                                parameterWithName("tuition").description("교육비 FREE 무료 또는 PAID 유료, 교육 공고만 조회").optional(),
                                parameterWithName("career").description("지원자 경력이 공고의 경력 범위에 포함, 0이면 미적용"),
                                parameterWithName("payment").description("최소 연봉 조건(만원)"),
                                parameterWithName("title").description("제목·회사명 검색어, 대소문자 무시"),
                                parameterWithName("page").description("0부터 시작하는 페이지 번호"),
                                parameterWithName("size").description("페이지 크기"),
                                parameterWithName("sort").description("호환용 수신값. 실제 조회는 ID 내림차순 고정")),
                        responseFields(
                                fieldWithPath("content[].id").description("공고 ID"),
                                fieldWithPath("content[].title").description("공고 제목"),
                                fieldWithPath("content[].language").description("기술 언어 목록"),
                                fieldWithPath("content[].position").description("직무"),
                                fieldWithPath("content[].image").description("이미지 URL"),
                                fieldWithPath("content[].company").description("회사 또는 교육기관").optional(),
                                fieldWithPath("content[].announcementType").description("EMP 또는 EDU").optional(),
                                fieldWithPath("content[].recruitmentAudience").optional().description("명시적 모집 대상"),
                        fieldWithPath("content[].recruitmentClosed").description("운영자 수동 마감"),
                        fieldWithPath("content[].minCareer").description("최소 경력").optional(),
                                fieldWithPath("content[].maxCareer").description("최대 경력, 0이면 상한 없음").optional(),
                                org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath("content[].education").description("교육 정보, 채용은 null").optional(),
                                fieldWithPath("content[].publicationStatus").description("게시 상태"),
                                fieldWithPath("content[].payment").description("채용 연봉 또는 교육비, 만원").optional(),
                                fieldWithPath("content[].startedDate").description("모집 시작 시각").optional(),
                                fieldWithPath("content[].deadLineDate").description("모집 마감 시각").optional(),
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
                .career(new Career(0, 3)).announcementType(AnnouncementType.EMP)
                .image(new UploadFile("docs-image.png", "docs-saved-image.png"))
                .build();
        ReflectionTestUtils.setField(announcement, "id", 71L);
        List<AnnouncementScroll> response = List.of(new AnnouncementScroll(announcement,
                "https://inhatc-demp.s3.ap-northeast-2.amazonaws.com/docs-saved-image.png"));
        when(announcementService.findScrollResponses()).thenReturn(response);

        mockMvc.perform(get("/api/announce/scroll")
                        .header("X-AUTH-TOKEN", DOCS_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(71))
                .andExpect(jsonPath("$[0].title").value("docs-backend-job"))
                .andExpect(jsonPath("$[0].image").value("https://inhatc-demp.s3.ap-northeast-2.amazonaws.com/docs-saved-image.png"))
                .andExpect(jsonPath("$[0].announcementType").value("EMP"))
                .andExpect(jsonPath("$[0].minCareer").value(0))
                .andExpect(jsonPath("$[0].maxCareer").value(3))
                .andDo(document("announcement-scroll",
                        requestHeaders(headerWithName("X-AUTH-TOKEN").description("로그인 시 발급된 JWT")),
                        responseFields(
                                fieldWithPath("[].id").description("공고 ID"),
                                fieldWithPath("[].title").description("공고 제목"),
                                fieldWithPath("[].company.name").description("회사 또는 교육기관 이름"),
                                fieldWithPath("[].announcementType").description("EMP 채용 / EDU 교육"),
                                fieldWithPath("[].recruitmentAudience").optional().description("명시적 모집 대상"),
                        fieldWithPath("[].recruitmentClosed").description("운영자 수동 마감"),
                        fieldWithPath("[].minCareer").description("최소 경력 연차"),
                                fieldWithPath("[].maxCareer").description("최대 경력 연차, 0이면 상한 없음"),
                                fieldWithPath("[].image").description("이미지 URL"))));
    }

    @Test
    @DisplayName("없는 공고의 404 응답을 문서화한다")
    void documentsMissingAnnouncementDetail() throws Exception {
        Long announcementId = 999L;
        when(announcementService.findDetailResponse(announcementId)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/announce/detail/{AnnouncementId}", announcementId)
                        .header("X-AUTH-TOKEN", DOCS_TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""))
                .andDo(document("announcement-detail-not-found",
                        requestHeaders(headerWithName("X-AUTH-TOKEN").description("로그인 시 발급된 JWT")),
                        pathParameters(parameterWithName("AnnouncementId").description("조회할 공고 ID"))));
    }

}
