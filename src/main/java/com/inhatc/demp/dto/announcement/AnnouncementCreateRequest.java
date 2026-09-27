package com.inhatc.demp.dto.announcement;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class AnnouncementCreateRequest extends AnnouncementFields {
    public AnnouncementCreateRequest() { setPublicationStatus(com.inhatc.demp.domain.announcement.PublicationStatus.DRAFT); }
    private MultipartFile image;
}
