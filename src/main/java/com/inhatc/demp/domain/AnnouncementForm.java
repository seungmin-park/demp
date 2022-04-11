package com.inhatc.demp.domain;

import lombok.*;
import org.hibernate.validator.constraints.URL;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;

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
    @NotBlank
    private String language;

    @URL
    @NotBlank
    private String accessUrl;
    @NotBlank
    private String career;

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
