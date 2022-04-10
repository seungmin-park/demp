package com.inhatc.demp.controller;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.domain.AnnouncementForm;
import com.inhatc.demp.dto.AnnouncementDetail;
import com.inhatc.demp.dto.AnnouncementResponse;
import com.inhatc.demp.dto.AnnouncementScroll;
import com.inhatc.demp.dto.AnnouncementSearchCondition;
import com.inhatc.demp.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
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
    public List<AnnouncementResponse> announce(@ModelAttribute AnnouncementSearchCondition announcementSearchCondition) {
        log.info("AnnouncementController.announce");
        List<Announcement> announcements = announcementService.findAllByAnnouncementType(announcementSearchCondition);
        List<AnnouncementResponse> list = new ArrayList<>();
        for (Announcement announcement : announcements) {
            AnnouncementResponse announcementResponse = new AnnouncementResponse(announcement);
            list.add(announcementResponse);
        }
        return list;
    }

    @GetMapping("/scroll")
    public List<AnnouncementScroll> scroll() {
        log.info("AnnouncementController.scroll");
        List<Announcement> announcements = announcementService.findAll();
        List<AnnouncementScroll> result = announcements.stream()
                .map(a -> new AnnouncementScroll(a.getId(), a.getTitle(), a.getCompany(),a.getImage().getSaveFileName()))
                .collect(Collectors.toList());
        log.info("result={}", result);
        return result;
    }

    @GetMapping("/detail/{AnnouncementId}")
    public ResponseEntity<AnnouncementDetail> detail(@PathVariable Long AnnouncementId) {
        log.info("AnnouncementController.Detail");
        Optional<Announcement> optionalAnnouncement = announcementService.findById(AnnouncementId);
        if (optionalAnnouncement.isEmpty()) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Announcement announcement = optionalAnnouncement.get();

        AnnouncementDetail result = new AnnouncementDetail(announcement.getImage().getSaveFileName(), announcement.getCompany(), announcement.getTitle(),
                announcement.getStartedDate(), announcement.getDeadLineDate(), announcement.getLanguage(), announcement.getPosition(),
                announcement.getPayment(), announcement.getCareer(), announcement.getContent(), announcement.getAccessUrl(), announcement.getAnnouncementType());

        log.info("result={}", result);
        return new ResponseEntity<>(result, HttpStatus.OK);
    }

    @PostMapping(value = "/add")
    public String saveTest(@ModelAttribute AnnouncementForm param) throws IOException {
        log.info("AnnouncementController.saveTest");
        log.info("{}",param);
        announcementService.save(param);
        return "ok";
    }
}
