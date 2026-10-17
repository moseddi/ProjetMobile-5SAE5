package com.hangout;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Basic Spring Boot Context Load Smoke Test.
 */
@SpringBootTest
@ActiveProfiles("test")
class HangOutApplicationTests {

    @Test
    @DisplayName("Verify Spring Application Context Loads")
    void contextLoads() {
        // Verifies Spring context initializes properly
    }
}
