package com.kredio.backend.repository;

import com.kredio.backend.entity.InterestRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterestRateRepository extends JpaRepository<InterestRate, UUID> {
    List<InterestRate> findByIsGlobalTrueAndIsActiveTrue();
    List<InterestRate> findByTenantIdAndIsActiveTrue(UUID tenantId);
}