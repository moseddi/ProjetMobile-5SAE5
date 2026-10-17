package com.hangout.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {
    
    // Injected instead of calling LocalDateTime.now() directly so tests can fix the date
    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
