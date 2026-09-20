package com.inhatc.demp.config;

import java.util.UUID;
import java.io.IOException;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDataInitializationTest {
    @Test
    @DisplayName("로컬 H2는 Flyway 없이 스키마를 보완하고 생성 후 예제 SQL을 실행한다")
    void usesHibernateOnlyForLocalSchema() throws IOException {
        org.springframework.core.env.PropertySource<?> local = new YamlPropertySourceLoader().load("local",
                new FileSystemResource("src/main/resources/application-local.yml")).get(0);
        assertThat(local.getProperty("spring.jpa.hibernate.ddl-auto")).isEqualTo("update");
        assertThat(local.getProperty("spring.jpa.defer-datasource-initialization")).isEqualTo(true);
        assertThat(org.springframework.util.ClassUtils.isPresent("org.flywaydb.core.Flyway", getClass().getClassLoader()))
                .isFalse();
    }

    @Test
    @DisplayName("local 설정은 전용 seed 위치를 선택하지만 자동 테스트에서는 SQL을 실행하지 않는다")
    void selectsLocalConfigurationWithoutLoadingFixtures() throws IOException {
        assertThat(new YamlPropertySourceLoader().load("local",
                new FileSystemResource("src/main/resources/application-local.yml"))
                .get(0).getProperty("spring.sql.init.mode")).isEqualTo("always");
        try (ConfigurableApplicationContext context = DatabaseLifecycleTest.start(
                "local_contract_" + UUID.randomUUID().toString().replace("-", ""),
                "local", "--spring.sql.init.mode=never")) {
            assertThat(context.getEnvironment().getActiveProfiles()).containsExactly("local");
            assertThat(context.getEnvironment().getProperty("spring.sql.init.data-locations"))
                    .isEqualTo("classpath:local/data.sql");
            assertThat(context.getEnvironment().getProperty("spring.sql.init.mode")).isEqualTo("never");
            assertThat(context.getBean(JdbcTemplate.class).queryForObject("select count(*) from member", Long.class)).isZero();
            assertThat(context.containsBean("initDb")).isFalse();
        }
    }
}
