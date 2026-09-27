package com.inhatc.demp.dto.announcement;

import com.inhatc.demp.domain.announcement.Announcement;
import com.inhatc.demp.domain.announcement.JobPosition;
import com.inhatc.demp.domain.announcement.Language;
import java.util.HashSet;
import java.time.LocalDateTime;
import com.inhatc.demp.domain.announcement.AnnouncementType;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnnouncementResponse {

    private com.inhatc.demp.domain.announcement.RecruitmentAudience recruitmentAudience;
    private Long id;
    private String title;
    private Set<Language> language = new HashSet<>();
    private JobPosition position;
    private String image;
    private com.inhatc.demp.domain.announcement.PublicationStatus publicationStatus;
    private com.inhatc.demp.domain.announcement.EducationDetails education;
    private String company;
    private AnnouncementType announcementType;
    private Integer minCareer;
    private Integer maxCareer;
    private Integer payment;
    private LocalDateTime startedDate;
    private LocalDateTime deadLineDate;

    public AnnouncementResponse(Announcement announcement, String imageUrl) {
        this.education = announcement.getEducation();
        this.publicationStatus = announcement.getPublicationStatus();
        this.recruitmentAudience = announcement.getRecruitmentAudience();
        this.id = announcement.getId();
        this.title = announcement.getTitle();
        this.language = new HashSet<>(announcement.getDescription().getLanguages());
        this.position = announcement.getJobPosition();
        this.image = imageUrl;
        this.company = announcement.getCompany() == null ? null : announcement.getCompany().getName();
        this.announcementType = announcement.getAnnouncementType();
        this.minCareer = announcement.getCareer() == null ? null : announcement.getCareer().getMinCareer();
        this.maxCareer = announcement.getCareer() == null ? null : announcement.getCareer().getMaxCareer();
        this.payment = announcement.getDescription().getPayment();
        this.startedDate = announcement.getRecruitPeriod() == null ? null : announcement.getRecruitPeriod().getStartedDate();
        this.deadLineDate = announcement.getRecruitPeriod() == null ? null : announcement.getRecruitPeriod().getDeadLineDate();
    }
}
