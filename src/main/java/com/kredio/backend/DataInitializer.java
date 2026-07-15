package com.kredio.backend;

import com.kredio.backend.entity.Tenant; // <-- Ajusta el nombre si tu entidad se llama diferente
import com.kredio.backend.entity.User;
import com.kredio.backend.repository.TenantRepository; // <-- Ajusta el nombre si tu repositorio se llama diferente
import com.kredio.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

@Configuration
@RequiredArgsConstructor
public class DataInitializer {

    private final UserRepository userRepository;
    private final TenantRepository tenantRepository; // Inyectamos el repositorio de Tenant
    private final PasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner initDatabase() {
        return args -> {
            String encodedPassword = passwordEncoder.encode("admin123");

            // 1. BUSCAR O CREAR EL TENANT DE PRUEBA
            Tenant demoTenant = tenantRepository.findByName("Demo Tenant")
                    .orElseGet(() -> {
                        Tenant newTenant = Tenant.builder()
                                .name("Demo Tenant")
                                .subdomain("demo") // <-- ¡AGREGA ESTO! (Es obligatorio)
                                // .currencyCode("USD") // <-- Si tu entidad tiene otros campos obligatorios (ej. @Column(nullable=false)), agrégalos aquí también.
                                // .countryId(someUuid)
                                .build();
                        return tenantRepository.save(newTenant);
                    });

            UUID tenantId = demoTenant.getId();

            // 2. CREAR USUARIO ADMINISTRADOR DEL TENANT
            if (userRepository.findByEmailAndTenantId("admin@tenant.com", tenantId).isEmpty()) {
                User tenantAdmin = User.builder()
                        .tenantId(tenantId)
                        .role("ADMIN")
                        .fullName("Admin del Tenant Demo")
                        .email("admin@tenant.com")
                        .passwordHash(encodedPassword)
                        .isActive(true)
                        .failedLoginAttempts(0)
                        .build();
                userRepository.save(tenantAdmin);
                System.out.println("✅ USUARIO ADMIN TENANT CREADO: admin@tenant.com / admin123");
            }

            // 3. CREAR USUARIO COBRADOR DEL TENANT
            if (userRepository.findByEmailAndTenantId("cobrador@tenant.com", tenantId).isEmpty()) {
                User collectorUser = User.builder()
                        .tenantId(tenantId)
                        .role("COLLECTOR")
                        .fullName("Cobrador de Prueba")
                        .email("cobrador@tenant.com")
                        .passwordHash(encodedPassword)
                        .isActive(true)
                        .failedLoginAttempts(0)
                        .build();
                userRepository.save(collectorUser);
                System.out.println("✅ USUARIO COBRADOR TENANT CREADO: cobrador@tenant.com / admin123");
            }

            System.out.println("=================================================================");
            System.out.println("🚀 ¡BASE DE DATOS INICIALIZADA CON ÉXITO!");
            System.out.println("=================================================================");
        };
    }
}