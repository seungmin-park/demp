package com.inhatc.demp.domain.announcement;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
@Entity @Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "announcement_id", nullable = false)
    @org.hibernate.annotations.OnDelete(action = org.hibernate.annotations.OnDeleteAction.CASCADE)
    private Announcement announcement;
    @Column(length = 1000, nullable = false) private String message;
    private String reporter;
    private LocalDateTime createdAt;
    @Column(length = 1000) private String resolution;
    private String resolvedBy;
    private LocalDateTime resolvedAt;
    @Builder
    private AnnouncementReport(Announcement announcement, String message, String reporter, LocalDateTime createdAt) {
        this.announcement = announcement; this.message = message; this.reporter = reporter; this.createdAt = createdAt;
    }
    public void resolve(String note, String actor, LocalDateTime now) {
        if (resolvedAt != null) return;
        resolution = note; resolvedBy = actor; resolvedAt = now;
    }
}
