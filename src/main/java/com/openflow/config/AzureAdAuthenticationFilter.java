package com.openflow.config;

import com.openflow.model.User;
import com.openflow.service.AzureAdUserService;
import com.openflow.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Component
public class AzureAdAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private AzureAdUserService azureAdUserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        
        // Handle OAuth2 Login flow (user logged in via browser redirect)
        if (authentication instanceof OAuth2AuthenticationToken) {
            OAuth2AuthenticationToken oauth2Auth = (OAuth2AuthenticationToken) authentication;
            OAuth2User oauth2User = oauth2Auth.getPrincipal();
            
            try {
                // Sync user from Azure AD to local database (role extracted from claims)
                User user = azureAdUserService.findOrCreateFromOAuth2User(oauth2User);
                
                // Create authorities based on user role
                List<GrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                
                // Create UserDetails for Spring Security
                UserDetails userDetails = org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password(user.getPassword() != null ? user.getPassword() : "")
                        .authorities(authorities)
                        .build();
                
                // Create authentication token with user details
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(authentication.getDetails());
                
                // Set authentication in context
                SecurityContextHolder.getContext().setAuthentication(authToken);
                
                logger.info("Azure AD OAuth2 user authenticated: " + user.getUsername() + " with role: " + user.getRole());
                
            } catch (Exception e) {
                logger.error("Error processing Azure AD OAuth2 authentication", e);
            }
        }
        // Handle JWT Resource Server flow (API calls with Bearer token)
        else if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();
            
            try {
                // Sync user from Azure AD to local database (role extracted from claims)
                User user = azureAdUserService.findOrCreateFromAzureAd(jwt);
                
                // Create authorities based on user role
                List<GrantedAuthority> authorities = new ArrayList<>();
                authorities.add(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
                
                // Create UserDetails for Spring Security
                UserDetails userDetails = org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password(user.getPassword() != null ? user.getPassword() : "")
                        .authorities(authorities)
                        .build();
                
                // Create authentication token with user details
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(authentication.getDetails());
                
                // Set authentication in context
                SecurityContextHolder.getContext().setAuthentication(authToken);
                
            } catch (Exception e) {
                logger.error("Error processing Azure AD JWT authentication", e);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}

