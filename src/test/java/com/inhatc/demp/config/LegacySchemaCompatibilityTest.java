package com.inhatc.demp.config;

import com.inhatc.demp.DempApplication;
import com.inhatc.demp.domain.Hashtag;
import com.inhatc.demp.repository.HashtagRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import static org.assertj.core.api.Assertions.assertThat;

class LegacySchemaCompatibilityTest {
    @Test
    @DisplayName("본문 첨부 테이블만 추가한 기존 Hibernate5 스키마에서 데이터와 ID 연속성을 보존한다")
    void readsAndWritesLegacySchema() {
        new WebApplicationContextRunner()
                .withInitializer(new ConfigDataApplicationContextInitializer())
                .withUserConfiguration(DempApplication.class)
                .withPropertyValues(
                        "spring.datasource.url=jdbc:h2:mem:legacy-schema;DB_CLOSE_DELAY=0",
                        "spring.jpa.hibernate.ddl-auto=validate",
                        "spring.sql.init.mode=always",
                        "spring.sql.init.schema-locations=classpath:legacy/hibernate5-schema.sql,classpath:db/manual/announcement-body-images.sql")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    HashtagRepository tags = context.getBean(HashtagRepository.class);
                    assertThat(tags.findById(1000L).orElseThrow().getTagName()).isEqualTo("legacy-preserved");
                    Hashtag saved = tags.save(new Hashtag("new-after-upgrade"));
                    assertThat(saved.getId()).isEqualTo(1001L);
                    assertThat(tags.findById(saved.getId()).orElseThrow().getTagName()).isEqualTo("new-after-upgrade");
                });
    }
}
