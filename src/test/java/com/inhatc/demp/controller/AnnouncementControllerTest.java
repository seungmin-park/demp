package com.inhatc.demp.controller;

import com.inhatc.demp.config.SecurityConfiguration;
import com.inhatc.demp.config.WebConfig;
import com.inhatc.demp.config.jwt.JwtTokenProvider;
import com.inhatc.demp.dto.announcement.AnnouncementSearchCondition;
import com.inhatc.demp.service.AnnouncementService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.SliceImpl;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@MockBean(JwtTokenProvider.class)
@WebMvcTest(AnnouncementController.class)
@ContextConfiguration(classes = {AnnouncementController.class, ExController.class, SecurityConfiguration.class, WebConfig.class})
@WithMockUser(roles = "USER")
class AnnouncementControllerTest {

    @MockBean
    private AnnouncementService announcementService;
    @Autowired
    private MockMvc mockMvc;

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
