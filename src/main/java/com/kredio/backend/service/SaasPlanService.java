package com.kredio.backend.service;

import com.kredio.backend.entity.SaasPlan;
import com.kredio.backend.repository.SaasPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SaasPlanService {

    private final SaasPlanRepository saasPlanRepository;

    public List<SaasPlan> getAllPlans() {
        return saasPlanRepository.findAll();
    }

    public SaasPlan getPlanById(UUID id) {
        return saasPlanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Plan no encontrado"));
    }

    @Transactional
    public SaasPlan createPlan(SaasPlan plan) {
        plan.setIsActive(true);
        return saasPlanRepository.save(plan);
    }

    @Transactional
    public SaasPlan updatePlan(UUID id, SaasPlan planDetails) {
        SaasPlan plan = getPlanById(id);

        plan.setName(planDetails.getName());
        plan.setDescription(planDetails.getDescription());
        plan.setMonthlyPrice(planDetails.getMonthlyPrice());
        plan.setTrialDays(planDetails.getTrialDays());
        plan.setMaxUsers(planDetails.getMaxUsers());
        plan.setMaxActiveLoans(planDetails.getMaxActiveLoans());
        plan.setMaxPortfolioVolume(planDetails.getMaxPortfolioVolume());
        plan.setMaxCollectors(planDetails.getMaxCollectors());
        plan.setMaxClients(planDetails.getMaxClients());
        plan.setMaxCustomRoles(planDetails.getMaxCustomRoles());
        plan.setMaxCustomInterestRates(planDetails.getMaxCustomInterestRates());
        plan.setFeatures(planDetails.getFeatures());
        plan.setSupportLevel(planDetails.getSupportLevel());
        plan.setIsActive(planDetails.getIsActive());

        return saasPlanRepository.save(plan);
    }

    @Transactional
    public void deletePlan(UUID id) {
        SaasPlan plan = getPlanById(id);
        saasPlanRepository.delete(plan);
    }
}