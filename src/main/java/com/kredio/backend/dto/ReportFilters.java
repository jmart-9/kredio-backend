package com.kredio.backend.dto;

import java.time.Instant;
import java.util.UUID;

public record ReportFilters(
        Instant startDate,
        Instant endDate,
        UUID clientId,
        UUID portfolioId,
        String status
) {
}