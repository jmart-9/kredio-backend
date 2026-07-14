package com.kredio.backend.repository;

import com.kredio.backend.entity.SaasPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SaasPlanRepository extends JpaRepository<SaasPlan, UUID> {
}
