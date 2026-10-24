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
        -- 1. Agregar nuevos campos con valores por defecto a la tabla loans
        EXECUTE format('ALTER TABLE %I.loans ADD COLUMN IF NOT EXISTS day_count_convention VARCHAR(20) DEFAULT ''ACTUAL_360''', tenant_schema);
        EXECUTE format('ALTER TABLE %I.loans ADD COLUMN IF NOT EXISTS early_repayment_policy VARCHAR(30) DEFAULT ''WITH_CONDONATION''', tenant_schema);
        EXECUTE format('ALTER TABLE %I.loans ADD COLUMN IF NOT EXISTS grace_days INTEGER DEFAULT 0', tenant_schema);
        EXECUTE format('ALTER TABLE %I.loans ADD COLUMN IF NOT EXISTS adjust_for_business_days BOOLEAN DEFAULT false', tenant_schema);

        -- 2. Migrar datos existentes: 'SIMPLE' -> 'FLAT'
        EXECUTE format('UPDATE %I.loans SET amortization_method = ''FLAT'' WHERE amortization_method = ''SIMPLE''', tenant_schema);

        -- 3. Agregar comentarios a las columnas de loans
        EXECUTE format('COMMENT ON COLUMN %I.loans.day_count_convention IS ''Convención de conteo de días: ACTUAL_360, ACTUAL_365, THIRTY_360''', tenant_schema);
        EXECUTE format('COMMENT ON COLUMN %I.loans.early_repayment_policy IS ''Política de condonación: NO_CONDONATION, WITH_CONDONATION''', tenant_schema);
        EXECUTE format('COMMENT ON COLUMN %I.loans.grace_days IS ''Días de gracia antes de aplicar intereses moratorios''', tenant_schema);
        EXECUTE format('COMMENT ON COLUMN %I.loans.adjust_for_business_days IS ''Ajustar fechas de pago por días inhábiles''', tenant_schema);

        -- 4. Crear tabla de historial de condonaciones en el schema del tenant
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.condonation_history (
                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                tenant_id UUID NOT NULL,
                loan_id UUID NOT NULL,
                schedule_id UUID,
                payment_id UUID,
                condoned_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
                condoned_interest NUMERIC(19,4) DEFAULT 0,
                condoned_principal NUMERIC(19,4) DEFAULT 0,
                reason TEXT,
                approved_by UUID,
                approved_at TIMESTAMP,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )', tenant_schema);

        -- 5. Crear índices para la tabla de condonaciones (con nombre único por schema)
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_condonation_loan_id_%s ON %I.condonation_history(loan_id)', replace(tenant_schema, '-', '_'), tenant_schema);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_condonation_payment_id_%s ON %I.condonation_history(payment_id)', replace(tenant_schema, '-', '_'), tenant_schema);

        -- 6. Agregar comentarios a la tabla de condonaciones
        EXECUTE format('COMMENT ON TABLE %I.condonation_history IS ''Historial de condonaciones de intereses por pagos anticipados''', tenant_schema);
        EXECUTE format('COMMENT ON COLUMN %I.condonation_history.condoned_amount IS ''Monto total condonado''', tenant_schema);
        EXECUTE format('COMMENT ON COLUMN %I.condonation_history.condoned_interest IS ''Interés condonado''', tenant_schema);
        EXECUTE format('COMMENT ON COLUMN %I.condonation_history.condoned_principal IS ''Capital condonado''', tenant_schema);

        RAISE NOTICE '✅ Esquema % sincronizado correctamente', tenant_schema;
    END LOOP;

    RAISE NOTICE '🎉 Proceso de sincronización de tenants completado exitosamente';
END $$;