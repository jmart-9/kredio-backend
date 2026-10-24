-- Agregar columnas faltantes en todos los schemas de tenants

-- Función para agregar columnas en todos los tenants
DO $$
DECLARE
    tenant_schema TEXT;
BEGIN
    -- Iterar sobre todos los schemas de tenants
    FOR tenant_schema IN
        SELECT schema_name
        FROM information_schema.schemata
        WHERE schema_name LIKE 'tenant_%'
    LOOP
        -- Agregar applied_to_principal en payments
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = tenant_schema
            AND table_name = 'payments'
            AND column_name = 'applied_to_principal'
        ) THEN
            EXECUTE format('ALTER TABLE %I.payments ADD COLUMN applied_to_principal NUMERIC(19,4)', tenant_schema);
            RAISE NOTICE '✅ Columna applied_to_principal agregada en schema: %', tenant_schema;
        END IF;

        -- Agregar moratory_interest en loan_schedules
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = tenant_schema
            AND table_name = 'loan_schedules'
            AND column_name = 'moratory_interest'
        ) THEN
            EXECUTE format('ALTER TABLE %I.loan_schedules ADD COLUMN moratory_interest NUMERIC(19,4)', tenant_schema);
            RAISE NOTICE '✅ Columna moratory_interest agregada en schema: %', tenant_schema;
        END IF;
    END LOOP;

    RAISE NOTICE '🎉 Proceso completado';
END $$;