package com.kredio.backend.controller;

import com.kredio.backend.entity.PaymentMethod;
import com.kredio.backend.service.PaymentMethodService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payment-methods")
@RequiredArgsConstructor
public class PaymentMethodController {

    private final PaymentMethodService paymentMethodService;

    @GetMapping
    public ResponseEntity<List<PaymentMethod>> getPaymentMethods(HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        return ResponseEntity.ok(paymentMethodService.getPaymentMethodsByTenant(tenantId));
    }

    @PostMapping
    public ResponseEntity<PaymentMethod> createPaymentMethod(
            @RequestBody PaymentMethod method,
            HttpServletRequest httpRequest) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        return ResponseEntity.ok(paymentMethodService.createPaymentMethod(method, tenantId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PaymentMethod> updatePaymentMethod(
            @PathVariable UUID id,
            @RequestBody PaymentMethod method) {
        return ResponseEntity.ok(paymentMethodService.updatePaymentMethod(id, method));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaymentMethod(@PathVariable UUID id) {
        paymentMethodService.deletePaymentMethod(id);
        return ResponseEntity.ok().build();
    }
}
