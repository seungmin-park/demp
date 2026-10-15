package com.inhatc.demp.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "announcement_bookmark", uniqueConstraints = @UniqueConstraint(name = "uk_bookmark_member_announcement", columnNames = {"member_id", "announcement_id"}),
        indexes = @Index(name = "idx_bookmark_member_order", columnList = "member_id,id"))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AnnouncementBookmark {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member member;
    // Keep an unavailable saved item after the source announcement is deleted.
    @Column(name = "announcement_id", nullable = false)
    private long announcementId;
    @Column(nullable = false)
    private LocalDateTime savedAt;

    public static AnnouncementBookmark saveFor(Member member, long announcementId, LocalDateTime savedAt) {
        var bookmark = new AnnouncementBookmark();
        bookmark.member = member; bookmark.announcementId = announcementId; bookmark.savedAt = savedAt;
        return bookmark;
    }
}
