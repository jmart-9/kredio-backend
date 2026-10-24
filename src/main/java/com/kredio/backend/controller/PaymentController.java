package com.kredio.backend.controller;

import com.kredio.backend.dto.PaymentRequest;
import com.kredio.backend.dto.PaymentResponse;
import com.kredio.backend.entity.Payment;
import com.kredio.backend.service.PaymentReceiptService;
import com.kredio.backend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentReceiptService paymentReceiptService;

    @PostMapping
    @PreAuthorize("hasAuthority('LOAN:PAYMENT')")
    public ResponseEntity<PaymentResponse> registerPayment(
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        UUID authenticatedUserId = (UUID) httpRequest.getAttribute("userId");
        if (authenticatedUserId == null) {
            throw new RuntimeException("No se pudo identificar al usuario autenticado");
        }
        PaymentResponse response = paymentService.registerPayment(tenantId, request, authenticatedUserId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/{paymentId}/void")
    @PreAuthorize("hasAnyAuthority('PAYMENT:ANNUL', 'ADMIN', 'ADMIN_TENANT')")
    public ResponseEntity<?> voidPayment(
            @PathVariable UUID paymentId,
            HttpServletRequest httpRequest) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        UUID userId = (UUID) httpRequest.getAttribute("userId");

        try {
            // Llamar al nuevo método annulPayment con PIN dummy para compatibilidad
            paymentService.annulPayment(tenantId, paymentId, userId, "Anulación rápida", "0000");
            return ResponseEntity.ok(Map.of("message", "Pago anulado"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/loan/{loanId}")
    @PreAuthorize("hasAuthority('LOAN:READ')")
    public ResponseEntity<List<Payment>> getPaymentsByLoan(
            @PathVariable UUID loanId,
            HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        List<Payment> payments = paymentService.getPaymentsByLoan(tenantId, loanId);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{paymentId}/receipt")
    @PreAuthorize("hasAnyAuthority('PAYMENT:READ', 'LOAN:READ', 'ADMIN')")  // ✅ Relajar la autorización
    public ResponseEntity<byte[]> downloadReceipt(
            @PathVariable UUID paymentId,
            HttpServletRequest httpRequest) {

        log.info("📄 Solicitud de recibo para pago: {}", paymentId);

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");

        if (tenantId == null) {
            log.error("❌ tenantId es null en la request");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            byte[] pdf = paymentReceiptService.generateReceiptPdf(paymentId, tenantId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData(
                    "attachment",
                    "recibo_pago_" + paymentId.toString().substring(0, 8) + ".pdf"
            );
            headers.setContentLength(pdf.length);

            log.info("✅ Recibo enviado: {} bytes", pdf.length);
            return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
        } catch (Exception e) {
            log.error("❌ Error al generar recibo: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}