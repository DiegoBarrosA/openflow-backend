package com.openflow.controller;

import com.openflow.dto.AuthRequest;
import com.openflow.dto.AuthResponse;
import com.openflow.dto.RegisterRequest;
import com.openflow.dto.UserInfoResponse;
import com.openflow.model.Role;
import com.openflow.model.User;
import com.openflow.service.JwtService;
import com.openflow.service.UserService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class AuthController {
    @Autowired
    private UserService userService;
    
    @Autowired
    private JwtService jwtService;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            AuthResponse response = userService.register(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody AuthRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().trim().isEmpty() ||
            request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"Username and password are required\"}");
        }
        try {
            AuthResponse response = userService.login(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("{\"error\":\"Not authenticated\"}");
            }
            
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            // Handle null role for existing users (default to USER)
            String roleName = user.getRole() != null ? user.getRole().name() : Role.USER.name();
            
            UserInfoResponse response = new UserInfoResponse(
                    user.getUsername(),
                    user.getEmail(),
                    user.getAuthProvider(),
                    roleName
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:3000}")
    private String frontendUrl;

    @GetMapping("/azure/success")
    public ResponseEntity<?> azureAdSuccess(HttpServletResponse response) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            
            if (authentication == null || !authentication.isAuthenticated()) {
                // Redirect to frontend with error
                String redirectUrl = getFrontendBaseUrl() + "/oauth-callback?error=authentication_failed";
                response.sendRedirect(redirectUrl);
                return null;
            }
            
            String username = authentication.getName();
            User user = userService.findByUsername(username);
            
            if (user == null) {
                String redirectUrl = getFrontendBaseUrl() + "/oauth-callback?error=user_not_found";
                response.sendRedirect(redirectUrl);
                return null;
            }
            
            // Handle null role for existing users (default to USER)
            Role userRole = user.getRole() != null ? user.getRole() : Role.USER;
            
            // Generate JWT token with role
            String token = jwtService.generateToken(user.getUsername(), userRole);
            
            // Redirect to frontend with token and role
            String redirectUrl = getFrontendBaseUrl() + "/oauth-callback?token=" + 
                java.net.URLEncoder.encode(token, "UTF-8") + 
                "&username=" + java.net.URLEncoder.encode(user.getUsername(), "UTF-8") +
                "&role=" + java.net.URLEncoder.encode(userRole.name(), "UTF-8");
            
            response.sendRedirect(redirectUrl);
            return null;
        } catch (Exception e) {
            try {
                String redirectUrl = getFrontendBaseUrl() + "/oauth-callback?error=" + 
                    java.net.URLEncoder.encode(e.getMessage(), "UTF-8");
                response.sendRedirect(redirectUrl);
            } catch (Exception ex) {
                // Fallback
            }
            return null;
        }
    }
    
    private String getFrontendBaseUrl() {
        // Get first origin from CORS allowed origins
        if (frontendUrl != null && !frontendUrl.trim().isEmpty()) {
            String[] origins = frontendUrl.split(",");
            if (origins.length > 0) {
                return origins[0].trim();
            }
        }
        return "http://localhost:3000";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(MethodArgumentNotValidException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body("{\"error\":\"Validation failed: " + ex.getMessage() + "\"}");
    }
}

