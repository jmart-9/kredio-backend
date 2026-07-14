package com.kredio.backend.controller;

import com.kredio.backend.entity.Role;
import com.kredio.backend.entity.RolePermission;
import com.kredio.backend.service.RoleService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public ResponseEntity<List<Role>> getAllRoles(HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        // Devolver roles globales + roles del tenant
        List<Role> globalRoles = roleService.getGlobalRoles();
        List<Role> tenantRoles = roleService.getRolesByTenant(tenantId);
        globalRoles.addAll(tenantRoles);
        return ResponseEntity.ok(globalRoles);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Role> getRoleById(@PathVariable UUID id) {
        return ResponseEntity.ok(roleService.getRoleById(id));
    }

    @PostMapping
    public ResponseEntity<Role> createRole(@RequestBody Role role, HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        role.setTenantId(tenantId);
        return ResponseEntity.ok(roleService.createRole(role));
    }

    @PostMapping("/{id}/permissions")
    public ResponseEntity<Role> assignPermissions(
            @PathVariable UUID id,
            @RequestBody Map<String, List<UUID>> body) {
        return ResponseEntity.ok(roleService.assignPermissions(id, body.get("permissionIds")));
    }

    @GetMapping("/{id}/permissions")
    public ResponseEntity<List<RolePermission>> getRolePermissions(@PathVariable UUID id) {
        return ResponseEntity.ok(roleService.getRolePermissions(id));
    }
}
