package com.openflow.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.openflow.dto.AuthRequest;
import com.openflow.dto.AuthResponse;
import com.openflow.dto.RegisterRequest;
import com.openflow.model.Role;
import com.openflow.model.User;
import com.openflow.service.JwtService;
import com.openflow.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller integration tests for AuthController.
 * Tests authentication endpoints: register, login, me.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private com.openflow.config.AzureAdAuthenticationFilter azureAdAuthenticationFilter;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private AuthResponse authResponse;

    @BeforeEach
    void setUp() {
        reset(userService, jwtService);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(Role.USER);
        testUser.setAuthProvider("jwt");

        authResponse = new AuthResponse("jwt-token-123", "testuser", "USER");
    }

    /**
     * Test successful user registration.
     */
    @Test
    void testRegister_Success() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");

        when(userService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    /**
     * Test registration with duplicate username.
     */
    @Test
    void testRegister_DuplicateUsername() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Username already exists"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Username already exists")));
    }

    /**
     * Test registration with invalid data (validation failure).
     */
    @Test
    void testRegister_ValidationFailure() throws Exception {
        // Arrange - missing required fields
        RegisterRequest request = new RegisterRequest();
        request.setUsername("ab"); // Too short (min 3)
        request.setEmail("invalid-email"); // Invalid email format
        request.setPassword("12345"); // Too short (min 6)

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Test successful login.
     */
    @Test
    void testLogin_Success() throws Exception {
        // Arrange
        AuthRequest request = new AuthRequest();
        request.setUsername("testuser");
        request.setPassword("password123");

        when(userService.login(any(AuthRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token-123"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    /**
     * Test login with invalid credentials.
     */
    @Test
    void testLogin_InvalidCredentials() throws Exception {
        // Arrange
        AuthRequest request = new AuthRequest();
        request.setUsername("wronguser");
        request.setPassword("wrongpassword");

        when(userService.login(any(AuthRequest.class)))
                .thenThrow(new RuntimeException("Invalid username or password"));

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Invalid username or password")));
    }

    /**
     * Test login with empty credentials.
     */
    @Test
    void testLogin_EmptyCredentials() throws Exception {
        // Arrange
        AuthRequest request = new AuthRequest();
        request.setUsername("");
        request.setPassword("");

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Username and password are required")));
    }

    /**
     * Test getting current user info when authenticated.
     */
    @Test
    void testGetCurrentUser_Authenticated() throws Exception {
        // Arrange
        when(userService.findByUsername("testuser")).thenReturn(testUser);

        // Act & Assert
        mockMvc.perform(get("/api/auth/me")
                        .with(SecurityMockMvcRequestPostProcessors.user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    /**
     * Test getting all users for task assignment.
     */
    @Test
    void testGetAllUsers_Authenticated() throws Exception {
        // Arrange
        User user1 = new User();
        user1.setId(1L);
        user1.setUsername("user1");

        User user2 = new User();
        user2.setId(2L);
        user2.setUsername("user2");

        List<User> users = Arrays.asList(user1, user2);
        when(userService.getAllUsers()).thenReturn(users);

        // Act & Assert
        mockMvc.perform(get("/api/auth/users")
                        .with(SecurityMockMvcRequestPostProcessors.user("testuser").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].username").value("user1"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].username").value("user2"));
    }
}

