package com.kredio.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GlobalDashboardDTO {

    // Resumen de Tenants
    private long totalTenants;
    private long activeTenants;
    private long suspendedTenants;
    private long cancelledTenants;
    private long trialTenants;

    // Resumen de Usuarios
    private long totalUsers;

    // Resumen Financiero
    private BigDecimal monthlyRecurringRevenue; // MRR
    private BigDecimal totalPortfolioValue;     // Valor total de cartera

    // Distribución por Plan
    private Map<String, Long> tenantsByPlan;

    // Alertas
    private List<TenantAlertDTO> expiringTrials;
    private List<TenantAlertDTO> exceededLimits;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantAlertDTO {
        private String tenantName;
        private String subdomain;
        private String alertType; // "TRIAL_EXPIRING", "LIMIT_EXCEEDED"
        private String message;
        private int daysRemaining;
    }
}