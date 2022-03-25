package com.inhatc.demp.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AnnouncementDto {

    private Long id;
    private String title;
    private String image;
    private String company;
}
