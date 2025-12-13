package com.openflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserInfoResponse {
    private Long id;
    private String username;
    private String email;
    private String authProvider;
    private String role;
    private String profilePictureUrl;
    
    public UserInfoResponse(String username, String email, String authProvider, String role) {
        this.username = username;
        this.email = email;
        this.authProvider = authProvider;
        this.role = role;
    }
}

