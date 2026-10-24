package com.kredio.backend.engine.amortization;

import com.kredio.backend.engine.AmortizationEngine;
import com.kredio.backend.engine.AmortizationMethod;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Factory pattern para seleccionar el motor de amortización correcto.
 * Spring inyecta automáticamente todos los AmortizationEngine registrados.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AmortizationFactory {

    private final Map<AmortizationMethod, AmortizationEngine> engines;

    /**
     * Obtiene el motor de amortización según el método.
     */
    public AmortizationEngine getEngine(AmortizationMethod method) {
        // Manejar legacy: SIMPLE -> FLAT
        if (method == AmortizationMethod.SIMPLE) {
            method = AmortizationMethod.FLAT;
        }

        AmortizationEngine engine = engines.get(method);
        if (engine == null) {
            throw new IllegalArgumentException("Método de amortización no soportado: " + method);
        }
        log.debug("🔧 Motor de amortización seleccionado: {}", method);
        return engine;
    }

    /**
     * Obtiene el motor desde un String (para compatibilidad con datos legacy).
     */
    public AmortizationEngine getEngine(String methodString) {
        if (methodString == null || methodString.isBlank()) {
            return getEngine(AmortizationMethod.FRENCH);
        }

        AmortizationMethod method;
        try {
            method = AmortizationMethod.valueOf(methodString.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("️ Método de amortización desconocido: {}, usando FRENCH por defecto", methodString);
            method = AmortizationMethod.FRENCH;
        }

        return getEngine(method);
    }

    /**
     * Obtiene todos los motores disponibles.
     */
    public Map<AmortizationMethod, AmortizationEngine> getAllEngines() {
        return engines;
    }
}