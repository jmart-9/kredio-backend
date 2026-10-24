-- =============================================================================
-- KREDIO V2 - Arquitectura Completa de Negocio
-- =============================================================================

-- 1. TABLA DE PAÍSES (Multi-país Centroamérica)
CREATE TABLE IF NOT EXISTS countries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(3) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    currency_code VARCHAR(3) NOT NULL,
    currency_symbol VARCHAR(5) NOT NULL,
    max_legal_interest_rate NUMERIC(5,2),
    date_format VARCHAR(10) DEFAULT 'DD/MM/YYYY',
    number_format VARCHAR(10) DEFAULT '1,000.00',
    id_document_types JSONB DEFAULT '[]'::jsonb,
    regulatory_requirements JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 2. TABLA DE PLANES SaaS
CREATE TABLE IF NOT EXISTS saas_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    monthly_price NUMERIC(10,2) NOT NULL,
    trial_days INT DEFAULT 0,
    max_users INT NOT NULL DEFAULT 10,
    max_active_loans INT NOT NULL DEFAULT 100,
    max_portfolio_volume NUMERIC(15,2) NOT NULL DEFAULT 100000.00,
    max_collectors INT NOT NULL DEFAULT 5,
    max_clients INT NOT NULL DEFAULT 500,
    max_custom_roles INT NOT NULL DEFAULT 0,
    max_custom_interest_rates INT NOT NULL DEFAULT 2,
    features JSONB DEFAULT '{}'::jsonb,
    support_level VARCHAR(20) DEFAULT 'BASIC',
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 3. TABLA DE SEGUIMIENTO DE USO
CREATE TABLE IF NOT EXISTS usage_tracking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    metric_name VARCHAR(50) NOT NULL,
    current_value NUMERIC(15,2) NOT NULL DEFAULT 0,
    limit_value NUMERIC(15,2) NOT NULL,
    percentage_used NUMERIC(5,2) NOT NULL DEFAULT 0,
    exceeded BOOLEAN DEFAULT false,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, metric_name)
);

-- 4. TABLA DE PERMISOS GRANULARES
CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL,
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. TABLA DE ROLES
CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES public.tenants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_global BOOLEAN DEFAULT false,
    is_system BOOLEAN DEFAULT false,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, name)
);

-- 6. TABLA DE PERMISOS POR ROL
CREATE TABLE IF NOT EXISTS role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    UNIQUE(role_id, permission_id)
);

-- 7. TABLA DE TASAS DE INTERÉS
CREATE TABLE IF NOT EXISTS interest_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES public.tenants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    annual_rate NUMERIC(5,2) NOT NULL CHECK (annual_rate >= 0),
    description TEXT,
    is_global BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 8. TABLA DE REFERENCIAS PERSONALES
CREATE TABLE IF NOT EXISTS client_references (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    relationship VARCHAR(50),
    address TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 9. TABLA DE SOLICITUDES DE RENEGOCIACIÓN
CREATE TABLE IF NOT EXISTS renegotiation_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id UUID NOT NULL REFERENCES loans(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    collector_id UUID NOT NULL,
    requested_changes JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    admin_reviewer_id UUID,
    review_notes TEXT,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 10. TABLA DE MÉTODOS DE PAGO CONFIGURABLES
CREATE TABLE IF NOT EXISTS payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    code VARCHAR(20) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, code)
);

-- 11. TABLA DE REPORTES PROGRAMADOS
CREATE TABLE IF NOT EXISTS scheduled_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    report_type VARCHAR(50) NOT NULL,
    frequency VARCHAR(20) NOT NULL,
    custom_days JSONB,
    email_recipients JSONB NOT NULL,
    format VARCHAR(10) DEFAULT 'PDF',
    is_active BOOLEAN DEFAULT true,
    last_sent_at TIMESTAMP WITH TIME ZONE,
    next_scheduled_at TIMESTAMP WITH TIME ZONE,
    created_by UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insertar métodos de pago por defecto para cada tenant existente
INSERT INTO payment_methods (tenant_id, name, code)
SELECT id, 'Efectivo', 'CASH' FROM public.tenants
ON CONFLICT (tenant_id, code) DO NOTHING;

INSERT INTO payment_methods (tenant_id, name, code)
SELECT id, 'Transferencia Bancaria', 'TRANSFER' FROM public.tenants
ON CONFLICT (tenant_id, code) DO NOTHING;

-- 12. TABLA DE LOGS DE IMPERSONATION
CREATE TABLE IF NOT EXISTS impersonation_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_global_id UUID NOT NULL,
    impersonated_user_id UUID NOT NULL,
    impersonated_tenant_id UUID NOT NULL REFERENCES public.tenants(id),
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP WITH TIME ZONE,
    actions_performed JSONB DEFAULT '[]'::jsonb,
    ip_address VARCHAR(45)
);

-- 13. TABLA DE NOTIFICACIONES
CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES public.tenants(id) ON DELETE CASCADE,
    user_id UUID,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT false,
    priority VARCHAR(10) DEFAULT 'NORMAL',
    metadata JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP WITH TIME ZONE
);

-- Índices
CREATE INDEX IF NOT EXISTS idx_usage_tracking_tenant ON usage_tracking(tenant_id);
CREATE INDEX IF NOT EXISTS idx_roles_tenant ON roles(tenant_id);
CREATE INDEX IF NOT EXISTS idx_roles_global ON roles(is_global);
CREATE INDEX IF NOT EXISTS idx_role_permissions_role ON role_permissions(role_id);
CREATE INDEX IF NOT EXISTS idx_role_permissions_permission ON role_permissions(permission_id);
CREATE INDEX IF NOT EXISTS idx_interest_rates_tenant ON interest_rates(tenant_id);
CREATE INDEX IF NOT EXISTS idx_interest_rates_global ON interest_rates(is_global);
CREATE INDEX IF NOT EXISTS idx_client_references_client ON client_references(client_id);
CREATE INDEX IF NOT EXISTS idx_renegotiation_loan ON renegotiation_requests(loan_id);
CREATE INDEX IF NOT EXISTS idx_renegotiation_status ON renegotiation_requests(status);
CREATE INDEX IF NOT EXISTS idx_payment_methods_tenant ON payment_methods(tenant_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_reports_tenant ON scheduled_reports(tenant_id);
CREATE INDEX IF NOT EXISTS idx_impersonation_admin ON impersonation_logs(admin_global_id);
CREATE INDEX IF NOT EXISTS idx_impersonation_tenant ON impersonation_logs(impersonated_tenant_id);
CREATE INDEX IF NOT EXISTS idx_notifications_tenant ON notifications(tenant_id);
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id);
CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications(tenant_id, is_read) WHERE is_read = false;