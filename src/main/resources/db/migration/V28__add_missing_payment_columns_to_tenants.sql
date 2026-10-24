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
        -- Agregar columnas de condonación a loan_schedules en cada schema de tenant
        EXECUTE format('ALTER TABLE %I.loan_schedules ADD COLUMN IF NOT EXISTS is_condonated BOOLEAN DEFAULT FALSE', tenant_schema);
        EXECUTE format('ALTER TABLE %I.loan_schedules ADD COLUMN IF NOT EXISTS condonated_amount NUMERIC(19,4)', tenant_schema);
        EXECUTE format('ALTER TABLE %I.loan_schedules ADD COLUMN IF NOT EXISTS condonated_interest NUMERIC(19,4)', tenant_schema);
        EXECUTE format('ALTER TABLE %I.loan_schedules ADD COLUMN IF NOT EXISTS condonated_at TIMESTAMP', tenant_schema);
        EXECUTE format('ALTER TABLE %I.loan_schedules ADD COLUMN IF NOT EXISTS condonated_by UUID', tenant_schema);
        EXECUTE format('ALTER TABLE %I.loan_schedules ADD COLUMN IF NOT EXISTS adjusted_principal NUMERIC(19,4)', tenant_schema);
        EXECUTE format('ALTER TABLE %I.loan_schedules ADD COLUMN IF NOT EXISTS adjusted_interest NUMERIC(19,4)', tenant_schema);
        EXECUTE format('ALTER TABLE %I.loan_schedules ADD COLUMN IF NOT EXISTS adjusted_amount NUMERIC(19,4)', tenant_schema);

        -- Crear índices para mejorar rendimiento
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_loan_schedules_condonated ON %I.loan_schedules(is_condonated)', tenant_schema, tenant_schema);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_loan_schedules_loan_status ON %I.loan_schedules(loan_id, status)', tenant_schema, tenant_schema);

        RAISE NOTICE '✅ Esquema % sincronizado: columnas de condonación agregadas a loan_schedules', tenant_schema;
    END LOOP;

    -- También ejecutar en el schema public por si acaso
    ALTER TABLE public.loan_schedules ADD COLUMN IF NOT EXISTS is_condonated BOOLEAN DEFAULT FALSE;
    ALTER TABLE public.loan_schedules ADD COLUMN IF NOT EXISTS condonated_amount NUMERIC(19,4);
    ALTER TABLE public.loan_schedules ADD COLUMN IF NOT EXISTS condonated_interest NUMERIC(19,4);
    ALTER TABLE public.loan_schedules ADD COLUMN IF NOT EXISTS condonated_at TIMESTAMP;
    ALTER TABLE public.loan_schedules ADD COLUMN IF NOT EXISTS condonated_by UUID;
    ALTER TABLE public.loan_schedules ADD COLUMN IF NOT EXISTS adjusted_principal NUMERIC(19,4);
    ALTER TABLE public.loan_schedules ADD COLUMN IF NOT EXISTS adjusted_interest NUMERIC(19,4);
    ALTER TABLE public.loan_schedules ADD COLUMN IF NOT EXISTS adjusted_amount NUMERIC(19,4);

    CREATE INDEX IF NOT EXISTS idx_public_loan_schedules_condonated ON public.loan_schedules(is_condonated);
    CREATE INDEX IF NOT EXISTS idx_public_loan_schedules_loan_status ON public.loan_schedules(loan_id, status);

    RAISE NOTICE '🎉 Sincronización de columnas de condonación en loan_schedules completada exitosamente en todos los tenants';
END $$;