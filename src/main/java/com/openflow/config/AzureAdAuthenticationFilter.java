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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
public class AzureAdAuthenticationFilter extends OncePerRequestFilter {
    
    @Autowired
    private AzureAdUserService azureAdUserService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        
        // Check if we have an Azure AD JWT token in the security context
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication instanceof JwtAuthenticationToken) {
            JwtAuthenticationToken jwtAuth = (JwtAuthenticationToken) authentication;
            Jwt jwt = jwtAuth.getToken();
            
            try {
                // Sync user from Azure AD to local database
                User user = azureAdUserService.findOrCreateFromAzureAd(jwt);
                
                // Create UserDetails for Spring Security
                UserDetails userDetails = org.springframework.security.core.userdetails.User
                        .withUsername(user.getUsername())
                        .password(user.getPassword() != null ? user.getPassword() : "")
                        .authorities(new ArrayList<>())
                        .build();
                
                // Create authentication token with user details
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                authToken.setDetails(authentication.getDetails());
                
                // Set authentication in context
                SecurityContextHolder.getContext().setAuthentication(authToken);
                
            } catch (Exception e) {
                // Log error but don't fail the request
                logger.error("Error processing Azure AD authentication", e);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}

