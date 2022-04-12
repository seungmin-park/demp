package com.inhatc.demp.dto.announcement;

import com.inhatc.demp.domain.Announcement;
import com.inhatc.demp.domain.AnnouncementType;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.AnnotatedArrayType;

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
        this.image = announcement.getImage().getSaveFileName();
    }
}
