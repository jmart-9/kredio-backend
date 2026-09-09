package com.kredio.backend.service;

import com.kredio.backend.dto.LoginRequest;
import com.kredio.backend.dto.LoginResponse;
import com.kredio.backend.entity.Tenant;
import com.kredio.backend.entity.User;
import com.kredio.backend.repository.TenantRepository;
import com.kredio.backend.repository.UserRepository;
import com.kredio.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        log.info("🔐 Intentando iniciar sesión con el email: {}", request.email());

        // 1. Buscar usuario por email
        Optional<User> userOpt = userRepository.findByEmail(request.email());
        if (userOpt.isEmpty()) {
            log.warn("⚠️ Fallo de login: No se encontró ningún usuario con el email '{}'", request.email());
            throw new RuntimeException("Credenciales inválidas");
        }
        User user = userOpt.get();

        // 2. Verificar que el usuario esté activo
        if (user.getIsActive() == null || !user.getIsActive()) {
            log.warn("⚠️ Fallo de login: El usuario '{}' está inactivo", user.getEmail());
            throw new RuntimeException("Usuario inactivo. Contacte al administrador.");
        }

        // 3. Verificar contraseña
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            log.warn("⚠️ Fallo de login: Contraseña incorrecta para el usuario '{}'", user.getEmail());
            throw new RuntimeException("Credenciales inválidas");
        }

        // 4. Obtener el tenant para extraer el schemaName
        Tenant tenant = tenantRepository.findById(user.getTenantId())
                .orElseThrow(() -> {
                    log.error("❌ Error crítico: El usuario '{}' tiene un tenant_id '{}' que no existe en la BD", user.getEmail(), user.getTenantId());
                    return new RuntimeException("Tenant no encontrado");
                });

        // 5. Determinar el nombre legible del rol
        String userRole = user.getRole() != null ? user.getRole() : "UNKNOWN";
        String roleName = switch (userRole) {
            case "ADMIN_GLOBAL" -> "Administrador Global";
            case "ADMIN_TENANT" -> "Administrador de Tenant";
            case "COBRADOR" -> "Cobrador / Cajero";
            case "AUDITOR" -> "Auditor";
            case "TECH" -> "Soporte Técnico";
            default -> userRole;
        };

        // 6. Asignar permisos (Hardcodeados para el ejemplo, idealmente vendrían de la BD)
        List<String> permissions;
        if ("ADMIN_GLOBAL".equals(userRole)) {
            permissions = List.of(
                    "GLOBAL:MANAGE", "USER:CREATE", "USER:READ", "USER:UPDATE", "USER:DELETE",
                    "LOAN:CREATE", "LOAN:READ", "LOAN:UPDATE", "LOAN:PAYMENT",
                    "CLIENT:CREATE", "CLIENT:READ", "REPORT:VIEW", "CONFIG:MANAGE", "AUDIT:READ"
            );
        } else {
            // Permisos por defecto para ADMIN_TENANT y otros roles operativos
            permissions = List.of(
                    "LOAN:READ", "LOAN:CREATE", "LOAN:UPDATE", "LOAN:PAYMENT", "LOAN:APPROVE",
                    "CLIENT:READ", "CLIENT:CREATE", "REPORT:VIEW"
            );
        }

        // 7. Generar token JWT con los permisos y schemaName
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getTenantId(),
                tenant.getSchemaName(),
                userRole,
                user.getEmail(),
                permissions
        );

        log.info("✅ Login exitoso para el usuario: {} (Tenant: {})", user.getEmail(), tenant.getSchemaName());

        // 8. Retornar la respuesta completa
        return new LoginResponse(
                token,
                user.getId(),
                user.getTenantId(),
                user.getFullName(),
                userRole,
                user.getEmail(),
                roleName,
                permissions
        );
    }
}