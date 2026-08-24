package com.kredio.backend.security;

/**
 * Contexto ThreadLocal que almacena el schema actual del tenant.
 * Se establece en cada petición HTTP y se limpia al finalizar.
 */
public class TenantContext {
    private static final ThreadLocal<String> currentSchema = new ThreadLocal<>();

    public static void setCurrentSchema(String schema) {
        currentSchema.set(schema);
    }

    public static String getCurrentSchema() {
        return currentSchema.get();
    }

    public static void clear() {
        currentSchema.remove();
    }
}