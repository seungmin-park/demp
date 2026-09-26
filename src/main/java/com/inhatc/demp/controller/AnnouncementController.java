package com.inhatc.demp.controller;

import com.inhatc.demp.domain.announcement.Announcement;
import com.inhatc.demp.dto.announcement.*;
import com.inhatc.demp.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
        List<Announcement> announcements = announcementService.findAll();
        List<AnnouncementScroll> result = announcements.stream()
                .map(AnnouncementScroll::new)
                .collect(Collectors.toList());

        return result;
    }

    @GetMapping("/detail/{AnnouncementId}")
    public ResponseEntity<AnnouncementDetailResponse> getAnnouncementDetail(@PathVariable("AnnouncementId") Long announcementId) {
        Optional<Announcement> optionalAnnouncement = announcementService.findById(announcementId);
        if (optionalAnnouncement.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Announcement announcement = optionalAnnouncement.get();
        AnnouncementDetailResponse result = AnnouncementDetailResponse.from(announcement);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping(value = "/add")
    public String createAnnouncement(@Valid @ModelAttribute AnnouncementCreateRequest param) throws IOException {
        announcementService.createAnnouncement(param);
        return "ok";
    }
}
