package com.kredio.backend.service;

import com.kredio.backend.dto.LoginRequest;
import com.kredio.backend.dto.LoginResponse;
import com.kredio.backend.entity.User;
import com.kredio.backend.repository.UserRepository;
import com.kredio.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

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
        // Buscar usuario por email (en todos los tenants)
        Optional<User> userOpt = userRepository.findByEmail(request.email());

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Credenciales inválidas");
        }

        User user = userOpt.get();

        // Verificar que el usuario esté activo
        if (!user.getIsActive()) {
            throw new RuntimeException("Usuario inactivo. Contacte al administrador.");
        }

        // Verificar contraseña
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new RuntimeException("Credenciales inválidas");
        }

        // Generar token JWT
        String token = jwtUtil.generateToken(
                user.getId(),
                user.getTenantId(),
                user.getRole(),
                user.getEmail()
        );

        return new LoginResponse(
                token,
                user.getId(),
                user.getTenantId(),
                user.getFullName(),
                user.getRole(),
                user.getEmail()
        );
    }
}