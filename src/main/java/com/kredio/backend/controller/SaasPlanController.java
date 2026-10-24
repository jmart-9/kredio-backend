package com.kredio.backend.controller;

import com.kredio.backend.entity.SaasPlan;
import com.kredio.backend.service.SaasPlanService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/plans")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('GLOBAL:MANAGE')")
public class SaasPlanController {

    private final SaasPlanService saasPlanService;

    @GetMapping
    public ResponseEntity<List<SaasPlan>> getAllPlans() {
        return ResponseEntity.ok(saasPlanService.getAllPlans());
    }

    @GetMapping("/{id}")
    public ResponseEntity<SaasPlan> getPlanById(@PathVariable UUID id) {
        return ResponseEntity.ok(saasPlanService.getPlanById(id));
    }

    @PostMapping
    public ResponseEntity<SaasPlan> createPlan(@RequestBody SaasPlan plan) {
        return ResponseEntity.ok(saasPlanService.createPlan(plan));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SaasPlan> updatePlan(@PathVariable UUID id, @RequestBody SaasPlan plan) {
        return ResponseEntity.ok(saasPlanService.updatePlan(id, plan));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePlan(@PathVariable UUID id) {
        saasPlanService.deletePlan(id);
        return ResponseEntity.ok().build();
    }
}
