-- Agregar campos de condonación a loan_schedules
ALTER TABLE loan_schedules ADD COLUMN IF NOT EXISTS is_condonated BOOLEAN DEFAULT FALSE;
ALTER TABLE loan_schedules ADD COLUMN IF NOT EXISTS condonated_amount NUMERIC(19,4);
ALTER TABLE loan_schedules ADD COLUMN IF NOT EXISTS condonated_interest NUMERIC(19,4);
ALTER TABLE loan_schedules ADD COLUMN IF NOT EXISTS condonated_at TIMESTAMP;
ALTER TABLE loan_schedules ADD COLUMN IF NOT EXISTS condonated_by UUID;
ALTER TABLE loan_schedules ADD COLUMN IF NOT EXISTS adjusted_principal NUMERIC(19,4);
ALTER TABLE loan_schedules ADD COLUMN IF NOT EXISTS adjusted_interest NUMERIC(19,4);
ALTER TABLE loan_schedules ADD COLUMN IF NOT EXISTS adjusted_amount NUMERIC(19,4);

-- Índices para mejorar rendimiento
CREATE INDEX IF NOT EXISTS idx_loan_schedules_condonated ON loan_schedules(is_condonated);
CREATE INDEX IF NOT EXISTS idx_loan_schedules_loan_status ON loan_schedules(loan_id, status);