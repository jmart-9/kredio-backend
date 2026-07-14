package com.kredio.backend.service;

import com.kredio.backend.entity.RenegotiationRequest;
import com.kredio.backend.repository.RenegotiationRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RenegotiationService {

    private final RenegotiationRequestRepository renegotiationRequestRepository;

    public List<RenegotiationRequest> getRequestsByLoan(UUID loanId) {
        return renegotiationRequestRepository.findByLoanId(loanId);
    }

    public List<RenegotiationRequest> getRequestsByTenantAndStatus(UUID tenantId, RenegotiationRequest.RenegotiationStatus status) {
        return renegotiationRequestRepository.findByTenantIdAndStatus(tenantId, status);
    }

    @Transactional
    public RenegotiationRequest createRequest(RenegotiationRequest request) {
        request.setStatus(RenegotiationRequest.RenegotiationStatus.PENDING);
        return renegotiationRequestRepository.save(request);
    }

    @Transactional
    public RenegotiationRequest approveRequest(UUID requestId, UUID adminId, String reviewNotes) {
        RenegotiationRequest request = renegotiationRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        request.setStatus(RenegotiationRequest.RenegotiationStatus.APPROVED);
        request.setAdminReviewerId(adminId);
        request.setReviewNotes(reviewNotes);
        request.setReviewedAt(java.time.Instant.now());

        // Aquí deberías aplicar los cambios al préstamo
        // applyRenegotiationToLoan(request);

        return renegotiationRequestRepository.save(request);
    }

    @Transactional
    public RenegotiationRequest rejectRequest(UUID requestId, UUID adminId, String reviewNotes) {
        RenegotiationRequest request = renegotiationRequestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Solicitud no encontrada"));

        request.setStatus(RenegotiationRequest.RenegotiationStatus.REJECTED);
        request.setAdminReviewerId(adminId);
        request.setReviewNotes(reviewNotes);
        request.setReviewedAt(java.time.Instant.now());

        return renegotiationRequestRepository.save(request);
    }
}
