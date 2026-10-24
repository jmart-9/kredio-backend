package com.kredio.backend.controller;

import com.kredio.backend.dto.TenantRequest;
import com.kredio.backend.entity.Tenant;
import com.kredio.backend.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/tenants")
@RequiredArgsConstructor
public class TenantController {

    private final TenantService tenantService;

    /**
     * Crear un nuevo tenant con su propio schema
     */
    @PostMapping
    public ResponseEntity<Tenant> createTenant(@Valid @RequestBody TenantRequest request) {
        try {
            Tenant tenant = tenantService.createTenant(
                    request.name(),
                    request.subdomain(),
                    request.adminEmail(),
                    request.adminPassword(),
                    request.adminFullName()
            );
            return ResponseEntity.status(HttpStatus.CREATED).body(tenant);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Listar todos los tenants (solo Admin Global)
     */
    @GetMapping
    public ResponseEntity<List<Tenant>> getAllTenants() {
        return ResponseEntity.ok(tenantService.getAllTenants());
    }

    /**
     * Obtener un tenant por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Tenant> getTenantById(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.getTenantById(id));
    }

    /**
     * Suspender un tenant
     */
    @PutMapping("/{id}/suspend")
    public ResponseEntity<Tenant> suspendTenant(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.suspendTenant(id));
    }

    /**
     * Reactivar un tenant
     */
    @PutMapping("/{id}/activate")
    public ResponseEntity<Tenant> activateTenant(@PathVariable UUID id) {
        return ResponseEntity.ok(tenantService.activateTenant(id));
    }
}