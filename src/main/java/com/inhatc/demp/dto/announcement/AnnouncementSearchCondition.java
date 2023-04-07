package com.inhatc.demp.dto.announcement;

import com.inhatc.demp.domain.announcemnet.AnnouncementType;
import com.inhatc.demp.domain.announcemnet.JobPosition;
import com.inhatc.demp.domain.announcemnet.Language;
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
    private int career;
    private int payment;
    private String title;
}
