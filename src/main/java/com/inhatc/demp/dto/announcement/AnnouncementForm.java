package com.inhatc.demp.dto.announcement;

import com.inhatc.demp.domain.AnnouncementType;
import com.inhatc.demp.domain.Company;
import com.inhatc.demp.domain.Language;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.validator.constraints.URL;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AnnouncementForm {

    @NotBlank
    private String title;
    @NotBlank
    private String position;
    @NotBlank
    private String content;
    @NotNull
    private Set<Language> languages = new HashSet<>();

    @URL
    @NotBlank
    private String accessUrl;
    @NotBlank
    private int minCareer;
    @NotBlank
    private int maxCareer;

    @NotNull
    @Min(2400)
    private int payment;

    @NotNull
    private Company company;

    @NotNull
    private MultipartFile image;

    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startedDate;
    @NotNull
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime deadLineDate;

    @NotNull
    private AnnouncementType type;

}
