package com.inhatc.demp.dto.announcement;

import com.inhatc.demp.domain.announcement.Announcement;
import com.inhatc.demp.domain.announcement.Company;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class AnnouncementScroll {
    private Long id;
    private String title;
    private Company company;
    private String image;

    public AnnouncementScroll(Announcement announcement, String imageUrl) {
        this.id = announcement.getId();
        this.title = announcement.getTitle();
        this.company = announcement.getCompany();
        this.image = imageUrl;
    }
}
