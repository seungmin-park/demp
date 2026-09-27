package com.inhatc.demp.domain.announcement;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import lombok.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EducationDetails {
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private DeliveryMode deliveryMode;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private EducationRegion region;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private Commitment commitment;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private FundingType fundingType;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private SelectionProcess selectionProcess;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private LearningLevel learningLevel;
    private LocalDate learningStartDate;
    private LocalDate learningEndDate;
    private Integer durationDays;

    public EducationDetails(DeliveryMode deliveryMode, EducationRegion region, Commitment commitment,
                            FundingType fundingType, SelectionProcess selectionProcess, LearningLevel learningLevel,
                            LocalDate learningStartDate, LocalDate learningEndDate) {
        if (learningEndDate != null && (learningStartDate == null || learningEndDate.isBefore(learningStartDate)))
            throw new IllegalArgumentException("교육 종료일은 시작일 이후여야 합니다.");
        this.deliveryMode = deliveryMode; this.region = region; this.commitment = commitment;
        this.fundingType = fundingType; this.selectionProcess = selectionProcess; this.learningLevel = learningLevel;
        this.learningStartDate = learningStartDate; this.learningEndDate = learningEndDate;
        this.durationDays = learningStartDate == null || learningEndDate == null ? null
                : Math.toIntExact(ChronoUnit.DAYS.between(learningStartDate, learningEndDate) + 1);
    }
}
