package com.kredio.backend.dto.reports;

import java.math.BigDecimal;
import java.util.UUID;

public record CollectionPerformanceResponse(
        UUID collectorId,
        String collectorName,
        int totalVisits,                  // Total de pagos registrados
        int successfulCollections,        // Pagos que cubrieron cuota completa
        BigDecimal totalCollected,        // Monto total cobrado
        BigDecimal averageTicket,         // Ticket promedio
        int assignedLoans,                // Préstamos asignados (futuro)
        BigDecimal collectionEfficiency   // Eficiencia de cobro (%)
) {
}