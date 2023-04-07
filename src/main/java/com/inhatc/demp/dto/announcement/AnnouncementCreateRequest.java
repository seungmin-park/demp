package com.inhatc.demp.dto.announcement;

import com.inhatc.demp.domain.announcemnet.Description;
import com.inhatc.demp.domain.announcemnet.AnnouncementType;
import com.inhatc.demp.domain.announcemnet.Career;
import com.inhatc.demp.domain.announcemnet.Company;
import com.inhatc.demp.domain.announcemnet.JobPosition;
import com.inhatc.demp.domain.announcemnet.RecruitPeriod;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementCreateRequest {

    @NotBlank
    private String title;
    @NotBlank
    private JobPosition position;
    private Career career;
    private Description description;
    @NotNull
    private Company company;

    @NotNull
    private MultipartFile image;

    private RecruitPeriod recruitPeriod;

    @NotNull
    private AnnouncementType type;

}
