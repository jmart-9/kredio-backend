package com.kredio.backend.controller;

import com.kredio.backend.entity.User;
import com.kredio.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import com.kredio.backend.dto.ChangePasswordRequest;
import jakarta.validation.Valid;
import java.util.Map;


import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class UserController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // ✅ Agregado para encriptar la contraseña

    // 1. Obtener todos los usuarios del tenant
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers(HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        List<User> users = userRepository.findByTenantId(tenantId);

        // ✅ Por seguridad, no enviamos el hash de la contraseña al frontend
        users.forEach(user -> user.setPasswordHash(null));

        return ResponseEntity.ok(users);
    }

    // 2. Crear un nuevo usuario
    @PostMapping
    public ResponseEntity<User> createUser(
            @RequestBody User user,
            HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");

        // Validar que el email no exista ya en el sistema
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new RuntimeException("El email ya está registrado en el sistema");
        }

        // Asignar datos del tenant y valores por defecto
        user.setTenantId(tenantId);
        user.setPasswordHash(passwordEncoder.encode("123456")); // Contraseña inicial por defecto
        user.setIsActive(true);

        User savedUser = userRepository.save(user);
        savedUser.setPasswordHash(null); // No devolver el hash por seguridad

        return ResponseEntity.ok(savedUser);
    }

    // 3. Actualizar un usuario existente
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(
            @PathVariable UUID id,
            @RequestBody User userDetails,
            HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");

        User existingUser = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Validar que el usuario pertenezca al tenant
        if (!existingUser.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Acceso denegado: el usuario no pertenece a esta financiera");
        }

        // Actualizar campos permitidos
        existingUser.setFullName(userDetails.getFullName());
        existingUser.setEmail(userDetails.getEmail());
        existingUser.setRole(userDetails.getRole());
        existingUser.setIsActive(userDetails.getIsActive());

        User updatedUser = userRepository.save(existingUser);
        updatedUser.setPasswordHash(null); // No devolver el hash

        return ResponseEntity.ok(updatedUser);
    }

    // 4. Eliminar un usuario
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");

        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        if (!user.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Acceso denegado: el usuario no pertenece a esta financiera");
        }

        userRepository.delete(user);
        return ResponseEntity.ok().build();
    }

    //5. Cambiar contraseña
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            HttpServletRequest httpRequest) {

        UUID userId = (UUID) httpRequest.getAttribute("userId");
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");

        // Buscar el usuario
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // Validar que el usuario pertenezca al tenant
        if (!user.getTenantId().equals(tenantId)) {
            return ResponseEntity.status(403).body(Map.of(
                    "error", "Acceso denegado"
            ));
        }

        // Validar que las contraseñas nuevas coincidan
        if (!request.newPassword().equals(request.confirmPassword())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Las nuevas contraseñas no coinciden"
            ));
        }

        // Validar contraseña actual
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "La contraseña actual es incorrecta"
            ));
        }

        // Validar que la nueva contraseña no sea igual a la actual
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "La nueva contraseña debe ser diferente a la actual"
            ));
        }

        // Actualizar contraseña
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "message", "Contraseña cambiada correctamente"
        ));
    }
}