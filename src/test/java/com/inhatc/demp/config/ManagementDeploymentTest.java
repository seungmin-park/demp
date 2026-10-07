package com.inhatc.demp.config;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class ManagementDeploymentTest {
    @Test
    @DisplayName("지표와 상태는 별도 관리 포트에서만 제공하고 환경 설정은 노출하지 않는다")
    void servesMetricsOnSeparatePort() throws Exception {
        try (ConfigurableApplicationContext context = DatabaseLifecycleTest.start(
                "metrics_" + UUID.randomUUID(), "filesystem",
                "--spring.jpa.hibernate.ddl-auto=create-drop", "--management.server.port=0")) {
            String managementPort = context.getEnvironment().getProperty("local.management.port");
            assertThat(managementPort).as("독립 관리 서버가 시작되어야 한다").isNotNull();
            String appPort = context.getEnvironment().getProperty("local.server.port");
            assertThat(get(managementPort, "/actuator/health").statusCode()).isEqualTo(200);
            HttpResponse<String> metrics = get(managementPort, "/actuator/prometheus");
            assertThat(metrics.statusCode()).isEqualTo(200);
            assertThat(metrics.body()).contains("jvm_memory_used_bytes");
            assertThat(get(appPort, "/actuator/prometheus").statusCode()).isNotEqualTo(200);
            assertThat(get(managementPort, "/actuator/env").statusCode()).isNotEqualTo(200);
            assertThat(context.getEnvironment().getProperty("spring.sql.init.mode")).isEqualTo("never");
            assertThat(context.containsBean("initDb")).isFalse();
        }
    }

    private HttpResponse<String> get(String port, String path) throws Exception {
        return HttpClient.newHttpClient().send(HttpRequest.newBuilder(
                URI.create("http://127.0.0.1:" + port + path)).GET().build(), HttpResponse.BodyHandlers.ofString());
    }
}
