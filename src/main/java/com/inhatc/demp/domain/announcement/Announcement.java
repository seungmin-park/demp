package com.inhatc.demp.domain.announcement;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import jakarta.persistence.Embedded;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.JoinColumn;
import java.util.List;
import java.util.ArrayList;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@jakarta.persistence.Table(uniqueConstraints = @jakarta.persistence.UniqueConstraint(name = "uk_announcement_source", columnNames = "duplicate_key"))
@Getter
@ToString
@EntityListeners(AuditingEntityListener.class)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Announcement {

    @Id
    @GeneratedValue(generator = "announcement_legacy_id")
    @SequenceGenerator(name = "announcement_legacy_id", sequenceName = "hibernate_sequence", allocationSize = 1)
    private Long id;
    private String title;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @org.hibernate.annotations.ColumnDefault("'PUBLISHED'")
    private PublicationStatus publicationStatus = PublicationStatus.PUBLISHED;

    public boolean isPublished() { return publicationStatus == PublicationStatus.PUBLISHED; }
    public void changePublication(PublicationStatus status) { if (status != null) publicationStatus = status; }
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    private RecruitmentAudience recruitmentAudience;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR)
    private EmploymentType employmentType;

    public void changeEmploymentType(EmploymentType employmentType) {
        this.employmentType = announcementType == AnnouncementType.EMP ? employmentType : null;
    }
    private String cohort;
    private Integer stipendAmount;
    private String stipendNote;
    @jakarta.persistence.Column(length = 64)
    private String duplicateKey;
    public void changeRecruitment(RecruitmentAudience audience, String cohort, Integer stipendAmount, String stipendNote) {
        if (stipendAmount != null && stipendAmount < 0) throw new IllegalArgumentException("지원금은 0 이상이어야 합니다.");
        this.recruitmentAudience = announcementType == AnnouncementType.EMP ? audience : null;
        this.cohort = announcementType == AnnouncementType.EDU ? cohort : null;
        this.stipendAmount = announcementType == AnnouncementType.EDU ? stipendAmount : null;
        this.stipendNote = announcementType == AnnouncementType.EDU ? stipendNote : null;
        this.duplicateKey = AnnouncementSourceKey.of(description.getAccessUrl(), company.getName(), this.cohort);
    }
    @org.hibernate.annotations.ColumnDefault("false")
    private boolean recruitmentClosed;
    public void changeRecruitmentClosed(Boolean closed) { if (closed != null) recruitmentClosed = closed; }
    private String sourceName;
    private String sourceIdentifier;
    @jakarta.persistence.Column(length = 2048)
    private String applicationUrl;
    private java.time.LocalDateTime sourceVerifiedAt;
    @ElementCollection
    @CollectionTable(name = "announcement_revision", joinColumns = @JoinColumn(name = "announcement_id"))
    @jakarta.persistence.OrderColumn(name = "revision_order")
    private List<PublicationRevision> publicationHistory = new ArrayList<>();

    public void recordPublication(String sourceName, String sourceIdentifier, String applicationUrl,
                                  boolean verified, String actor, java.time.LocalDateTime now) {
        this.sourceName = sourceName;
        this.sourceIdentifier = sourceIdentifier;
        this.applicationUrl = applicationUrl == null || applicationUrl.isBlank() ? null : applicationUrl;
        this.sourceVerifiedAt = verified ? now : null;
        publicationHistory.add(PublicationRevision.builder()
                .actor(actor)
                .changedAt(now)
                .status(publicationStatus)
                .title(title)
                .sourceUrl(description.getAccessUrl())
                .build());
    }

    @Embedded
    private EducationDetails education;

    public void changeEducation(EducationDetails education) {
        this.education = announcementType == AnnouncementType.EDU ? education : null;
    }
    @Embedded
    private Career career;
    @Embedded
    private Description description;
    @Embedded
    private Company company;
    @Embedded
    private UploadFile image;
    @ElementCollection
    @CollectionTable(name = "announcement_body_image", joinColumns = @JoinColumn(name = "announcement_id"))
    private List<UploadFile> bodyImages = new ArrayList<>();
    @Embedded
    private RecruitPeriod recruitPeriod;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private AnnouncementType announcementType;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private JobPosition jobPosition;


    public void replaceBodyImages(List<UploadFile> images) {
        this.bodyImages.clear();
        this.bodyImages.addAll(images);
    }

    public void changeDescription(Description description) {
        this.description = description;
    }

    public void revise(String title, Company company, Career career, RecruitPeriod period, Description description,
                       AnnouncementType type, JobPosition position, UploadFile replacement) {
        this.title = title; this.company = company; this.career = career; this.recruitPeriod = period;
        this.description = description; this.announcementType = type; this.jobPosition = position;
        if (replacement != null) this.image = replacement;
    }

    @Builder
    private Announcement(String title, Career career, Description description, Company company, UploadFile image,
                        RecruitPeriod recruitPeriod, AnnouncementType announcementType, JobPosition jobPosition) {
        this.title = title;
        this.career = career;
        this.description = description;
        this.company = company;
        this.image = image;
        this.recruitPeriod = recruitPeriod;
        this.announcementType = announcementType;
        this.jobPosition = jobPosition;
    }
}
