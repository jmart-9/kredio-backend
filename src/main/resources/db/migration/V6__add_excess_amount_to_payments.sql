-- =============================================================================
-- KREDIO V15 - Agregar columna excess_amount a payments
-- =============================================================================

-- 1. Agregar columna excess_amount al schema public
ALTER TABLE public.payments
ADD COLUMN IF NOT EXISTS excess_amount NUMERIC(19,4) DEFAULT 0;

-- 2. Aplicar cambios a todos los schemas de tenants existentes
DO $$
DECLARE
    schema_name TEXT;
BEGIN
    FOR schema_name IN
        SELECT nspname
        FROM pg_namespace
        WHERE nspname LIKE 'tenant_%'
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS excess_amount NUMERIC(19,4) DEFAULT 0',
            schema_name
        );
    END LOOP;

    RAISE NOTICE '✅ Columna excess_amount agregada a todas las tablas payments';
END $$;