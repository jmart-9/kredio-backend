package com.kredio.backend.service;

import com.kredio.backend.entity.ImpersonationLog;
import com.kredio.backend.entity.Tenant;
import com.kredio.backend.entity.User;
import com.kredio.backend.repository.ImpersonationLogRepository;
import com.kredio.backend.repository.TenantRepository;
import com.kredio.backend.repository.UserRepository;
import com.kredio.backend.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImpersonationService {

    private final ImpersonationLogRepository impersonationLogRepository;
    private final UserRepository userRepository;
    private final TenantRepository tenantRepository; // ✅ AGREGADO
    private final JwtUtil jwtUtil;

    @Transactional
    public String startImpersonation(UUID adminGlobalId, UUID targetUserId, String ipAddress) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        // ✅ NUEVO: Obtener el schemaName del tenant del usuario objetivo
        Tenant tenant = tenantRepository.findById(targetUser.getTenantId())
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        // Crear log de impersonation
        ImpersonationLog log = ImpersonationLog.builder()
                .adminGlobalId(adminGlobalId)
                .impersonatedUserId(targetUserId)
                .impersonatedTenantId(targetUser.getTenantId())
                .ipAddress(ipAddress)
                .startedAt(Instant.now())
                .build();
        impersonationLogRepository.save(log);

        // TODO: En el futuro, obtén los permisos reales del usuario objetivo desde la BD
        List<String> permissions = List.of("USER:READ", "LOAN:READ", "LOAN:CREATE", "CLIENT:READ", "REPORT:VIEW");

        // ✅ NUEVO: Generar token con schemaName (6 argumentos)
        return jwtUtil.generateToken(
                targetUser.getId(),
                targetUser.getTenantId(),
                tenant.getSchemaName(), // ✅ AGREGADO
                targetUser.getRole(),
                targetUser.getEmail(),
                permissions
        );
    }

    @Transactional
    public void endImpersonation(UUID logId) {
        ImpersonationLog log = impersonationLogRepository.findById(logId)
                .orElseThrow(() -> new RuntimeException("Log de impersonation no encontrado"));
        log.setEndedAt(Instant.now());
        impersonationLogRepository.save(log);
    }

    public List<ImpersonationLog> getImpersonationLogs() {
        return impersonationLogRepository.findAll();
    }

    public List<ImpersonationLog> getLogsByAdmin(UUID adminGlobalId) {
        return impersonationLogRepository.findAll()
                .stream()
                .filter(log -> log.getAdminGlobalId().equals(adminGlobalId))
                .toList();
    }
}