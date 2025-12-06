package com.openflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private String role;
    
    // Constructor for backward compatibility
    public AuthResponse(String token, String username) {
        this.token = token;
        this.username = username;
        this.role = "USER"; // Default role
    }
}

