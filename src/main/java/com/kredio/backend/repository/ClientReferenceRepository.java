package com.kredio.backend.repository;

import com.kredio.backend.entity.ClientReference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClientReferenceRepository extends JpaRepository<ClientReference, UUID> {
    List<ClientReference> findByClientId(UUID clientId);
}