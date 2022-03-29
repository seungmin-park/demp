package com.inhatc.demp.controller;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.domain.AnnouncementType;
import com.inhatc.demp.dto.AnnouncementResponse;
import com.inhatc.demp.dto.AnnouncementScroll;
import com.inhatc.demp.dto.AnnouncementSearchCondition;
import com.inhatc.demp.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/announce")
@RequiredArgsConstructor
public class AnnouncementController{

    private final AnnouncementService announcementService;

    @GetMapping("")
    public List<AnnouncementResponse> announce(@ModelAttribute AnnouncementSearchCondition announcementSearchCondition) {
        log.info("AnnouncementController.announce");
        List<Announcement> announcements = announcementService.findAllByAnnouncementType(announcementSearchCondition);
        return announcements.stream().map(AnnouncementResponse::new).collect(Collectors.toList());
    }

    @GetMapping("/scroll")
    public List<AnnouncementScroll> scroll() {
        log.info("AnnouncementController.scroll");
        List<Announcement> announcements = announcementService.findAll();
        List<AnnouncementScroll> result = announcements.stream()
                .map(a -> new AnnouncementScroll(a.getId(), a.getTitle(), a.getCompany()))
                .collect(Collectors.toList());
        log.info("result={}",result);
        return result;
    }

    @GetMapping("/detail/{AnnouncementId}")
    public ResponseEntity<Announcement> detail(@PathVariable Long AnnouncementId) {
        log.info("AnnouncementController.Detail");
        Optional<Announcement> result = announcementService.findById(AnnouncementId);
        if (result.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
        log.info("result={}",result);
        return new ResponseEntity<>(result.get(),HttpStatus.OK);
    }
}
