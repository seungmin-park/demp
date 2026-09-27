package com.inhatc.demp.dto.announcement;

import com.inhatc.demp.domain.announcement.*;
import java.time.LocalDate;
import com.inhatc.demp.domain.announcement.JobPosition;
import com.inhatc.demp.domain.announcement.Language;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.List;
import java.util.ArrayList;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@NoArgsConstructor
public class AnnouncementFields {

    private DeliveryMode deliveryMode;
    private EducationRegion region;
    private Commitment commitment;
    private FundingType fundingType;
    private SelectionProcess selectionProcess;
    private LearningLevel learningLevel;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate learningStartDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate learningEndDate;

    @AssertTrue(message = "교육 종료일은 시작일 이후여야 합니다.")
    public boolean isLearningPeriodValid() {
        return type != AnnouncementType.EDU || learningEndDate == null
                || learningStartDate != null && !learningEndDate.isBefore(learningStartDate);
    }

    public Career toCareer() {
        return type == AnnouncementType.EDU || recruitmentAudience == RecruitmentAudience.NEW || recruitmentAudience == RecruitmentAudience.ANY
                ? Career.builder().minCareer(0).maxCareer(0).build() : Career.builder().minCareer(minCareer).maxCareer(maxCareer).build();
    }
    public String sourceKey() {
        String key = AnnouncementSourceKey.of(accessUrl, company, type == AnnouncementType.EDU ? cohort : null);
        if (key == null) throw new com.inhatc.demp.error.ApiException(org.springframework.http.HttpStatus.BAD_REQUEST);
        return key;
    }

    public EducationDetails toEducationDetails() {
        return type == AnnouncementType.EDU ? EducationDetails.builder()
                .deliveryMode(deliveryMode)
                .region(region)
                .commitment(commitment)
                .fundingType(fundingType)
                .selectionProcess(selectionProcess)
                .learningLevel(learningLevel)
                .learningStartDate(learningStartDate)
                .learningEndDate(learningEndDate)
                .build() : null;
    }

    private PublicationStatus publicationStatus;
    private com.inhatc.demp.domain.announcement.RecruitmentAudience recruitmentAudience;
    private String cohort;
    @Min(0)
    private Integer stipendAmount;
    private String stipendNote;
    private Boolean recruitmentClosed;
    private String sourceName;
    private String sourceIdentifier;
    @URL
    @jakarta.validation.constraints.Pattern(regexp = "(?i)^(https?://.*)?$")
    private String applicationUrl;
    private boolean sourceVerified;

    private List<MultipartFile> bodyImages = new ArrayList<>();

    @NotBlank
    private String title;
    @NotBlank
    private String company;
    @NotNull
    private JobPosition position;
    @Min(0)
    private int minCareer;
    @Min(0)
    private int maxCareer;
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startedDate;
    @NotNull
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime deadLineDate;
    @NotBlank
    private String content;
    @NotBlank
    @URL
    @jakarta.validation.constraints.Pattern(regexp = "(?i)^https?://.*", message = "원문은 http 또는 https 주소여야 합니다.")
    private String accessUrl;
    @Min(0)
    private Integer payment;
    private com.inhatc.demp.domain.announcement.SalaryStatus salaryStatus;
    @Min(0)
    private Integer salaryMax;

    @AssertTrue(message = "공개 연봉과 상한 금액을 확인해 주세요.")
    public boolean isSalaryValid() {
        if (type != AnnouncementType.EMP) return true;
        if (salaryStatus == com.inhatc.demp.domain.announcement.SalaryStatus.UNDISCLOSED
                || salaryStatus == com.inhatc.demp.domain.announcement.SalaryStatus.NEGOTIABLE) return true;
        return (salaryStatus != com.inhatc.demp.domain.announcement.SalaryStatus.DISCLOSED || payment != null && payment > 0)
                && (salaryMax == null || payment != null && salaryMax >= payment);
    }

    public com.inhatc.demp.domain.announcement.Description toDescription(String safeHtml) {
        return com.inhatc.demp.domain.announcement.Description.builder()
                .content(safeHtml)
                .accessUrl(accessUrl)
                .payment(payment)
                .languages(language)
                .salaryStatus(type == AnnouncementType.EMP ? salaryStatus : null)
                .salaryMax(type == AnnouncementType.EMP ? salaryMax : null)
                .build();
    }
    @NotEmpty
    private Set<Language> language = new LinkedHashSet<>();
    @NotNull
    private AnnouncementType type;

    @AssertTrue(message = "본문 내용을 입력해 주세요.")
    public boolean isContentTextPresent() {
        return content != null && !org.jsoup.Jsoup.parse(content).text().isBlank();
    }

    @AssertTrue(message = "최소 경력은 최대 경력보다 클 수 없습니다.")
    public boolean isCareerRangeValid() {
        return maxCareer == 0 || minCareer <= maxCareer;
    }

    @AssertTrue(message = "마감일은 시작일보다 빠를 수 없습니다.")
    public boolean isRecruitPeriodValid() {
        return startedDate == null || deadLineDate == null || !deadLineDate.isBefore(startedDate);
    }
}
