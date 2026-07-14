package com.kredio.backend.service;

import com.kredio.backend.entity.Role;
import com.kredio.backend.entity.RolePermission;
import com.kredio.backend.repository.RolePermissionRepository;
import com.kredio.backend.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    public List<Role> getAllRoles() {
        return roleRepository.findAll();
    }

    public List<Role> getGlobalRoles() {
        return roleRepository.findByIsGlobalTrue();
    }

    public List<Role> getRolesByTenant(UUID tenantId) {
        return roleRepository.findByTenantId(tenantId);
    }

    public Role getRoleById(UUID id) {
        return roleRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
    }

    @Transactional
    public Role createRole(Role role) {
        role.setIsGlobal(false);
        role.setIsSystem(false);
        return roleRepository.save(role);
    }

    @Transactional
    public Role assignPermissions(UUID roleId, List<UUID> permissionIds) {
        // Eliminar permisos anteriores
        List<RolePermission> existingPermissions = rolePermissionRepository.findByRoleId(roleId);
        rolePermissionRepository.deleteAll(existingPermissions);

        // Asignar nuevos permisos
        for (UUID permissionId : permissionIds) {
            RolePermission rolePermission = RolePermission.builder()
                    .roleId(roleId)
                    .permissionId(permissionId)
                    .build();
            rolePermissionRepository.save(rolePermission);
        }

        return getRoleById(roleId);
    }

    public List<RolePermission> getRolePermissions(UUID roleId) {
        return rolePermissionRepository.findByRoleId(roleId);
    }
}
