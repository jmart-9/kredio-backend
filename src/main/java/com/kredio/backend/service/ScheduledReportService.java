package com.kredio.backend.service;

import com.kredio.backend.entity.ScheduledReport;
import com.kredio.backend.repository.ScheduledReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ScheduledReportService {

    private final ScheduledReportRepository scheduledReportRepository;

    public List<ScheduledReport> getActiveReportsByTenant(UUID tenantId) {
        return scheduledReportRepository.findByTenantIdAndIsActiveTrue(tenantId);
    }

    public ScheduledReport getReportById(UUID id) {
        return scheduledReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reporte programado no encontrado"));
    }

    @Transactional
    public ScheduledReport createReport(ScheduledReport report, UUID tenantId, UUID createdBy) {
        report.setTenantId(tenantId);
        report.setCreatedBy(createdBy);
        report.setIsActive(true);
        return scheduledReportRepository.save(report);
    }

    @Transactional
    public ScheduledReport updateReport(UUID id, ScheduledReport reportDetails) {
        ScheduledReport report = getReportById(id);

        report.setName(reportDetails.getName());
        report.setReportType(reportDetails.getReportType());
        report.setFrequency(reportDetails.getFrequency());
        report.setCustomDays(reportDetails.getCustomDays());
        report.setEmailRecipients(reportDetails.getEmailRecipients());
        report.setFormat(reportDetails.getFormat());
        report.setIsActive(reportDetails.getIsActive());

        return scheduledReportRepository.save(report);
    }

    @Transactional
    public void deleteReport(UUID id) {
        ScheduledReport report = getReportById(id);
        scheduledReportRepository.delete(report);
    }

    @Transactional
    public void markAsSent(UUID id) {
        ScheduledReport report = getReportById(id);
        report.setLastSentAt(java.time.Instant.now());
        scheduledReportRepository.save(report);
    }
}