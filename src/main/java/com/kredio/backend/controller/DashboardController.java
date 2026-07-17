package com.kredio.backend.controller;

import com.kredio.backend.dto.DashboardStatsDTO;
import com.kredio.backend.service.DashboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('REPORT:VIEW')")
    public ResponseEntity<DashboardStatsDTO> getDashboardStats(HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        DashboardStatsDTO stats = dashboardService.getDashboardStats(tenantId);
        return ResponseEntity.ok(stats);
    }
}