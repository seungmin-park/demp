package com.inhatc.demp.controller;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.dto.announcement.*;
import com.inhatc.demp.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/announce")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @GetMapping("")
    public Slice<AnnouncementResponse> getAllAnnounces(@ModelAttribute AnnouncementSearchCondition announcementSearchCondition, Pageable pageable) {
        log.info("AnnouncementController.pageTest");
        return announcementService.getAnnounceScroll(announcementSearchCondition, pageable);
    }

    @GetMapping("/scroll")
    public List<AnnouncementScroll> scroll() {
        log.info("AnnouncementController.scroll");
        List<Announcement> announcements = announcementService.findAll();
        List<AnnouncementScroll> result = announcements.stream()
                .map(a -> new AnnouncementScroll(a))
                .collect(Collectors.toList());
        log.info("result={}", result);
        return result;
    }

    @GetMapping("/detail/{AnnouncementId}")
    public ResponseEntity<AnnouncementDetail> getDetailAnnounce(@PathVariable Long AnnouncementId) {
        log.info("AnnouncementController.Detail");
        Optional<Announcement> optionalAnnouncement = announcementService.findById(AnnouncementId);
        if (optionalAnnouncement.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Announcement announcement = optionalAnnouncement.get();
        AnnouncementDetail result = AnnouncementDetail.getBuild(announcement);

        log.info("result={}", result);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping(value = "/add")
    public String saveAnnounce(@Valid @ModelAttribute AnnouncementForm param) throws IOException {
        log.info("AnnouncementController.saveTest");
        log.info("{}",param);
        announcementService.save(param);
        return "ok";
    }
}
