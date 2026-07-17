package com.kredio.backend.service;

import com.kredio.backend.dto.LoginRequest;
import com.kredio.backend.dto.LoginResponse;
import com.kredio.backend.entity.User;
import com.kredio.backend.repository.UserRepository;
import com.kredio.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * Autentica un usuario y genera un token JWT
     */
    public LoginResponse login(LoginRequest request) {
        // 1. Buscar usuario por email (en todos los tenants)
        Optional<User> userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Credenciales inválidas");
        }

        User user = userOpt.get();

        // 2. Verificar que el usuario esté activo
        if (user.getIsActive() == null || !user.getIsActive()) {
            throw new RuntimeException("Usuario inactivo. Contacte al administrador.");
        }

        // 3. Verificar contraseña
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        // 4. Determinar el nombre legible del rol
        String roleName = switch (user.getRole()) {
            case "ADMIN_GLOBAL" -> "Administrador Global";
            case "ADMIN_TENANT" -> "Administrador de Tenant";
            case "COBRADOR" -> "Cobrador / Cajero";
            case "AUDITOR" -> "Auditor";
            case "TECH" -> "Soporte Técnico";
            default -> user.getRole();
        };

        // 5. Asignar permisos (Si es ADMIN_GLOBAL, le damos todos, incluido el comodín)
        List<String> permissions;
        if ("ADMIN_GLOBAL".equals(user.getRole())) {
            permissions = List.of(
                    "GLOBAL:MANAGE", "USER:CREATE", "USER:READ", "USER:UPDATE", "USER:DELETE",
                    "LOAN:CREATE", "LOAN:READ", "LOAN:UPDATE", "LOAN:PAYMENT",
                    "CLIENT:CREATE", "CLIENT:READ", "REPORT:VIEW", "CONFIG:MANAGE", "AUDIT:READ"
            );
        } else {
            // Para otros roles, por ahora les damos un set básico (luego lo leerás de la BD)
            permissions = List.of("LOAN:READ", "CLIENT:READ", "REPORT:VIEW");
        }

        // 6. Generar token JWT con los permisos
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getTenantId(),
                user.getRole(),
                user.getEmail(),
                permissions
        );

        // 7. Retornar la respuesta completa
        return new LoginResponse(
                token,
                user.getId(),
                user.getTenantId(),
                user.getFullName(),
                user.getRole(),
                user.getEmail(),
                roleName,
                permissions
        );
    }
}