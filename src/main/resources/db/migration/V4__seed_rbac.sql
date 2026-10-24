-- =============================================================================
-- KREDIO V4 - Seed de RBAC (Role-Based Access Control) - 100% Idempotente
-- =============================================================================

-- 1. INSERTAR PERMISOS BASE
INSERT INTO permissions (code, name, category, description) VALUES
('USER:CREATE', 'Crear Usuario', 'USERS', 'Puede crear nuevos usuarios'),
('USER:READ', 'Ver Usuarios', 'USERS', 'Puede ver la lista de usuarios'),
('USER:UPDATE', 'Editar Usuario', 'USERS', 'Puede editar usuarios'),
('USER:DELETE', 'Eliminar Usuario', 'USERS', 'Puede eliminar usuarios'),
('LOAN:CREATE', 'Crear Préstamo', 'LOANS', 'Puede crear nuevos préstamos'),
('LOAN:READ', 'Ver Préstamos', 'LOANS', 'Puede ver préstamos'),
('LOAN:UPDATE', 'Editar Préstamo', 'LOANS', 'Puede editar préstamos'),
('LOAN:PAYMENT', 'Registrar Pago', 'LOANS', 'Puede registrar pagos a préstamos'),
('CLIENT:CREATE', 'Crear Cliente', 'CLIENTS', 'Puede crear nuevos clientes'),
('CLIENT:READ', 'Ver Clientes', 'CLIENTS', 'Puede ver clientes'),
('REPORT:VIEW', 'Ver Reportes', 'REPORTS', 'Puede ver reportes y dashboard'),
('CONFIG:MANAGE', 'Gestionar Configuración', 'CONFIG', 'Puede cambiar configuración del tenant'),
('AUDIT:READ', 'Ver Auditoría', 'AUDIT', 'Puede ver logs y auditoría'),
('GLOBAL:MANAGE', 'Gestión Global', 'GLOBAL', 'Acceso total a todos los tenants')
ON CONFLICT (code) DO NOTHING;

-- 2. INSERTAR ROLES GLOBALES
INSERT INTO roles (tenant_id, name, description, is_global, is_system) VALUES
(NULL, 'ADMIN_GLOBAL', 'Administrador con acceso total a todo el SaaS', true, true),
(NULL, 'TECH', 'Soporte técnico con acceso de lectura y edición limitada', true, true),
(NULL, 'ADMIN_TENANT', 'Administrador de una financiera/tenant específica', true, true),
(NULL, 'COBRADOR', 'Usuario operativo que registra pagos y ve su cartera', true, true),
(NULL, 'AUDITOR', 'Usuario que solo puede ver reportes, logs y auditoría', true, true)
ON CONFLICT (tenant_id, name) DO NOTHING;

-- 3. ASIGNAR PERMISOS A ROLES (Idempotente)
-- Admin Global: Todos los permisos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r CROSS JOIN permissions p WHERE r.name = 'ADMIN_GLOBAL'
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Tech: Permisos de soporte
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'TECH' AND p.code IN ('USER:READ', 'LOAN:READ', 'CLIENT:READ', 'REPORT:VIEW', 'AUDIT:READ')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Admin Tenant: Permisos operativos completos
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'ADMIN_TENANT' AND p.code NOT IN ('GLOBAL:MANAGE')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Cobrador: Permisos de cobranza
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'COBRADOR' AND p.code IN ('CLIENT:READ', 'LOAN:READ', 'LOAN:PAYMENT', 'REPORT:VIEW')
ON CONFLICT (role_id, permission_id) DO NOTHING;

-- Auditor: Solo lectura
INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id FROM roles r, permissions p
WHERE r.name = 'AUDITOR' AND p.category IN ('REPORTS', 'AUDIT')
ON CONFLICT (role_id, permission_id) DO NOTHING;