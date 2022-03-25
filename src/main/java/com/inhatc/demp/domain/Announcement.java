package com.inhatc.demp.domain;

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

    private String image;
    private String company;
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

    public Announcement(String image, String company, String title, LocalDateTime startedDate, LocalDateTime deadLineDate, String language, String position, int payment, String career, String content, String accessUrl, AnnouncementType announcementType) {
        this.image = image;
        this.company = company;
        this.title = title;
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
