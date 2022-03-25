package com.inhatc.demp.controller;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.domain.AnnouncementType;
import com.inhatc.demp.dto.AnnouncementDto;
import com.inhatc.demp.dto.AnnouncementSearch;
import com.inhatc.demp.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/announce")
@RequiredArgsConstructor
public class AnnouncementController{

    private final AnnouncementService announcementService;

    @PostMapping("")
    public List<Announcement> announce(@RequestBody AnnouncementSearch announcementSearch) {
        log.info("AnnouncementController.announce");
        List<Announcement> result = announcementService.findByAnnouncementType(AnnouncementType.valueOf(announcementSearch.getTypeName()));
        return result;
    }

    @PostMapping("/scroll")
    public List<AnnouncementDto> scroll() {
        log.info("AnnouncementController.scroll");
        List<Announcement> announcements = announcementService.findAll();

        List<AnnouncementDto> result = new ArrayList<>();
        for (Announcement announcement : announcements) {
            result.add(new AnnouncementDto(announcement.getId(), announcement.getTitle(), announcement.getImage(), announcement.getCompany()));
        }

        return result;
    }

    @PostMapping("/detail")
    public Announcement detail(@RequestBody AnnouncementSearch announcementSearch) {
        log.info("AnnouncementController.Detail");
        Optional<Announcement> result = announcementService.findById(announcementSearch.getId());
        log.info("result={}",result);
        return result.get();
    }
}
