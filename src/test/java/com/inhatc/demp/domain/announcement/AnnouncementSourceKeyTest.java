package com.inhatc.demp.domain.announcement;
import org.junit.jupiter.api.*;
import static org.assertj.core.api.Assertions.*;
class AnnouncementSourceKeyTest {
    @Test
    @DisplayName("국제화 원문의 기본 포트는 같고 별도 포트의 공고는 구별한다")
    void preservesInternationalNonDefaultPort() {
        String ordinary = AnnouncementSourceKey.of("https://채용.example.com/jobs/1", "DEMP", null);
        assertThat(AnnouncementSourceKey.of("https://채용.example.com:443/jobs/1", "DEMP", null)).isEqualTo(ordinary);
        assertThat(AnnouncementSourceKey.of("https://채용.example.com:8443/jobs/1", "DEMP", null)).isNotEqualTo(ordinary);
    }
}
