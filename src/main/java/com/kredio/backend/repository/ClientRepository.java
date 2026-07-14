package com.kredio.backend.repository;

import com.kredio.backend.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {

    @Query("SELECT c FROM Client c WHERE c.tenantId = :tenantId ORDER BY c.fullName ASC")
    List<Client> findByTenantId(@Param("tenantId") UUID tenantId);

    @Query("SELECT c FROM Client c WHERE c.tenantId = :tenantId AND LOWER(c.fullName) LIKE LOWER(CONCAT('%', :search, '%')) ORDER BY c.fullName ASC")
    List<Client> findByTenantIdAndSearch(@Param("tenantId") UUID tenantId, @Param("search") String search);

    @Query("SELECT COUNT(c) > 0 FROM Client c WHERE c.dpiOrId = :dpiOrId")
    boolean existsByDpiOrId(@Param("dpiOrId") String dpiOrId);

    List<Client> findByPortfolioId(UUID portfolioId);
}