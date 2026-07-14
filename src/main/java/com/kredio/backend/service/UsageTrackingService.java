package com.kredio.backend.service;

import com.kredio.backend.entity.UsageTracking;
import com.kredio.backend.repository.UsageTrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UsageTrackingService {

    private final UsageTrackingRepository usageTrackingRepository;
    private final NotificationService notificationService;

    public List<UsageTracking> getUsageByTenant(UUID tenantId) {
        return usageTrackingRepository.findAll()
                .stream()
                .filter(ut -> ut.getTenantId().equals(tenantId))
                .toList();
    }

    public UsageTracking getUsageByTenantAndMetric(UUID tenantId, String metricName) {
        return usageTrackingRepository.findByTenantIdAndMetricName(tenantId, metricName)
                .orElse(null);
    }

    @Transactional
    public UsageTracking updateUsage(UUID tenantId, String metricName, BigDecimal currentValue, BigDecimal limitValue) {
        UsageTracking usage = usageTrackingRepository.findByTenantIdAndMetricName(tenantId, metricName)
                .orElse(UsageTracking.builder()
                        .tenantId(tenantId)
                        .metricName(metricName)
                        .currentValue(BigDecimal.ZERO)
                        .limitValue(limitValue)
                        .percentageUsed(BigDecimal.ZERO)
                        .exceeded(false)
                        .build());

        usage.setCurrentValue(currentValue);
        usage.setLimitValue(limitValue);

        // Calcular porcentaje
        if (limitValue.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal percentage = currentValue.multiply(BigDecimal.valueOf(100))
                    .divide(limitValue, 2, RoundingMode.HALF_UP);
            usage.setPercentageUsed(percentage);

            // Verificar si excedió el límite
            boolean isExceeded = currentValue.compareTo(limitValue) >= 0;
            usage.setExceeded(isExceeded);

            // Enviar notificación si está al 90% o excedió
            if (percentage.compareTo(BigDecimal.valueOf(90)) >= 0 && !isExceeded) {
                notificationService.sendPlanExceededNotification(
                        tenantId,
                        metricName,
                        currentValue.intValue(),
                        limitValue.intValue()
                );
            }
        }

        return usageTrackingRepository.save(usage);
    }

    public boolean checkLimit(UUID tenantId, String metricName) {
        UsageTracking usage = getUsageByTenantAndMetric(tenantId, metricName);
        if (usage == null) {
            return true; // No hay límite configurado
        }
        return usage.getCurrentValue().compareTo(usage.getLimitValue()) < 0;
    }
}
