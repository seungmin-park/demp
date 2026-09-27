package com.inhatc.demp.domain.announcement;

import java.time.LocalDateTime;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitPeriod {

    private LocalDateTime startedDate;
    private LocalDateTime deadLineDate;

    public RecruitPeriod(LocalDateTime startedDate, LocalDateTime deadLineDate) {
        if (startedDate == null || deadLineDate == null) {
            throw new IllegalArgumentException("공고 기간은 필수입니다.");
        }
        if (deadLineDate.isBefore(startedDate)) {
            throw new IllegalArgumentException("마감일은 시작일보다 빠를 수 없습니다.");
        }
        this.startedDate = startedDate;
        this.deadLineDate = deadLineDate;
    }
}
