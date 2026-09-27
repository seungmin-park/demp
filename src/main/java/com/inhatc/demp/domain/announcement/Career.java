package com.inhatc.demp.domain.announcement;

import lombok.Builder;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Career {

    private int minCareer;
    private int maxCareer;

    @Builder
    private Career(int minCareer, int maxCareer) {
        if (minCareer < 0 || maxCareer < 0) {
            throw new IllegalArgumentException("경력은 음수가 될 수 없습니다.");
        }
        if (maxCareer != 0 && minCareer > maxCareer) {
            throw new IllegalArgumentException("최소 경력은 최대 경력보다 클 수 없습니다.");
        }
        this.minCareer = minCareer;
        this.maxCareer = maxCareer;
    }
}
