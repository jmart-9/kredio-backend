-- Agregar columna moratory_interest a todos los schemas de tenants
DO $$
DECLARE
    schema_record RECORD;
BEGIN
    -- Recorrer todos los schemas que empiecen con 'tenant_'
    FOR schema_record IN
        SELECT schema_name
        FROM information_schema.schemata
        WHERE schema_name LIKE 'tenant_%'
    LOOP
        -- Verificar si la tabla loan_schedules existe en este schema
        IF EXISTS (
            SELECT 1
            FROM information_schema.tables
            WHERE table_schema = schema_record.schema_name
            AND table_name = 'loan_schedules'
        ) THEN
            -- Agregar la columna si no existe
            EXECUTE format(
                'ALTER TABLE %I.loan_schedules
                 ADD COLUMN IF NOT EXISTS moratory_interest DECIMAL(19, 4) DEFAULT 0',
                schema_record.schema_name
            );

            -- Agregar comentario
            EXECUTE format(
                'COMMENT ON COLUMN %I.loan_schedules.moratory_interest IS ''Interés moratorio acumulado por cuota vencida''',
                schema_record.schema_name
            );

            RAISE NOTICE '✅ Columna moratory_interest agregada en schema: %', schema_record.schema_name;
        END IF;
    END LOOP;

    RAISE NOTICE '🎉 Proceso completado';
END $$;