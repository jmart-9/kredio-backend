-- =============================================================================
-- KREDIO V2 - Arquitectura Completa de Negocio
-- Migración V2: Multi-país, Planes SaaS, Renegociaciones, Permisos Granulares
-- =============================================================================

-- 1. TABLA DE PAÍSES (Multi-país Centroamérica)
CREATE TABLE countries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(3) NOT NULL UNIQUE, -- GT, SV, HN, NI, CR, PA
    name VARCHAR(100) NOT NULL,
    currency_code VARCHAR(3) NOT NULL, -- GTQ, USD, HNL, NIO, CRC
    currency_symbol VARCHAR(5) NOT NULL, -- Q, $, L, C$, ₡
    max_legal_interest_rate NUMERIC(5,2), -- Tasa máxima legal anual (NULL si no aplica)
    date_format VARCHAR(10) DEFAULT 'DD/MM/YYYY',
    number_format VARCHAR(10) DEFAULT '1,000.00',
    id_document_types JSONB DEFAULT '[]'::jsonb, -- ["DUI", "DPI", "Identidad"]
    regulatory_requirements JSONB DEFAULT '{}'::jsonb, -- Requisitos específicos por país
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insertar países de Centroamérica
INSERT INTO countries (code, name, currency_code, currency_symbol, max_legal_interest_rate, id_document_types) VALUES
('GT', 'Guatemala', 'GTQ', 'Q', 60.00, '["DPI", "Pasaporte"]'::jsonb),
('SV', 'El Salvador', 'USD', '$', 30.00, '["DUI", "Pasaporte"]'::jsonb),
('HN', 'Honduras', 'HNL', 'L', 50.00, '["Identidad", "Pasaporte"]'::jsonb),
('NI', 'Nicaragua', 'NIO', 'C$', 40.00, '["Cédula", "Pasaporte"]'::jsonb),
('CR', 'Costa Rica', 'CRC', '₡', 35.00, '["Cédula", "Pasaporte"]'::jsonb),
('PA', 'Panamá', 'USD', '$', 25.00, '["Cédula", "Pasaporte"]'::jsonb);

-- 2. TABLA DE PLANES SaaS
CREATE TABLE saas_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    description TEXT,
    monthly_price NUMERIC(10,2) NOT NULL,
    trial_days INT DEFAULT 0,

    -- Límites configurables
    max_users INT NOT NULL DEFAULT 10,
    max_active_loans INT NOT NULL DEFAULT 100,
    max_portfolio_volume NUMERIC(15,2) NOT NULL DEFAULT 100000.00,
    max_collectors INT NOT NULL DEFAULT 5,
    max_clients INT NOT NULL DEFAULT 500,
    max_custom_roles INT NOT NULL DEFAULT 0,
    max_custom_interest_rates INT NOT NULL DEFAULT 2,

    -- Funcionalidades (JSONB para flexibilidad)
    features JSONB DEFAULT '{}'::jsonb, -- {"reports": true, "api_access": false, etc.}
    support_level VARCHAR(20) DEFAULT 'BASIC', -- BASIC, STANDARD, PREMIUM, 24/7

    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Crear plan Trial por defecto
INSERT INTO saas_plans (name, description, monthly_price, trial_days, max_users, max_active_loans, max_portfolio_volume, max_collectors, max_clients, max_custom_roles, max_custom_interest_rates, features, support_level) VALUES
('Trial', 'Plan de prueba de 15 días', 0.00, 15, 3, 10, 10000.00, 2, 50, 0, 1, '{"reports": false, "api_access": false, "custom_branding": false}'::jsonb, 'BASIC');

-- 3. ACTUALIZAR TABLA TENANTS (Financieras)
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS country_id UUID REFERENCES countries(id);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS currency_code VARCHAR(3) DEFAULT 'USD';
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS currency_symbol VARCHAR(5) DEFAULT '$';
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS plan_id UUID REFERENCES saas_plans(id);
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS plan_start_date DATE;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS plan_end_date DATE;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS is_trial BOOLEAN DEFAULT false;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS payment_method VARCHAR(20); -- CARD, TRANSFER, PAYPAL
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) DEFAULT 'PENDING'; -- PENDING, PAID, OVERDUE
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS notification_90_percent_sent BOOLEAN DEFAULT false;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS custom_branding_enabled BOOLEAN DEFAULT false;
ALTER TABLE tenants ADD COLUMN IF NOT EXISTS logo_url TEXT;




-- 4. TABLA DE SEGUIMIENTO DE USO (Usage Tracking)
CREATE TABLE usage_tracking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    metric_name VARCHAR(50) NOT NULL, -- 'users', 'active_loans', 'portfolio_volume', etc.
    current_value NUMERIC(15,2) NOT NULL DEFAULT 0,
    limit_value NUMERIC(15,2) NOT NULL,
    percentage_used NUMERIC(5,2) NOT NULL DEFAULT 0,
    exceeded BOOLEAN DEFAULT false,
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, metric_name)
);

CREATE INDEX idx_usage_tracking_tenant ON usage_tracking(tenant_id);

-- 5. TABLA DE PERMISOS GRANULARES
CREATE TABLE permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50) NOT NULL, -- USERS, LOANS, COLLECTIONS, CLIENTS, REPORTS, CONFIG
    description TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Insertar permisos base
INSERT INTO permissions (code, name, category, description) VALUES
-- Usuarios
('create_users', 'Crear Usuarios', 'USERS', 'Puede crear nuevos usuarios en la financiera'),
('edit_users', 'Editar Usuarios', 'USERS', 'Puede editar información de usuarios'),
('delete_users', 'Eliminar Usuarios', 'USERS', 'Puede eliminar usuarios'),
('assign_roles', 'Asignar Roles', 'USERS', 'Puede asignar roles a usuarios'),
('view_audit_logs', 'Ver Logs de Auditoría', 'USERS', 'Puede ver el historial de cambios'),

-- Préstamos
('create_loans', 'Crear Préstamos', 'LOANS', 'Puede crear nuevos préstamos'),
('approve_loans', 'Aprobar Préstamos', 'LOANS', 'Puede aprobar préstamos solicitados'),
('reject_loans', 'Rechazar Préstamos', 'LOANS', 'Puede rechazar préstamos'),
('edit_loans', 'Editar Préstamos', 'LOANS', 'Puede modificar préstamos existentes'),
('delete_loans', 'Eliminar Préstamos', 'LOANS', 'Puede eliminar préstamos'),
('renegotiate_loans', 'Renegociar Préstamos', 'LOANS', 'Puede solicitar o aprobar renegociaciones'),
('condone_interest', 'Condonar Intereses', 'LOANS', 'Puede condonar intereses y moratorios (nunca capital)'),

-- Cobranza
('register_payments', 'Registrar Pagos', 'COLLECTIONS', 'Puede registrar pagos de clientes'),
('edit_payments', 'Editar Pagos', 'COLLECTIONS', 'Puede modificar pagos registrados'),
('delete_payments', 'Eliminar Pagos', 'COLLECTIONS', 'Puede eliminar pagos'),
('apply_moratory_interest', 'Aplicar Intereses Moratorios', 'COLLECTIONS', 'Puede configurar y aplicar moratorios'),
('view_payment_history', 'Ver Historial de Pagos', 'COLLECTIONS', 'Puede ver historial completo de pagos'),

-- Clientes
('create_clients', 'Crear Clientes', 'CLIENTS', 'Puede registrar nuevos clientes'),
('edit_clients', 'Editar Clientes', 'CLIENTS', 'Puede modificar información de clientes'),
('delete_clients', 'Eliminar Clientes', 'CLIENTS', 'Puede eliminar clientes'),
('view_client_history', 'Ver Historial Completo', 'CLIENTS', 'Puede ver todo el historial del cliente'),

-- Reportes
('view_dashboard', 'Ver Dashboard', 'REPORTS', 'Puede acceder al dashboard principal'),
('export_excel', 'Exportar a Excel', 'REPORTS', 'Puede exportar datos a Excel'),
('export_pdf', 'Exportar a PDF', 'REPORTS', 'Puede exportar datos a PDF'),
('schedule_reports', 'Programar Reportes', 'REPORTS', 'Puede programar envío automático de reportes'),
('view_fiscal_reports', 'Ver Reportes Fiscales', 'REPORTS', 'Puede ver reportes para entidades regulatorias'),
('view_accounting_reports', 'Ver Reportes Contables', 'REPORTS', 'Puede ver balance, resultados, diario, mayor'),

-- Configuración
('configure_interest_rates', 'Configurar Tasas de Interés', 'CONFIG', 'Puede crear y modificar tasas de interés'),
('configure_payment_methods', 'Configurar Métodos de Pago', 'CONFIG', 'Puede definir métodos de pago aceptados'),
('configure_payment_order', 'Configurar Orden de Pagos', 'CONFIG', 'Puede definir orden de aplicación de pagos parciales'),
('configure_moratory_rates', 'Configurar Tasas Moratorias', 'CONFIG', 'Puede configurar tasas de interés moratorio'),
('manage_plans', 'Gestionar Planes', 'CONFIG', 'Solo Admin Global: puede crear y modificar planes SaaS'),
('manage_global_roles', 'Gestionar Roles Globales', 'CONFIG', 'Solo Admin Global: puede crear roles globales'),
('impersonate_users', 'Suplantar Usuarios', 'CONFIG', 'Solo Admin Global: puede hacer impersonation');

-- 6. TABLA DE ROLES (Ahora con permisos granulares)
CREATE TABLE roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE, -- NULL para roles globales
    name VARCHAR(100) NOT NULL,
    description TEXT,
    is_global BOOLEAN DEFAULT false, -- TRUE si fue creado por Admin Global
    is_system BOOLEAN DEFAULT false, -- TRUE para roles del sistema (ADMIN, COLLECTOR, AUDITOR)
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, name)
);

CREATE INDEX idx_roles_tenant ON roles(tenant_id);
CREATE INDEX idx_roles_global ON roles(is_global);

-- 7. TABLA DE PERMISOS POR ROL (Many-to-Many)
CREATE TABLE role_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    role_id UUID NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    UNIQUE(role_id, permission_id)
);

CREATE INDEX idx_role_permissions_role ON role_permissions(role_id);
CREATE INDEX idx_role_permissions_permission ON role_permissions(permission_id);

-- 8. ACTUALIZAR TABLA USERS (Ahora usa roles personalizados)
ALTER TABLE users ALTER COLUMN role DROP NOT NULL;
ALTER TABLE users ADD COLUMN IF NOT EXISTS role_id UUID REFERENCES roles(id);
ALTER TABLE users ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INT DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS account_locked_until TIMESTAMP WITH TIME ZONE;

-- 9. TABLA DE TASAS DE INTERÉS
CREATE TABLE interest_rates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID REFERENCES tenants(id) ON DELETE CASCADE, -- NULL para tasas globales
    name VARCHAR(100) NOT NULL,
    annual_rate NUMERIC(5,2) NOT NULL CHECK (annual_rate >= 0),
    description TEXT,
    is_global BOOLEAN DEFAULT false,
    is_active BOOLEAN DEFAULT true,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_interest_rates_tenant ON interest_rates(tenant_id);
CREATE INDEX idx_interest_rates_global ON interest_rates(is_global);

-- Insertar tasas globales de ejemplo
INSERT INTO interest_rates (name, annual_rate, description, is_global) VALUES
('Tasa Básica Anual', 12.00, 'Tasa estándar para préstamos personales', true),
('Tasa Preferencial', 8.50, 'Tasa reducida para clientes frecuentes', true),
('Tasa Comercial', 18.00, 'Tasa para préstamos comerciales', true);

-- 10. ACTUALIZAR TABLA LOANS (Préstamos)
ALTER TABLE loans ADD COLUMN IF NOT EXISTS approval_status VARCHAR(20) DEFAULT 'PENDING'; -- PENDING, APPROVED, REJECTED
ALTER TABLE loans ADD COLUMN IF NOT EXISTS approved_by UUID REFERENCES users(id);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS interest_rate_id UUID REFERENCES interest_rates(id);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS is_custom_rate BOOLEAN DEFAULT false;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS moratory_rate_daily NUMERIC(5,4) DEFAULT 0.0010; -- 0.10% diario por defecto
ALTER TABLE loans ADD COLUMN IF NOT EXISTS payment_order JSONB DEFAULT '["MORATORY", "INTEREST", "CAPITAL"]'::jsonb;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS guarantee_description TEXT; -- Textbox para garantías
ALTER TABLE loans ADD COLUMN IF NOT EXISTS references_count INT DEFAULT 0;

-- 11. TABLA DE REFERENCIAS PERSONALES
CREATE TABLE client_references (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    client_id UUID NOT NULL REFERENCES clients(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    full_name VARCHAR(150) NOT NULL,
    phone VARCHAR(20),
    relationship VARCHAR(50), -- Familiar, Amigo, Trabajo, etc.
    address TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_client_references_client ON client_references(client_id);

-- 12. TABLA DE SOLICITUDES DE RENEGOCIACIÓN
CREATE TABLE renegotiation_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    loan_id UUID NOT NULL REFERENCES loans(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    collector_id UUID NOT NULL REFERENCES users(id),

    -- Cambios solicitados (JSONB para flexibilidad)
    requested_changes JSONB NOT NULL, -- {"new_term_months": 24, "new_payment_date": "2026-08-01", "condone_moratory": true}

    -- Estado de la solicitud
    status VARCHAR(20) DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
    admin_reviewer_id UUID REFERENCES users(id),
    review_notes TEXT,
    reviewed_at TIMESTAMP WITH TIME ZONE,

    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_renegotiation_loan ON renegotiation_requests(loan_id);
CREATE INDEX idx_renegotiation_status ON renegotiation_requests(status);

-- 13. TABLA DE MÉTODOS DE PAGO CONFIGURABLES
CREATE TABLE payment_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL, -- Efectivo, Transferencia, Tarjeta
    code VARCHAR(20) NOT NULL, -- CASH, TRANSFER, CARD
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(tenant_id, code)
);

CREATE INDEX idx_payment_methods_tenant ON payment_methods(tenant_id);

-- Insertar métodos de pago por defecto para cada tenant existente
INSERT INTO payment_methods (tenant_id, name, code)
SELECT id, 'Efectivo', 'CASH' FROM tenants;

INSERT INTO payment_methods (tenant_id, name, code)
SELECT id, 'Transferencia Bancaria', 'TRANSFER' FROM tenants;

INSERT INTO payment_methods (tenant_id, name, code)
SELECT id, 'Tarjeta de Débito/Crédito', 'CARD' FROM tenants;

-- 14. ACTUALIZAR TABLA PAYMENTS (Pagos)
ALTER TABLE payments ADD COLUMN IF NOT EXISTS payment_method_id UUID REFERENCES payment_methods(id);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS applied_to JSONB; -- {"moratory": 50.00, "interest": 100.00, "capital": 738.49}
ALTER TABLE payments ADD COLUMN IF NOT EXISTS excess_amount NUMERIC(19,4) DEFAULT 0; -- Pago de más aplicado a capital

-- 15. TABLA DE REPORTES PROGRAMADOS
CREATE TABLE scheduled_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    report_type VARCHAR(50) NOT NULL, -- FISCAL, ACCOUNTING, PORTFOLIO, COLLECTIONS
    frequency VARCHAR(20) NOT NULL, -- DAILY, WEEKLY, MONTHLY, QUARTERLY, ANNUAL, CUSTOM
    custom_days JSONB, -- Para frecuencia personalizada: [1, 15] (días 1 y 15 de cada mes)
    email_recipients JSONB NOT NULL, -- ["admin@financiera.com", "contador@financiera.com"]
    format VARCHAR(10) DEFAULT 'PDF', -- PDF, EXCEL, BOTH
    is_active BOOLEAN DEFAULT true,
    last_sent_at TIMESTAMP WITH TIME ZONE,
    next_scheduled_at TIMESTAMP WITH TIME ZONE,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_scheduled_reports_tenant ON scheduled_reports(tenant_id);

-- 16. TABLA DE LOGS DE IMPERSONATION
CREATE TABLE impersonation_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_global_id UUID NOT NULL REFERENCES users(id),
    impersonated_user_id UUID NOT NULL REFERENCES users(id),
    impersonated_tenant_id UUID NOT NULL REFERENCES tenants(id),
    started_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP WITH TIME ZONE,
    actions_performed JSONB DEFAULT '[]'::jsonb,
    ip_address VARCHAR(45)
);

CREATE INDEX idx_impersonation_admin ON impersonation_logs(admin_global_id);
CREATE INDEX idx_impersonation_tenant ON impersonation_logs(impersonated_tenant_id);

-- 17. TABLA DE NOTIFICACIONES (Para excedentes, trial, etc.)
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id),
    type VARCHAR(50) NOT NULL, -- PLAN_EXCEEDED_90, PLAN_EXCEEDED_100, TRIAL_ENDING, SYNC_FAILED, RENEGOTIATION_APPROVED
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    is_read BOOLEAN DEFAULT false,
    priority VARCHAR(10) DEFAULT 'NORMAL', -- LOW, NORMAL, HIGH, CRITICAL
    metadata JSONB, -- Datos adicionales específicos del tipo de notificación
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    read_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_notifications_tenant ON notifications(tenant_id);
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_unread ON notifications(tenant_id, is_read) WHERE is_read = false;

-- 18. CREAR ROLES DEL SISTEMA
-- Admin Global (Super Admin)
INSERT INTO roles (name, description, is_global, is_system) VALUES
('ADMIN_GLOBAL', 'Administrador global con todos los permisos', true, true),
('OWNER', 'Propietario de la financiera', true, true),
('TECH', 'Soporte técnico del SaaS', true, true);

-- Roles por defecto para financieras
INSERT INTO roles (name, description, is_global, is_system) VALUES
('ADMIN', 'Administrador de la financiera', true, true),
('COLLECTOR', 'Cobrador de campo', true, true),
('AUDITOR', 'Auditor/Contador (solo lectura)', true, true);

-- 19. ASIGNAR PERMISOS A ROLES DEL SISTEMA
-- Admin Global: Todos los permisos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p WHERE r.name = 'ADMIN_GLOBAL';

-- Owner: Casi todos los permisos excepto gestión de planes globales
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'OWNER'
AND p.code NOT IN ('manage_plans', 'manage_global_roles', 'impersonate_users');

-- Tech: Permisos de soporte y lectura
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'TECH'
AND p.code IN ('view_audit_logs', 'view_dashboard', 'view_client_history', 'view_payment_history', 'impersonate_users');

-- Admin de financiera: Permisos operativos completos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN'
AND p.category IN ('USERS', 'LOANS', 'COLLECTIONS', 'CLIENTS', 'REPORTS', 'CONFIG')
AND p.code NOT IN ('manage_plans', 'manage_global_roles', 'impersonate_users');

-- Collector: Permisos de cobranza y lectura de clientes
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'COLLECTOR'
AND p.code IN ('register_payments', 'view_payment_history', 'view_client_history', 'renegotiate_loans');

-- Auditor: Solo lectura de reportes
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'AUDITOR'
AND p.category IN ('REPORTS');

-- 20. ACTUALIZAR USUARIO ADMIN EXISTENTE
-- Asignar rol ADMIN_GLOBAL al usuario que creamos antes
UPDATE users SET role_id = (SELECT id FROM roles WHERE name = 'ADMIN_GLOBAL')
WHERE email = 'admin@kredio.com';

-- 21. CREAR TRIGGER PARA ACTUALIZAR usage_tracking AUTOMÁTICAMENTE
CREATE OR REPLACE FUNCTION update_usage_tracking()
RETURNS TRIGGER AS $$
BEGIN
    -- Actualizar conteo de usuarios
    IF TG_TABLE_NAME = 'users' THEN
        IF TG_OP = 'INSERT' THEN
            INSERT INTO usage_tracking (tenant_id, metric_name, current_value, limit_value, percentage_used)
            VALUES (NEW.tenant_id, 'users', 1, 0, 0)
            ON CONFLICT (tenant_id, metric_name) DO UPDATE
            SET current_value = usage_tracking.current_value + 1,
                last_updated = CURRENT_TIMESTAMP;
        ELSIF TG_OP = 'DELETE' THEN
            UPDATE usage_tracking SET current_value = current_value - 1, last_updated = CURRENT_TIMESTAMP
            WHERE tenant_id = OLD.tenant_id AND metric_name = 'users';
        END IF;
    END IF;

    -- Actualizar conteo de préstamos activos
    IF TG_TABLE_NAME = 'loans' THEN
        IF TG_OP = 'INSERT' AND NEW.status = 'ACTIVE' THEN
            INSERT INTO usage_tracking (tenant_id, metric_name, current_value, limit_value, percentage_used)
            VALUES (NEW.tenant_id, 'active_loans', 1, 0, 0)
            ON CONFLICT (tenant_id, metric_name) DO UPDATE
            SET current_value = usage_tracking.current_value + 1,
                last_updated = CURRENT_TIMESTAMP;
        ELSIF TG_OP = 'UPDATE' THEN
            -- Lógica más compleja para cambios de estado
            NULL; -- Implementar según necesidad
        END IF;
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Aplicar triggers
CREATE TRIGGER trigger_update_usage_users AFTER INSERT OR DELETE ON users FOR EACH ROW EXECUTE FUNCTION update_usage_tracking();
CREATE TRIGGER trigger_update_usage_loans AFTER INSERT OR UPDATE ON loans FOR EACH ROW EXECUTE FUNCTION update_usage_tracking();

-- 22. CREAR FUNCIÓN PARA VERIFICAR LÍMITES DEL PLAN
CREATE OR REPLACE FUNCTION check_plan_limits()
RETURNS TRIGGER AS $$
DECLARE
    plan_limit INT;
    current_usage INT;
    tenant_plan_id UUID;
BEGIN
    -- Obtener el plan del tenant
    SELECT plan_id INTO tenant_plan_id FROM tenants WHERE id = NEW.tenant_id;

    -- Verificar límite de usuarios
    IF TG_TABLE_NAME = 'users' THEN
        SELECT max_users INTO plan_limit FROM saas_plans WHERE id = tenant_plan_id;
        SELECT COUNT(*) INTO current_usage FROM users WHERE tenant_id = NEW.tenant_id;

        IF current_usage >= plan_limit THEN
            RAISE EXCEPTION 'Límite de usuarios alcanzado. Plan actual permite % usuarios.', plan_limit;
        END IF;
    END IF;

    -- Verificar límite de préstamos activos
    IF TG_TABLE_NAME = 'loans' AND NEW.status = 'ACTIVE' THEN
        SELECT max_active_loans INTO plan_limit FROM saas_plans WHERE id = tenant_plan_id;
        SELECT COUNT(*) INTO current_usage FROM loans WHERE tenant_id = NEW.tenant_id AND status = 'ACTIVE';

        IF current_usage >= plan_limit THEN
            RAISE EXCEPTION 'Límite de préstamos activos alcanzado. Plan actual permite % préstamos.', plan_limit;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql SECURITY DEFINER;

-- Aplicar triggers de validación
CREATE TRIGGER trigger_check_user_limits BEFORE INSERT ON users FOR EACH ROW EXECUTE FUNCTION check_plan_limits();
CREATE TRIGGER trigger_check_loan_limits BEFORE INSERT OR UPDATE ON loans FOR EACH ROW EXECUTE FUNCTION check_plan_limits();

-- =============================================================================
-- FIN DE MIGRACIÓN V2
-- =============================================================================