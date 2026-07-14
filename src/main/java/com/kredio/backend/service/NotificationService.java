package com.kredio.backend.service;

import com.kredio.backend.entity.Notification;
import com.kredio.backend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<Notification> getUnreadNotificationsByTenant(UUID tenantId) {
        return notificationRepository.findByTenantIdAndIsReadFalse(tenantId);
    }

    public List<Notification> getUnreadNotificationsByUser(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public Notification createNotification(Notification notification) {
        notification.setIsRead(false);
        return notificationRepository.save(notification);
    }

    @Transactional
    public void markAsRead(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notificación no encontrada"));

        notification.setIsRead(true);
        notification.setReadAt(java.time.Instant.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(UUID userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalse(userId);
        for (Notification notification : notifications) {
            notification.setIsRead(true);
            notification.setReadAt(java.time.Instant.now());
        }
        notificationRepository.saveAll(notifications);
    }

    @Transactional
    public void sendPlanExceededNotification(UUID tenantId, String metricName, int currentValue, int limitValue) {
        Notification notification = Notification.builder()
                .tenantId(tenantId)
                .type("PLAN_EXCEEDED")
                .title("Límite de plan excedido")
                .message(String.format("Has excedido el límite de %s: %d/%d", metricName, currentValue, limitValue))
                .priority("HIGH")
                .metadata(String.format("{\"metric\":\"%s\",\"current\":%d,\"limit\":%d}", metricName, currentValue, limitValue))
                .build();

        createNotification(notification);
    }

    @Transactional
    public void sendTrialEndingNotification(UUID tenantId, int daysRemaining) {
        Notification notification = Notification.builder()
                .tenantId(tenantId)
                .type("TRIAL_ENDING")
                .title("Período de trial terminando")
                .message(String.format("Tu período de prueba termina en %d días. Actualiza tu plan para continuar.", daysRemaining))
                .priority("CRITICAL")
                .metadata(String.format("{\"daysRemaining\":%d}", daysRemaining))
                .build();

        createNotification(notification);
    }

    @Transactional
    public void sendRenegotiationApprovedNotification(UUID userId, UUID loanId) {
        Notification notification = Notification.builder()
                .tenantId(null) // Se obtendría del préstamo
                .userId(userId)
                .type("RENEGOTIATION_APPROVED")
                .title("Renegociación aprobada")
                .message("Tu solicitud de renegociación ha sido aprobada por el administrador.")
                .priority("NORMAL")
                .metadata(String.format("{\"loanId\":\"%s\"}", loanId))
                .build();

        createNotification(notification);
    }
}
