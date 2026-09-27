package com.inhatc.demp.dto.announcement;

import com.inhatc.demp.domain.announcement.AnnouncementType;
import com.inhatc.demp.domain.announcement.JobPosition;
import com.inhatc.demp.domain.announcement.Language;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;
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
    private String accessUrl;
    @Min(0)
    private int payment;
    @NotEmpty
    private Set<Language> language = new LinkedHashSet<>();
    @NotNull
    private AnnouncementType type;

    @AssertTrue(message = "최소 경력은 최대 경력보다 클 수 없습니다.")
    public boolean isCareerRangeValid() {
        return maxCareer == 0 || minCareer <= maxCareer;
    }

    @AssertTrue(message = "마감일은 시작일보다 빠를 수 없습니다.")
    public boolean isRecruitPeriodValid() {
        return startedDate == null || deadLineDate == null || !deadLineDate.isBefore(startedDate);
    }
}
