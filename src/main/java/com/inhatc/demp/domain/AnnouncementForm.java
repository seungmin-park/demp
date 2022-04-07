package com.inhatc.demp.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnnouncementForm {
    private String title;
    private String position;
    private String content;
    private String language;
    private String accessUrl;
    private String career;
    private int payment;
    private Company company;
    private MultipartFile image;
    private String startedDate;
    private String deadLineDate;
    private AnnouncementType type;

}
