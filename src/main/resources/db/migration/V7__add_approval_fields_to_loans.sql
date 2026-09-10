-- =============================================================================
-- KREDIO V7 - Agregar campos de aprobación a loans (Idempotente)
-- =============================================================================
ALTER TABLE loans ADD COLUMN IF NOT EXISTS approval_status VARCHAR(50);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS approved_by UUID;