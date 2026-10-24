package com.kredio.backend.controller;

import com.kredio.backend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
@Slf4j
public class AnnulmentController {

    private final PaymentService paymentService;

    @PostMapping("/{paymentId}/annul")
    @PreAuthorize("hasAnyAuthority('PAYMENT:ANNUL', 'ADMIN', 'ADMIN_TENANT', 'SUPERVISOR')")
    public ResponseEntity<?> annulPayment(
            @PathVariable UUID paymentId,
            @RequestBody AnnulmentRequest request,
            HttpServletRequest httpRequest) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        UUID annulledByUserId = (UUID) httpRequest.getAttribute("userId");

        log.info("🔄 Solicitud de anulación: paymentId={}, userId={}", paymentId, annulledByUserId);

        try {
            paymentService.annulPayment(
                    tenantId,
                    paymentId,
                    annulledByUserId,
                    request.reason(),
                    request.supervisorPin()
            );

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Pago anulado correctamente",
                    "paymentId", paymentId.toString()
            ));
        } catch (Exception e) {
            log.error("❌ Error al anular pago: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    public record AnnulmentRequest(
            @NotBlank(message = "El motivo es obligatorio")
            String reason,
            @NotBlank(message = "El PIN es obligatorio")
            String supervisorPin
    ) {}
}