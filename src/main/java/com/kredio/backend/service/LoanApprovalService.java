package com.kredio.backend.service;

import com.kredio.backend.entity.Loan;
import com.kredio.backend.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoanApprovalService {

    private final LoanRepository loanRepository;

    @Transactional
    public Loan approveLoan(UUID loanId, UUID adminId, UUID tenantId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        if (!loan.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Acceso denegado");
        }
        if (loan.getApprovalStatus() != Loan.ApprovalStatus.PENDING) {
            throw new RuntimeException("El préstamo no está en estado pendiente");
        }

        loan.setApprovalStatus(Loan.ApprovalStatus.APPROVED);
        loan.setApprovedBy(adminId);
        loan.setApprovedAt(Instant.now());
        loan.setStatus(Loan.LoanStatus.ACTIVE); // Se activa al aprobar

        return loanRepository.save(loan);
    }

    @Transactional
    public Loan rejectLoan(UUID loanId, UUID adminId, UUID tenantId, String reason) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        if (!loan.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Acceso denegado");
        }

        loan.setApprovalStatus(Loan.ApprovalStatus.REJECTED);
        loan.setApprovedBy(adminId);
        loan.setApprovedAt(Instant.now());
        // El status se mantiene en PENDING para que no aparezca como activo

        return loanRepository.save(loan);
    }

    public List<Loan> getPendingLoans(UUID tenantId) {
        return loanRepository.findByTenantIdAndApprovalStatus(tenantId, Loan.ApprovalStatus.PENDING);
    }
}