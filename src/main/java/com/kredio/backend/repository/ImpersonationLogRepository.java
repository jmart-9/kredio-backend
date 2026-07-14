package com.kredio.backend.repository;

import com.kredio.backend.entity.ImpersonationLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ImpersonationLogRepository extends JpaRepository<ImpersonationLog, UUID> {
}
