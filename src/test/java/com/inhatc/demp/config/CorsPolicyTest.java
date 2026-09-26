package com.inhatc.demp.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost:5050,https://demp.example")
class CorsPolicyTest {
    @Autowired MockMvc mvc;

    @Test
    @DisplayName("허용한 출처의 사전 요청에 해당 출처를 응답한다")
    void allowsConfiguredOrigin() throws Exception {
        mvc.perform(options("/api/question")
                        .header("Origin", "http://localhost:5050")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5050"));
    }

    @Test
    @DisplayName("허용하지 않은 출처의 사전 요청을 거절한다")
    void rejectsUnknownOrigin() throws Exception {
        mvc.perform(options("/api/question")
                        .header("Origin", "https://unknown.example")
                        .header("Access-Control-Request-Method", "GET"))
                .andExpect(status().isForbidden());
    }
}
