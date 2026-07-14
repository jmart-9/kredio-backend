package com.kredio.backend.repository;

import com.kredio.backend.entity.UsageTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UsageTrackingRepository extends JpaRepository<UsageTracking, UUID> {
    Optional<UsageTracking> findByTenantIdAndMetricName(UUID tenantId, String metricName);
}
