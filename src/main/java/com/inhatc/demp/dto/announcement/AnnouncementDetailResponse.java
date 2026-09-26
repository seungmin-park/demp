package com.inhatc.demp.dto.announcement;

import static com.inhatc.demp.config.aws.AwsS3Config.BUCKET_URL;

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

    private String image;
    private Company company;
    private String title;
    private int minCareer;
    private int maxCareer;
    private LocalDateTime startedDate;
    private LocalDateTime deadLineDate;
    private String content;
    private String accessUrl;
    private int payment;
    private Set<Language> language;
    private JobPosition position;
    private AnnouncementType announcementType;

    public static AnnouncementDetailResponse from(Announcement announcement) {
        return AnnouncementDetailResponse.builder()
                .title(announcement.getTitle())
                .company(announcement.getCompany())
                .announcementType(announcement.getAnnouncementType())
                .image(BUCKET_URL + announcement.getImage().getSaveFileName())
                .position(announcement.getJobPosition())
                .minCareer(announcement.getCareer().getMinCareer())
                .maxCareer(announcement.getCareer().getMaxCareer())
                .startedDate(announcement.getRecruitPeriod().getStartedDate())
                .deadLineDate(announcement.getRecruitPeriod().getDeadLineDate())
                .content(announcement.getDescription().getContent())
                .accessUrl(announcement.getDescription().getAccessUrl())
                .payment(announcement.getDescription().getPayment())
                .language(new LinkedHashSet<>(announcement.getDescription().getLanguages()))
                .build();
    }
}
