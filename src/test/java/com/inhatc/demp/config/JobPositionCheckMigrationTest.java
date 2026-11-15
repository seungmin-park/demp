package com.inhatc.demp.config;

import com.inhatc.demp.domain.announcement.JobPosition;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class JobPositionCheckMigrationTest {
    private static final Pattern ALLOWED = Pattern.compile("CHECK \\(job_position IN \\(([^)]*)\\)\\)");

    private String migration() throws Exception {
        try (var stream = getClass().getResourceAsStream("/db/manual/announcement-job-position-check.sql")) {
            assertThat(stream).as("운영 분야 CHECK 갱신 SQL이 패키징되어야 한다").isNotNull();
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void assertEnumContract(String sql) {
        assertThat(sql).as("Hibernate가 컬럼 순서로 붙인 이름 대신 제약 내용으로 기존 CHECK를 찾아야 한다")
                .contains("information_schema.CHECK_CONSTRAINTS", "tc.TABLE_NAME = 'announcement'", "LIKE '%`job_position`%'")
                .doesNotContain("announcement_chk_");
        Matcher allowed = ALLOWED.matcher(sql);
        assertThat(allowed.find()).as("ADD CONSTRAINT announcement_job_position_chk의 허용 목록").isTrue();
        assertThat(sql).contains("ADD CONSTRAINT announcement_job_position_chk " + allowed.group());
        List<String> values = Pattern.compile("'([^']+)'").matcher(allowed.group(1)).results().map(m -> m.group(1)).toList();
        assertThat(values).doesNotHaveDuplicates()
                .containsExactlyInAnyOrderElementsOf(Arrays.stream(JobPosition.values()).map(Enum::name).toList());
    }

    @Test
    @DisplayName("운영 분야 CHECK 갱신 SQL은 이름을 가정하지 않고 기존 제약을 찾아 현재 모든 분야를 허용한다")
    void includesCurrentJobPositionContract() throws Exception {
        assertEnumContract(migration());
    }

    @Test
    @DisplayName("분야 CHECK에서 SRE가 누락되거나 알 수 없는 값이 추가되면 계약 검사가 거절한다")
    void rejectsMigrationDrift() throws Exception {
        String sql = migration();
        assertThatThrownBy(() -> assertEnumContract(sql.replace("'SRE', ", ""))).isInstanceOf(AssertionError.class);
        assertThatThrownBy(() -> assertEnumContract(sql.replace("'SRE', ", "'INVALID_POSITION', 'SRE', "))).isInstanceOf(AssertionError.class);
    }
}
