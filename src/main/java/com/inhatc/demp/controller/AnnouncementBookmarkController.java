package com.inhatc.demp.controller;

import com.inhatc.demp.config.security.MemberPrincipal;
import com.inhatc.demp.dto.announcement.*;
import com.inhatc.demp.service.AnnouncementBookmarkService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/me/bookmarks")
@RequiredArgsConstructor
public class AnnouncementBookmarkController {
    private final AnnouncementBookmarkService bookmarks;
    public record SaveRequest(@NotNull Boolean saved) {}
    public record SavedIds(List<Long> savedIds) {}
    @PutMapping("/{announcementId}")
    public BookmarkState setSaved(@AuthenticationPrincipal MemberPrincipal member, @PathVariable long announcementId,
                                 @Valid @RequestBody SaveRequest request) {
        return bookmarks.setSaved(member.getMemberId(), announcementId, request.saved());
    }
    @GetMapping("/status")
    public SavedIds status(@AuthenticationPrincipal MemberPrincipal member,
                           @RequestParam(defaultValue = "") List<Long> announcementIds) {
        return new SavedIds(bookmarks.savedIds(member.getMemberId(), announcementIds));
    }
    @GetMapping
    public BookmarkPage list(@AuthenticationPrincipal MemberPrincipal member,
                             @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return bookmarks.list(member.getMemberId(), page, size);
    }
}
