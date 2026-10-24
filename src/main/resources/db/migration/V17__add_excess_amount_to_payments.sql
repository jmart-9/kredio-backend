-- Agregar columna excess_amount a la tabla payments
ALTER TABLE payments ADD COLUMN IF NOT EXISTS excess_amount DECIMAL(19, 4);

-- Agregar comentarios para documentación
COMMENT ON COLUMN payments.excess_amount IS 'Monto excedente cuando el pago es mayor al saldo pendiente';