DO $$
DECLARE
    rec RECORD;
BEGIN
    -- Iterar sobre todos los schemas que empiezan con 'tenant_' usando un alias 's'
    FOR rec IN
        SELECT s.schema_name
        FROM information_schema.schemata s
        WHERE s.schema_name LIKE 'tenant_%'
    LOOP
        -- Agregar columnas faltantes a la tabla payments en cada schema de tenant
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS applied_to_moratory NUMERIC(19,4) DEFAULT 0', rec.schema_name);
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS applied_to_interest NUMERIC(19,4) DEFAULT 0', rec.schema_name);
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS applied_to_principal NUMERIC(19,4) DEFAULT 0', rec.schema_name);
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS excess_amount NUMERIC(19,4) DEFAULT 0', rec.schema_name);

        RAISE NOTICE '✅ Esquema % sincronizado: columnas agregadas a payments', rec.schema_name;
    END LOOP;

    -- También ejecutar en el schema public por si acaso
    ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS applied_to_moratory NUMERIC(19,4) DEFAULT 0;
    ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS applied_to_interest NUMERIC(19,4) DEFAULT 0;
    ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS applied_to_principal NUMERIC(19,4) DEFAULT 0;
    ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS excess_amount NUMERIC(19,4) DEFAULT 0;

    RAISE NOTICE '🎉 Sincronización de columnas de payments en todos los tenants completada exitosamente';
END $$;