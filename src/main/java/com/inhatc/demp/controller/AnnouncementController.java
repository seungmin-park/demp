package com.inhatc.demp.controller;

import com.inhatc.demp.dto.announcement.*;
import com.inhatc.demp.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/announce")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping("")
    public Slice<AnnouncementResponse> listAnnouncements(@ModelAttribute AnnouncementSearchCondition announcementSearchCondition, Pageable pageable) {
        return announcementService.findAnnouncementSlice(announcementSearchCondition, pageable);
    }

    @GetMapping("/scroll")
    public List<AnnouncementScroll> scroll() {
        return announcementService.findScrollResponses();
    }

    @GetMapping("/detail/{AnnouncementId}")
    public ResponseEntity<AnnouncementDetailResponse> getAnnouncementDetail(@PathVariable("AnnouncementId") Long announcementId,
            @RequestParam(defaultValue = "true") boolean recordView) {
        Optional<AnnouncementDetailResponse> response = recordView ? announcementService.recordViewAndGetDetail(announcementId) : announcementService.findDetailResponse(announcementId);
        if (response.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        return new ResponseEntity<>(response.get(), HttpStatus.OK);
    }

    @PostMapping(value = "/add")
    public String createAnnouncement(@Valid @ModelAttribute AnnouncementCreateRequest param, java.security.Principal principal) throws IOException {
        announcementService.createAnnouncement(param, principal.getName());
        return "ok";
    }
}
