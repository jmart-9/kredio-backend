package com.kredio.backend.service;

import com.kredio.backend.dto.LoanResponse;
import com.kredio.backend.entity.Loan;
import com.kredio.backend.repository.ClientRepository;
import com.kredio.backend.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanQueryService {

    private final LoanRepository loanRepository;
    private final ClientRepository clientRepository;

    public List<LoanResponse> findAllByTenant(UUID tenantId) {
        List<Loan> loans = loanRepository.findByTenantId(tenantId);
        return mapToResponse(loans);
    }

    public List<LoanResponse> findByStatus(UUID tenantId, String status) {
        Loan.LoanStatus loanStatus = Loan.LoanStatus.valueOf(status.toUpperCase());
        List<Loan> loans = loanRepository.findByTenantIdAndStatus(tenantId, loanStatus);
        return mapToResponse(loans);
    }

    public List<LoanResponse> findByClient(UUID tenantId, UUID clientId) {
        List<Loan> loans = loanRepository.findByTenantIdAndClientId(tenantId, clientId);
        return mapToResponse(loans);
    }

    public Loan findById(UUID loanId, UUID tenantId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Préstamo no encontrado"));

        if (!loan.getTenantId().equals(tenantId)) {
            throw new RuntimeException("Acceso denegado: el préstamo no pertenece a esta financiera");
        }

        return loan;
    }

    private List<LoanResponse> mapToResponse(List<Loan> loans) {
        return loans.stream()
                .map(loan -> {
                    String clientName = clientRepository.findById(loan.getClientId())
                            .map(client -> client.getFullName())
                            .orElse("Cliente no encontrado");

                    return new LoanResponse(
                            loan.getId(),
                            loan.getClientId(),
                            clientName,
                            loan.getPrincipalAmount(),
                            loan.getInterestRate(),
                            loan.getTermMonths(),
                            loan.getStatus().name(),
                            loan.getCurrentBalance(),
                            loan.getOpeningDate(),
                            loan.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());
    }
}