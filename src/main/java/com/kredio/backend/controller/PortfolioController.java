package com.kredio.backend.controller;

import com.kredio.backend.dto.PortfolioResponse;
import com.kredio.backend.dto.PortfolioRequest;
import com.kredio.backend.dto.PortfolioMetrics;
import com.kredio.backend.dto.ClientResponse;
import com.kredio.backend.dto.AssignClientRequest;
import com.kredio.backend.service.PortfolioService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/portfolios")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping
    public ResponseEntity<List<PortfolioResponse>> getAllPortfolios(HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        List<PortfolioResponse> portfolios = portfolioService.getAllPortfolios(tenantId);
        return ResponseEntity.ok(portfolios);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PortfolioResponse> getPortfolioById(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        PortfolioResponse portfolio = portfolioService.getPortfolioById(id, tenantId);
        return ResponseEntity.ok(portfolio);
    }

    @GetMapping("/metrics")
    public ResponseEntity<PortfolioMetrics> getMetrics(HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        PortfolioMetrics metrics = portfolioService.calculateMetrics(tenantId);
        return ResponseEntity.ok(metrics);
    }

    @GetMapping("/{id}/metrics")
    public ResponseEntity<PortfolioMetrics> getPortfolioMetrics(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        PortfolioMetrics metrics = portfolioService.calculateMetricsByPortfolio(id, tenantId);
        return ResponseEntity.ok(metrics);
    }

    @PostMapping
    public ResponseEntity<PortfolioResponse> createPortfolio(
            @RequestBody PortfolioRequest request,
            HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        PortfolioResponse portfolio = portfolioService.createPortfolio(
                tenantId, request.getName(), request.getDescription(), request.getAssignedUserId()
        );
        return ResponseEntity.ok(portfolio);
    }

    @PutMapping("/{id}")
    public ResponseEntity<PortfolioResponse> updatePortfolio(
            @PathVariable UUID id,
            @RequestBody PortfolioRequest request,
            HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        PortfolioResponse portfolio = portfolioService.updatePortfolio(
                id, tenantId, request.getName(), request.getDescription(), request.getAssignedUserId()
        );
        return ResponseEntity.ok(portfolio);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePortfolio(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        portfolioService.deletePortfolio(id, tenantId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/clients")
    public ResponseEntity<List<ClientResponse>> getClientsByPortfolio(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        List<ClientResponse> clients = portfolioService.getClientsByPortfolio(id, tenantId);
        return ResponseEntity.ok(clients);
    }

    @PostMapping("/assign-client")
    public ResponseEntity<Void> assignClientToPortfolio(
            @RequestBody AssignClientRequest request,
            HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        UUID userId = (UUID) httpRequest.getAttribute("userId");
        portfolioService.assignClientToPortfolio(request.getClientId(), request.getPortfolioId(), tenantId, userId);
        return ResponseEntity.ok().build();
    }
}