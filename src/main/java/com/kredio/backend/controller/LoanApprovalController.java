package com.kredio.backend.controller;

import com.kredio.backend.entity.Loan;
import com.kredio.backend.service.LoanApprovalService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanApprovalController {

    private final LoanApprovalService loanApprovalService;

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('LOAN:APPROVE')")
    public ResponseEntity<Loan> approveLoan(@PathVariable UUID id, HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        UUID adminId = (UUID) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(loanApprovalService.approveLoan(id, adminId, tenantId));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('LOAN:APPROVE')")
    public ResponseEntity<Loan> rejectLoan(@PathVariable UUID id, @RequestBody Map<String, String> body, HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        UUID adminId = (UUID) httpRequest.getAttribute("userId");
        String reason = body.getOrDefault("reason", "Sin razón especificada");
        return ResponseEntity.ok(loanApprovalService.rejectLoan(id, adminId, tenantId, reason));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAuthority('LOAN:READ')")
    public ResponseEntity<List<Loan>> getPendingLoans(HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        return ResponseEntity.ok(loanApprovalService.getPendingLoans(tenantId));
    }
}