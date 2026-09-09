package com.kredio.backend.repository;

import com.kredio.backend.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RoleRepository extends JpaRepository<Role, UUID> {
    List<Role> findByIsGlobalTrue();
    List<Role> findByTenantId(UUID tenantId);

    Optional<Role> findFirstByNameAndIsGlobalTrue(String name);
}