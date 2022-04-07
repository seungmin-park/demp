package com.inhatc.demp.dto;

import com.inhatc.demp.domain.Company;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class AnnouncementScroll {
    private Long id;
    private String title;
    private Company company;
    private String image;
}
