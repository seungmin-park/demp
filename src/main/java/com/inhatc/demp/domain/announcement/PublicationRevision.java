package com.inhatc.demp.domain.announcement;
import java.time.LocalDateTime;
import jakarta.persistence.*;
import lombok.*;
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PublicationRevision {
    private String actor;
    private LocalDateTime changedAt;
    @Enumerated(EnumType.STRING) @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.VARCHAR)
    private PublicationStatus status;
    private String title;
    @Column(length = 2048) private String sourceUrl;

    @Builder
    private PublicationRevision(String actor, LocalDateTime changedAt, PublicationStatus status,
                                String title, String sourceUrl) {
        this.actor = actor;
        this.changedAt = changedAt;
        this.status = status;
        this.title = title;
        this.sourceUrl = sourceUrl;
    }
}
