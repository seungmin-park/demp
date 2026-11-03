package com.inhatc.demp.config;

import com.inhatc.demp.DempApplication;
import com.inhatc.demp.domain.Hashtag;
import com.inhatc.demp.repository.HashtagRepository;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LegacySchemaCompatibilityTest {
    private static final List<String> LEGACY_BASE = List.of("hibernate5-schema", "employment-type-fixture");
    private static final List<String> LEGACY_WITH_DUPLICATE_TAGS = List.of("hibernate5-schema", "employment-type-fixture", "duplicate-hashtag-fixture");
    /** Manual production SQL in the order an operator applies it. */
    private static final List<String> MANUAL_BEFORE_UNIQUENESS = List.of("announcement-body-images", "announcement-compensation",
            "announcement-education", "announcement-publication", "content-reaction", "member-login-protection", "answer-cursor-index",
            "announcement-employment-type", "announcement-view-count", "announcement-source-identity", "announcement-recruitment-facts",
            "member-self-service", "announcement-deadline-mode", "announcement-bookmarks");
    private static final List<String> MANUAL_ALL = java.util.stream.Stream.concat(MANUAL_BEFORE_UNIQUENESS.stream(),
            java.util.stream.Stream.of("member-username-unique", "hashtag-tag-name-unique")).toList();

    private static String schemaLocations(List<String> legacy, List<String> manual) {
        return java.util.stream.Stream.concat(legacy.stream().map(name -> "classpath:legacy/" + name + ".sql"),
                manual.stream().map(name -> "classpath:db/manual/" + name + ".sql")).collect(java.util.stream.Collectors.joining(","));
    }

    private static List<String> without(List<String> scripts, String omitted) {
        return scripts.stream().filter(name -> !name.equals(omitted)).toList();
    }
    @Test
    @DisplayName("답변 커서 인덱스를 포함한 기존 Hibernate5 스키마에서 데이터와 ID 연속성을 보존한다")
    void readsAndWritesLegacySchema() {
        new WebApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(DempApplication.class)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:h2:mem:legacy-schema;MODE=MySQL;DB_CLOSE_DELAY=0",
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "spring.sql.init.mode=always",
                        "spring.sql.init.schema-locations=" + schemaLocations(LEGACY_BASE, MANUAL_BEFORE_UNIQUENESS))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
                    assertThat(jdbc.queryForList("""
                            SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
                            WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'ANNOUNCEMENT'
                            """, String.class)).contains("EMPLOYMENT_TYPE");
                    assertThat(jdbc.queryForList("SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'ANNOUNCEMENT'", String.class)).contains("HITS");
                    assertThat(jdbc.queryForMap("SELECT title, name, payment, employment_type FROM announcement WHERE id = 900"))
                            .containsEntry("TITLE", "기존 고용 형태 미확인 공고")
                            .containsEntry("NAME", "기존 회사")
                            .containsEntry("PAYMENT", 4500)
                            .containsEntry("EMPLOYMENT_TYPE", null);
                    assertThat(jdbc.queryForList("""
                            SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.INDEX_COLUMNS
                            WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'ANSWER'
                              AND INDEX_NAME = 'IDX_ANSWER_QUESTION_CURSOR'
                            ORDER BY ORDINAL_POSITION
                            """, String.class)).containsExactly("QUESTION_ID", "ANSWER_ID");
                    HashtagRepository tags = context.getBean(HashtagRepository.class);
                    assertThat(tags.findById(1000L).orElseThrow().getTagName()).isEqualTo("legacy-preserved");
                    Hashtag saved = tags.save(Hashtag.builder().tagName("new-after-upgrade").build());
                    assertThat(saved.getId()).isEqualTo(1001L);
                    assertThat(tags.findById(saved.getId()).orElseThrow().getTagName()).isEqualTo("new-after-upgrade");
                });
    }

    @Test
    @DisplayName("조회수 컬럼 SQL이 빠진 기존 스키마는 validate 기동을 거절한다")
    void refusesSchemaMissingAnnouncementHits() {
        new WebApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(DempApplication.class)
                .withPropertyValues("spring.datasource.url=jdbc:h2:mem:missing-announcement-hits;MODE=MySQL;DB_CLOSE_DELAY=0",
                        "spring.jpa.hibernate.ddl-auto=validate", "spring.sql.init.mode=always",
                        "spring.sql.init.schema-locations=" + schemaLocations(LEGACY_BASE, without(MANUAL_BEFORE_UNIQUENESS, "announcement-view-count")))
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("missing column [hits]");
                });
    }

    @Test
    @DisplayName("수동 스키마 변경 후 기존 DB는 중복 태그를 하나로 합치고 같은 로그인 아이디·태그 이름의 추가 저장을 거절한다")
    void manualSchemaEnforcesUsernameAndTagNameUniqueness() {
        new WebApplicationContextRunner().withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(DempApplication.class)
                .withPropertyValues("spring.datasource.url=jdbc:h2:mem:legacy-uniqueness;MODE=MySQL;DB_CLOSE_DELAY=0",
                        "spring.jpa.hibernate.ddl-auto=validate", "spring.sql.init.mode=always",
                        "spring.sql.init.schema-locations=" + schemaLocations(LEGACY_WITH_DUPLICATE_TAGS, MANUAL_ALL))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
                    assertThat(jdbc.queryForList("SELECT hashtag_id FROM hashtag WHERE tag_name = 'legacy-duplicate'", Long.class))
                            .containsExactly(1100L);
                    assertThat(jdbc.queryForList("SELECT hashtag_id FROM question_hashtag WHERE question_id = 1300", Long.class))
                            .containsExactly(1100L);
                    assertThatThrownBy(() -> jdbc.update("INSERT INTO hashtag (hashtag_id, tag_name) VALUES (1102, 'legacy-duplicate')"))
                            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
                    assertThatThrownBy(() -> jdbc.update("INSERT INTO member (member_id, password, username) VALUES (1201, 'other-hash', 'legacy-writer')"))
                            .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
                });
    }
}
