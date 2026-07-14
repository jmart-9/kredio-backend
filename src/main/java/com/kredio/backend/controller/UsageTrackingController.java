package com.kredio.backend.controller;

import com.kredio.backend.entity.UsageTracking;
import com.kredio.backend.service.UsageTrackingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/usage")
@RequiredArgsConstructor
public class UsageTrackingController {

    private final UsageTrackingService usageTrackingService;

    @GetMapping
    public ResponseEntity<List<UsageTracking>> getUsage(HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        return ResponseEntity.ok(usageTrackingService.getUsageByTenant(tenantId));
    }

    @GetMapping("/{metricName}")
    public ResponseEntity<UsageTracking> getUsageByMetric(
            HttpServletRequest httpRequest,
            @PathVariable String metricName) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        UsageTracking usage = usageTrackingService.getUsageByTenantAndMetric(tenantId, metricName);
        return ResponseEntity.ok(usage);
    }

    @PostMapping("/update")
    public ResponseEntity<UsageTracking> updateUsage(
            HttpServletRequest httpRequest,
            @RequestBody Map<String, Object> body) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        String metricName = (String) body.get("metricName");
        BigDecimal currentValue = new BigDecimal(body.get("currentValue").toString());
        BigDecimal limitValue = new BigDecimal(body.get("limitValue").toString());

        return ResponseEntity.ok(usageTrackingService.updateUsage(tenantId, metricName, currentValue, limitValue));
    }

    @GetMapping("/check/{metricName}")
    public ResponseEntity<Boolean> checkLimit(
            HttpServletRequest httpRequest,
            @PathVariable String metricName) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        return ResponseEntity.ok(usageTrackingService.checkLimit(tenantId, metricName));
    }
}