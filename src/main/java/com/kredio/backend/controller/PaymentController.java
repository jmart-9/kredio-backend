package com.kredio.backend.controller;

import com.kredio.backend.dto.PaymentRequest;
import com.kredio.backend.dto.PaymentResponse;
import com.kredio.backend.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> registerPayment(
            @Valid @RequestBody PaymentRequest request,
            HttpServletRequest httpRequest) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        PaymentResponse response = paymentService.registerPayment(request, tenantId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }
}