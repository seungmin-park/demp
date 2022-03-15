package com.inhatc.demp.domain;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import javax.persistence.*;

@Entity
@Getter
@ToString
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement {

    @Id
    @GeneratedValue
    private Long id;

    private String title;
    private String image;
    private String skill;
    private String career;

    @Lob
    private String content;

    @Enumerated(EnumType.STRING)
    private AnnouncementType announcementType;

    public Announcement(String title, String image, String skill, String career, String content, AnnouncementType announcementType) {
        this.title = title;
        this.image = image;
        this.skill = skill;
        this.career = career;
        this.content = content;
        this.announcementType = announcementType;
    }

    public Announcement(String title, String image, String skill, String career, AnnouncementType announcementType) {
        this.title = title;
        this.image = image;
        this.skill = skill;
        this.career = career;
        this.announcementType = announcementType;
    }
}
