package com.kredio.backend.controller;

import com.kredio.backend.service.OverdueService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/overdue")
@RequiredArgsConstructor
public class OverdueController {

    private final OverdueService overdueService;

    /**
     * Endpoint manual para actualizar el estado de morosidad
     * POST /api/v1/overdue/update
     */
    @PostMapping("/update")
    public ResponseEntity<Map<String, Object>> updateOverdue(HttpServletRequest httpRequest) {
        UUID tenantId = (UUID) httpRequest.getAttribute("tenantId");
        int updatedCount = overdueService.updateOverdueStatus(tenantId);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Actualización completada",
                "updatedSchedules", updatedCount
        ));
    }
}