-- V31__add_loan_commission_and_disbursement_fields.sql
-- Agregar campos de comisión, desembolso y configuración de mora

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
        -- Agregar campos a la tabla loans
        EXECUTE format('ALTER TABLE %I.loans ADD COLUMN IF NOT EXISTS disbursement_method VARCHAR(20) DEFAULT ''CASH''', tenant_schema);
        EXECUTE format('ALTER TABLE %I.loans ADD COLUMN IF NOT EXISTS administrative_fee NUMERIC(19,4) DEFAULT 0', tenant_schema);
        EXECUTE format('ALTER TABLE %I.loans ADD COLUMN IF NOT EXISTS fee_collection_mode VARCHAR(20) DEFAULT ''DEDUCTED''', tenant_schema);
        EXECUTE format('ALTER TABLE %I.loans ADD COLUMN IF NOT EXISTS moratory_type VARCHAR(20) DEFAULT ''PERCENTAGE''', tenant_schema);
        EXECUTE format('ALTER TABLE %I.loans ADD COLUMN IF NOT EXISTS moratory_value NUMERIC(19,4) DEFAULT 0', tenant_schema);

        RAISE NOTICE '✅ Esquema % sincronizado: campos de comisión y desembolso agregados', tenant_schema;
    END LOOP;

    -- También ejecutar en el schema public
    ALTER TABLE public.loans ADD COLUMN IF NOT EXISTS disbursement_method VARCHAR(20) DEFAULT 'CASH';
    ALTER TABLE public.loans ADD COLUMN IF NOT EXISTS administrative_fee NUMERIC(19,4) DEFAULT 0;
    ALTER TABLE public.loans ADD COLUMN IF NOT EXISTS fee_collection_mode VARCHAR(20) DEFAULT 'DEDUCTED';
    ALTER TABLE public.loans ADD COLUMN IF NOT EXISTS moratory_type VARCHAR(20) DEFAULT 'PERCENTAGE';
    ALTER TABLE public.loans ADD COLUMN IF NOT EXISTS moratory_value NUMERIC(19,4) DEFAULT 0;

    RAISE NOTICE '🎉 Migración completada en todos los schemas';
END $$;