package com.inhatc.demp.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AnnouncementImageUrlTest {
    @Test
    @DisplayName("설정한 공개 주소와 저장 키를 슬래시 하나로 연결한다")
    void usesConfiguredBaseUrl() {
        AnnouncementImageUrl imageUrl = new AnnouncementImageUrl("https://cdn.example/images");

        assertThat(imageUrl.forKey("saved.png")).isEqualTo("https://cdn.example/images/saved.png");
    }
}
