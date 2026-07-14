package com.kredio.backend.repository;

import com.kredio.backend.entity.RenegotiationRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RenegotiationRequestRepository extends JpaRepository<RenegotiationRequest, UUID> {
    List<RenegotiationRequest> findByLoanId(UUID loanId);
    List<RenegotiationRequest> findByTenantIdAndStatus(UUID tenantId, RenegotiationRequest.RenegotiationStatus status);
}
