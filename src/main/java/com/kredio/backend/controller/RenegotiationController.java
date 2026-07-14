package com.kredio.backend.controller;

import com.kredio.backend.entity.RenegotiationRequest;
import com.kredio.backend.service.RenegotiationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/renegotiations")
@RequiredArgsConstructor
public class RenegotiationController {

    private final RenegotiationService renegotiationService;

    @GetMapping("/loan/{loanId}")
    public ResponseEntity<List<RenegotiationRequest>> getRequestsByLoan(@PathVariable UUID loanId) {
        return ResponseEntity.ok(renegotiationService.getRequestsByLoan(loanId));
    }

    @GetMapping("/pending")
    public ResponseEntity<List<RenegotiationRequest>> getPendingRequests(HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        return ResponseEntity.ok(renegotiationService.getRequestsByTenantAndStatus(
                tenantId,
                RenegotiationRequest.RenegotiationStatus.PENDING
        ));
    }

    @PostMapping
    public ResponseEntity<RenegotiationRequest> createRequest(
            @RequestBody RenegotiationRequest request,
            HttpServletRequest httpRequest) {

        UUID collectorId = (UUID) httpRequest.getAttribute("userId");
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");

        request.setCollectorId(collectorId);
        request.setTenantId(tenantId);

        return ResponseEntity.ok(renegotiationService.createRequest(request));
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<RenegotiationRequest> approveRequest(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {

        UUID adminId = (UUID) httpRequest.getAttribute("userId");
        String reviewNotes = body.getOrDefault("reviewNotes", "");

        return ResponseEntity.ok(renegotiationService.approveRequest(id, adminId, reviewNotes));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<RenegotiationRequest> rejectRequest(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            HttpServletRequest httpRequest) {

        UUID adminId = (UUID) httpRequest.getAttribute("userId");
        String reviewNotes = body.getOrDefault("reviewNotes", "");

        return ResponseEntity.ok(renegotiationService.rejectRequest(id, adminId, reviewNotes));
    }
}
