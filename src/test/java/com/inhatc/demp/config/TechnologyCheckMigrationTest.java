package com.inhatc.demp.config;

import com.inhatc.demp.domain.announcement.Language;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class TechnologyCheckMigrationTest {
    private String migration() throws Exception {
        try (var stream = getClass().getResourceAsStream("/db/manual/announcement-technologies-check.sql")) {
            assertThat(stream).as("운영 기술 CHECK 갱신 SQL이 패키징되어야 한다").isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void assertEnumContract(String sql) {
        List<String> values = Pattern.compile("'([^']+)'").matcher(sql).results().map(m -> m.group(1)).toList();
        assertThat(sql).contains("ALTER TABLE language DROP CHECK language_chk_1", "ADD CONSTRAINT language_chk_1 CHECK (languages IN (");
        assertThat(values).doesNotHaveDuplicates().containsExactlyInAnyOrderElementsOf(Arrays.stream(Language.values()).map(Enum::name).toList());
    }

    @Test
    @DisplayName("운영 기술 CHECK 갱신 SQL은 현재 모든 enum을 포함하고 제약을 유지한다")
    void includesCurrentTechnologyContract() throws Exception {
        assertEnumContract(migration());
    }

    @Test
    @DisplayName("기술 CHECK에서 C++이 누락되거나 알 수 없는 값이 추가되면 계약 검사가 거절한다")
    void rejectsMigrationDrift() throws Exception {
        String sql = migration();
        assertThatThrownBy(() -> assertEnumContract(sql.replace("'CPP', ", ""))).isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertEnumContract(sql.replace("'CPP', ", "'INVALID_TECH', 'CPP', "))).isInstanceOf(AssertionError.class);
    }
}
