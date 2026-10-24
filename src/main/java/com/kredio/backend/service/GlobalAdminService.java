package com.kredio.backend.service;

import com.kredio.backend.entity.SaasPlan;
import com.kredio.backend.entity.Tenant;
import com.kredio.backend.repository.SaasPlanRepository;
import com.kredio.backend.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j  // ✅ Esto habilita la variable 'log'
public class GlobalAdminService {

    private final TenantRepository tenantRepository;
    private final TenantService tenantService;
    private final SaasPlanRepository saasPlanRepository;
    private final JdbcTemplate jdbcTemplate;

    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    public Tenant getTenantById(UUID id) {
        return tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));
    }

    public Tenant createTenant(String name, String subdomain, String adminEmail, String adminPassword, String adminFullName) {
        return tenantService.createTenant(name, subdomain, adminEmail, adminPassword, adminFullName);
    }

    @Transactional
    public Tenant updateTenantStatus(UUID id, String status) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        try {
            tenant.setStatus(Tenant.TenantStatus.valueOf(status.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Estado inválido. Use ACTIVE o SUSPENDED");
        }

        return tenantRepository.save(tenant);
    }

    @Transactional
    public void deleteTenant(UUID id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        // 1. Eliminar el schema del tenant
        try {
            jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + tenant.getSchemaName() + "\" CASCADE");
            log.info("✅ Schema eliminado: {}", tenant.getSchemaName());
        } catch (Exception e) {
            log.error("⚠️ Error al eliminar schema: {}", e.getMessage());
        }

        // 2. Eliminar el registro del tenant
        tenantRepository.delete(tenant);
        log.info("✅ Tenant eliminado: {}", tenant.getName());
    }

    @Transactional
    public Tenant assignPlan(UUID tenantId, UUID planId, LocalDate planEndDate) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Tenant no encontrado"));

        SaasPlan plan = saasPlanRepository.findById(planId)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado"));

        tenant.setPlanId(planId);
        tenant.setPlanStartDate(LocalDate.now());
        tenant.setPlanEndDate(planEndDate);

        // Si el plan tiene días de trial, marcamos el tenant como trial
        if (plan.getTrialDays() != null && plan.getTrialDays() > 0) {
            tenant.setIsTrial(true);
        } else {
            tenant.setIsTrial(false);
        }

        log.info("✅ Plan '{}' asignado al tenant '{}'", plan.getName(), tenant.getName());
        return tenantRepository.save(tenant);
    }
}