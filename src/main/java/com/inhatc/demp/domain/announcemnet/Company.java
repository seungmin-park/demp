package com.inhatc.demp.domain.announcemnet;

import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company {

    private String name;

    public Company(String name) {
        this.name = name;
    }
}
