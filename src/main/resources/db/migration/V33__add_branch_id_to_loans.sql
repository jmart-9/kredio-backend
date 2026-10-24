-- V34__add_branch_id_to_all_loans.sql
-- Agrega la columna branch_id a la tabla loans en todos los schemas de tenants y en public

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
        -- Agregar la columna branch_id
        EXECUTE format('ALTER TABLE %I.loans ADD COLUMN IF NOT EXISTS branch_id UUID', tenant_schema);

        -- Agregar índice para mejorar performance
        -- (Se incluye el nombre del schema en el índice para evitar colisiones de nombres)
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_loans_branch_id ON %I.loans(branch_id)',
                       replace(tenant_schema, '-', '_'), tenant_schema);

        RAISE NOTICE '✅ Columna branch_id e índice agregados en schema: %', tenant_schema;
    END LOOP;

    -- ==========================================
    -- 2. Aplicar también al schema public por consistencia
    -- ==========================================
    ALTER TABLE public.loans ADD COLUMN IF NOT EXISTS branch_id UUID;
    CREATE INDEX IF NOT EXISTS idx_public_loans_branch_id ON public.loans(branch_id);

    RAISE NOTICE '🎉 Migración completada: branch_id agregado a la tabla loans en todos los schemas';
END $$;

-- =========================================================================
-- NOTA SOBRE CLAVE FORÁNEA (Opcional)
-- =========================================================================
-- Si en el futuro decides habilitar la relación con la tabla de sucursales,
-- puedes ejecutar esto manualmente (asumiendo que la tabla está en 'public'):
--
-- DO $$
-- DECLARE tenant_schema TEXT;
-- BEGIN
--     FOR tenant_schema IN SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE 'tenant_%' LOOP
--         EXECUTE format('ALTER TABLE %I.loans ADD CONSTRAINT fk_%s_loans_branch FOREIGN KEY (branch_id) REFERENCES public.branch_offices(branch_id) ON DELETE SET NULL', tenant_schema, replace(tenant_schema, '-', '_'));
--     END LOOP;
--     ALTER TABLE public.loans ADD CONSTRAINT fk_public_loans_branch FOREIGN KEY (branch_id) REFERENCES public.branch_offices(branch_id) ON DELETE SET NULL;
-- END $$;