package com.inhatc.demp.domain.announcement;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.HashSet;
import java.util.Set;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@EqualsAndHashCode
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Description {

    @Lob
    private String content;
    private String accessUrl;
    private int payment;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @ElementCollection
    @CollectionTable(joinColumns = @JoinColumn(name = "announcement_id"), name = "language")
    private Set<Language> languages = new HashSet<>();

    public Description(String content, String accessUrl, int payment, Set<Language> languages) {
        this.content = content;
        this.accessUrl = accessUrl;
        this.payment = payment;
        this.languages = new HashSet<>(languages);
    }
}
