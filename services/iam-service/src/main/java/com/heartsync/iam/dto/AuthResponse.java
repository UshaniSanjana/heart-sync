package com.heartsync.iam.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String tokenType;     // always "Bearer"
    private long   expiresIn;     // seconds
    private String userId;
    private String email;
    private String fullName;
    private String role;
}
