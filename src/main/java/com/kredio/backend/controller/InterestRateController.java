package com.kredio.backend.controller;

import com.kredio.backend.entity.InterestRate;
import com.kredio.backend.service.InterestRateService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/interest-rates")
@RequiredArgsConstructor
public class InterestRateController {

    private final InterestRateService interestRateService;

    @GetMapping("/global")
    public ResponseEntity<List<InterestRate>> getGlobalRates() {
        return ResponseEntity.ok(interestRateService.getGlobalRates());
    }

    @GetMapping("/tenant")
    public ResponseEntity<List<InterestRate>> getTenantRates(HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        return ResponseEntity.ok(interestRateService.getTenantRates(tenantId));
    }

    @GetMapping("/all")
    public ResponseEntity<List<InterestRate>> getAllActiveRates(HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        return ResponseEntity.ok(interestRateService.getAllActiveRates(tenantId));
    }

    @PostMapping
    public ResponseEntity<InterestRate> createRate(
            @RequestBody InterestRate rate,
            @RequestBody Map<String, Boolean> body,
            HttpServletRequest httpRequest) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        Boolean isGlobal = body.getOrDefault("isGlobal", false);

        // Solo admin global puede crear tasas globales
        if (isGlobal) {
            // Aquí deberías verificar que el usuario sea ADMIN_GLOBAL
        }

        return ResponseEntity.ok(interestRateService.createRate(rate, tenantId, isGlobal));
    }

    @PutMapping("/{id}")
    public ResponseEntity<InterestRate> updateRate(@PathVariable UUID id, @RequestBody InterestRate rate) {
        return ResponseEntity.ok(interestRateService.updateRate(id, rate));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRate(@PathVariable UUID id) {
        interestRateService.deleteRate(id);
        return ResponseEntity.ok().build();
    }
}
