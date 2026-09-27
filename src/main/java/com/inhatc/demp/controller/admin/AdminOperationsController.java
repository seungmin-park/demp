package com.inhatc.demp.controller.admin;

import com.inhatc.demp.dto.admin.*;
import com.inhatc.demp.dto.announcement.*;
import com.inhatc.demp.error.ApiException;
import com.inhatc.demp.service.AnnouncementService;
import com.inhatc.demp.service.admin.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminOperationsController {
    private final AdminOverviewService overview;
    private final AnnouncementService announcements;
    private final AdminAnnouncementService management;
    private final AdminCommunityService community;

    @GetMapping("/overview")
    public AdminOverview overview() { return overview.overview(); }

    @GetMapping("/announcements")
    public Slice<AnnouncementResponse> announcements(@ModelAttribute AnnouncementSearchCondition condition,
                                                     @RequestParam(defaultValue = "0") int page) {
        if (page < 0) throw new ApiException(HttpStatus.BAD_REQUEST);
        return announcements.findAnnouncementSlice(condition, PageRequest.of(page, 20));
    }
    @GetMapping("/announcements/{id}")
    public AnnouncementDetailResponse announcement(@PathVariable long id) {
        return announcements.findDetailResponse(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND));
    }
    @PostMapping("/announcements")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminMutationResult createAnnouncement(@Valid @ModelAttribute AnnouncementCreateRequest request) throws IOException {
        announcements.createAnnouncement(request); return new AdminMutationResult(false);
    }
    @PatchMapping("/announcements/{id}")
    public AdminMutationResult updateAnnouncement(@PathVariable long id, @Valid @ModelAttribute AnnouncementUpdateRequest request) throws IOException {
        return management.update(id, request);
    }
    @DeleteMapping("/announcements/{id}")
    public AdminMutationResult deleteAnnouncement(@PathVariable long id) { return management.delete(id); }

    @GetMapping("/questions")
    public AdminPage<AdminPost> questions(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page) {
        return AdminPage.from(community.questions(q,page));
    }
    @GetMapping("/answers")
    public AdminPage<AdminPost> answers(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page) {
        return AdminPage.from(community.answers(q,page));
    }
    @GetMapping("/questions/{id}")
    public AdminPost question(@PathVariable long id) { return community.question(id); }
    @GetMapping("/answers/{id}")
    public AdminPost answer(@PathVariable long id) { return community.answer(id); }
    @PatchMapping("/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateQuestion(@PathVariable long id, @Valid @RequestBody QuestionEdit request) {
        community.updateQuestion(id,request.title(),request.content());
    }
    @PatchMapping("/answers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateAnswer(@PathVariable long id, @Valid @RequestBody AnswerEdit request) {
        community.updateAnswer(id,request.content());
    }
    @DeleteMapping("/questions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuestion(@PathVariable long id) { community.deleteQuestion(id); }
    @DeleteMapping("/answers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAnswer(@PathVariable long id) { community.deleteAnswer(id); }
    public record QuestionEdit(@NotBlank String title, @NotBlank String content) {}
    public record AnswerEdit(@NotBlank String content) {}
}
