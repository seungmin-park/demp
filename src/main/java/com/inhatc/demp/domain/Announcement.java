package com.inhatc.demp.domain;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@ToString
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement {

    @Id
    @GeneratedValue
    private Long id;
    private String title;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(joinColumns = @JoinColumn(name = "announcement_id"), name = "language")
    private List<String> languages = new ArrayList<>();
    private String position;
    private String career;
    @Lob
    private String content;
    private String accessUrl;
    private int payment;
    @Embedded
    private Company company;
    @Embedded
    private UploadFile image;
    @Enumerated(EnumType.STRING)
    private AnnouncementType announcementType;

    private LocalDateTime startedDate;
    private LocalDateTime deadLineDate;

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdDate;

    @Builder
    public Announcement(String title, List<String> languages, String position, String career, String content, String accessUrl, int payment, Company company, UploadFile image, AnnouncementType announcementType, LocalDateTime startedDate, LocalDateTime deadLineDate) {
        this.title = title;
        this.languages.addAll(languages);
        this.position = position;
        this.career = career;
        this.content = content;
        this.accessUrl = accessUrl;
        this.payment = payment;
        this.company = company;
        this.image = image;
        this.announcementType = announcementType;
        this.startedDate = startedDate;
        this.deadLineDate = deadLineDate;
    }
}
