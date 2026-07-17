package com.kredio.backend.service;

import com.kredio.backend.entity.ImpersonationLog;
import com.kredio.backend.entity.User;
import com.kredio.backend.repository.ImpersonationLogRepository;
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
    private final JwtUtil jwtUtil;

    @Transactional
    public String startImpersonation(UUID adminGlobalId, UUID targetUserId, String ipAddress) {
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

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

        // Generar token con los datos del usuario objetivo y sus permisos
        return jwtUtil.generateToken(
                targetUser.getId(),
                targetUser.getTenantId(),
                targetUser.getRole(),
                targetUser.getEmail(),
                permissions // ✅ 5to argumento agregado
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