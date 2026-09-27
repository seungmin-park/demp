package com.inhatc.demp.service;
import com.inhatc.demp.domain.announcement.AnnouncementReport;
import com.inhatc.demp.repository.announcement.*;
import com.inhatc.demp.error.ApiException;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
@Service @RequiredArgsConstructor @Transactional(readOnly = true)
public class AnnouncementReportService {
    private final AnnouncementRepository announcements;
    private final AnnouncementReportRepository reports;
    private final Clock clock;
    public record Report(long id, long announcementId, String title, String message, String reporter, LocalDateTime createdAt, String resolution, String resolvedBy, LocalDateTime resolvedAt) {}
    @Transactional
    public void submit(long id, String message, String actor) {
        requireText(message);
        var announcement = announcements.findByIdForMutation(id).filter(item -> item.isPublished()).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
        reports.save(new AnnouncementReport(announcement, message.trim(), actor, LocalDateTime.now(clock)));
    }
    public Page<Report> list(boolean all, int page) {
        if (page < 0) throw new ApiException(HttpStatus.BAD_REQUEST);
        return reports.findReports(all, PageRequest.of(page,20,Sort.by(Sort.Direction.DESC,"id"))).map(r -> new Report(r.getId(), r.getAnnouncement().getId(), r.getAnnouncement().getTitle(), r.getMessage(), r.getReporter(), r.getCreatedAt(), r.getResolution(), r.getResolvedBy(), r.getResolvedAt()));
    }
    @Transactional
    public void resolve(long id, String note, String actor) {
        requireText(note);
        reports.findForUpdate(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND)).resolve(note.trim(), actor, LocalDateTime.now(clock));
    }
    private void requireText(String value) { if (value == null || value.isBlank() || value.length() > 1000) throw new ApiException(HttpStatus.BAD_REQUEST); }
}
