package com.kredio.backend.controller;

import com.kredio.backend.dto.PaymentRequest;
import com.kredio.backend.dto.PaymentResponse;
import com.kredio.backend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    /**
     * Registrar un nuevo pago
     * POST /api/v1/payments
     */
    @PostMapping
    @PreAuthorize("hasAuthority('LOAN:PAYMENT')")
    public ResponseEntity<PaymentResponse> registerPayment(
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        PaymentResponse response = paymentService.registerPayment(tenantId, request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Anular un pago (void)
     * PUT /api/v1/payments/{id}/void
     */
    @PutMapping("/{id}/void")
    @PreAuthorize("hasAuthority('LOAN:PAYMENT')")
    public ResponseEntity<Void> voidPayment(
            @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        paymentService.voidPayment(tenantId, id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Obtener historial de pagos de un préstamo
     * GET /api/v1/payments?loanId=xxx
     */
    @GetMapping
    @PreAuthorize("hasAuthority('LOAN:READ')")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByLoan(
            @RequestParam UUID loanId,
            HttpServletRequest httpRequest) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        List<PaymentResponse> payments = paymentService.getPaymentsByLoan(tenantId, loanId);

        return ResponseEntity.ok(payments);
    }
}