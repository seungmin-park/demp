package com.inhatc.demp.dto.announcement;

import com.inhatc.demp.domain.announcement.Announcement;
import com.inhatc.demp.domain.announcement.Company;
import com.inhatc.demp.domain.announcement.AnnouncementType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class AnnouncementScroll {
    private com.inhatc.demp.domain.announcement.RecruitmentAudience recruitmentAudience;
    private Long id;
    private String title;
    private Company company;
    private boolean recruitmentClosed;
    private String image;
    private AnnouncementType announcementType;
    private Integer minCareer;
    private Integer maxCareer;

    public AnnouncementScroll(Announcement announcement, String imageUrl) {
        this.recruitmentAudience = announcement.getRecruitmentAudience();
        this.recruitmentClosed = announcement.isRecruitmentClosed();
        this.id = announcement.getId();
        this.title = announcement.getTitle();
        this.company = announcement.getCompany();
        this.image = imageUrl;
        this.announcementType = announcement.getAnnouncementType();
        this.minCareer = announcement.getCareer() == null ? null : announcement.getCareer().getMinCareer();
        this.maxCareer = announcement.getCareer() == null ? null : announcement.getCareer().getMaxCareer();
    }
}
