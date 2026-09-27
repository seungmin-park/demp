package com.inhatc.demp.dto.announcement;

import com.inhatc.demp.domain.announcement.AnnouncementType;
import com.inhatc.demp.domain.announcement.JobPosition;
import com.inhatc.demp.domain.announcement.Language;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AnnouncementSearchCondition {

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
