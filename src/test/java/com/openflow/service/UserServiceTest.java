package com.openflow.service;

import com.openflow.dto.AuthRequest;
import com.openflow.dto.AuthResponse;
import com.openflow.dto.RegisterRequest;
import com.openflow.model.Role;
import com.openflow.model.User;
import com.openflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserService.
 * Covers authentication module test cases: AUTH-01 to AUTH-05.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private S3Service s3Service;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private RegisterRequest registerRequest;
    private AuthRequest authRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setRole(Role.USER);
        testUser.setAuthProvider("jwt");

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("password123");

        authRequest = new AuthRequest();
        authRequest.setUsername("testuser");
        authRequest.setPassword("password123");
    }

    /**
     * AUTH-01: Test successful login with valid credentials.
     */
    @Test
    void testLoginSuccess() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(jwtService.generateToken("testuser", Role.USER)).thenReturn("jwt-token");

        // Act
        AuthResponse response = userService.login(authRequest);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.getToken());
        assertEquals("testuser", response.getUsername());
        assertEquals("USER", response.getRole());
        verify(userRepository).findByUsername("testuser");
        verify(passwordEncoder).matches("password123", "encodedPassword");
        verify(jwtService).generateToken("testuser", Role.USER);
    }

    /**
     * AUTH-02: Test login failure with invalid credentials.
     */
    @Test
    void testLoginFailInvalidCredentials() {
        // Arrange - User not found
        when(userRepository.findByUsername("invaliduser")).thenReturn(Optional.empty());

        AuthRequest invalidRequest = new AuthRequest();
        invalidRequest.setUsername("invaliduser");
        invalidRequest.setPassword("wrongpassword");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.login(invalidRequest);
        });
        assertEquals("Invalid username or password", exception.getMessage());
    }

    /**
     * AUTH-02 (variant): Test login failure with wrong password.
     */
    @Test
    void testLoginFailWrongPassword() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongpassword", "encodedPassword")).thenReturn(false);

        AuthRequest wrongPasswordRequest = new AuthRequest();
        wrongPasswordRequest.setUsername("testuser");
        wrongPasswordRequest.setPassword("wrongpassword");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.login(wrongPasswordRequest);
        });
        assertEquals("Invalid username or password", exception.getMessage());
    }

    /**
     * AUTH-03: Test successful user registration.
     */
    @Test
    void testRegisterNewUser() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedNewPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(2L);
            return user;
        });
        when(jwtService.generateToken(anyString(), any(Role.class))).thenReturn("new-jwt-token");

        // Act
        AuthResponse response = userService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("new-jwt-token", response.getToken());
        assertEquals("newuser", response.getUsername());
        assertEquals("USER", response.getRole());
        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("newuser@example.com");
        verify(userRepository).save(any(User.class));
    }

    /**
     * AUTH-04: Test registration failure with duplicate username.
     */
    @Test
    void testRegisterDuplicateUsername() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.register(registerRequest);
        });
        assertEquals("Username already exists", exception.getMessage());
        verify(userRepository).existsByUsername("newuser");
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * AUTH-04 (variant): Test registration failure with duplicate email.
     */
    @Test
    void testRegisterDuplicateEmail() {
        // Arrange
        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("newuser@example.com")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.register(registerRequest);
        });
        assertEquals("Email already exists", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * AUTH-05: Test find or create user from Azure AD - existing user.
     */
    @Test
    void testFindOrCreateFromAzureAd_ExistingUser() {
        // Arrange
        String azureAdId = "azure-123";
        testUser.setAzureAdId(azureAdId);
        testUser.setAuthProvider("azure");
        when(userRepository.findByAzureAdId(azureAdId)).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.findOrCreateFromAzureAd(azureAdId, "test@example.com", "Test User");

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(azureAdId, result.getAzureAdId());
        verify(userRepository).findByAzureAdId(azureAdId);
        verify(userRepository, never()).save(any(User.class));
    }

    /**
     * AUTH-05 (variant): Test find or create user from Azure AD - new user.
     */
    @Test
    void testFindOrCreateFromAzureAd_NewUser() {
        // Arrange
        String azureAdId = "azure-new-123";
        String email = "newazure@example.com";
        String name = "New Azure User";
        
        when(userRepository.findByAzureAdId(azureAdId)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(3L);
            return user;
        });

        // Act
        User result = userService.findOrCreateFromAzureAd(azureAdId, email, name);

        // Assert
        assertNotNull(result);
        assertEquals(3L, result.getId());
        assertEquals(azureAdId, result.getAzureAdId());
        assertEquals(email, result.getEmail());
        assertEquals("azure", result.getAuthProvider());
        verify(userRepository).save(any(User.class));
    }

    /**
     * AUTH-05 (variant): Test find or create user from Azure AD - link existing email.
     */
    @Test
    void testFindOrCreateFromAzureAd_LinkExistingEmail() {
        // Arrange
        String azureAdId = "azure-link-123";
        String email = "test@example.com";
        
        when(userRepository.findByAzureAdId(azureAdId)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        User result = userService.findOrCreateFromAzureAd(azureAdId, email, "Test User");

        // Assert
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals("both", result.getAuthProvider());
        verify(userRepository).save(any(User.class));
    }

    /**
     * Test findByUsername - user exists.
     */
    @Test
    void testFindByUsername_Exists() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        User result = userService.findByUsername("testuser");

        // Assert
        assertNotNull(result);
        assertEquals("testuser", result.getUsername());
    }

    /**
     * Test findByUsername - user not found.
     */
    @Test
    void testFindByUsername_NotFound() {
        // Arrange
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            userService.findByUsername("unknown");
        });
        assertEquals("User not found", exception.getMessage());
    }
}

