package com.kredio.backend.engine;

/**
 * Política de condonación para pagos anticipados (Método Flat)
 */
public enum EarlyRepaymentPolicy {
    /**
     * Sin condonación: Se exigen las cuotas restantes completas
     * (Capital + Interés total pactado)
     */
    NO_CONDONATION,

    /**
     * Con condonación: Se devenga únicamente el interés del periodo
     * actual y se liquida el saldo de capital vivo pendiente,
     * perdonando los intereses futuros no devengados
     */
    WITH_CONDONATION
}