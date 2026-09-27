package com.inhatc.demp.domain.announcement;

import lombok.Builder;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Company {

    private String name;

    @Builder
    private Company(String name) {
        this.name = name;
    }
}
