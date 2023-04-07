package com.inhatc.demp.domain.announcemnet;

import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@ToString
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement {

    @Id
    @GeneratedValue
    private Long id;
    private String title;
    @Embedded
    private Career career;
    @Embedded
    private Description description;
    @Embedded
    private Company company;
    @Embedded
    private UploadFile image;
    @Embedded
    private RecruitPeriod recruitPeriod;
    @Enumerated(EnumType.STRING)
    private AnnouncementType announcementType;
    @Enumerated(EnumType.STRING)
    private JobPosition jobPosition;


    @Builder
    public Announcement(String title, Career career, Description description, Company company, UploadFile image,
                        RecruitPeriod recruitPeriod, AnnouncementType announcementType, JobPosition jobPosition) {
        this.title = title;
        this.career = career;
        this.description = description;
        this.company = company;
        this.image = image;
        this.recruitPeriod = recruitPeriod;
        this.announcementType = announcementType;
        this.jobPosition = jobPosition;
    }
}
