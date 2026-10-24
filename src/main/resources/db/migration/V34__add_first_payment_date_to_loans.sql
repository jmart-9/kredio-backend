-- V34__add_first_payment_date_to_loans.sql
-- Agrega la columna first_payment_date a la tabla loans en todos los schemas

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
        -- Agregar la columna first_payment_date
        EXECUTE format('ALTER TABLE %I.loans ADD COLUMN IF NOT EXISTS first_payment_date DATE', tenant_schema);
        RAISE NOTICE '✅ Columna first_payment_date agregada en schema: %', tenant_schema;
    END LOOP;

    -- ==========================================
    -- 2. Aplicar también al schema public por consistencia
    -- ==========================================
    ALTER TABLE public.loans ADD COLUMN IF NOT EXISTS first_payment_date DATE;

    RAISE NOTICE '🎉 Migración completada: first_payment_date agregado a la tabla loans en todos los schemas';
END $$;