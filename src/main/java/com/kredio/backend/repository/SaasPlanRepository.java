package com.kredio.backend.repository;

import com.kredio.backend.entity.SaasPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface SaasPlanRepository extends JpaRepository<SaasPlan, UUID> {
    List<SaasPlan> findByIsActiveTrue();
}