package com.kredio.backend.repository;

import com.kredio.backend.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {

    List<Portfolio> findByTenantIdAndIsActiveTrue(UUID tenantId);

    @Query("SELECT p FROM Portfolio p WHERE p.tenantId = :tenantId AND p.name = :name AND p.isActive = true")
    Optional<Portfolio> findActiveByTenantIdAndName(@Param("tenantId") UUID tenantId, @Param("name") String name);

    @Query("SELECT COUNT(c) FROM Client c WHERE c.portfolioId = :portfolioId")
    int countClientsByPortfolio(@Param("portfolioId") UUID portfolioId);

    @Query("SELECT COUNT(l) FROM Loan l WHERE l.portfolioId = :portfolioId AND l.status = 'ACTIVE'")
    int countActiveLoansByPortfolio(@Param("portfolioId") UUID portfolioId);
}