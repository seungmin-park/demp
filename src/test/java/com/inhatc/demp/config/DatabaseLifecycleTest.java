package com.inhatc.demp.config;

import com.inhatc.demp.DempApplication;
import com.inhatc.demp.domain.Member;
import com.inhatc.demp.repository.MemberRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseLifecycleTest {
    @Test
    @DisplayName("기본 프로필을 같은 격리 DB로 재시작해도 저장한 회원이 남는다")
    void preservesMemberAcrossRestart() {
        String database = "lifecycle_" + UUID.randomUUID().toString().replace("-", "");
        prepareSchema(database);
        try (ConfigurableApplicationContext context = start(database, "")) {
            context.getBean(MemberRepository.class).save(new Member("retained-member", "password", List.of("ROLE_USER")));
        }
        try (ConfigurableApplicationContext context = start(database, "")) {
            assertThat(context.getBean(MemberRepository.class).findByUsername("retained-member")).isPresent();
        }
    }

    @Test
    @DisplayName("기본 프로필은 예제 회원과 공고를 생성하지 않는다")
    void defaultProfileHasNoSeed() {
        assertNoSeed("");
    }

    @Test
    @DisplayName("test 프로필은 예제 회원과 공고를 생성하지 않는다")
    void testProfileHasNoSeed() {
        assertNoSeed("test");
    }

    private void assertNoSeed(String profile) {
        String configuration = profile.equals("test") ? "src/test/resources/application.yml" : "src/main/resources/application.yml";
        String database = "empty_" + UUID.randomUUID().toString().replace("-", "");
        prepareSchema(database);
        try (ConfigurableApplicationContext context = start(database, profile,
                "--spring.config.location=file:" + configuration)) {
            JdbcTemplate jdbc = context.getBean(JdbcTemplate.class);
            assertThat(jdbc.queryForObject("select count(*) from member", Long.class)).isZero();
            assertThat(jdbc.queryForObject("select count(*) from announcement", Long.class)).isZero();
            assertThat(context.containsBean("initDb")).isFalse();
            assertThat(context.getEnvironment().getProperty("spring.sql.init.mode")).isEqualTo("never");
        }
    }

    private void prepareSchema(String database) {
        // 테스트의 격리 H2만 준비한다. 이후 실제 검증 기동에는 기본 validate가 적용된다.
        try (ConfigurableApplicationContext ignored = start(database, "", "--spring.jpa.hibernate.ddl-auto=create")) {
        }
    }

    static ConfigurableApplicationContext start(String database, String profile, String... overrides) {
        List<String> arguments = new ArrayList<>(Arrays.asList(
                "--spring.config.location=file:src/main/resources/application.yml",
                "--spring.profiles.active=" + profile,
                "--spring.datasource.url=jdbc:h2:mem:" + database + ";MODE=MySQL;DB_CLOSE_DELAY=-1",
                "--spring.datasource.driver-class-name=org.h2.Driver", "--spring.datasource.username=sa",
                "--spring.datasource.password=", "--spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
                "--spring.jwt.secret=test-only-jwt-secret-not-for-production", "--server.port=0",
                "--cloud.aws.credentials.access-key=test", "--cloud.aws.credentials.secret-key=test",
                "--cloud.aws.region.static=ap-northeast-2", "--cloud.aws.s3.bucket=test-unused",
                "--logging.level.root=ERROR"));
        arguments.addAll(Arrays.asList(overrides));
        return new SpringApplicationBuilder(DempApplication.class).run(arguments.toArray(new String[0]));
    }
}
