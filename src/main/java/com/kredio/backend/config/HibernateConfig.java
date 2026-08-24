package com.kredio.backend.config;

import com.kredio.backend.security.TenantConnectionProvider;
import com.kredio.backend.security.TenantIdentifierResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configura Hibernate para usar multi-tenancy por SCHEMA.
 * Registra el TenantConnectionProvider y TenantIdentifierResolver.
 */
@Configuration
@RequiredArgsConstructor
public class HibernateConfig {

    private final TenantConnectionProvider tenantConnectionProvider;
    private final TenantIdentifierResolver tenantIdentifierResolver;

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return properties -> {
            // Habilitar multi-tenancy por SCHEMA
            properties.put("hibernate.multiTenancy", "SCHEMA");
            // Registrar el proveedor de conexiones gestionado por Spring
            properties.put("hibernate.multi_tenant_connection_provider", tenantConnectionProvider);
            // Registrar el resolvedor de identificador gestionado por Spring
            properties.put("hibernate.tenant_identifier_resolver", tenantIdentifierResolver);
        };
    }
}


