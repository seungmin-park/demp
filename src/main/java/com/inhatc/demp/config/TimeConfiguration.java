package com.inhatc.demp.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfiguration {
    @Bean
    public Clock recruitmentClock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
