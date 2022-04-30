package com.inhatc.demp.dto.announcement;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class AnnouncementSearchCondition {

    private String typeName;
    private List<String> positions = new ArrayList<>();
    private List<String> languages = new ArrayList<>();
    private int payment;
    private String title;
}