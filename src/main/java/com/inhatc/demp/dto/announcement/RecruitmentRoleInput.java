package com.inhatc.demp.dto.announcement;

import com.inhatc.demp.domain.announcement.*;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RecruitmentRoleInput {
    @NotBlank @Size(max = 255) private String title;
    private JobPosition position;
    @Size(max = 255) private String location;
    @Size(max = 2000) private String qualifications;
    @Size(max = 2000) private String requiredSkills;
    @Size(max = 2000) private String preferredSkills;
    @Size(max = 2000) private String usedTechnologies;
    public RecruitmentRole toRole() { return new RecruitmentRole(title, position, location, qualifications, requiredSkills, preferredSkills, usedTechnologies); }
}
