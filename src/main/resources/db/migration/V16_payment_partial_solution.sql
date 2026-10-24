-- Agregar columna excess_amount a la tabla payments
ALTER TABLE payments
ADD COLUMN IF NOT EXISTS excess_amount DECIMAL(10,2) DEFAULT 0;

-- También agregar a las tablas de tenant si existen
DO $$
DECLARE
    schema_name TEXT;
BEGIN
    FOR schema_name IN
        SELECT schema_name
        FROM information_schema.schemata
        WHERE schema_name LIKE 'tenant_%'
    LOOP
        EXECUTE format(
            'ALTER TABLE %I.payments ADD COLUMN IF NOT EXISTS excess_amount DECIMAL(10,2) DEFAULT 0',
            schema_name
        );
    END LOOP;
END$$;