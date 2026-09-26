package com.inhatc.demp.controller;

import com.inhatc.demp.config.SecurityConfiguration;
import com.inhatc.demp.config.WebConfig;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.domain.announcement.Announcement;
import com.inhatc.demp.domain.announcement.AnnouncementType;
import com.inhatc.demp.domain.announcement.Career;
import com.inhatc.demp.domain.announcement.Company;
import com.inhatc.demp.domain.announcement.Description;
import com.inhatc.demp.domain.announcement.JobPosition;
import com.inhatc.demp.domain.announcement.Language;
import com.inhatc.demp.domain.announcement.RecruitPeriod;
import com.inhatc.demp.domain.announcement.UploadFile;
import com.inhatc.demp.dto.announcement.AnnouncementCreateRequest;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.inhatc.demp.service.AnnouncementService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import com.inhatc.demp.support.WithMember;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MockBean(JwtTokenProvider.class)
@WebMvcTest(AnnouncementController.class)
@ContextConfiguration(classes = {AnnouncementController.class, ExController.class, SecurityConfiguration.class, WebConfig.class})
@WithMember
class AnnouncementControllerTest {

    @MockBean
    private AnnouncementService announcementService;
    @Autowired
    private MockMvc mockMvc;

    @ParameterizedTest
    @DisplayName("EMP와 EDU 공고를 평면 multipart 계약으로 등록한다")
    @EnumSource(AnnouncementType.class)
    void bindsFlatMultipartRequest(AnnouncementType type) throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "company.png", "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

        mockMvc.perform(multipart("/api/announce/add")
                        .file(image)
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .param("title", "백엔드 채용")
                        .param("company", "DEMP")
                        .param("type", type.name())
                        .param("position", "BACKEND")
                        .param("minCareer", "0")
                        .param("maxCareer", "3")
                        .param("startedDate", "2026-09-01T00:00:00")
                        .param("deadLineDate", "2026-09-30T23:59:00")
                        .param("content", "설명")
                        .param("accessUrl", "https://example.com/jobs/1")
                        .param("payment", "3000")
                        .param("language", "JAVA", "SPRING"))
                .andExpect(status().isOk());

        ArgumentCaptor<AnnouncementCreateRequest> request = ArgumentCaptor.forClass(AnnouncementCreateRequest.class);
        verify(announcementService).save(request.capture());
        BeanWrapper fields = new BeanWrapperImpl(request.getValue());
        assertThat(fields.getPropertyValue("title")).isEqualTo("백엔드 채용");
        assertThat(fields.getPropertyValue("company")).isEqualTo("DEMP");
        assertThat(fields.getPropertyValue("type")).isEqualTo(type);
        assertThat(fields.getPropertyValue("position")).isEqualTo(JobPosition.BACKEND);
        assertThat(fields.getPropertyValue("minCareer")).isEqualTo(0);
        assertThat(fields.getPropertyValue("maxCareer")).isEqualTo(3);
        assertThat(fields.getPropertyValue("startedDate")).isEqualTo(LocalDateTime.of(2026, 9, 1, 0, 0));
        assertThat(fields.getPropertyValue("deadLineDate")).isEqualTo(LocalDateTime.of(2026, 9, 30, 23, 59));
        assertThat(fields.getPropertyValue("content")).isEqualTo("설명");
        assertThat(fields.getPropertyValue("accessUrl")).isEqualTo("https://example.com/jobs/1");
        assertThat(fields.getPropertyValue("payment")).isEqualTo(3000);
        assertThat(fields.getPropertyValue("language")).isEqualTo(Set.of(Language.JAVA, Language.SPRING));
    }

    @Test
    @DisplayName("잘못된 공고 enum은 서비스 호출 없이 400으로 거절한다")
    void rejectsUnknownEnum() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "company.png", "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

        mockMvc.perform(multipart("/api/announce/add")
                        .file(image)
                        .param("title", "백엔드 채용")
                        .param("company", "DEMP")
                        .param("type", "UNKNOWN")
                        .param("position", "BACKEND"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(400));

        verifyNoInteractions(announcementService);
    }

    @ParameterizedTest
    @DisplayName("경력 범위나 모집 기간이 역전된 공고는 서비스 호출 없이 400으로 거절한다")
    @CsvSource({
            "4, 3, 2026-09-01T00:00:00, 2026-09-30T23:59:00",
            "0, 3, 2026-09-30T23:59:00, 2026-09-01T00:00:00"
    })
    void rejectsReversedRanges(int minCareer, int maxCareer, String startedDate, String deadLineDate)
            throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "image", "company.png", "image/png", new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47});

        mockMvc.perform(multipart("/api/announce/add")
                        .file(image)
                        .param("title", "백엔드 채용")
                        .param("company", "DEMP")
                        .param("type", "EMP")
                        .param("position", "BACKEND")
                        .param("minCareer", String.valueOf(minCareer))
                        .param("maxCareer", String.valueOf(maxCareer))
                        .param("startedDate", startedDate)
                        .param("deadLineDate", deadLineDate)
                        .param("content", "설명")
                        .param("accessUrl", "https://example.com/jobs/1")
                        .param("payment", "3000")
                        .param("language", "JAVA"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value(400));

        verifyNoInteractions(announcementService);
    }

    @Test
    @DisplayName("공고 상세 응답은 값 객체를 평면 필드로 반환한다")
    void returnsFlatDetailResponse() throws Exception {
        Announcement announcement = Announcement.builder()
                .title("백엔드 채용")
                .company(new Company("DEMP"))
                .announcementType(AnnouncementType.EMP)
                .jobPosition(JobPosition.BACKEND)
                .career(new Career(0, 3))
                .recruitPeriod(new RecruitPeriod(
                        LocalDateTime.of(2026, 9, 1, 0, 0),
                        LocalDateTime.of(2026, 9, 30, 23, 59)))
                .description(new Description("설명", "https://example.com/jobs/1", 3000,
                        Set.of(Language.JAVA, Language.SPRING)))
                .image(new UploadFile("company.png", "saved-company.png"))
                .build();
        when(announcementService.findById(71L)).thenReturn(Optional.of(announcement));

        mockMvc.perform(get("/api/announce/detail/71"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("백엔드 채용"))
                .andExpect(jsonPath("$.company.name").value("DEMP"))
                .andExpect(jsonPath("$.announcementType").value("EMP"))
                .andExpect(jsonPath("$.position").value("BACKEND"))
                .andExpect(jsonPath("$.minCareer").value(0))
                .andExpect(jsonPath("$.maxCareer").value(3))
                .andExpect(jsonPath("$.startedDate").value("2026-09-01T00:00:00"))
                .andExpect(jsonPath("$.deadLineDate").value("2026-09-30T23:59:00"))
                .andExpect(jsonPath("$.content").value("설명"))
                .andExpect(jsonPath("$.accessUrl").value("https://example.com/jobs/1"))
                .andExpect(jsonPath("$.payment").value(3000))
                .andExpect(jsonPath("$.language.length()").value(2))
                .andExpect(jsonPath("$.career").doesNotExist())
                .andExpect(jsonPath("$.recruitPeriod").doesNotExist())
                .andExpect(jsonPath("$.description").doesNotExist());
    }

    @Test
    @DisplayName("없는 공고를 상세 조회하면 404를 반환한다")
    void returnsNotFoundWhenAnnouncementDoesNotExist() throws Exception {
        when(announcementService.findById(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/announce/detail/999"))
                .andExpect(status().isNotFound());
    }
    @Test
    @DisplayName("공고 검색어와 페이지 요청을 서비스에 전달한다")
    void passesSearchAndPagination() throws Exception {
        when(announcementService.getAnnounceScroll(any(), any())).thenReturn(new SliceImpl<>(List.of()));

        mockMvc.perform(get("/api/announce").param("title", "backend")
                        .param("page", "1").param("size", "3"))
                .andExpect(status().isOk());

        ArgumentCaptor<AnnouncementSearchCondition> condition = ArgumentCaptor.forClass(AnnouncementSearchCondition.class);
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(announcementService).getAnnounceScroll(condition.capture(), pageable.capture());
        assertThat(condition.getValue().getTitle()).isEqualTo("backend");
        assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
        assertThat(pageable.getValue().getPageSize()).isEqualTo(3);
    }

}
