-- Agregar columna created_at a la tabla loan_schedules en schema public
ALTER TABLE public.loan_schedules
ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP;

-- Agregar columna created_at a todas las tablas loan_schedules de los tenants existentes
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
            'ALTER TABLE %I.loan_schedules ADD COLUMN IF NOT EXISTS created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP',
            schema_name
        );
    END LOOP;

    RAISE NOTICE '✅ Columna created_at agregada a todas las tablas loan_schedules';
END $$;