package com.openflow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@org.springframework.context.annotation.Profile("!test")
public class SecurityConfig {
    private static final Logger logger = LoggerFactory.getLogger(SecurityConfig.class);
    
    @Autowired
    @org.springframework.context.annotation.Lazy
    private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Autowired(required = false)
    private AzureAdAuthenticationFilter azureAdAuthenticationFilter;
    
    @Value("${auth.mode:both}")
    private String authMode;
    
    @Value("${AZURE_CLIENT_ID:}")
    private String azureClientId;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    private boolean isAzureEnabled() {
        // Azure is enabled if auth mode is "azure" or "both", AND Azure credentials are present
        boolean modeAllowsAzure = "azure".equalsIgnoreCase(authMode) || "both".equalsIgnoreCase(authMode);
        boolean hasAzureCredentials = azureClientId != null && !azureClientId.trim().isEmpty();
        return modeAllowsAzure && hasAzureCredentials;
    }
    
    private boolean isJwtEnabled() {
        // JWT is enabled if auth mode is "jwt", "local", or "both"
        return !"azure".equalsIgnoreCase(authMode);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        logger.info("=== Security Configuration ===");
        logger.info("AUTH_MODE value: '{}'", authMode);
        logger.info("Azure Client ID present: {}", azureClientId != null && !azureClientId.trim().isEmpty());
        logger.info("JWT Auth enabled: {}", isJwtEnabled());
        logger.info("Azure OAuth enabled: {}", isAzureEnabled());
        
        http.csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()));
        
        // Session management: OAuth2 Login requires sessions, JWT is stateless
        if (isAzureEnabled()) {
            // Use IF_REQUIRED for OAuth2 Login flow (sessions needed for OAuth2 callback)
            http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED));
            logger.info("Session policy: IF_REQUIRED (for OAuth2 Login)");
        } else {
            // Use STATELESS for pure JWT authentication
            http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
            logger.info("Session policy: STATELESS (JWT only)");
        }
        
        http.authorizeHttpRequests(auth -> auth
                // Public endpoints - no authentication required
                .requestMatchers("/api/auth/**", "/h2-console/**", "/oauth2/**", "/login/oauth2/**", "/login").permitAll()
                // Public board endpoints - anonymous access allowed
                .requestMatchers("/api/public/**").permitAll()
                // All other requests require authentication
                // Role-based authorization is handled via @PreAuthorize annotations on controllers
                .anyRequest().authenticated()
            );
        
        // Add JWT filter if JWT auth is enabled
        if (isJwtEnabled()) {
            logger.info("Adding JwtAuthenticationFilter");
            http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        }
        
        // Configure OAuth2 if Azure AD is enabled
        if (isAzureEnabled()) {
            logger.info("Configuring OAuth2 Login for Azure AD");
            // OAuth2 Login for redirect flow (browser login)
            http.oauth2Login(oauth2 -> oauth2
                .defaultSuccessUrl("/api/auth/azure/success", true)
            );
            
            // NOTE: We do NOT enable oauth2ResourceServer here because:
            // - We use our own local JWTs for API authentication (via JwtAuthenticationFilter)
            // - oauth2ResourceServer would try to validate our local JWTs against Azure AD keys
            // - Azure OAuth2 Login is only used for the initial browser login flow
            
            // Add Azure AD filter if available
            if (azureAdAuthenticationFilter != null) {
                http.addFilterAfter(azureAdAuthenticationFilter, OAuth2LoginAuthenticationFilter.class);
            }
        } else {
            logger.info("Azure OAuth NOT configured (mode={}, hasCredentials={})", 
                authMode, azureClientId != null && !azureClientId.trim().isEmpty());
        }
        
        http.headers(headers -> headers.frameOptions(frameOptions -> frameOptions.disable())); // For H2 console

        return http.build();
    }

    @Value("${CORS_ALLOWED_ORIGINS:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        
        // Parse allowed origins - comma-separated list of specific origins
        // Wildcard "*" is not supported for security reasons
        if (allowedOrigins == null || allowedOrigins.trim().isEmpty()) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS must be set to a valid origin URL");
        }
        
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty())
            .collect(Collectors.toList());
        
        if (origins.isEmpty()) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS must contain at least one valid origin URL");
        }
        
        configuration.setAllowedOrigins(origins);
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}

