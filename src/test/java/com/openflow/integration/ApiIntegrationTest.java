package com.openflow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openflow.dto.AuthRequest;
import com.openflow.dto.AuthResponse;
import com.openflow.dto.BoardDto;
import com.openflow.dto.RegisterRequest;
import com.openflow.dto.TaskDto;
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

    @Autowired
    private ObjectMapper objectMapper;

    private String baseUrl;
    private String authToken;
    private Long testUserId;

    @BeforeEach
    void setUp() {
        baseUrl = "http://localhost:" + port + "/api";
        
        // Register a test user to get an auth token
        registerAndLoginTestUser();
    }

    private void registerAndLoginTestUser() {
        // First try to login in case user already exists
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setUsername("integrationtest");
        loginRequest.setPassword("testpass123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AuthRequest> loginEntity = new HttpEntity<>(loginRequest, headers);

        ResponseEntity<AuthResponse> loginResponse = restTemplate.postForEntity(
                baseUrl + "/auth/login",
                loginEntity,
                AuthResponse.class
        );

        if (loginResponse.getStatusCode() == HttpStatus.OK && loginResponse.getBody() != null) {
            authToken = loginResponse.getBody().getToken();
            return;
        }

        // If login failed, register new user
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("integrationtest");
        registerRequest.setEmail("integration@test.com");
        registerRequest.setPassword("testpass123");

        HttpEntity<RegisterRequest> registerEntity = new HttpEntity<>(registerRequest, headers);

        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                baseUrl + "/auth/register",
                registerEntity,
                AuthResponse.class
        );

        if (registerResponse.getStatusCode() == HttpStatus.OK && registerResponse.getBody() != null) {
            authToken = registerResponse.getBody().getToken();
        }
    }

    private HttpHeaders getAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (authToken != null) {
            headers.set("Authorization", "Bearer " + authToken);
        }
        return headers;
    }

    // ==================== API-01: GET /boards with valid token ====================

    /**
     * API-01: Test GET /api/boards with valid authentication token.
     * Should return a list of boards in JSON format.
     */
    @Test
    void testGetBoardsWithValidToken_ReturnsJsonList() {
        // Arrange
        HttpHeaders headers = getAuthHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<List> response = restTemplate.exchange(
                baseUrl + "/boards",
                HttpMethod.GET,
                entity,
                List.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        // Response should be a list (may be empty)
        assertTrue(response.getBody() instanceof List);
    }

    /**
     * API-01 (variant): Test GET /api/boards returns correct content type.
     */
    @Test
    void testGetBoardsWithValidToken_ReturnsJsonContentType() {
        // Arrange
        HttpHeaders headers = getAuthHeaders();
        HttpEntity<?> entity = new HttpEntity<>(headers);

        // Act
        ResponseEntity<String> response = restTemplate.exchange(
                baseUrl + "/boards",
                HttpMethod.GET,
                entity,
                String.class
        );

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getHeaders().getContentType().toString().contains("application/json"));
    }

    // ==================== API-02: POST /tasks without auth ====================

    /**
     * API-02: Test POST /api/tasks without authentication token.
     * Should return 401 Unauthorized or 403 Forbidden.
     */
    @Test
    void testPostTaskWithoutAuth_Returns401Or403() {
        // Arrange - No auth headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        TaskDto taskDto = new TaskDto();
        taskDto.setTitle("Unauthorized Task");
        taskDto.setDescription("This should fail");
        taskDto.setBoardId(1L);
        taskDto.setStatusId(1L);

        HttpEntity<TaskDto> entity = new HttpEntity<>(taskDto, headers);

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

    // ==================== Additional Integration Tests ====================

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
     * Test authentication flow - register, login, and access protected resource.
     */
    @Test
    void testAuthenticationFlow_RegisterLoginAccess() {
        // 1. Register new user
        String uniqueUsername = "testuser" + System.currentTimeMillis();
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername(uniqueUsername);
        registerRequest.setEmail(uniqueUsername + "@test.com");
        registerRequest.setPassword("password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RegisterRequest> registerEntity = new HttpEntity<>(registerRequest, headers);

        ResponseEntity<AuthResponse> registerResponse = restTemplate.postForEntity(
                baseUrl + "/auth/register",
                registerEntity,
                AuthResponse.class
        );

        assertEquals(HttpStatus.OK, registerResponse.getStatusCode());
        assertNotNull(registerResponse.getBody());
        assertNotNull(registerResponse.getBody().getToken());
        String newToken = registerResponse.getBody().getToken();

        // 2. Use token to access protected resource
        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.set("Authorization", "Bearer " + newToken);
        HttpEntity<?> authEntity = new HttpEntity<>(authHeaders);

        ResponseEntity<List> boardsResponse = restTemplate.exchange(
                baseUrl + "/boards",
                HttpMethod.GET,
                authEntity,
                List.class
        );

        // 3. Verify access granted
        assertEquals(HttpStatus.OK, boardsResponse.getStatusCode());
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
     * Test login with invalid credentials returns error.
     */
    @Test
    void testLoginWithInvalidCredentials_ReturnsBadRequest() {
        // Arrange
        AuthRequest loginRequest = new AuthRequest();
        loginRequest.setUsername("nonexistentuser");
        loginRequest.setPassword("wrongpassword");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<AuthRequest> entity = new HttpEntity<>(loginRequest, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/auth/login",
                entity,
                String.class
        );

        // Assert - Should return error
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
    }

    /**
     * Test registration with duplicate username returns error.
     */
    @Test
    void testRegisterDuplicateUsername_ReturnsBadRequest() {
        // First, ensure test user exists
        registerAndLoginTestUser();

        // Try to register with same username
        RegisterRequest duplicateRequest = new RegisterRequest();
        duplicateRequest.setUsername("integrationtest"); // Same as setUp user
        duplicateRequest.setEmail("different@test.com");
        duplicateRequest.setPassword("password123");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<RegisterRequest> entity = new HttpEntity<>(duplicateRequest, headers);

        // Act
        ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/auth/register",
                entity,
                String.class
        );

        // Assert - Should return error for duplicate
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().contains("Username already exists"));
    }
}

