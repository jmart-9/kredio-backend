-- =============================================================================
-- KREDIO V8 - Agregar columnas faltantes de forma segura (Idempotente)
-- =============================================================================

-- Agregar columna guarantee_description a loans si no existe
ALTER TABLE loans ADD COLUMN IF NOT EXISTS guarantee_description TEXT;

-- Agregar columna role_id a users si no existe
ALTER TABLE users ADD COLUMN IF NOT EXISTS role_id UUID;