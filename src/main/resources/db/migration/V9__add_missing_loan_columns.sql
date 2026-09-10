-- =============================================================================
-- KREDIO V5 - Crear tabla loans y columnas relacionadas (Idempotente)
-- =============================================================================

-- Crear tabla loans si no existe
CREATE TABLE IF NOT EXISTS loans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    client_id UUID NOT NULL,
    principal_amount NUMERIC(19,4) NOT NULL,
    interest_rate NUMERIC(5,2) NOT NULL,
    term_months INTEGER NOT NULL,
    opening_date DATE,
    status VARCHAR(20) NOT NULL,
    current_balance NUMERIC(19,4) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Agregar índices si no existen
CREATE INDEX IF NOT EXISTS idx_loans_tenant_id ON loans(tenant_id);
CREATE INDEX IF NOT EXISTS idx_loans_client_id ON loans(client_id);
CREATE INDEX IF NOT EXISTS idx_loans_status ON loans(status);

-- Agregar columnas adicionales si no existen (campos que usa Loan.java)
ALTER TABLE loans ADD COLUMN IF NOT EXISTS interest_rate_id UUID;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS guarantee_description TEXT;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS approval_status VARCHAR(50);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS approved_by UUID;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS rate_type VARCHAR(20) DEFAULT 'ANNUAL';
ALTER TABLE loans ADD COLUMN IF NOT EXISTS payment_frequency VARCHAR(20) DEFAULT 'MONTHLY';
ALTER TABLE loans ADD COLUMN IF NOT EXISTS amortization_method VARCHAR(50) DEFAULT 'FRENCH';
ALTER TABLE loans ADD COLUMN IF NOT EXISTS is_custom_rate BOOLEAN;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS moratory_rate_daily NUMERIC(5,4);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS payment_order JSONB;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS references_count INTEGER;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS portfolio_id UUID;

-- Crear tabla loan_schedules si no existe
CREATE TABLE IF NOT EXISTS loan_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    loan_id UUID NOT NULL,
    installment_number INTEGER NOT NULL,
    due_date DATE NOT NULL,
    expected_amount NUMERIC(19,4) NOT NULL,
    expected_principal NUMERIC(19,4) NOT NULL,
    expected_interest NUMERIC(19,4) NOT NULL,
    paid_date DATE,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Agregar índices para loan_schedules
CREATE INDEX IF NOT EXISTS idx_loan_schedules_tenant_id ON loan_schedules(tenant_id);
CREATE INDEX IF NOT EXISTS idx_loan_schedules_loan_id ON loan_schedules(loan_id);
CREATE INDEX IF NOT EXISTS idx_loan_schedules_status ON loan_schedules(status);
CREATE INDEX IF NOT EXISTS idx_loan_schedules_due_date ON loan_schedules(due_date);

-- Crear tabla payments si no existe
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    loan_id UUID NOT NULL,
    schedule_id UUID,
    collector_id UUID NOT NULL,
    payment_method_id UUID NOT NULL,
    amount NUMERIC(19,4) NOT NULL,
    payment_date DATE NOT NULL,
    is_printed BOOLEAN DEFAULT false,
    synced_from_offline BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Agregar índices para payments
CREATE INDEX IF NOT EXISTS idx_payments_tenant_id ON payments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payments_loan_id ON payments(loan_id);
CREATE INDEX IF NOT EXISTS idx_payments_collector_id ON payments(collector_id);
CREATE INDEX IF NOT EXISTS idx_payments_payment_date ON payments(payment_date);

-- Mensaje de confirmación
DO $$
BEGIN
    RAISE NOTICE '✅ Tablas loans, loan_schedules y payments creadas/actualizadas correctamente';
END $$;