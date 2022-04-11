package com.inhatc.demp.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.inhatc.demp.domain.AnnouncementType;
import com.inhatc.demp.domain.Company;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class AnnouncementDetail {

    private String image;
    private Company company;
    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    private LocalDateTime startedDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    private LocalDateTime deadLineDate;

    private String language;
    private String position;
    private int payment;
    private String career;
    private String content;
    private String accessUrl;
    private AnnouncementType announcementType;
}
