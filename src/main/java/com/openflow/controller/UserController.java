package com.openflow.controller;

import com.openflow.dto.UserInfoResponse;
import com.openflow.model.User;
import com.openflow.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * User management endpoints.
 */
@Tag(name = "Users", description = "User management and profile operations")
@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "${cors.allowed-origins}")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Get current user info.
     */
    @Operation(summary = "Get current user", description = "Get information about the currently authenticated user")
    @ApiResponse(responseCode = "200", description = "User info retrieved")
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<UserInfoResponse> getCurrentUser(Authentication authentication) {
        String username = authentication.getName();
        UserInfoResponse userInfo = userService.getUserInfo(username);
        return ResponseEntity.ok(userInfo);
    }

    /**
     * Get all users (for admin and user assignment).
     */
    @Operation(summary = "Get all users", description = "Get list of all users")
    @ApiResponse(responseCode = "200", description = "Users list retrieved")
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<Map<String, Object>>> getAllUsers() {
        List<User> users = userService.getAllUsers();
        List<Map<String, Object>> response = users.stream()
                .map(user -> Map.<String, Object>of(
                        "id", user.getId(),
                        "username", user.getUsername()
                ))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    /**
     * Upload profile picture.
     */
    @Operation(summary = "Upload profile picture", description = "Upload a new profile picture for the current user")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile picture uploaded"),
        @ApiResponse(responseCode = "400", description = "Invalid file or S3 not enabled")
    })
    @PostMapping("/me/profile-picture")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> uploadProfilePicture(
            @RequestParam("file") MultipartFile file,
            Authentication authentication) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }
            
            // Validate file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest().body(Map.of("error", "File must be an image"));
            }
            
            String username = authentication.getName();
            String url = userService.uploadProfilePicture(username, file);
            return ResponseEntity.ok(Map.of("url", url));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to upload profile picture"));
        }
    }

    /**
     * Get profile picture URL.
     */
    @Operation(summary = "Get profile picture URL", description = "Get the presigned URL for the current user's profile picture")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Profile picture URL retrieved"),
        @ApiResponse(responseCode = "404", description = "No profile picture set")
    })
    @GetMapping("/me/profile-picture")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<?> getProfilePictureUrl(Authentication authentication) {
        String username = authentication.getName();
        String url = userService.getProfilePictureUrl(username);
        
        if (url == null) {
            return ResponseEntity.ok(Map.of("url", (Object) null));
        }
        
        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * Delete profile picture.
     */
    @Operation(summary = "Delete profile picture", description = "Delete the current user's profile picture")
    @ApiResponse(responseCode = "204", description = "Profile picture deleted")
    @DeleteMapping("/me/profile-picture")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> deleteProfilePicture(Authentication authentication) {
        String username = authentication.getName();
        userService.deleteProfilePicture(username);
        return ResponseEntity.noContent().build();
    }
}

