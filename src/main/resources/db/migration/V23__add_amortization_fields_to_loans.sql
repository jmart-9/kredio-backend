-- Migración V23: Agregar campos de configuración de amortización a la tabla loans
ALTER TABLE loans
ADD COLUMN IF NOT EXISTS day_count_convention VARCHAR(20) DEFAULT 'ACTUAL_360',
ADD COLUMN IF NOT EXISTS early_repayment_policy VARCHAR(30) DEFAULT 'WITH_CONDONATION',
ADD COLUMN IF NOT EXISTS grace_days INTEGER DEFAULT 0,
ADD COLUMN IF NOT EXISTS adjust_for_business_days BOOLEAN DEFAULT false;

COMMENT ON COLUMN loans.day_count_convention IS 'Convención de conteo de días: ACTUAL_360, ACTUAL_365, THIRTY_360';
COMMENT ON COLUMN loans.early_repayment_policy IS 'Política de prepago: NO_CONDONATION, WITH_CONDONATION';
COMMENT ON COLUMN loans.grace_days IS 'Días de gracia antes de aplicar mora';
COMMENT ON COLUMN loans.adjust_for_business_days IS 'Ajustar fechas de vencimiento a días hábiles';