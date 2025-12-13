package com.openflow.service;

import com.openflow.dto.AuthRequest;
import com.openflow.dto.AuthResponse;
import com.openflow.dto.RegisterRequest;
import com.openflow.dto.UserInfoResponse;
import com.openflow.model.Role;
import com.openflow.model.User;
import com.openflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

@Service
public class UserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;
    
    /**
     * Get user role, defaulting to USER if null (for existing users before RBAC).
     */
    private Role getEffectiveRole(User user) {
        return user.getRole() != null ? user.getRole() : Role.USER;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new RuntimeException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setAuthProvider("jwt");
        user.setRole(Role.USER); // Explicitly set role for new users

        user = userRepository.save(user);

        Role role = getEffectiveRole(user);
        String token = jwtService.generateToken(user.getUsername(), role);
        return new AuthResponse(token, user.getUsername(), role.name());
    }

    public AuthResponse login(AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new RuntimeException("Invalid username or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid username or password");
        }

        Role role = getEffectiveRole(user);
        String token = jwtService.generateToken(user.getUsername(), role);
        return new AuthResponse(token, user.getUsername(), role.name());
    }

    @Autowired
    private S3Service s3Service;

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    /**
     * Get user info with profile picture URL.
     */
    public UserInfoResponse getUserInfo(String username) {
        User user = findByUsername(username);
        String profilePictureUrl = null;
        
        if (user.getProfilePictureKey() != null && s3Service.isEnabled()) {
            try {
                profilePictureUrl = s3Service.getPresignedUrl(user.getProfilePictureKey());
            } catch (Exception e) {
                // Ignore - profile picture not available
            }
        }
        
        UserInfoResponse response = new UserInfoResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setAuthProvider(user.getAuthProvider());
        response.setRole(getEffectiveRole(user).name());
        response.setProfilePictureUrl(profilePictureUrl);
        
        return response;
    }

    /**
     * Upload profile picture for user.
     */
    public String uploadProfilePicture(String username, MultipartFile file) throws IOException {
        if (!s3Service.isEnabled()) {
            throw new RuntimeException("File storage is not enabled");
        }
        
        User user = findByUsername(username);
        
        // Delete old profile picture if exists
        if (user.getProfilePictureKey() != null) {
            s3Service.deleteFile(user.getProfilePictureKey());
        }
        
        // Upload new picture
        String s3Key = s3Service.uploadProfilePicture(file, user.getId());
        user.setProfilePictureKey(s3Key);
        userRepository.save(user);
        
        return s3Service.getPresignedUrl(s3Key);
    }

    /**
     * Get profile picture URL for user.
     */
    public String getProfilePictureUrl(String username) {
        User user = findByUsername(username);
        
        if (user.getProfilePictureKey() == null) {
            return null;
        }
        
        if (!s3Service.isEnabled()) {
            return null;
        }
        
        return s3Service.getPresignedUrl(user.getProfilePictureKey());
    }

    /**
     * Delete profile picture for user.
     */
    public void deleteProfilePicture(String username) {
        User user = findByUsername(username);
        
        if (user.getProfilePictureKey() != null) {
            s3Service.deleteFile(user.getProfilePictureKey());
            user.setProfilePictureKey(null);
            userRepository.save(user);
        }
    }

    /**
     * Find or create user from Azure AD claims.
     * 
     * @param azureAdId Azure AD object ID
     * @param email User email
     * @param name User display name
     * @return User entity
     */
    public User findOrCreateFromAzureAd(String azureAdId, String email, String name) {
        // This method is kept for compatibility but AzureAdUserService handles the logic
        // The actual implementation uses JWT directly
        Optional<User> existingUser = userRepository.findByAzureAdId(azureAdId);
        if (existingUser.isPresent()) {
            return existingUser.get();
        }
        
        // If not found by Azure AD ID, check by email
        if (email != null) {
            Optional<User> userByEmail = userRepository.findByEmail(email);
            if (userByEmail.isPresent()) {
                User user = userByEmail.get();
                user.setAzureAdId(azureAdId);
                user.setAuthProvider("both");
                return userRepository.save(user);
            }
        }
        
        // Create new user
        String username = email != null ? email.split("@")[0] : 
                          (name != null ? name.replaceAll("\\s+", "").toLowerCase() : azureAdId);
        
        User newUser = new User();
        newUser.setAzureAdId(azureAdId);
        newUser.setEmail(email != null ? email : azureAdId + "@azure.local");
        newUser.setUsername(username);
        newUser.setPassword(null);
        newUser.setAuthProvider("azure");
        
        return userRepository.save(newUser);
    }
}

