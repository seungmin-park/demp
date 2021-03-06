package com.inhatc.demp.dto.announcement;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.domain.AnnouncementType;
import com.inhatc.demp.domain.Company;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import static com.inhatc.demp.config.aws.AwsS3Config.BUCKET_URL;

@Data
@Builder
@AllArgsConstructor
public class AnnouncementDetail {

    private String image;
    private Company company;
    private String title;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    private LocalDateTime startedDate;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "Asia/Seoul")
    private LocalDateTime deadLineDate;

    private String language;
    private String position;
    private int payment;
    private int minCareer;
    private int maxCareer;
    private String content;
    private String accessUrl;
    private AnnouncementType announcementType;

    public static AnnouncementDetail getBuild(Announcement announcement) {
        return AnnouncementDetail.builder()
                .title(announcement.getTitle())
                .content(announcement.getContent().replaceAll("\r\n","<br>"))
                .company(announcement.getCompany())
                .payment(announcement.getPayment())
                .announcementType(announcement.getAnnouncementType())
                .accessUrl(announcement.getAccessUrl())
                .startedDate(announcement.getStartedDate())
                .deadLineDate(announcement.getDeadLineDate())
                .image(BUCKET_URL+announcement.getImage().getSaveFileName())
                .position(announcement.getPosition())
                .minCareer(announcement.getMinCareer())
                .maxCareer(announcement.getMaxCareer())
                .language(announcement.getLanguages().toString())
                .build();
    }
}
