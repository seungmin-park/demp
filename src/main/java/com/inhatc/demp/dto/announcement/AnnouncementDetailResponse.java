package com.inhatc.demp.dto.announcement;

import static com.inhatc.demp.config.aws.AwsS3Config.BUCKET_URL;

import com.inhatc.demp.domain.announcemnet.Announcement;
import com.inhatc.demp.domain.announcemnet.AnnouncementType;
import com.inhatc.demp.domain.announcemnet.Career;
import com.inhatc.demp.domain.announcemnet.Company;
import com.inhatc.demp.domain.announcemnet.Description;
import com.inhatc.demp.domain.announcemnet.JobPosition;
import com.inhatc.demp.domain.announcemnet.RecruitPeriod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@AllArgsConstructor
public class AnnouncementDetailResponse {

    private String image;
    private Company company;
    private String title;
    private RecruitPeriod recruitPeriod;
    private Career career;
    private Description description;
    private JobPosition position;
    private AnnouncementType announcementType;

    public static AnnouncementDetailResponse getBuild(Announcement announcement) {
        return AnnouncementDetailResponse.builder()
                .title(announcement.getTitle())
                .description(announcement.getDescription())
                .company(announcement.getCompany())
                .announcementType(announcement.getAnnouncementType())
                .recruitPeriod(announcement.getRecruitPeriod())
                .image(BUCKET_URL + announcement.getImage().getSaveFileName())
                .position(announcement.getJobPosition())
                .career(announcement.getCareer())
                .build();
    }
}
