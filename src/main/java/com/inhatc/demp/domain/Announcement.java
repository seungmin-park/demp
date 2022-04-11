package com.inhatc.demp.domain;

import jdk.jfr.Timestamp;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement {

    @Id
    @GeneratedValue
    private Long id;
    private String title;
    private String language;
    private String position;
    private String career;
    @Lob
    private String content;
    private String accessUrl;
    private int payment;
    @Embedded
    private Company company;
    @Embedded
    private UploadFile image;
    @Enumerated(EnumType.STRING)
    private AnnouncementType announcementType;

    private LocalDateTime startedDate;
    private LocalDateTime deadLineDate;

    @Timestamp
    private LocalDateTime createdDate = LocalDateTime.now();

    public Announcement(String title, String language, String position, String career, String content, String accessUrl, int payment, Company company, UploadFile image, AnnouncementType announcementType, LocalDateTime startedDate, LocalDateTime deadLineDate) {
        this.title = title;
        this.language = language;
        this.position = position;
        this.career = career;
        this.content = content;
        this.accessUrl = accessUrl;
        this.payment = payment;
        this.company = company;
        this.image = image;
        this.announcementType = announcementType;
        this.startedDate = startedDate;
        this.deadLineDate = deadLineDate;
    }
}
