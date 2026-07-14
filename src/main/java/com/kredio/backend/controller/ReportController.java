package com.kredio.backend.controller;

import com.kredio.backend.dto.reports.CashflowProjectionResponse;
import com.kredio.backend.dto.reports.CollectionPerformanceResponse;
import com.kredio.backend.dto.reports.PortfolioAtRiskResponse;
import com.kredio.backend.service.ReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Cartera en riesgo (PAR 30, 60, 90)
     * El indicador más importante para microfinanzas
     */
    @GetMapping("/portfolio-at-risk")
    public ResponseEntity<PortfolioAtRiskResponse> getPortfolioAtRisk(HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        return ResponseEntity.ok(reportService.getPortfolioAtRisk(tenantId));
    }

    /**
     * Desempeño de cobranza por cobrador
     * @param days Rango de días (default: 30)
     */
    @GetMapping("/collection-performance")
    public ResponseEntity<List<CollectionPerformanceResponse>> getCollectionPerformance(
            HttpServletRequest httpRequest,
            @RequestParam(defaultValue = "30") int days) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        return ResponseEntity.ok(reportService.getCollectionPerformance(tenantId, days));
    }

    /**
     * Proyección de flujo de caja (30, 60, 90 días)
     */
    @GetMapping("/cashflow-projection")
    public ResponseEntity<CashflowProjectionResponse> getCashflowProjection(HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        return ResponseEntity.ok(reportService.getCashflowProjection(tenantId));
    }
}
