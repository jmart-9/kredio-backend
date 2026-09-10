package com.kredio.backend.dto;

import java.util.List;
import java.util.UUID;

public record LoginResponse(
        String token,
        UUID userId,
        UUID tenantId,
        String fullName,
        String role,
        String email,
        String roleName,          // ✅ NUEVO: Nombre legible del rol
        List<String> permissions   // ✅ NUEVO: Lista de permisos
) {}