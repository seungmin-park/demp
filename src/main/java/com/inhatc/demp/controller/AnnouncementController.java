package com.inhatc.demp.controller;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.domain.AnnouncementType;
import com.inhatc.demp.dto.AnnouncementDto;
import com.inhatc.demp.service.AnnouncementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/announce")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    @PostMapping("")
    public List<Announcement> announce(@RequestBody AnnouncementDto announcementDto) {
        log.info("AnnouncementController.announce");
        List<Announcement> result = announcementService.findByAnnouncementType(AnnouncementType.valueOf(announcementDto.getTypeName()));
        return result;
    }

    @PostMapping("/detail")
    public Announcement Detail(@RequestBody AnnouncementDto announcementDto) {
        log.info("AnnouncementController.Detail");
        Optional<Announcement> result = announcementService.findById(announcementDto.getId());
        log.info("result={}",result);
        return result.get();
    }
}
