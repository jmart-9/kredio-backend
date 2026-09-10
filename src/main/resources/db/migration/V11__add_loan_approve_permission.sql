-- =============================================================================
-- KREDIO V10 - Agregar permiso LOAN:APPROVE para Flujo de Aprobación
-- =============================================================================

-- 1. Insertar el permiso LOAN:APPROVE en la tabla global de permisos (schema public)
INSERT INTO public.permissions (id, code, name, category, description, created_at)
VALUES (
    gen_random_uuid(),
    'LOAN:APPROVE',
    'Aprobar/Rechazar Préstamos',
    'LOANS',
    'Permiso para aprobar o rechazar préstamos pendientes de aprobación',
    CURRENT_TIMESTAMP
)
ON CONFLICT (code) DO NOTHING;

-- 2. Asignar el permiso LOAN:APPROVE al rol ADMIN_TENANT global
-- (Solo si no está ya asignado)
INSERT INTO public.role_permissions (id, role_id, permission_id)
SELECT
    gen_random_uuid(),
    r.id AS role_id,
    p.id AS permission_id
FROM public.roles r
CROSS JOIN public.permissions p
WHERE r.name = 'ADMIN_TENANT'
  AND r.is_global = true
  AND p.code = 'LOAN:APPROVE'
  AND NOT EXISTS (
      SELECT 1 FROM public.role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_id = p.id
  );

-- 3. Replicar el permiso en todos los schemas de tenants existentes
-- (Por si tienen su propia copia local de permissions)
DO $$
DECLARE
    schema_name TEXT;
    perm_id UUID;
    admin_role_id UUID;
BEGIN
    -- Obtener el ID del permiso que acabamos de crear
    SELECT id INTO perm_id FROM public.permissions WHERE code = 'LOAN:APPROVE';

    IF perm_id IS NULL THEN
        RAISE EXCEPTION 'No se encontró el permiso LOAN:APPROVE';
    END IF;

    -- Iterar por todos los schemas de tenants
    FOR schema_name IN
        SELECT nspname
        FROM pg_namespace
        WHERE nspname LIKE 'tenant_%'
    LOOP
        -- Verificar si el schema tiene tabla permissions
        IF EXISTS (
            SELECT 1 FROM information_schema.tables
            WHERE table_schema = schema_name
            AND table_name = 'permissions'
        ) THEN
            -- Insertar el permiso en el schema del tenant (si no existe)
            EXECUTE format(
                'INSERT INTO %I.permissions (id, code, name, category, description, created_at)
                 VALUES (%L, %L, %L, %L, %L, CURRENT_TIMESTAMP)
                 ON CONFLICT (code) DO NOTHING',
                schema_name,
                gen_random_uuid(),
                'LOAN:APPROVE',
                'Aprobar/Rechazar Préstamos',
                'LOANS',
                'Permiso para aprobar o rechazar préstamos pendientes de aprobación'
            );

            -- Obtener el ID del rol ADMIN_TENANT en este tenant (si existe)
            EXECUTE format(
                'SELECT id FROM %I.roles WHERE name = %L AND is_global = true LIMIT 1',
                schema_name,
                'ADMIN_TENANT'
            ) INTO admin_role_id;

            -- Si existe el rol, asignarle el permiso
            IF admin_role_id IS NOT NULL THEN
                EXECUTE format(
                    'INSERT INTO %I.role_permissions (id, role_id, permission_id)
                     SELECT %L, %L, p.id
                     FROM %I.permissions p
                     WHERE p.code = %L
                     AND NOT EXISTS (
                         SELECT 1 FROM %I.role_permissions rp
                         WHERE rp.role_id = %L AND rp.permission_id = p.id
                     )',
                    schema_name,
                    gen_random_uuid(),
                    admin_role_id,
                    schema_name,
                    'LOAN:APPROVE',
                    schema_name,
                    admin_role_id
                );
            END IF;
        END IF;
    END LOOP;

    RAISE NOTICE '✅ Permiso LOAN:APPROVE agregado correctamente en todos los schemas';
END $$;