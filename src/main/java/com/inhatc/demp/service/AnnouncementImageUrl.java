package com.inhatc.demp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementImageUrl {
    private final String baseUrl;

    public AnnouncementImageUrl(@Value("${cloud.aws.s3.public-base-url:https://inhatc-demp.s3.ap-northeast-2.amazonaws.com/}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String forImage(com.inhatc.demp.domain.announcement.UploadFile image) {
        return image == null ? "" : forKey(image.getSaveFileName());
    }

    public String forKey(String key) {
        return (baseUrl.endsWith("/") ? baseUrl : baseUrl + "/") + key;
    }
}
