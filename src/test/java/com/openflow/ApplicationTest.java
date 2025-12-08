package com.openflow;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Integration test to verify that the Spring application context loads successfully.
 * Uses test profile to avoid loading complex security configurations.
 */
@SpringBootTest
@ActiveProfiles("test")
class ApplicationTest {

    @Test
    void contextLoads() {
        // Test that Spring context loads successfully with test profile
    }
}




