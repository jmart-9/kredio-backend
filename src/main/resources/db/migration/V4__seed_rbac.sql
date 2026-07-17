-- 1. Insertar Permisos Base
INSERT INTO permissions (id, code, name, category, description) VALUES
(gen_random_uuid(), 'USER:CREATE', 'Crear Usuario', 'USERS', 'Puede crear nuevos usuarios'),
(gen_random_uuid(), 'USER:READ', 'Ver Usuarios', 'USERS', 'Puede ver la lista de usuarios'),
(gen_random_uuid(), 'USER:UPDATE', 'Editar Usuario', 'USERS', 'Puede editar usuarios'),
(gen_random_uuid(), 'USER:DELETE', 'Eliminar Usuario', 'USERS', 'Puede eliminar usuarios'),
(gen_random_uuid(), 'LOAN:CREATE', 'Crear Préstamo', 'LOANS', 'Puede crear nuevos préstamos'),
(gen_random_uuid(), 'LOAN:READ', 'Ver Préstamos', 'LOANS', 'Puede ver préstamos'),
(gen_random_uuid(), 'LOAN:UPDATE', 'Editar Préstamo', 'LOANS', 'Puede editar préstamos'),
(gen_random_uuid(), 'LOAN:PAYMENT', 'Registrar Pago', 'LOANS', 'Puede registrar pagos a préstamos'),
(gen_random_uuid(), 'CLIENT:CREATE', 'Crear Cliente', 'CLIENTS', 'Puede crear nuevos clientes'),
(gen_random_uuid(), 'CLIENT:READ', 'Ver Clientes', 'CLIENTS', 'Puede ver clientes'),
(gen_random_uuid(), 'REPORT:VIEW', 'Ver Reportes', 'REPORTS', 'Puede ver reportes y dashboard'),
(gen_random_uuid(), 'CONFIG:MANAGE', 'Gestionar Configuración', 'CONFIG', 'Puede cambiar configuración del tenant'),
(gen_random_uuid(), 'AUDIT:READ', 'Ver Auditoría', 'AUDIT', 'Puede ver logs y auditoría'),
(gen_random_uuid(), 'GLOBAL:MANAGE', 'Gestión Global', 'GLOBAL', 'Acceso total a todos los tenants y configuración');

-- 2. Insertar Roles Globales (tenant_id = NULL)
-- Nota: Reemplaza los UUIDs de permission_id con los generados arriba en tu ejecución real,
-- o usa una consulta CTE para hacerlo dinámico. Aquí usamos una aproximación segura:

-- Rol: Admin Global (Tiene todos los permisos, incluyendo GLOBAL:MANAGE)
INSERT INTO roles (id, tenant_id, name, description, is_global, is_system)
VALUES (gen_random_uuid(), NULL, 'ADMIN_GLOBAL', 'Administrador con acceso total a todo el SaaS', true, true);

-- Rol: Tech Support
INSERT INTO roles (id, tenant_id, name, description, is_global, is_system)
VALUES (gen_random_uuid(), NULL, 'TECH', 'Soporte técnico con acceso de lectura y edición limitada', true, true);

-- Rol: Admin Tenant (Se creará por defecto para cada tenant, pero definimos la plantilla global)
INSERT INTO roles (id, tenant_id, name, description, is_global, is_system)
VALUES (gen_random_uuid(), NULL, 'ADMIN_TENANT', 'Administrador de una financiera/tenant específica', true, true);

-- Rol: Cobrador/Cajero
INSERT INTO roles (id, tenant_id, name, description, is_global, is_system)
VALUES (gen_random_uuid(), NULL, 'COBRADOR', 'Usuario operativo que registra pagos y ve su cartera', true, true);

-- Rol: Auditor
INSERT INTO roles (id, tenant_id, name, description, is_global, is_system)
VALUES (gen_random_uuid(), NULL, 'AUDITOR', 'Usuario que solo puede ver reportes, logs y auditoría', true, true);