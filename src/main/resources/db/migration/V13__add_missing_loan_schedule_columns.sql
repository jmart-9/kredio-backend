-- =============================================================================
-- KREDIO V13 - Agregar columnas faltantes a loan_schedules
-- =============================================================================

-- 1. Agregar columna paid_amount al schema public
ALTER TABLE public.loan_schedules
ADD COLUMN IF NOT EXISTS paid_amount NUMERIC(19,4) DEFAULT 0;

-- 2. Agregar columna paid_date (cambiar a TIMESTAMP para compatibilidad con Instant)
ALTER TABLE public.loan_schedules
ADD COLUMN IF NOT EXISTS paid_date TIMESTAMP WITH TIME ZONE;

-- 3. Aplicar cambios a todos los schemas de tenants existentes
DO $$
DECLARE
    schema_name TEXT;
BEGIN
    FOR schema_name IN
        SELECT nspname
        FROM pg_namespace
        WHERE nspname LIKE 'tenant_%'
    LOOP
        -- Agregar paid_amount
        EXECUTE format(
            'ALTER TABLE %I.loan_schedules ADD COLUMN IF NOT EXISTS paid_amount NUMERIC(19,4) DEFAULT 0',
            schema_name
        );

        -- Agregar paid_date
        EXECUTE format(
            'ALTER TABLE %I.loan_schedules ADD COLUMN IF NOT EXISTS paid_date TIMESTAMP WITH TIME ZONE',
            schema_name
        );
    END LOOP;

    RAISE NOTICE '✅ Columnas paid_amount y paid_date agregadas a todas las tablas loan_schedules';
END $$;