package com.kredio.backend.engine;

import java.util.List;

/**
 * Interfaz principal del motor de amortización.
 * Cada implementación calcula el calendario según su método.
 */
public interface AmortizationEngine {

    /**
     * Genera el calendario de amortización completo
     * @param request Parámetros del préstamo
     * @return Lista de cuotas con todos los detalles
     */
    List<AmortizationScheduleDTO> generateSchedule(AmortizationRequest request);

    /**
     * Retorna el método que implementa este motor
     */
    AmortizationMethod getMethod();
}