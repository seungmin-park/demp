package com.inhatc.demp.config;

import com.inhatc.demp.dto.announcement.AnnouncementDetailResponse;
import com.inhatc.demp.service.AnnouncementService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("local")
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:local-seed-test;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.sql.init.mode=always"
})
class LocalSeedTest {

    @Autowired
    private AnnouncementService announcementService;

    @Test
    @DisplayName("로컬 공고 예제는 이미지 주소와 함께 조회된다")
    void localAnnouncementHasImageUrl() {
        AnnouncementDetailResponse detail = announcementService.findDetailResponse(-1L).orElseThrow();

        assertThat(detail.getTitle()).isEqualTo("로컬 개발자 모집 예제");
        assertThat(detail.getImage()).endsWith("noimg.jpg");
    }
}
