package com.inhatc.demp.domain.announcement;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** One role owns its location and qualification statements, including AND/OR wording. */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class RecruitmentRole {
    @Column(name = "role_title") private String title;
    @Enumerated(EnumType.STRING) @JdbcTypeCode(SqlTypes.VARCHAR) private JobPosition position;
    private String location;
    @Column(length = 2000) private String qualifications;
    @Column(length = 2000) private String requiredSkills;
    @Column(length = 2000) private String preferredSkills;
    @Column(length = 2000) private String usedTechnologies;
}
