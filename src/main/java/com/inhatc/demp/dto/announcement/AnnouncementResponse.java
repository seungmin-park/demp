package com.inhatc.demp.dto.announcement;

import static com.inhatc.demp.config.aws.AwsS3Config.BUCKET_URL;

import com.inhatc.demp.domain.announcemnet.Announcement;
import com.inhatc.demp.domain.announcemnet.JobPosition;
import com.inhatc.demp.domain.announcemnet.Language;
import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnnouncementResponse {

    private Long id;
    private String title;
    private Set<Language> language = new HashSet<>();
    private JobPosition position;
    private String image;

    public AnnouncementResponse(Announcement announcement) {
        this.id = announcement.getId();
        this.title = announcement.getTitle();
        this.language = new HashSet<>(announcement.getDescription().getLanguages());
        this.position = announcement.getJobPosition();
        this.image = BUCKET_URL + announcement.getImage().getSaveFileName();
    }
}
