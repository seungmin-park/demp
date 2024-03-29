package com.inhatc.demp.domain.announcemnet;

import java.time.LocalDateTime;
import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecruitPeriod {

    private LocalDateTime startedDate;
    private LocalDateTime deadLineDate;

    public RecruitPeriod(LocalDateTime startedDate, LocalDateTime deadLineDate) {
        this.startedDate = startedDate;
        this.deadLineDate = deadLineDate;
    }
}
