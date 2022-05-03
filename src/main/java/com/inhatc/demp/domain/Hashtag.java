package com.inhatc.demp.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

@Entity
@Getter
@NoArgsConstructor
public class Hashtag {

    @Id
    @GeneratedValue
    private Long id;

    private String tagName;

    public Hashtag(String tagName) {
        this.tagName = tagName;
    }
}
