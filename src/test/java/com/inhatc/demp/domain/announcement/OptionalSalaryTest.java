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
        Description result = new Description("본문", "https://example.com", null, Set.of(Language.JAVA), status, 6000);
        assertThat(result.getPayment()).isNull(); assertThat(result.getSalaryMax()).isNull();
    }
}
