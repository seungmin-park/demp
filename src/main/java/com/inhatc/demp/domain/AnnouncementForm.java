package com.inhatc.demp.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AnnouncementForm {

    @NotBlank
    private String title;
    @NotBlank
    private String position;
    @NotBlank
    private String content;
    @NotBlank
    private String language;

    @URL
    @NotBlank
    private String accessUrl;
    @NotBlank
    private String career;

    @Min(2400)
    private int payment;
    @NotBlank
    private Company company;

    @NotNull
    private MultipartFile image;

    private String startedDate;
    private String deadLineDate;

    @NotBlank
    private AnnouncementType type;

}
