package com.kredio.backend.controller;

import com.kredio.backend.dto.GlobalDashboardDTO;
import com.kredio.backend.service.GlobalDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/global/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('GLOBAL:MANAGE')")
public class GlobalDashboardController {

    private final GlobalDashboardService globalDashboardService;

    @GetMapping
    public ResponseEntity<GlobalDashboardDTO> getDashboard() {
        return ResponseEntity.ok(globalDashboardService.getGlobalDashboard());
    }
}