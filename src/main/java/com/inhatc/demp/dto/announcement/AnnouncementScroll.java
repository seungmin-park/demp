package com.inhatc.demp.dto.announcement;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.domain.Company;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import static com.inhatc.demp.config.aws.AwsS3Config.BUCKET_URL;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class AnnouncementScroll {
    private Long id;
    private String title;
    private Company company;
    private String image;

    public AnnouncementScroll(Announcement announcement) {
        this.id = announcement.getId();
        this.title = announcement.getTitle();
        this.company = announcement.getCompany();
        this.image = BUCKET_URL + announcement.getImage().getSaveFileName();
    }
}
