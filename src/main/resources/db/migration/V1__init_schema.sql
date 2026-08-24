-- =============================================================================
-- KREDIO - Migración V1: Esquema inicial (Tablas de Tenant)
-- NOTA: La tabla 'tenants' es GLOBAL y reside en el schema 'public' (gestionada por JPA).
-- =============================================================================

-- 1. TABLA DE USUARIOS
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    failed_login_attempts INT DEFAULT 0,
    account_locked_until TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, email)
);

-- 2. TABLA DE CLIENTES
CREATE TABLE IF NOT EXISTS clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    portfolio_id UUID,
    full_name VARCHAR(150) NOT NULL,
    dpi_or_id VARCHAR(50) NOT NULL,
    email VARCHAR(100),
    address TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. TABLA DE PRÉSTAMOS
CREATE TABLE IF NOT EXISTS loans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    portfolio_id UUID,
    client_id UUID NOT NULL,
    principal_amount NUMERIC(19,4) NOT NULL CHECK (principal_amount > 0),
    interest_rate NUMERIC(5,2) NOT NULL CHECK (interest_rate >= 0),
    rate_type VARCHAR(20) DEFAULT 'ANNUAL',
    payment_frequency VARCHAR(20) DEFAULT 'MONTHLY',
    amortization_method VARCHAR(20) DEFAULT 'FRENCH',
    opening_date DATE NOT NULL,
    term_months INT NOT NULL CHECK (term_months > 0),
    current_balance NUMERIC(19,4) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 4. TABLA DE CUOTAS (Loan Schedule)
CREATE TABLE IF NOT EXISTS loan_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    loan_id UUID NOT NULL,
    installment_number INT NOT NULL CHECK (installment_number > 0),
    due_date DATE NOT NULL,
    expected_amount NUMERIC(19,4) NOT NULL,
    expected_principal NUMERIC(19,4) NOT NULL,
    expected_interest NUMERIC(19,4) NOT NULL,
    paid_date TIMESTAMP WITH TIME ZONE,
    status VARCHAR(20) DEFAULT 'PENDING',
    UNIQUE(loan_id, installment_number)
);

-- 5. TABLA DE PAGOS
CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    loan_id UUID NOT NULL,
    collector_id UUID NOT NULL,
    amount_paid NUMERIC(19,4) NOT NULL CHECK (amount_paid > 0),
    payment_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    method VARCHAR(20) DEFAULT 'CASH',
    is_printed BOOLEAN DEFAULT false,
    synced_from_offline BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 6. TABLA DE AUDITORÍA
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    user_id UUID,
    action VARCHAR(10) NOT NULL,
    table_name VARCHAR(50) NOT NULL,
    record_id UUID NOT NULL,
    old_data JSONB,
    new_data JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- ÍNDICES CRÍTICOS
CREATE INDEX IF NOT EXISTS idx_users_tenant ON users(tenant_id);
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_clients_tenant ON clients(tenant_id);
CREATE INDEX IF NOT EXISTS idx_loans_tenant ON loans(tenant_id);
CREATE INDEX IF NOT EXISTS idx_loans_client ON loans(client_id);
CREATE INDEX IF NOT EXISTS idx_loan_schedules_tenant ON loan_schedules(tenant_id);
CREATE INDEX IF NOT EXISTS idx_loan_schedules_loan ON loan_schedules(loan_id);
CREATE INDEX IF NOT EXISTS idx_loan_schedules_due_date ON loan_schedules(due_date);
CREATE INDEX IF NOT EXISTS idx_payments_tenant ON payments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_payments_loan ON payments(loan_id);
CREATE INDEX IF NOT EXISTS idx_audit_tenant ON audit_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_created ON audit_logs(created_at);