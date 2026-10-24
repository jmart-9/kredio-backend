package com.kredio.backend.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TenantRequest(
        @NotBlank(message = "El nombre del tenant es obligatorio")
        @Size(max = 100, message = "El nombre no puede exceder 100 caracteres")
        String name,

        @NotBlank(message = "El subdominio es obligatorio")
        @Size(max = 50, message = "El subdominio no puede exceder 50 caracteres")
        String subdomain,

        @NotBlank(message = "El email del administrador es obligatorio")
        @Email(message = "El email no es válido")
        String adminEmail,

        @NotBlank(message = "La contraseña del administrador es obligatoria")
        @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
        String adminPassword,

        @NotBlank(message = "El nombre del administrador es obligatorio")
        String adminFullName
) {}