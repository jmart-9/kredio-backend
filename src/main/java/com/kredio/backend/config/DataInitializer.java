package com.kredio.backend.config;

import com.kredio.backend.entity.Tenant;
import com.kredio.backend.entity.User;
import com.kredio.backend.repository.TenantRepository;
import com.kredio.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.sql.DataSource;
import java.util.UUID;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            log.info("🚀 Iniciando inicialización de base de datos...");

            // 1. CREAR TENANT DEMO
            Tenant demoTenant = tenantRepository.findByName("Demo Tenant")
                    .orElseGet(() -> {
                        log.info("📝 Creando tenant 'Demo Tenant'...");
                        String schemaName = "tenant_demo";

                        jdbcTemplate.execute("CREATE SCHEMA IF NOT EXISTS \"" + schemaName + "\"");
                        log.info("✅ Schema '{}' creado en PostgreSQL", schemaName);

                        Flyway flyway = Flyway.configure()
                                .dataSource(dataSource)
                                .schemas(schemaName)
                                .locations("classpath:db/migration")
                                .baselineOnMigrate(true)
                                .load();
                        flyway.migrate();
                        log.info("✅ Migraciones de Flyway ejecutadas en schema '{}'", schemaName);

                        Tenant newTenant = Tenant.builder()
                                .name("Demo Tenant")
                                .subdomain("demo")
                                .schemaName(schemaName)
                                .status(Tenant.TenantStatus.ACTIVE)
                                .isTrial(true)
                                .build();

                        Tenant savedTenant = tenantRepository.save(newTenant);

                        // Insertar datos semilla con el ID real del tenant
                        jdbcTemplate.execute("SET search_path TO \"" + schemaName + "\"");
                        jdbcTemplate.execute("INSERT INTO payment_methods (id, tenant_id, name, code, is_active) VALUES " +
                                "(gen_random_uuid(), '" + savedTenant.getId() + "', 'Efectivo', 'CASH', true), " +
                                "(gen_random_uuid(), '" + savedTenant.getId() + "', 'Transferencia', 'TRANSFER', true)");
                        jdbcTemplate.execute("SET search_path TO public");
                        log.info("✅ Datos semilla insertados en schema '{}'", schemaName);

                        return savedTenant;
                    });

            UUID tenantId = demoTenant.getId();
            log.info("✅ Tenant registrado: {} (ID: {}, Schema: {})", demoTenant.getName(), tenantId, demoTenant.getSchemaName());

            // 2. CREAR USUARIO ADMIN GLOBAL
            // ⚠️ IMPORTANTE: Se asigna al 'tenantId' del Demo Tenant porque la BD exige que NO sea null.
            // Sus permisos globales se otorgan mediante el ROL "ADMIN_GLOBAL", no mediante un tenant_id nulo.
            if (userRepository.findByEmailAndTenantId("admin@kredio.com", tenantId).isEmpty()) {
                log.info("📝 Creando usuario ADMIN GLOBAL...");
                User globalAdmin = User.builder()
                        .tenantId(tenantId)
                        .role("ADMIN_GLOBAL")
                        .fullName("Administrador Global SaaS")
                        .email("admin@kredio.com")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .isActive(true)
                        .failedLoginAttempts(0)
                        .build();
                userRepository.save(globalAdmin);
                log.info("✅ USUARIO ADMIN GLOBAL CREADO: admin@kredio.com / admin123");
            }

            // 3. CREAR USUARIO ADMIN DEL TENANT
            if (userRepository.findByEmailAndTenantId("admin@demo.com", tenantId).isEmpty()) {
                log.info("📝 Creando usuario admin@demo.com...");
                User adminUser = User.builder()
                        .tenantId(tenantId)
                        .role("ADMIN_TENANT")
                        .fullName("Admin del Tenant Demo")
                        .email("admin@demo.com")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .isActive(true)
                        .failedLoginAttempts(0)
                        .build();
                userRepository.save(adminUser);
                log.info("✅ USUARIO ADMIN TENANT CREADO: admin@demo.com / admin123");
            }

            // 4. CREAR USUARIO COBRADOR
            if (userRepository.findByEmailAndTenantId("cobrador@demo.com", tenantId).isEmpty()) {
                log.info("📝 Creando usuario cobrador@demo.com...");
                User collectorUser = User.builder()
                        .tenantId(tenantId)
                        .role("COBRADOR")
                        .fullName("Cobrador de Prueba")
                        .email("cobrador@demo.com")
                        .passwordHash(passwordEncoder.encode("admin123"))
                        .isActive(true)
                        .failedLoginAttempts(0)
                        .build();
                userRepository.save(collectorUser);
                log.info("✅ USUARIO COBRADOR CREADO: cobrador@demo.com / admin123");
            }

            log.info("=================================================================");
            log.info("🎉 ¡BASE DE DATOS INICIALIZADA CON ÉXITO!");
            log.info("=================================================================");
            log.info("Tenant: {}", demoTenant.getName());
            log.info("Schema: {}", demoTenant.getSchemaName());
            log.info("Usuarios creados:");
            log.info("  - admin@kredio.com / admin123 (ADMIN GLOBAL)");
            log.info("  - admin@demo.com / admin123 (ADMIN TENANT)");
            log.info("  - cobrador@demo.com / admin123 (COBRADOR)");
            log.info("=================================================================");
        };
    }
}