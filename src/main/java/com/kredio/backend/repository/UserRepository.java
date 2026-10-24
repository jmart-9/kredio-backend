package com.kredio.backend.repository;

import com.kredio.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    // Busca un usuario por su correo electrónico
    Optional<User> findByEmail(String email);

    // Busca todos los usuarios de un tenant específico
    List<User> findByTenantId(UUID tenantId);

    List<User> findAllByEmail(String email);

    // Verifica si un correo ya está registrado
    boolean existsByEmail(String email);

    // ✅ NUEVO: Busca por email Y tenant (perfecto para tu @UniqueConstraint)
    Optional<User> findByEmailAndTenantId(String email, UUID tenantId);

}