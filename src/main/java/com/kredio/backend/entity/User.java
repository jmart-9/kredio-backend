package com.kredio.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tenant_id", "email"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "role_id")
    private UUID roleId;

    @Column(name = "role", nullable = false, length = 30)
    private String role;  // ✅ String, no enum

    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @Column(nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "failed_login_attempts")
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "account_locked_until")
    private Instant accountLockedUntil;

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    @Column(name = "created_at")
    @CreationTimestamp
    private Instant createdAt;

    // ✅ NUEVOS CAMPOS PARA PIN (Fase 1)
    @Column(name = "pin_hash", length = 255)
    private String pinHash;

    @Column(name = "pin_attempts")
    @Builder.Default
    private Integer pinAttempts = 0;

    @Column(name = "pin_locked_until")
    private Instant pinLockedUntil;
}