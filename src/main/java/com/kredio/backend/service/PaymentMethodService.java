package com.kredio.backend.service;

import com.kredio.backend.entity.PaymentMethod;
import com.kredio.backend.repository.PaymentMethodRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;

    public List<PaymentMethod> getPaymentMethodsByTenant(UUID tenantId) {
        return paymentMethodRepository.findByTenantIdAndIsActiveTrue(tenantId);
    }

    @Transactional
    public PaymentMethod createPaymentMethod(PaymentMethod method, UUID tenantId) {
        method.setTenantId(tenantId);
        method.setIsActive(true);
        return paymentMethodRepository.save(method);
    }

    @Transactional
    public PaymentMethod updatePaymentMethod(UUID id, PaymentMethod methodDetails) {
        PaymentMethod method = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Método de pago no encontrado"));

        method.setName(methodDetails.getName());
        method.setCode(methodDetails.getCode());
        method.setIsActive(methodDetails.getIsActive());

        return paymentMethodRepository.save(method);
    }

    @Transactional
    public void deletePaymentMethod(UUID id) {
        PaymentMethod method = paymentMethodRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Método de pago no encontrado"));
        paymentMethodRepository.delete(method);
    }
}
