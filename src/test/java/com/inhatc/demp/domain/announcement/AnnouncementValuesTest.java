package com.inhatc.demp.domain.announcement;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class AnnouncementValuesTest {

    @Test
    @DisplayName("경력의 최솟값과 최댓값은 음수가 될 수 없다")
    void rejectsNegativeCareer() {
        assertThatThrownBy(() -> Career.builder().minCareer(-1).maxCareer(3).build()).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Career.builder().minCareer(0).maxCareer(-1).build()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("유한한 최대 경력은 최소 경력보다 작을 수 없다")
    void rejectsReversedCareerRange() {
        assertThatThrownBy(() -> Career.builder().minCareer(5).maxCareer(3).build()).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("최대 경력 0은 상한 없음으로 허용한다")
    void acceptsUnlimitedMaximumCareer() {
        assertThatCode(() -> Career.builder().minCareer(10).maxCareer(0).build()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("공고 마감일은 시작일보다 빠를 수 없다")
    void rejectsReversedRecruitPeriod() {
        assertThatThrownBy(() -> RecruitPeriod.builder()
                .startedDate(LocalDateTime.of(2026, 9, 30, 23, 59))
                .deadLineDate(LocalDateTime.of(2026, 9, 1, 0, 0))
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
