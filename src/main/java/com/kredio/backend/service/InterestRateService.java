package com.kredio.backend.service;

import com.kredio.backend.entity.InterestRate;
import com.kredio.backend.repository.InterestRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterestRateService {

    private final InterestRateRepository interestRateRepository;

    public List<InterestRate> getGlobalRates() {
        return interestRateRepository.findByIsGlobalTrueAndIsActiveTrue();
    }

    public List<InterestRate> getTenantRates(UUID tenantId) {
        return interestRateRepository.findByTenantIdAndIsActiveTrue(tenantId);
    }

    public List<InterestRate> getAllActiveRates(UUID tenantId) {
        List<InterestRate> rates = getGlobalRates();
        rates.addAll(getTenantRates(tenantId));
        return rates;
    }

    @Transactional
    public InterestRate createRate(InterestRate rate, UUID tenantId, boolean isGlobal) {
        rate.setTenantId(isGlobal ? null : tenantId);
        rate.setIsGlobal(isGlobal);
        rate.setIsActive(true);
        return interestRateRepository.save(rate);
    }

    @Transactional
    public InterestRate updateRate(UUID id, InterestRate rateDetails) {
        InterestRate rate = interestRateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tasa de interés no encontrada"));

        rate.setName(rateDetails.getName());
        rate.setAnnualRate(rateDetails.getAnnualRate());
        rate.setDescription(rateDetails.getDescription());
        rate.setIsActive(rateDetails.getIsActive());

        return interestRateRepository.save(rate);
    }

    @Transactional
    public void deleteRate(UUID id) {
        InterestRate rate = interestRateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tasa de interés no encontrada"));
        interestRateRepository.delete(rate);
    }
}