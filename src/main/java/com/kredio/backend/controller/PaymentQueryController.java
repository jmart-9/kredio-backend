package com.kredio.backend.controller;

import com.kredio.backend.entity.Payment;
import com.kredio.backend.repository.PaymentRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentQueryController {

    private final PaymentRepository paymentRepository;

    // ✅ CORREGIDO: Cambiado a "/history" para evitar el conflicto de mapeo ambiguo
    // con el @GetMapping base de PaymentController
    @GetMapping("/history")
    public ResponseEntity<List<Payment>> getAllPayments(
            HttpServletRequest httpRequest,
            @RequestParam(required = false) UUID loanId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        List<Payment> payments;

        if (loanId != null) {
            payments = paymentRepository.findByTenantIdAndLoanId(tenantId, loanId);
        } else if (startDate != null && endDate != null) {
            payments = paymentRepository.findByTenantIdAndDateRange(tenantId, startDate, endDate);
        } else {
            payments = paymentRepository.findByTenantId(tenantId);
        }

        return ResponseEntity.ok(payments);
    }
}