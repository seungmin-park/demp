package com.inhatc.demp.service;
import com.inhatc.demp.domain.announcement.*;
import com.inhatc.demp.repository.announcement.AnnouncementRepository;
import com.inhatc.demp.error.ApiException;
import java.time.LocalDateTime;
import java.util.Set;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest
class AnnouncementReportServiceTest {
    @Autowired AnnouncementReportService reports;
    @Autowired AnnouncementRepository repository;
    @AfterEach void cleanup() { repository.deleteAll(); }
    @Test
    @DisplayName("회원 제보가 관리자 목록에 저장되고 처리자 및 처리 내용과 함께 완료된다")
    void reportAndResolve() {
        long id = published(); reports.submit(id, "원문이 마감되었습니다", "member-a");
        var page = reports.list(false,0); assertThat(page.getContent()).hasSize(1); assertThat(page.getTotalElements()).isEqualTo(1);
        var report = page.getContent().getFirst(); assertThat(report.reporter()).isEqualTo("member-a");
        reports.resolve(report.id(), "원문 확인 후 마감 처리", "operator-b");
        assertThat(reports.list(false,0).getContent()).isEmpty();
        var resolved = reports.list(true,0).getContent().getFirst(); assertThat(resolved.resolvedBy()).isEqualTo("operator-b");
        assertThat(resolved.resolvedAt()).isNotNull(); assertThat(resolved.resolution()).isEqualTo("원문 확인 후 마감 처리");
        repository.deleteById(id); assertThat(reports.list(true,0).getContent()).isEmpty();
    }
    @Test
    @DisplayName("비공개 공고 제보와 빈 제보는 저장하지 않는다")
    void hiddenOrBlankReportRejected() {
        long id = published(); var item = repository.findById(id).orElseThrow(); item.changePublication(PublicationStatus.HIDDEN); repository.save(item);
        assertThatThrownBy(() -> reports.submit(id, "제보", "member")).isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> reports.submit(id, "  ", "member")).isInstanceOf(ApiException.class);
        assertThat(reports.list(true,0).getContent()).isEmpty();
    }
    private long published() {
        return repository.save(Announcement.builder().title("제보 테스트").company(new Company("DEMP")).career(new Career(0,0))
            .description(new Description("본문", "https://example.com/report", null, Set.of(Language.JAVA)))
            .recruitPeriod(new RecruitPeriod(LocalDateTime.of(2026,1,1,0,0), LocalDateTime.of(2026,12,31,0,0)))
            .announcementType(AnnouncementType.EMP).jobPosition(JobPosition.BACKEND).build()).getId();
    }
}
