package com.openflow.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for API endpoints.
 * Tests API-01 and API-02 from the test plan.
 * 
 * These tests run against a real application context with H2 database.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class ApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    private String baseUrl;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
    }

    // ==================== API-02: Protected endpoints without auth ====================

    /**
     * API-02: Test POST /api/tasks without authentication token.
     * Should return 401 Unauthorized or 403 Forbidden.
     */
    @Test
    void testPostTaskWithoutAuth_Returns401Or403() {
        // Arrange - No auth headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String taskJson = "{\"title\":\"Unauthorized Task\",\"description\":\"This should fail\",\"boardId\":1,\"statusId\":1}";
        HttpEntity<String> entity = new HttpEntity<>(taskJson, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/tasks",
                entity,
                String.class
        );

        // Assert - Should be 401 or 403
        assertTrue(
                response.getStatusCode() == HttpStatus.UNAUTHORIZED ||
                response.getStatusCode() == HttpStatus.FORBIDDEN,
                "Expected 401 or 403, got: " + response.getStatusCode()
        );
    }

    /**
     * API-02 (variant): Test GET /api/tasks without authentication.
     * Should return 401 Unauthorized or 403 Forbidden.
     */
    @Test
    void testGetTasksWithoutAuth_Returns401Or403() {
        // Arrange - No auth headers
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/tasks?boardId=1",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Assert - Should be 401 or 403
        assertTrue(
                response.getStatusCode() == HttpStatus.UNAUTHORIZED ||
                response.getStatusCode() == HttpStatus.FORBIDDEN,
                "Expected 401 or 403, got: " + response.getStatusCode()
        );
    }

    /**
     * API-02 (variant): Test DELETE /api/boards without authentication.
     * Should return 401 Unauthorized or 403 Forbidden.
     */
    @Test
    void testDeleteBoardWithoutAuth_Returns401Or403() {
        // Arrange - No auth headers
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/boards/1",
                HttpMethod.DELETE,
                entity,
                String.class
        );

        // Assert - Should be 401 or 403
        assertTrue(
                response.getStatusCode() == HttpStatus.UNAUTHORIZED ||
                response.getStatusCode() == HttpStatus.FORBIDDEN,
                "Expected 401 or 403, got: " + response.getStatusCode()
        );
    }

    /**
     * API-02 (variant): Test GET /api/boards without authentication.
     * Should return 401 Unauthorized or 403 Forbidden.
     */
    @Test
    void testGetBoardsWithoutAuth_Returns401Or403() {
        // Arrange - No auth headers
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/boards",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Assert - Should be 401 or 403
        assertTrue(
                response.getStatusCode() == HttpStatus.UNAUTHORIZED ||
                response.getStatusCode() == HttpStatus.FORBIDDEN,
                "Expected 401 or 403, got: " + response.getStatusCode()
        );
    }

    // ==================== Public Endpoints Tests ====================

    /**
     * Test public boards endpoint (no auth required).
     */
    @Test
    void testGetPublicBoards_NoAuthRequired() {
        // Arrange - No auth headers
        HttpHeaders headers = new HttpHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<List> response = restTemplate.exchange(
                baseUrl + "/public/boards",
                HttpMethod.GET,
                entity,
                List.class
        );

        // Assert - Public endpoint should work without auth
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }

    /**
     * Test invalid token returns 401/403.
     */
    @Test
    void testInvalidToken_Returns401Or403() {
        // Arrange - Invalid token
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer invalid-token-12345");
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/boards",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Assert - Invalid token should fail
        assertTrue(
                response.getStatusCode() == HttpStatus.UNAUTHORIZED ||
                response.getStatusCode() == HttpStatus.FORBIDDEN,
                "Expected 401 or 403 for invalid token, got: " + response.getStatusCode()
        );
    }

    /**
     * Test health/actuator endpoint is accessible.
     */
    @Test
    void testHealthEndpoint_Accessible() {
        // Act
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/actuator/health",
                String.class
        );

        // Assert - Health endpoint should be accessible
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody().contains("UP") || response.getBody().contains("status"));
    }
}
