package com.inhatc.demp.domain.announcemnet;

import java.util.HashSet;
import java.util.Set;
import javax.persistence.CollectionTable;
import javax.persistence.ElementCollection;
import javax.persistence.Embeddable;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Description {

    @Lob
    private String content;
    private String accessUrl;
    private int payment;

    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(joinColumns = @JoinColumn(name = "announcement_id"), name = "language")
    private Set<Language> languages = new HashSet<>();

    public Description(String content, String accessUrl, int payment) {
        this.content = content;
        this.accessUrl = accessUrl;
        this.payment = payment;
    }
}
