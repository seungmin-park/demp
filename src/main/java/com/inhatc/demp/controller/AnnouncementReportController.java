package com.inhatc.demp.controller;
import com.inhatc.demp.service.AnnouncementReportService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
@RestController @RequiredArgsConstructor
public class AnnouncementReportController {
    private final AnnouncementReportService reports;
    public record Submission(@NotBlank @Size(max = 1000) String message) {}
    public record Resolution(@NotBlank @Size(max = 1000) String note) {}
    @PostMapping("/api/announce/{id}/reports") @ResponseStatus(HttpStatus.CREATED)
    public void submit(@PathVariable long id, @Valid @RequestBody Submission request, Principal principal) { reports.submit(id, request.message(), principal.getName()); }
    @GetMapping("/api/admin/announcement-reports")
    public Page<AnnouncementReportService.Report> list(@RequestParam(defaultValue = "false") boolean all, @RequestParam(defaultValue = "0") int page) { return reports.list(all,page); }
    @PatchMapping("/api/admin/announcement-reports/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resolve(@PathVariable long id, @Valid @RequestBody Resolution request, Principal principal) { reports.resolve(id, request.note(), principal.getName()); }
}
