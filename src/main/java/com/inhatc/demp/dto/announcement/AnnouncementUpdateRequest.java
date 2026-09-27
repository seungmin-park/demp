package com.inhatc.demp.dto.announcement;

import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class AnnouncementUpdateRequest extends AnnouncementFields {
    private MultipartFile image;
}
