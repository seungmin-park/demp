package com.inhatc.demp.dto.announcement;

import com.inhatc.demp.config.aws.AwsS3Config;
import com.inhatc.demp.domain.Announcement;
import lombok.Getter;
import lombok.Setter;

import static com.inhatc.demp.config.aws.AwsS3Config.*;

@Getter
@Setter
public class AnnouncementResponse {

    private Long id;
    private String title;
    private String language;
    private String position;
    private String image;

    public AnnouncementResponse(Announcement announcement) {
        this.id = announcement.getId();
        this.title = announcement.getTitle();
        this.language = announcement.getLanguages().toString();
        this.position = announcement.getPosition();
        this.image = BUCKET_URL + announcement.getImage().getSaveFileName();
    }
}
