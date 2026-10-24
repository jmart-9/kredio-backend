DO $$
DECLARE
    tenant_schema TEXT;
BEGIN
    -- Iterar sobre todos los schemas que empiezan con 'tenant_'
    FOR tenant_schema IN
        SELECT schema_name
        FROM information_schema.schemata
        WHERE schema_name LIKE 'tenant_%'
    LOOP
        -- Agregar columnas a la tabla payments en cada schema de tenant
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS applied_to_moratory NUMERIC(19,4)', tenant_schema);
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS applied_to_interest NUMERIC(19,4)', tenant_schema);
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS applied_to_principal NUMERIC(19,4)', tenant_schema);
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS excess_amount NUMERIC(19,4)', tenant_schema);

        RAISE NOTICE '✅ Esquema % sincronizado: columnas agregadas a payments', tenant_schema;
    END LOOP;

    RAISE NOTICE '🎉 Sincronización de columnas de payments en tenants completada exitosamente';
END $$;