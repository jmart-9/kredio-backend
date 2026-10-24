-- Agregar columnas faltantes a la tabla loans
ALTER TABLE loans ADD COLUMN IF NOT EXISTS is_custom_rate BOOLEAN DEFAULT false;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS moratory_rate_daily NUMERIC(5,4);
ALTER TABLE loans ADD COLUMN IF NOT EXISTS payment_order TEXT;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS references_count INTEGER DEFAULT 0;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS portfolio_id UUID;

-- Agregar columnas faltantes a la tabla loan_schedules
ALTER TABLE loan_schedules ADD COLUMN IF NOT EXISTS paid_amount NUMERIC(19,4);
ALTER TABLE loan_schedules ADD COLUMN IF NOT EXISTS paid_date TIMESTAMP;

-- Agregar columnas faltantes a la tabla payments
ALTER TABLE payments ADD COLUMN IF NOT EXISTS is_voided BOOLEAN DEFAULT false;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS voided_at TIMESTAMP;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS voided_by UUID;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE payments ADD COLUMN IF NOT EXISTS excess_amount NUMERIC(19,4);
ALTER TABLE payments ADD COLUMN IF NOT EXISTS schedule_id UUID;