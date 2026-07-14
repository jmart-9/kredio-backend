package com.kredio.backend.controller;

import com.kredio.backend.entity.ClientReference;
import com.kredio.backend.service.ClientReferenceService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/client-references")
@RequiredArgsConstructor
public class ClientReferenceController {

    private final ClientReferenceService clientReferenceService;

    @GetMapping("/client/{clientId}")
    public ResponseEntity<List<ClientReference>> getReferencesByClient(@PathVariable UUID clientId) {
        return ResponseEntity.ok(clientReferenceService.getReferencesByClient(clientId));
    }

    @PostMapping
    public ResponseEntity<ClientReference> createReference(
            @RequestBody ClientReference reference,
            HttpServletRequest httpRequest) {

        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        return ResponseEntity.ok(clientReferenceService.createReference(reference, tenantId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientReference> updateReference(
            @PathVariable UUID id,
            @RequestBody ClientReference reference) {
        return ResponseEntity.ok(clientReferenceService.updateReference(id, reference));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteReference(@PathVariable UUID id) {
        clientReferenceService.deleteReference(id);
        return ResponseEntity.ok().build();
    }
}