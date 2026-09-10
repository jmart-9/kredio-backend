package com.kredio.backend.service;

import com.kredio.backend.dto.GlobalDashboardDTO;
import com.kredio.backend.entity.SaasPlan;
import com.kredio.backend.entity.Tenant;
import com.kredio.backend.repository.SaasPlanRepository;
import com.kredio.backend.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalDashboardService {

    private final TenantRepository tenantRepository;
    private final SaasPlanRepository saasPlanRepository; // ✅ Agregado
    private final JdbcTemplate jdbcTemplate;

    public GlobalDashboardDTO getGlobalDashboard() {
        List<Tenant> allTenants = tenantRepository.findAll();

        // 1. Resumen de Tenants
        long totalTenants = allTenants.size();
        long activeTenants = allTenants.stream().filter(t -> t.getStatus() == Tenant.TenantStatus.ACTIVE).count();
        long suspendedTenants = allTenants.stream().filter(t -> t.getStatus() == Tenant.TenantStatus.SUSPENDED).count();
        long cancelledTenants = allTenants.stream().filter(t -> t.getStatus() == Tenant.TenantStatus.CANCELLED).count();
        long trialTenants = allTenants.stream().filter(t -> Boolean.TRUE.equals(t.getIsTrial())).count();

        // 2. Total de usuarios
        Long totalUsers = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);

        // 3. MRR y Distribución por Plan (Cálculo Real)
        BigDecimal mrr = BigDecimal.ZERO;
        Map<String, Long> tenantsByPlan = new HashMap<>();
        List<Tenant> activeTenantsList = allTenants.stream()
                .filter(t -> t.getStatus() == Tenant.TenantStatus.ACTIVE).toList();

        for (Tenant tenant : activeTenantsList) {
            if (tenant.getPlanId() != null) {
                Optional<SaasPlan> planOpt = saasPlanRepository.findById(tenant.getPlanId());
                if (planOpt.isPresent()) {
                    SaasPlan plan = planOpt.get();
                    mrr = mrr.add(plan.getMonthlyPrice()); // ✅ Sumar precio real
                    tenantsByPlan.merge(plan.getName(), 1L, Long::sum); // ✅ Agrupar por nombre
                }
            }
        }

        // 4. Valor total de cartera
        BigDecimal totalPortfolioValue = calculateTotalPortfolioValue(activeTenantsList);

        // 5. Alertas de Trial Expiring (Usando planEndDate)
        List<GlobalDashboardDTO.TenantAlertDTO> expiringTrials = findExpiringTrials(allTenants);

        return GlobalDashboardDTO.builder()
                .totalTenants(totalTenants)
                .activeTenants(activeTenants)
                .suspendedTenants(suspendedTenants)
                .cancelledTenants(cancelledTenants)
                .trialTenants(trialTenants)
                .totalUsers(totalUsers != null ? totalUsers : 0)
                .monthlyRecurringRevenue(mrr)
                .totalPortfolioValue(totalPortfolioValue)
                .tenantsByPlan(tenantsByPlan)
                .expiringTrials(expiringTrials)
                .exceededLimits(new ArrayList<>())
                .build();
    }

    private BigDecimal calculateTotalPortfolioValue(List<Tenant> tenants) {
        BigDecimal total = BigDecimal.ZERO;
        for (Tenant tenant : tenants) {
            try {
                // ✅ CAMBIADO: Usar CAST para manejar el tipo enum correctamente
                BigDecimal portfolioValue = jdbcTemplate.queryForObject(
                        String.format(
                                "SELECT COALESCE(SUM(current_balance), 0) FROM %s.loans WHERE status::text = 'ACTIVE'",
                                tenant.getSchemaName()
                        ),
                        BigDecimal.class
                );
                if (portfolioValue != null) {
                    total = total.add(portfolioValue);
                }
            } catch (Exception e) {
                log.warn("No se pudo calcular portfolio para tenant {}: {}", tenant.getName(), e.getMessage());
            }
        }
        return total;
    }

    private List<GlobalDashboardDTO.TenantAlertDTO> findExpiringTrials(List<Tenant> tenants) {
        List<GlobalDashboardDTO.TenantAlertDTO> alerts = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (Tenant tenant : tenants) {
            if (!Boolean.TRUE.equals(tenant.getIsTrial())) continue;
            if (tenant.getPlanEndDate() == null) continue;

            long daysRemaining = ChronoUnit.DAYS.between(today, tenant.getPlanEndDate());

            if (daysRemaining <= 7 && daysRemaining >= 0) {
                alerts.add(GlobalDashboardDTO.TenantAlertDTO.builder()
                        .tenantName(tenant.getName())
                        .subdomain(tenant.getSubdomain())
                        .alertType("TRIAL_EXPIRING")
                        .message("Trial expira en " + daysRemaining + " días")
                        .daysRemaining((int) daysRemaining)
                        .build());
            }
        }
        return alerts;
    }
}