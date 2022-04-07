package com.inhatc.demp.dto;

import com.inhatc.demp.domain.AnnouncementType;
import com.inhatc.demp.domain.Company;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AnnouncementDetail {

    private String image;
    private Company company;
    private String title;
    private LocalDateTime startedDate;
    private LocalDateTime deadLineDate;
    private String language;
    private String position;
    private int payment;
    private String career;
    private String content;
    private String accessUrl;
    private AnnouncementType announcementType;
}
