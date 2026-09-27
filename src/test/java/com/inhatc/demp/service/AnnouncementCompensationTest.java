package com.inhatc.demp.service;

import com.inhatc.demp.domain.announcement.*;
import com.inhatc.demp.dto.announcement.*;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import com.inhatc.demp.service.admin.AdminAnnouncementService;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
class AnnouncementCompensationTest {
    @Autowired AnnouncementService service;
    @Autowired AdminAnnouncementService admin;
    @Autowired AnnouncementRepository repository;
    @AfterEach void cleanup() { repository.deleteAll(); }

    @Test
    @DisplayName("미공개 연봉과 교육비 미확인은 실제 저장 후 재조회해도 null이다")
    void preservesUnknown() throws Exception {
        for (AnnouncementType type : AnnouncementType.values()) {
            var request = new AnnouncementCreateRequest(); fill(request, type); service.createAnnouncement(request);
            var item = repository.findByTitle(type.name()).orElseThrow();
            assertThat(service.findDetailResponse(item.getId()).orElseThrow().getPayment()).isNull();
        }
    }

    @Test
    @DisplayName("연봉 범위를 저장하고 협의로 수정하면 이전 금액을 제거한다")
    void storesRangeAndClearsHiddenAmounts() throws Exception {
        var request = new AnnouncementCreateRequest(); fill(request, AnnouncementType.EMP);
        request.setSalaryStatus(SalaryStatus.DISCLOSED); request.setPayment(4000); request.setSalaryMax(6000);
        service.createAnnouncement(request);
        var id = repository.findByTitle("EMP").orElseThrow().getId();
        var detail = service.findDetailResponse(id).orElseThrow();
        assertThat(detail.getPayment()).isEqualTo(4000); assertThat(detail.getSalaryMax()).isEqualTo(6000);
        var update = new AnnouncementUpdateRequest(); fill(update, AnnouncementType.EMP);
        update.setSalaryStatus(SalaryStatus.NEGOTIABLE); update.setPayment(4000); update.setSalaryMax(6000);
        admin.update(id, update);
        var changed = service.findDetailResponse(id).orElseThrow();
        assertThat(changed.getSalaryStatus()).isEqualTo(SalaryStatus.NEGOTIABLE);
        assertThat(changed.getPayment()).isNull(); assertThat(changed.getSalaryMax()).isNull();
    }

    private void fill(AnnouncementFields request, AnnouncementType type) {
        request.setTitle(type.name()); request.setCompany("DEMP"); request.setType(type);
        request.setPosition(JobPosition.BACKEND); request.setLanguage(Set.of(Language.JAVA));
        request.setContent("확인한 공고"); request.setAccessUrl("https://example.com/job");
        request.setStartedDate(LocalDateTime.of(2026,9,1,0,0)); request.setDeadLineDate(LocalDateTime.of(2026,10,1,0,0));
    }
}
