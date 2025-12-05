package com.openflow.service;

import com.openflow.model.User;
import com.openflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AzureAdUserService {
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    /**
     * Extract user information from OAuth2User (OAuth2 Login flow) and sync to local database.
     * 
     * @param oauth2User The OAuth2 user from Azure AD login
     * @return User entity from database (created or updated)
     */
    public User findOrCreateFromOAuth2User(OAuth2User oauth2User) {
        // Extract claims from OAuth2 user attributes
        String azureAdId = oauth2User.getAttribute("sub"); // Subject claim
        if (azureAdId == null) {
            azureAdId = oauth2User.getAttribute("oid"); // Object ID as fallback
        }
        if (azureAdId == null) {
            azureAdId = oauth2User.getName(); // Principal name as last resort
        }
        
        String email = oauth2User.getAttribute("email");
        if (email == null) {
            email = oauth2User.getAttribute("preferred_username");
        }
        
        String name = oauth2User.getAttribute("name");
        String preferredUsername = oauth2User.getAttribute("preferred_username");
        
        // Use preferred_username or email as username, fallback to name
        String username = preferredUsername != null ? preferredUsername : 
                         (email != null ? email.split("@")[0] : 
                         (name != null ? name.replaceAll("\\s+", "").toLowerCase() : azureAdId));
        
        // Ensure username is unique
        username = ensureUniqueUsername(username, azureAdId);
        
        return syncUser(azureAdId, email, username);
    }

    /**
     * Extract user information from Azure AD JWT claims and sync to local database.
     * 
     * @param jwt The Azure AD JWT token
     * @return User entity from database (created or updated)
     */
    public User findOrCreateFromAzureAd(Jwt jwt) {
        // Extract claims from Azure AD JWT
        String azureAdId = jwt.getSubject(); // "sub" claim contains Azure AD object ID
        String email = jwt.getClaimAsString("email");
        if (email == null) {
            email = jwt.getClaimAsString("preferred_username");
        }
        String name = jwt.getClaimAsString("name");
        String preferredUsername = jwt.getClaimAsString("preferred_username");
        
        // Use preferred_username or email as username, fallback to name
        String username = preferredUsername != null ? preferredUsername : 
                         (email != null ? email.split("@")[0] : 
                         (name != null ? name.replaceAll("\\s+", "").toLowerCase() : azureAdId));
        
        // Ensure username is unique
        username = ensureUniqueUsername(username, azureAdId);
        
        return syncUser(azureAdId, email, username);
    }
    
    /**
     * Common method to sync user from Azure AD to local database.
     */
    private User syncUser(String azureAdId, String email, String username) {
        // Find existing user by Azure AD ID
        Optional<User> existingUser = userRepository.findByAzureAdId(azureAdId);
        
        if (existingUser.isPresent()) {
            User user = existingUser.get();
            // Update user info if needed
            boolean updated = false;
            if (email != null && !email.equals(user.getEmail())) {
                user.setEmail(email);
                updated = true;
            }
            if (!username.equals(user.getUsername())) {
                user.setUsername(username);
                updated = true;
            }
            if (!"azure".equals(user.getAuthProvider())) {
                user.setAuthProvider("azure");
                updated = true;
            }
            if (updated) {
                user = userRepository.save(user);
            }
            return user;
        }
        
        // Check if user exists by email (might be existing JWT user)
        if (email != null) {
            Optional<User> userByEmail = userRepository.findByEmail(email);
            if (userByEmail.isPresent()) {
                User user = userByEmail.get();
                // Link Azure AD to existing user
                user.setAzureAdId(azureAdId);
                user.setAuthProvider("both"); // User can use both methods
                return userRepository.save(user);
            }
        }
        
        // Create new user from Azure AD
        User newUser = new User();
        newUser.setAzureAdId(azureAdId);
        newUser.setEmail(email != null ? email : azureAdId + "@azure.local");
        newUser.setUsername(username);
        newUser.setPassword(null); // Azure AD users don't have passwords
        newUser.setAuthProvider("azure");
        
        return userRepository.save(newUser);
    }
    
    /**
     * Generate a JWT token for Azure AD user (for API consistency).
     * 
     * @param user The user entity
     * @return JWT token string
     */
    public String generateTokenForAzureUser(User user) {
        return jwtService.generateToken(user.getUsername());
    }
    
    /**
     * Ensure username is unique by appending Azure AD ID if needed.
     */
    private String ensureUniqueUsername(String baseUsername, String azureAdId) {
        String username = baseUsername;
        int suffix = 0;
        
        while (userRepository.existsByUsername(username)) {
            // If username exists, try appending a short suffix from Azure AD ID
            String suffixStr = azureAdId.length() > 8 ? azureAdId.substring(0, 8) : azureAdId;
            username = baseUsername + "_" + suffixStr;
            suffix++;
            
            // Prevent infinite loop
            if (suffix > 10) {
                username = baseUsername + "_" + System.currentTimeMillis();
                break;
            }
        }
        
        return username;
    }
}

