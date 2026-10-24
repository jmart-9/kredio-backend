package com.kredio.backend.engine;

/**
 * Métodos de amortización soportados por el motor financiero.
 */
public enum AmortizationMethod {
    FRENCH,   // Cuota fija (estándar bancario)
    FLAT,     // Interés fijo sobre capital inicial
    GERMAN,   // Capital fijo, cuota decreciente
    SIMPLE    // Legacy: por compatibilidad con datos antiguos (equivale a FLAT)
}