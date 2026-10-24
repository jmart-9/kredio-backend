-- V36__alter_payment_order_to_varchar.sql
-- Asegura que la columna payment_order exista y sea de tipo VARCHAR(100)

DO $$
DECLARE
    tenant_schema TEXT;
    col_exists BOOLEAN;
    col_type TEXT;
BEGIN
    -- ==========================================
    -- 1. Iterar sobre todos los schemas de tenants
    -- ==========================================
    FOR tenant_schema IN
        SELECT schema_name
        FROM information_schema.schemata
        WHERE schema_name LIKE 'tenant_%'
    LOOP
        -- Verificar si la columna existe
        SELECT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = tenant_schema AND table_name = 'loans' AND column_name = 'payment_order'
        ) INTO col_exists;

        IF col_exists THEN
            -- Verificar su tipo de dato
            SELECT data_type INTO col_type
            FROM information_schema.columns
            WHERE table_schema = tenant_schema AND table_name = 'loans' AND column_name = 'payment_order';

            IF col_type = 'jsonb' THEN
                -- Si es jsonb, la eliminamos y la creamos como varchar
                EXECUTE format('ALTER TABLE %I.loans DROP COLUMN payment_order', tenant_schema);
                EXECUTE format('ALTER TABLE %I.loans ADD COLUMN payment_order VARCHAR(100)', tenant_schema);
            END IF;
        ELSE
            -- Si no existe, la agregamos directamente
            EXECUTE format('ALTER TABLE %I.loans ADD COLUMN payment_order VARCHAR(100)', tenant_schema);
        END IF;

        RAISE NOTICE '✅ Columna payment_order verificada en schema: %', tenant_schema;
    END LOOP;

    -- ==========================================
    -- 2. Aplicar también al schema public
    -- ==========================================
    SELECT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'loans' AND column_name = 'payment_order'
    ) INTO col_exists;

    IF col_exists THEN
        SELECT data_type INTO col_type
        FROM information_schema.columns
        WHERE table_schema = 'public' AND table_name = 'loans' AND column_name = 'payment_order';

        IF col_type = 'jsonb' THEN
            ALTER TABLE public.loans DROP COLUMN payment_order;
            ALTER TABLE public.loans ADD COLUMN payment_order VARCHAR(100);
        END IF;
    ELSE
        -- Aquí es donde fallaba antes: ahora la agregamos si no existe
        ALTER TABLE public.loans ADD COLUMN payment_order VARCHAR(100);
    END IF;

    RAISE NOTICE '🎉 Migración completada: payment_order es VARCHAR(100) en todos los schemas';
END $$;