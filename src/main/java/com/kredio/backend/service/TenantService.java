package com.kredio.backend.service;

import com.kredio.backend.entity.Role;
import com.kredio.backend.entity.Tenant;
import com.kredio.backend.entity.User;
import com.kredio.backend.repository.RoleRepository;
import com.kredio.backend.repository.TenantRepository;
import com.kredio.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final PasswordEncoder passwordEncoder;

    public Tenant createTenant(String name, String subdomain, String adminEmail, String adminPassword, String adminFullName) {
        if (tenantRepository.findBySubdomain(subdomain).isPresent()) {
            throw new RuntimeException("El subdominio ya está en uso");
        }

        String schemaName = "tenant_" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        log.info("🔧 Iniciando creación de tenant: {} (schema: {})", name, schemaName);

        // 1. Crear schema (se commitea automáticamente al ser DDL fuera de transacción de Spring)
        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\"");
        log.info("✅ Schema creado: {}", schemaName);

        // 2. Ejecutar migraciones de Flyway (Flyway maneja su propia transacción)
        Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .schemas(schemaName)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .load();

        flyway.migrate();
        log.info("✅ Migraciones de Flyway ejecutadas en schema: {}", schemaName);

        // 3. Guardar el Tenant en 'public' (Spring Data JPA abre su propia transacción corta aquí)
        Tenant tenant = Tenant.builder()
                .name(name)
                .subdomain(subdomain)
                .schemaName(schemaName)
                .status(Tenant.TenantStatus.ACTIVE)
                .isTrial(true)
                .build();

        tenant = tenantRepository.save(tenant);
        log.info("✅ Tenant registrado en BD pública: {} (ID: {})", tenant.getName(), tenant.getId());

        // 4. Insertar datos semilla usando el ID real del tenant
        insertSeedData(schemaName, tenant.getId());

        // 5. Crear usuario admin
        createAdminUser(tenant.getId(), adminEmail, adminPassword, adminFullName);

        return tenant;
    }

    private void insertSeedData(String schemaName, UUID tenantId) {
        try {
            String sql1 = String.format("INSERT INTO %s.payment_methods (id, tenant_id, name, code, is_active) VALUES (gen_random_uuid(), '%s', 'Efectivo', 'CASH', true)", schemaName, tenantId);
            String sql2 = String.format("INSERT INTO %s.payment_methods (id, tenant_id, name, code, is_active) VALUES (gen_random_uuid(), '%s', 'Transferencia', 'TRANSFER', true)", schemaName, tenantId);

            jdbcTemplate.execute(sql1);
            jdbcTemplate.execute(sql2);
            log.info("✅ Datos semilla insertados en schema: {}", schemaName);
        } catch (Exception e) {
            log.error("⚠️ Error al insertar datos semilla: {}", e.getMessage());
        }
    }

    // ✅ MÉTODO CORREGIDO: Ahora asigna el roleId correctamente
    private void createAdminUser(UUID tenantId, String email, String password, String fullName) {
        try {
            // ✅ CAMBIADO: Usar findFirstByNameAndIsGlobalTrue para evitar duplicados
            Role adminTenantRole = roleRepository.findFirstByNameAndIsGlobalTrue("ADMIN_TENANT")
                    .orElseThrow(() -> new RuntimeException("Rol ADMIN_TENANT global no encontrado"));

            User adminUser = User.builder()
                    .tenantId(tenantId)
                    .role("ADMIN_TENANT")
                    .roleId(adminTenantRole.getId())
                    .fullName(fullName)
                    .email(email)
                    .passwordHash(passwordEncoder.encode(password))
                    .isActive(true)
                    .failedLoginAttempts(0)
                    .build();

            userRepository.save(adminUser);
            log.info("✅ Usuario admin creado con rol ID {}: {}", adminTenantRole.getId(), email);
        } catch (Exception e) {
            log.error("⚠️ Error al crear usuario admin: {}", e.getMessage());
            throw new RuntimeException("Error al crear usuario admin: " + e.getMessage());
        }
    }

    public List<Tenant> getAllTenants() {
        return tenantRepository.findAll();
    }

    public Tenant getTenantById(UUID id) {
        return tenantRepository.findById(id).orElseThrow(() -> new RuntimeException("Tenant no encontrado"));
    }

    @Transactional
    public Tenant suspendTenant(UUID id) {
        Tenant tenant = getTenantById(id);
        tenant.setStatus(Tenant.TenantStatus.SUSPENDED);
        return tenantRepository.save(tenant);
    }

    @Transactional
    public Tenant activateTenant(UUID id) {
        Tenant tenant = getTenantById(id);
        tenant.setStatus(Tenant.TenantStatus.ACTIVE);
        return tenantRepository.save(tenant);
    }
}