package com.kredio.backend.controller;

import com.kredio.backend.entity.ImpersonationLog;
import com.kredio.backend.service.ImpersonationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/impersonation")
@RequiredArgsConstructor
public class ImpersonationController {

    private final ImpersonationService impersonationService;

    @PostMapping("/start")
    public ResponseEntity<Map<String, String>> startImpersonation(
            @RequestBody Map<String, UUID> body,
            HttpServletRequest httpRequest) {

        UUID adminGlobalId = (UUID) httpRequest.getAttribute("userId");
        UUID targetUserId = body.get("targetUserId");
        String ipAddress = httpRequest.getRemoteAddr();

        String token = impersonationService.startImpersonation(adminGlobalId, targetUserId, ipAddress);

        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/end/{logId}")
    public ResponseEntity<Void> endImpersonation(@PathVariable UUID logId) {
        impersonationService.endImpersonation(logId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/logs")
    public ResponseEntity<List<ImpersonationLog>> getAllLogs() {
        return ResponseEntity.ok(impersonationService.getImpersonationLogs());
    }

    @GetMapping("/logs/admin")
    public ResponseEntity<List<ImpersonationLog>> getLogsByAdmin(HttpServletRequest httpRequest) {
        UUID adminGlobalId = (UUID) httpRequest.getAttribute("userId");
        return ResponseEntity.ok(impersonationService.getLogsByAdmin(adminGlobalId));
    }
}