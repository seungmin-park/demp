package com.inhatc.demp.dto.announcement;

import com.inhatc.demp.domain.announcement.Announcement;
import com.inhatc.demp.domain.announcement.AnnouncementType;
import com.inhatc.demp.domain.announcement.Company;
import com.inhatc.demp.domain.announcement.JobPosition;
import com.inhatc.demp.domain.announcement.Language;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class AnnouncementDetailResponse {

    private com.inhatc.demp.domain.announcement.RecruitmentAudience recruitmentAudience;
    private String cohort;
    private Integer stipendAmount;
    private String stipendNote;
    private String sourceName;
    private String sourceIdentifier;
    private String applicationUrl;
    private LocalDateTime sourceVerifiedAt;
    private String image;
    private com.inhatc.demp.domain.announcement.PublicationStatus publicationStatus;
    private com.inhatc.demp.domain.announcement.EducationDetails education;
    private Company company;
    private String title;
    private int minCareer;
    private int maxCareer;
    private LocalDateTime startedDate;
    private LocalDateTime deadLineDate;
    private String content;
    private String accessUrl;
    private Integer payment;
    private com.inhatc.demp.domain.announcement.SalaryStatus salaryStatus;
    private Integer salaryMax;
    private Set<Language> language;
    private JobPosition position;
    private AnnouncementType announcementType;

    public static AnnouncementDetailResponse from(Announcement announcement, String imageUrl) {
        return AnnouncementDetailResponse.builder()
                .recruitmentAudience(announcement.getRecruitmentAudience()).cohort(announcement.getCohort())
                .stipendAmount(announcement.getStipendAmount()).stipendNote(announcement.getStipendNote())
                .publicationStatus(announcement.getPublicationStatus())
                .sourceName(announcement.getSourceName()).sourceIdentifier(announcement.getSourceIdentifier())
                .applicationUrl(announcement.getApplicationUrl()).sourceVerifiedAt(announcement.getSourceVerifiedAt())
                .education(announcement.getEducation())
                .title(announcement.getTitle())
                .company(announcement.getCompany())
                .announcementType(announcement.getAnnouncementType())
                .image(imageUrl)
                .position(announcement.getJobPosition())
                .minCareer(announcement.getCareer().getMinCareer())
                .maxCareer(announcement.getCareer().getMaxCareer())
                .startedDate(announcement.getRecruitPeriod().getStartedDate())
                .deadLineDate(announcement.getRecruitPeriod().getDeadLineDate())
                .content(announcement.getDescription().getContent())
                .accessUrl(announcement.getDescription().getAccessUrl())
                .payment(announcement.getDescription().getPayment())
                .salaryStatus(announcement.getDescription().getSalaryStatus())
                .salaryMax(announcement.getDescription().getSalaryMax())
                .language(new LinkedHashSet<>(announcement.getDescription().getLanguages()))
                .build();
    }
}
