package com.inhatc.demp.domain.announcemnet;

import javax.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Career {

    private int minCareer;
    private int maxCareer;

    public Career(int minCareer, int maxCareer) {
        this.minCareer = minCareer;
        this.maxCareer = maxCareer;
    }
}
