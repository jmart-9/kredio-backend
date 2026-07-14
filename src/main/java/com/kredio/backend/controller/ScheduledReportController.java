package com.kredio.backend.controller;

import com.kredio.backend.entity.ScheduledReport;
import com.kredio.backend.service.ScheduledReportService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scheduled-reports")
@RequiredArgsConstructor
public class ScheduledReportController {

    private final ScheduledReportService scheduledReportService;

    @GetMapping
    public ResponseEntity<List<ScheduledReport>> getActiveReports(HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        return ResponseEntity.ok(scheduledReportService.getActiveReportsByTenant(tenantId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ScheduledReport> getReportById(@PathVariable UUID id) {
        return ResponseEntity.ok(scheduledReportService.getReportById(id));
    }

    @PostMapping
    public ResponseEntity<ScheduledReport> createReport(
            @RequestBody ScheduledReport report,
            HttpServletRequest httpRequest) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        UUID createdBy = (UUID) httpRequest.getAttribute("userId");

        return ResponseEntity.ok(scheduledReportService.createReport(report, tenantId, createdBy));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ScheduledReport> updateReport(
            @PathVariable UUID id,
            @RequestBody ScheduledReport report) {
        return ResponseEntity.ok(scheduledReportService.updateReport(id, report));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReport(@PathVariable UUID id) {
        scheduledReportService.deleteReport(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/mark-sent")
    public ResponseEntity<Void> markAsSent(@PathVariable UUID id) {
        scheduledReportService.markAsSent(id);
        return ResponseEntity.ok().build();
    }
}