-- V35__add_previous_loan_id_to_loans.sql
-- Agrega la columna previous_loan_id a la tabla loans en todos los schemas

DO $$
DECLARE
    tenant_schema TEXT;
BEGIN
    -- ==========================================
    -- 1. Iterar sobre todos los schemas de tenants
    -- ==========================================
    FOR tenant_schema IN
        SELECT schema_name
        FROM information_schema.schemata
        WHERE schema_name LIKE 'tenant_%'
    LOOP
        -- Agregar la columna previous_loan_id
        EXECUTE format('ALTER TABLE %I.loans ADD COLUMN IF NOT EXISTS previous_loan_id UUID', tenant_schema);
        RAISE NOTICE '✅ Columna previous_loan_id agregada en schema: %', tenant_schema;
    END LOOP;

    -- ==========================================
    -- 2. Aplicar también al schema public por consistencia
    -- ==========================================
    ALTER TABLE public.loans ADD COLUMN IF NOT EXISTS previous_loan_id UUID;

    RAISE NOTICE '🎉 Migración completada: previous_loan_id agregado a la tabla loans en todos los schemas';
END $$;