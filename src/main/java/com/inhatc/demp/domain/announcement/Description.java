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
    private Integer payment;
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private SalaryStatus salaryStatus;
    private Integer salaryMax;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @ElementCollection
    @CollectionTable(joinColumns = @JoinColumn(name = "announcement_id"), name = "language")
    private Set<Language> languages = new HashSet<>();

    public Description(String content, String accessUrl, Integer payment, Set<Language> languages) {
        this(content, accessUrl, payment, languages, null, null);
    }

    public Description(String content, String accessUrl, Integer payment, Set<Language> languages,
                       SalaryStatus salaryStatus, Integer salaryMax) {
        boolean hidden = salaryStatus == SalaryStatus.UNDISCLOSED || salaryStatus == SalaryStatus.NEGOTIABLE;
        if (hidden) { payment = null; salaryMax = null; }
        if (payment != null && payment < 0 || salaryMax != null && (payment == null || salaryMax < payment))
            throw new IllegalArgumentException("금액 범위를 확인해 주세요.");
        if (salaryStatus == SalaryStatus.DISCLOSED && (payment == null || payment <= 0))
            throw new IllegalArgumentException("공개 연봉을 입력해 주세요.");
        this.salaryStatus = salaryStatus;
        this.salaryMax = hidden ? null : salaryMax;
        this.content = content;
        this.accessUrl = accessUrl;
        this.payment = hidden ? null : payment;
        this.languages = new HashSet<>(languages);
    }
    public SalaryStatus getSalaryStatus() {
        return salaryStatus != null ? salaryStatus : payment != null && payment > 0 ? SalaryStatus.DISCLOSED : SalaryStatus.UNDISCLOSED;
    }

    public Description withContent(String sanitized) {
        return new Description(sanitized, accessUrl, payment, languages, salaryStatus, salaryMax);
    }
}
