package com.kredio.backend.security;

import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.stereotype.Component;

/**
 * Le dice a Hibernate qué schema usar en cada petición.
 * Lee el schema del TenantContext (ThreadLocal).
 */
@Component
public class TenantIdentifierResolver implements CurrentTenantIdentifierResolver {

    @Override
    public String resolveCurrentTenantIdentifier() {
        String schema = TenantContext.getCurrentSchema();
        // Si no hay contexto, usa 'public' por defecto (para migraciones o admins globales)
        return schema != null ? schema : "public";
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }
}