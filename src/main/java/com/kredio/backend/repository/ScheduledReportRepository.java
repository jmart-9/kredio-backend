package com.kredio.backend.repository;

import com.kredio.backend.entity.ScheduledReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ScheduledReportRepository extends JpaRepository<ScheduledReport, UUID> {
    List<ScheduledReport> findByTenantIdAndIsActiveTrue(UUID tenantId);
}
