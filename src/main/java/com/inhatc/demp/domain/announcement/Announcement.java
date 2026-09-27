package com.inhatc.demp.domain.announcement;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Embedded;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
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
    @GeneratedValue(generator = "announcement_legacy_id")
    @SequenceGenerator(name = "announcement_legacy_id", sequenceName = "hibernate_sequence", allocationSize = 1)
    private Long id;
    private String title;
    @Embedded
    private EducationDetails education;

    public void changeEducation(EducationDetails education) {
        this.education = announcementType == AnnouncementType.EDU ? education : null;
    }
    @Embedded
    private Career career;
    @Embedded
    private Description description;
    @Embedded
    private Company company;
    @Embedded
    private UploadFile image;
    @ElementCollection
    @CollectionTable(name = "announcement_body_image", joinColumns = @JoinColumn(name = "announcement_id"))
    private List<UploadFile> bodyImages = new ArrayList<>();
    @Embedded
    private RecruitPeriod recruitPeriod;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private AnnouncementType announcementType;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private JobPosition jobPosition;


    public void replaceBodyImages(List<UploadFile> images) {
        this.bodyImages.clear();
        this.bodyImages.addAll(images);
    }

    public void changeDescription(Description description) {
        this.description = description;
    }

    public void revise(String title, Company company, Career career, RecruitPeriod period, Description description,
                       AnnouncementType type, JobPosition position, UploadFile replacement) {
        this.title = title; this.company = company; this.career = career; this.recruitPeriod = period;
        this.description = description; this.announcementType = type; this.jobPosition = position;
        if (replacement != null) this.image = replacement;
    }

    @Builder
    private Announcement(String title, Career career, Description description, Company company, UploadFile image,
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
