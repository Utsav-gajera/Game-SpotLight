package com.gamestore.authuser.dto;

import java.time.Instant;

public record UserResponse(
        String id,
        String username,
        String email,
        String displayName,
        String role,
        Instant createdAt) {
}