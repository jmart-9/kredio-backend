-- =============================================================================
-- KREDIO V15 - Agregar columnas de anulación a payments en TODOS los schemas
-- =============================================================================

-- 1. Agregar columnas al schema public (por si acaso)
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS schedule_id UUID;
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS is_voided BOOLEAN DEFAULT FALSE;
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS voided_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS voided_by UUID;
ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS notes TEXT;

-- 2. Aplicar cambios a TODOS los schemas de tenants existentes
DO $$
DECLARE
    schema_name TEXT;
BEGIN
    FOR schema_name IN
        SELECT nspname
        FROM pg_namespace
        WHERE nspname LIKE 'tenant_%'
    LOOP
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS schedule_id UUID', schema_name);
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS is_voided BOOLEAN DEFAULT FALSE', schema_name);
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS voided_at TIMESTAMP WITH TIME ZONE', schema_name);
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS voided_by UUID', schema_name);
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS notes TEXT', schema_name);
    END LOOP;

    RAISE NOTICE '✅ Columnas de anulación agregadas a todas las tablas payments de tenants';
END $$;