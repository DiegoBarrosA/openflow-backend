package com.openflow.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserInfoResponse {
    private String username;
    private String email;
    private String authProvider;
    private String role;
}

