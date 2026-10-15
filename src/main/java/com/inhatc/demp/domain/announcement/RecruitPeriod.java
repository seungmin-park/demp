package com.inhatc.demp.domain.announcement;

import lombok.Builder;
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

    @jakarta.persistence.Enumerated(jakarta.persistence.EnumType.STRING)
    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    private DeadlineMode deadlineMode;
    public DeadlineMode getDeadlineMode() { return DeadlineMode.resolve(deadlineMode, deadLineDate); }
    public boolean hasConfirmedDeadline() { return getDeadlineMode().hasNoFixedDeadline() || getDeadlineMode() == DeadlineMode.FIXED && deadLineDate != null; }

    private LocalDateTime startedDate;
    private LocalDateTime deadLineDate;

    @Builder
    private RecruitPeriod(LocalDateTime startedDate, LocalDateTime deadLineDate, DeadlineMode deadlineMode) {
        if (startedDate != null && deadLineDate != null && deadLineDate.isBefore(startedDate)) {
            throw new IllegalArgumentException("마감일은 시작일보다 빠를 수 없습니다.");
        }
        var resolved = DeadlineMode.resolve(deadlineMode, deadLineDate);
        if (resolved != DeadlineMode.FIXED && deadLineDate != null) throw new InvalidRecruitmentDeadlineException();
        this.deadlineMode = resolved;
        this.startedDate = startedDate;
        this.deadLineDate = deadLineDate;
    }
}
