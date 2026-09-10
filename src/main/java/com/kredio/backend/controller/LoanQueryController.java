package com.kredio.backend.controller;

import com.kredio.backend.dto.LoanResponse;
import com.kredio.backend.entity.Loan;
import com.kredio.backend.entity.LoanSchedule;
import com.kredio.backend.repository.LoanScheduleRepository;
import com.kredio.backend.service.LoanQueryService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/loans")
@RequiredArgsConstructor
public class LoanQueryController {

    private final LoanQueryService loanQueryService;
    private final LoanScheduleRepository scheduleRepository;

    // ✅ AGREGADO: @PreAuthorize para proteger esta ruta
    @GetMapping
    @PreAuthorize("hasAuthority('LOAN:READ')")
    public ResponseEntity<List<LoanResponse>> getAllLoans(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) String search) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        List<LoanResponse> loans;

        if (clientId != null) {
            loans = loanQueryService.findByClient(tenantId, clientId);
        } else if (status != null && !status.isEmpty()) {
            loans = loanQueryService.findByStatus(tenantId, status);
        } else {
            loans = loanQueryService.findAllByTenant(tenantId);
        }

        if (search != null && !search.isEmpty()) {
            loans = loans.stream()
                    .filter(loan -> loan.clientName().toLowerCase().contains(search.toLowerCase()))
                    .collect(Collectors.toList());
        }

        return ResponseEntity.ok(loans);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('LOAN:READ')")
    public ResponseEntity<Loan> getLoanById(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        Loan loan = loanQueryService.findById(id, tenantId);
        return ResponseEntity.ok(loan);
    }

    @GetMapping("/{id}/schedules")
    @PreAuthorize("hasAuthority('LOAN:READ')")
    public ResponseEntity<List<LoanSchedule>> getLoanSchedules(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        loanQueryService.findById(id, tenantId);

        List<LoanSchedule> schedules = scheduleRepository.findByLoanIdOrderByInstallmentNumberAsc(id);
        return ResponseEntity.ok(schedules);
    }
}