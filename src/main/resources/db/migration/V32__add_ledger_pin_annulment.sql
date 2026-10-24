-- V32__add_ledger_pin_annulment.sql
-- Fase 1: Ledger inmutable + PIN de autorización + Anulación

DO $$
DECLARE
    tenant_schema TEXT;
BEGIN
    FOR tenant_schema IN
        SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE 'tenant_%'
    LOOP
        -- ============================================
        -- 1. TABLA: payment_ledger (LIBRO INMUTABLE)
        -- ============================================
        EXECUTE format('
            CREATE TABLE IF NOT EXISTS %I.payment_ledger (
                ledger_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                payment_id UUID NOT NULL REFERENCES %I.payments(id) ON DELETE CASCADE,
                schedule_id UUID NOT NULL REFERENCES %I.loan_schedules(id),
                entry_type VARCHAR(20) NOT NULL CHECK (entry_type IN (''PAYMENT'', ''REVERSAL'', ''ADJUSTMENT'')),
                allocated_to_moratory NUMERIC(19,4) DEFAULT 0,
                allocated_to_interest NUMERIC(19,4) DEFAULT 0,
                allocated_to_principal NUMERIC(19,4) DEFAULT 0,
                total_allocated NUMERIC(19,4) NOT NULL,
                previous_balance NUMERIC(19,4),
                current_balance NUMERIC(19,4),
                created_by UUID NOT NULL,
                reason TEXT,
                created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
            )', tenant_schema, tenant_schema, tenant_schema);

        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_ledger_payment ON %I.payment_ledger(payment_id)', tenant_schema, tenant_schema);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_ledger_schedule ON %I.payment_ledger(schedule_id)', tenant_schema, tenant_schema);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_ledger_entry ON %I.payment_ledger(entry_type)', tenant_schema, tenant_schema);
        EXECUTE format('CREATE INDEX IF NOT EXISTS idx_%s_ledger_created ON %I.payment_ledger(created_at DESC)', tenant_schema, tenant_schema);

        -- ============================================
        -- 2. CAMPOS: PIN en users
        -- ============================================
        EXECUTE format('ALTER TABLE %I.users ADD COLUMN IF NOT EXISTS pin_hash VARCHAR(255)', tenant_schema);
        EXECUTE format('ALTER TABLE %I.users ADD COLUMN IF NOT EXISTS pin_attempts INT DEFAULT 0', tenant_schema);
        EXECUTE format('ALTER TABLE %I.users ADD COLUMN IF NOT EXISTS pin_locked_until TIMESTAMP WITH TIME ZONE', tenant_schema);

        -- ============================================
        -- 3. CAMPOS: Anulación en payments
        -- ============================================
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS annulment_reason TEXT', tenant_schema);
        -- ✅ CORREGIDO: Ahora se pasan 2 argumentos (tenant_schema, tenant_schema) para los 2 marcadores %I
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS annulled_by_user_id UUID REFERENCES %I.users(id)', tenant_schema, tenant_schema);
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS annulled_at TIMESTAMP WITH TIME ZONE', tenant_schema);

        -- ============================================
        -- 4. CAMPOS: Idempotencia y geolocalización
        -- ============================================
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS client_transaction_uuid UUID UNIQUE', tenant_schema);
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS latitude NUMERIC(10, 8)', tenant_schema);
        EXECUTE format('ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS longitude NUMERIC(11, 8)', tenant_schema);

        RAISE NOTICE '✅ Fase 1 completada en schema %: ledger, PIN, anulación, idempotencia', tenant_schema;
    END LOOP;

    -- ============================================
    -- También en public por si acaso
    -- ============================================
    CREATE TABLE IF NOT EXISTS public.payment_ledger (
        ledger_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
        payment_id UUID NOT NULL REFERENCES public.payments(id) ON DELETE CASCADE,
        schedule_id UUID NOT NULL REFERENCES public.loan_schedules(id),
        entry_type VARCHAR(20) NOT NULL CHECK (entry_type IN ('PAYMENT', 'REVERSAL', 'ADJUSTMENT')),
        allocated_to_moratory NUMERIC(19,4) DEFAULT 0,
        allocated_to_interest NUMERIC(19,4) DEFAULT 0,
        allocated_to_principal NUMERIC(19,4) DEFAULT 0,
        total_allocated NUMERIC(19,4) NOT NULL,
        previous_balance NUMERIC(19,4),
        current_balance NUMERIC(19,4),
        created_by UUID NOT NULL,
        reason TEXT,
        created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
    );

    ALTER TABLE public.users ADD COLUMN IF NOT EXISTS pin_hash VARCHAR(255);
    ALTER TABLE public.users ADD COLUMN IF NOT EXISTS pin_attempts INT DEFAULT 0;
    ALTER TABLE public.users ADD COLUMN IF NOT EXISTS pin_locked_until TIMESTAMP WITH TIME ZONE;
    ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS annulment_reason TEXT;
    ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS annulled_by_user_id UUID REFERENCES public.users(id);
    ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS annulled_at TIMESTAMP WITH TIME ZONE;
    ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS client_transaction_uuid UUID UNIQUE;
    ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS latitude NUMERIC(10, 8);
    ALTER TABLE public.payments ADD COLUMN IF NOT EXISTS longitude NUMERIC(11, 8);

    RAISE NOTICE '🎉 Fase 1 completada en todos los schemas';
END $$;