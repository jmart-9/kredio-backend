package com.kredio.backend.controller;

import com.kredio.backend.entity.Tenant;
import com.kredio.backend.service.GlobalAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/global")
@RequiredArgsConstructor
@Slf4j // ✅ Esto habilita la variable 'log'
public class GlobalAdminController {

    private final GlobalAdminService globalAdminService;

    @GetMapping("/tenants")
    @PreAuthorize("hasAuthority('GLOBAL:MANAGE')")
    public ResponseEntity<?> getAllTenants() {
        return ResponseEntity.ok(globalAdminService.getAllTenants());
    }

    @PostMapping("/tenants")
    @PreAuthorize("hasAuthority('GLOBAL:MANAGE')")
    public ResponseEntity<?> createTenant(@RequestBody Map<String, String> request) {
        try {
            Tenant tenant = globalAdminService.createTenant(
                    request.get("name"),
                    request.get("subdomain"),
                    request.get("adminEmail"),
                    request.get("adminPassword"),
                    request.get("adminFullName")
            );
            return ResponseEntity.ok(tenant);
        } catch (Exception e) {
            log.error("Error creando tenant", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/tenants/{id}/status")
    @PreAuthorize("hasAuthority('GLOBAL:MANAGE')")
    public ResponseEntity<?> updateTenantStatus(@PathVariable UUID id, @RequestParam String status) {
        try {
            Tenant tenant = globalAdminService.updateTenantStatus(id, status);
            return ResponseEntity.ok(tenant);
        } catch (Exception e) {
            log.error("Error actualizando estado del tenant", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/tenants/{id}")
    @PreAuthorize("hasAuthority('GLOBAL:MANAGE')")
    public ResponseEntity<?> getTenantById(@PathVariable UUID id) {
        try {
            Tenant tenant = globalAdminService.getTenantById(id);
            return ResponseEntity.ok(tenant);
        } catch (Exception e) {
            log.error("Error obteniendo tenant", e);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/tenants/{id}")
    @PreAuthorize("hasAuthority('GLOBAL:MANAGE')")
    public ResponseEntity<?> deleteTenant(@PathVariable UUID id) {
        try {
            globalAdminService.deleteTenant(id);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            log.error("Error eliminando tenant", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PutMapping("/tenants/{id}/plan")
    @PreAuthorize("hasAuthority('GLOBAL:MANAGE')")
    public ResponseEntity<?> assignPlan(
            @PathVariable UUID id,
            @RequestParam UUID planId,
            @RequestParam LocalDate planEndDate) {
        try {
            Tenant tenant = globalAdminService.assignPlan(id, planId, planEndDate);
            return ResponseEntity.ok(tenant);
        } catch (Exception e) {
            log.error("Error asignando plan", e);
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}