package com.inhatc.demp.config;

import com.inhatc.demp.DempApplication;
import com.inhatc.demp.domain.Hashtag;
import com.inhatc.demp.repository.HashtagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import static org.assertj.core.api.Assertions.assertThat;

class LegacySchemaCompatibilityTest {
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
                        "spring.sql.init.schema-locations=classpath:legacy/hibernate5-schema.sql,classpath:legacy/employment-type-fixture.sql,classpath:db/manual/announcement-body-images.sql,classpath:db/manual/announcement-compensation.sql,classpath:db/manual/announcement-education.sql,classpath:db/manual/announcement-publication.sql,classpath:db/manual/content-reaction.sql,classpath:db/manual/member-login-protection.sql,classpath:db/manual/answer-cursor-index.sql,classpath:db/manual/announcement-employment-type.sql")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
                    assertThat(jdbc.queryForList("""
                            SELECT COLUMN_NAME FROM INFORMATION_SCHEMA.COLUMNS
                            WHERE TABLE_SCHEMA = 'PUBLIC' AND TABLE_NAME = 'ANNOUNCEMENT'
                            """, String.class)).contains("EMPLOYMENT_TYPE");
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
}
