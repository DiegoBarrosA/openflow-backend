package com.openflow.service;

import com.openflow.config.JwtUtil;
import com.openflow.model.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    @Autowired
    private JwtUtil jwtUtil;

    /**
     * Generate token without role (backward compatibility, defaults to USER).
     */
    public String generateToken(String username) {
        return jwtUtil.generateToken(username);
    }
    
    /**
     * Generate token with role claim.
     */
    public String generateToken(String username, Role role) {
        return jwtUtil.generateToken(username, role);
    }

    public String extractUsername(String token) {
        return jwtUtil.extractUsername(token);
    }
    
    /**
     * Extract role from JWT token.
     */
    public Role extractRole(String token) {
        return jwtUtil.extractRole(token);
    }

    public Boolean validateToken(String token, String username) {
        return jwtUtil.validateToken(token, username);
    }
}

