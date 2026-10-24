-- Agregar columna moratory_interest a loan_schedules
ALTER TABLE loan_schedules
ADD COLUMN IF NOT EXISTS moratory_interest DECIMAL(19, 4) DEFAULT 0;

-- Agregar comentario
COMMENT ON COLUMN loan_schedules.moratory_interest IS 'Interés moratorio acumulado por cuota vencida';