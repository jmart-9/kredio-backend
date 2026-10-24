-- Migración para agregar campos de amortización a loans
-- Los valores existentes se mapean automáticamente

-- Agregar nuevos campos con valores por defecto
ALTER TABLE loans ADD COLUMN IF NOT EXISTS day_count_convention VARCHAR(20) DEFAULT 'ACTUAL_360';
ALTER TABLE loans ADD COLUMN IF NOT EXISTS early_repayment_policy VARCHAR(30) DEFAULT 'WITH_CONDONATION';
ALTER TABLE loans ADD COLUMN IF NOT EXISTS grace_days INTEGER DEFAULT 0;
ALTER TABLE loans ADD COLUMN IF NOT EXISTS adjust_for_business_days BOOLEAN DEFAULT false;

-- Migrar datos existentes: 'SIMPLE' -> 'FLAT'
UPDATE loans SET amortization_method = 'FLAT' WHERE amortization_method = 'SIMPLE';

-- Agregar comentarios
COMMENT ON COLUMN loans.day_count_convention IS 'Convención de conteo de días: ACTUAL_360, ACTUAL_365, THIRTY_360';
COMMENT ON COLUMN loans.early_repayment_policy IS 'Política de condonación: NO_CONDONATION, WITH_CONDONATION';
COMMENT ON COLUMN loans.grace_days IS 'Días de gracia antes de aplicar intereses moratorios';
COMMENT ON COLUMN loans.adjust_for_business_days IS 'Ajustar fechas de pago por días inhábiles';

-- Crear tabla de historial de condonaciones
CREATE TABLE IF NOT EXISTS condonation_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    loan_id UUID NOT NULL,
    schedule_id UUID,
    payment_id UUID,
    condoned_amount NUMERIC(19,4) NOT NULL DEFAULT 0,
    condoned_interest NUMERIC(19,4) DEFAULT 0,
    condoned_principal NUMERIC(19,4) DEFAULT 0,
    reason TEXT,
    approved_by UUID,
    approved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Índices para la tabla de condonaciones
CREATE INDEX IF NOT EXISTS idx_condonation_loan_id ON condonation_history(loan_id);
CREATE INDEX IF NOT EXISTS idx_condonation_payment_id ON condonation_history(payment_id);

-- Agregar comentarios a la tabla
COMMENT ON TABLE condonation_history IS 'Historial de condonaciones de intereses por pagos anticipados';
COMMENT ON COLUMN condonation_history.condoned_amount IS 'Monto total condonado';
COMMENT ON COLUMN condonation_history.condoned_interest IS 'Interés condonado';
COMMENT ON COLUMN condonation_history.condoned_principal IS 'Capital condonado';