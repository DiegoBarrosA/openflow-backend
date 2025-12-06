package com.openflow.service;

import com.openflow.model.Role;
import com.openflow.model.User;
import com.openflow.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class AzureAdUserService {
    private static final Logger logger = LoggerFactory.getLogger(AzureAdUserService.class);
    
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;
    
    // Azure AD Group IDs for role mapping
    @Value("${azure.ad.admin-group-id:}")
    private String adminGroupId;
    
    @Value("${azure.ad.user-group-id:}")
    private String userGroupId;
    
    // App Role names for role mapping
    private static final String ADMIN_APP_ROLE = "Admin";
    private static final String USER_APP_ROLE = "User";

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
        
        // Extract role from Azure AD claims
        Role role = extractRoleFromOAuth2User(oauth2User);
        
        return syncUser(azureAdId, email, username, role);
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
        
        // Extract role from Azure AD JWT claims
        Role role = extractRoleFromJwt(jwt);
        
        return syncUser(azureAdId, email, username, role);
    }
    
    /**
     * Extract role from OAuth2User attributes (groups and roles claims).
     */
    @SuppressWarnings("unchecked")
    private Role extractRoleFromOAuth2User(OAuth2User oauth2User) {
        Map<String, Object> attributes = oauth2User.getAttributes();
        return extractRoleFromClaims(attributes);
    }
    
    /**
     * Extract role from JWT claims (groups and roles claims).
     */
    private Role extractRoleFromJwt(Jwt jwt) {
        return extractRoleFromClaims(jwt.getClaims());
    }
    
    /**
     * Extract role from claims map (supports both Azure AD Groups and App Roles).
     * Priority: Admin role takes precedence over User role.
     * 
     * @param claims The claims map from OAuth2User or JWT
     * @return Role.ADMIN if user is in admin group/role, otherwise Role.USER
     */
    @SuppressWarnings("unchecked")
    private Role extractRoleFromClaims(Map<String, Object> claims) {
        // Log all claims for debugging
        logger.info("=== Azure AD Claims Debug ===");
        logger.info("Admin Group ID configured: {}", adminGroupId);
        logger.info("All claims received: {}", claims.keySet());
        
        // Log ALL claim values for debugging
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            logger.info("Claim [{}] = {}", entry.getKey(), entry.getValue());
        }
        
        // Check Azure AD Groups (via "groups" claim)
        Object groupsClaim = claims.get("groups");
        logger.info("Groups claim value: {}", groupsClaim);
        if (groupsClaim != null) {
            List<String> groups = null;
            if (groupsClaim instanceof List) {
                groups = (List<String>) groupsClaim;
            } else if (groupsClaim instanceof Collection) {
                groups = List.copyOf((Collection<String>) groupsClaim);
            }
            
            if (groups != null) {
                logger.debug("Azure AD groups found: {}", groups);
                
                // Check for admin group
                if (adminGroupId != null && !adminGroupId.isEmpty() && groups.contains(adminGroupId)) {
                    logger.info("User is member of admin group: {}", adminGroupId);
                    return Role.ADMIN;
                }
            }
        }
        
        // Check Azure AD App Roles (via "roles" claim)
        Object rolesClaim = claims.get("roles");
        logger.info("Roles claim value: {}", rolesClaim);
        if (rolesClaim != null) {
            List<String> appRoles = null;
            if (rolesClaim instanceof List) {
                appRoles = (List<String>) rolesClaim;
            } else if (rolesClaim instanceof Collection) {
                appRoles = List.copyOf((Collection<String>) rolesClaim);
            }
            
            if (appRoles != null) {
                logger.debug("Azure AD app roles found: {}", appRoles);
                
                // Check for Admin app role
                if (appRoles.contains(ADMIN_APP_ROLE)) {
                    logger.info("User has Admin app role");
                    return Role.ADMIN;
                }
            }
        }
        
        // Default to USER role
        logger.debug("No admin group/role found, defaulting to USER role");
        return Role.USER;
    }
    
    /**
     * Common method to sync user from Azure AD to local database.
     */
    private User syncUser(String azureAdId, String email, String username, Role role) {
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
            // Always update role from Azure AD claims (real-time sync)
            if (role != user.getRole()) {
                logger.info("Updating user {} role from {} to {}", username, user.getRole(), role);
                user.setRole(role);
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
                user.setRole(role); // Update role from Azure AD
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
        newUser.setRole(role);
        
        logger.info("Creating new Azure AD user: {} with role: {}", username, role);
        return userRepository.save(newUser);
    }
    
    /**
     * Generate a JWT token for Azure AD user (for API consistency).
     * 
     * @param user The user entity
     * @return JWT token string
     */
    public String generateTokenForAzureUser(User user) {
        return jwtService.generateToken(user.getUsername(), user.getRole());
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

