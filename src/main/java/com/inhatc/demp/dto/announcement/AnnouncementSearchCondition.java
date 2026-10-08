package com.inhatc.demp.dto.announcement;

import com.inhatc.demp.domain.announcement.AnnouncementType;
import com.inhatc.demp.domain.announcement.JobPosition;
import com.inhatc.demp.domain.announcement.Language;
import com.inhatc.demp.domain.announcement.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnnouncementSearchCondition {

    private AnnouncementOrder orderBy = AnnouncementOrder.LATEST;

    private DeliveryMode deliveryMode;
    private EducationRegion region;
    private Commitment commitment;
    private FundingType fundingType;
    private SelectionProcess selectionProcess;
    private LearningLevel learningLevel;
    private EducationDuration duration;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startAfter;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startBefore;
    private AnnouncementType announcementType;
    private List<JobPosition> positions = new ArrayList<>();
    private Language language;
    private List<Language> languages = new ArrayList<>();
    private RecruitmentStatus recruitmentStatus;
    private Tuition tuition;
    private int career;
    private int payment;
    private String title;
}
