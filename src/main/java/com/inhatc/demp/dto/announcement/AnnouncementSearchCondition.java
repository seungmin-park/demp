package com.inhatc.demp.dto.announcement;

import com.inhatc.demp.domain.Language;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class AnnouncementSearchCondition {

    private String typeName;
    private List<String> positions = new ArrayList<>();
    private Set<Language> languages = new HashSet<>();
    private int payment;
    private String title;
}
