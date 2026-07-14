-- =============================================================================
-- KREDIO - Esquema de Base de Datos Multi-Tenant (PostgreSQL 16+)
-- Migración V1: Esquema inicial
-- =============================================================================

-- 1. CONFIGURACIÓN INICIAL
SET search_path TO public;

-- =============================================================================
-- 2. TABLAS GLOBALES (Sin tenant_id)
-- =============================================================================

CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    subdomain VARCHAR(50) UNIQUE NOT NULL, -- ej: financiera-x.kredio.com
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'SUSPENDED', 'CANCELLED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 3. TABLAS MULTI-TENANT (Todas con tenant_id)
-- =============================================================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    role VARCHAR(30) NOT NULL CHECK (role IN ('ADMIN', 'COLLECTOR', 'AUDITOR')),
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, email)
);

CREATE TABLE clients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    full_name VARCHAR(150) NOT NULL,
    dpi_or_id VARCHAR(50) NOT NULL, -- DPI, DUI, Cédula, etc.
    phone VARCHAR(20),
    address TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE loans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    client_id UUID NOT NULL REFERENCES clients(id),
    principal_amount NUMERIC(19,4) NOT NULL CHECK (principal_amount > 0),
    interest_rate NUMERIC(5,2) NOT NULL CHECK (interest_rate >= 0), -- Ej: 5.50
    term_months INT NOT NULL CHECK (term_months > 0),
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'ACTIVE', 'PAID', 'DEFAULTED')),
    current_balance NUMERIC(19,4) NOT NULL CHECK (current_balance >= 0),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE loan_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    loan_id UUID NOT NULL REFERENCES loans(id) ON DELETE CASCADE,
    installment_number INT NOT NULL CHECK (installment_number > 0),
    due_date DATE NOT NULL,
    expected_amount NUMERIC(19,4) NOT NULL CHECK (expected_amount >= 0),
    expected_principal NUMERIC(19,4) NOT NULL CHECK (expected_principal >= 0),
    expected_interest NUMERIC(19,4) NOT NULL CHECK (expected_interest >= 0),
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'PAID', 'OVERDUE')),
    UNIQUE(loan_id, installment_number)
);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    loan_id UUID NOT NULL REFERENCES loans(id),
    collector_id UUID NOT NULL REFERENCES users(id),
    amount_paid NUMERIC(19,4) NOT NULL CHECK (amount_paid > 0),
    payment_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    method VARCHAR(20) DEFAULT 'CASH' CHECK (method IN ('CASH', 'TRANSFER', 'OTHER')),
    is_printed BOOLEAN DEFAULT false, -- Controla si se imprimió el ticket físico
    synced_from_offline BOOLEAN DEFAULT false,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id UUID, -- Puede ser NULL si fue una acción del sistema
    action VARCHAR(10) NOT NULL CHECK (action IN ('INSERT', 'UPDATE', 'DELETE')),
    table_name VARCHAR(50) NOT NULL,
    record_id UUID NOT NULL,
    old_data JSONB,
    new_data JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- 4. ÍNDICES CRÍTICOS (Rendimiento Multi-Tenant)
-- =============================================================================

CREATE INDEX idx_users_tenant ON users(tenant_id);
CREATE INDEX idx_clients_tenant ON clients(tenant_id);
CREATE INDEX idx_clients_search ON clients(tenant_id, dpi_or_id); -- Búsqueda rápida por DPI
CREATE INDEX idx_loans_tenant_status ON loans(tenant_id, status);
CREATE INDEX idx_loans_client ON loans(tenant_id, client_id);
CREATE INDEX idx_payments_tenant_loan ON payments(tenant_id, loan_id);
CREATE INDEX idx_payments_date ON payments(tenant_id, payment_date);
CREATE INDEX idx_audit_tenant_time ON audit_logs(tenant_id, created_at DESC);

-- =============================================================================
-- 5. ROW-LEVEL SECURITY (RLS) - EL ESCUDO INVISIBLE
-- =============================================================================

-- Habilitar RLS en todas las tablas multi-tenant
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE clients ENABLE ROW LEVEL SECURITY;
ALTER TABLE loans ENABLE ROW LEVEL SECURITY;
ALTER TABLE loan_schedules ENABLE ROW LEVEL SECURITY;
ALTER TABLE payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE audit_logs ENABLE ROW LEVEL SECURITY;

-- Política de aislamiento: La DB filtra automáticamente por tenant_id
CREATE POLICY tenant_isolation_policy ON users FOR ALL USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
CREATE POLICY tenant_isolation_policy ON clients FOR ALL USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
CREATE POLICY tenant_isolation_policy ON loans FOR ALL USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
CREATE POLICY tenant_isolation_policy ON loan_schedules FOR ALL USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
CREATE POLICY tenant_isolation_policy ON payments FOR ALL USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
CREATE POLICY tenant_isolation_policy ON audit_logs FOR ALL USING (tenant_id = current_setting('app.current_tenant', true)::uuid);

-- Política especial para audit_logs: SOLO INSERTAR (Append-Only Inmutable)
CREATE POLICY audit_append_only ON audit_logs FOR INSERT WITH CHECK (true);

-- =============================================================================
-- 6. TRIGGER AUTOMÁTICO DE AUDITORÍA
-- =============================================================================

CREATE OR REPLACE FUNCTION audit_trigger_func()
RETURNS TRIGGER AS $$
BEGIN
    IF (TG_OP = 'DELETE') THEN
        INSERT INTO audit_logs (tenant_id, user_id, action, table_name, record_id, old_data, new_data)
        VALUES (OLD.tenant_id, current_setting('app.current_user_id', true)::uuid, 'DELETE', TG_TABLE_NAME, OLD.id, to_jsonb(OLD), NULL);
        RETURN OLD;
    ELSIF (TG_OP = 'UPDATE') THEN
        INSERT INTO audit_logs (tenant_id, user_id, action, table_name, record_id, old_data, new_data)
        VALUES (NEW.tenant_id, current_setting('app.current_user_id', true)::uuid, 'UPDATE', TG_TABLE_NAME, NEW.id, to_jsonb(OLD), to_jsonb(NEW));
        RETURN NEW;
    ELSIF (TG_OP = 'INSERT') THEN
        INSERT INTO audit_logs (tenant_id, user_id, action, table_name, record_id, old_data, new_data)
        VALUES (NEW.tenant_id, current_setting('app.current_user_id', true)::uuid, 'INSERT', TG_TABLE_NAME, NEW.id, NULL, to_jsonb(NEW));
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Aplicar el trigger a las tablas críticas financieras
CREATE TRIGGER audit_loans_trigger AFTER INSERT OR UPDATE OR DELETE ON loans FOR EACH ROW EXECUTE FUNCTION audit_trigger_func();
CREATE TRIGGER audit_payments_trigger AFTER INSERT OR UPDATE OR DELETE ON payments FOR EACH ROW EXECUTE FUNCTION audit_trigger_func();