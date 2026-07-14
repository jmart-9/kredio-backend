package com.kredio.backend.dto;

import java.util.UUID;

public record LoginResponse(
        String token,
        UUID userId,
        UUID tenantId,
        String fullName,
        String role,
        String email
) {
}