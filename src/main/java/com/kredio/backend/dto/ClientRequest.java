package com.kredio.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ClientRequest(
        @NotNull(message = "El nombre es obligatorio")
        String fullName,

        @NotBlank(message = "El DPI/ID es obligatorio")
        @Size(min = 5, max = 20, message = "El DPI debe tener entre 5 y 20 caracteres")
        String dpiOrId,

        @NotBlank(message = "El teléfono es obligatorio")
        String phone,

        String address,
        String email,
        String occupation,
        String monthlyIncome,

        // ✅ NUEVO CAMPO - Cartera asignada
        UUID portfolioId
) {
}