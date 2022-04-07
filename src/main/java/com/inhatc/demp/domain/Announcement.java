package com.inhatc.demp.domain;

import jdk.jfr.Timestamp;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

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

    @Embedded
    private UploadFile image;
    @Embedded
    private Company company;
    private String title;
    private LocalDateTime startedDate;
    private LocalDateTime deadLineDate;
    private String language;
    private String position;
    private int payment;
    private String career;
    @Lob
    private String content;

    private String accessUrl;

    @Enumerated(EnumType.STRING)
    private AnnouncementType announcementType;

    @Timestamp
    private LocalDateTime createdDate = LocalDateTime.now();

    public Announcement(UploadFile image, Company company, String title, LocalDateTime startedDate, LocalDateTime deadLineDate, String language, String position, int payment, String career, String content, String accessUrl, AnnouncementType announcementType) {
        this.company = company;
        this.title = title;
        this.image = image;
        this.startedDate = startedDate;
        this.deadLineDate = deadLineDate;
        this.language = language;
        this.position = position;
        this.payment = payment;
        this.career = career;
        this.content = content;
        this.accessUrl = accessUrl;
        this.announcementType = announcementType;
    }

    public Announcement(String title, UploadFile image, Company company,LocalDateTime startedDate, LocalDateTime deadLineDate, String language, String position, int payment, String career, String content, String accessUrl, AnnouncementType announcementType) {
        this.company = company;
        this.title = title;
        this.image = image;
        this.startedDate = startedDate;
        this.deadLineDate = deadLineDate;
        this.language = language;
        this.position = position;
        this.payment = payment;
        this.career = career;
        this.content = content;
        this.accessUrl = accessUrl;
        this.announcementType = announcementType;
    }
}
