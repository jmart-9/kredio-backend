-- =============================================================================
-- KREDIO V14 - Agregar campos de pago a loan_schedules
-- =============================================================================

-- 1. Agregar columna paid_amount al schema public
ALTER TABLE public.loan_schedules
ADD COLUMN IF NOT EXISTS paid_amount NUMERIC(19,4) DEFAULT 0;

-- 2. Cambiar paid_date de DATE a TIMESTAMP WITH TIME ZONE (si existe)
-- Primero eliminamos la columna si existe como DATE
ALTER TABLE public.loan_schedules
DROP COLUMN IF EXISTS paid_date;

-- Luego la recreamos como TIMESTAMP
ALTER TABLE public.loan_schedules
ADD COLUMN paid_date TIMESTAMP WITH TIME ZONE;

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

        -- Cambiar paid_date
        EXECUTE format(
            'ALTER TABLE %I.loan_schedules DROP COLUMN IF EXISTS paid_date',
            schema_name
        );

        EXECUTE format(
            'ALTER TABLE %I.loan_schedules ADD COLUMN paid_date TIMESTAMP WITH TIME ZONE',
            schema_name
        );
    END LOOP;

    RAISE NOTICE '✅ Campos paid_amount y paid_date agregados a todas las tablas loan_schedules';
END $$;