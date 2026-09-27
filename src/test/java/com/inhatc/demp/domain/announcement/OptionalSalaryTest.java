package com.inhatc.demp.domain.announcement;

import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import static org.assertj.core.api.Assertions.*;

class OptionalSalaryTest {
    @ParameterizedTest
    @DisplayName("미공개와 협의는 남아 있는 상한 금액을 무시하고 제거한다")
    @EnumSource(value = SalaryStatus.class, names = {"UNDISCLOSED", "NEGOTIABLE"})
    void ignoresStaleHiddenRange(SalaryStatus status) {
        Description result = Description.builder()
                .content("본문")
                .accessUrl("https://example.com")
                .payment(null)
                .languages(Set.of(Language.JAVA))
                .salaryStatus(status)
                .salaryMax(6000)
                .build();
        assertThat(result.getPayment()).isNull(); assertThat(result.getSalaryMax()).isNull();
    }
}
