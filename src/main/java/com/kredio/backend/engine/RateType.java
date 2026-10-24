package com.kredio.backend.engine;

public enum RateType {
    ANNUAL,    // ✅ Legacy: Para compatibilidad con datos antiguos
    MONTHLY,   // ✅ Legacy: Para compatibilidad con datos antiguos
    NOMINAL,   // Nuevo: Tasa nominal
    EFFECTIVE  // Nuevo: Tasa efectiva
}