package com.jeepclub.backend.billing.core.domain.enums;

public enum PaymentAcceptancePolicy {

    /**
     * Pagamento aceito somente até a data de vencimento.
     *
     * Exemplo:
     * - inscrição de evento
     * - reserva com prazo
     */
    UNTIL_DUE_DATE,

    /**
     * Pagamento aceito mesmo após a data de vencimento, sem limite final.
     *
     * Exemplo:
     * - anuidade
     * - mensalidade
     */
    AFTER_DUE_DATE,

    /**
     * Pagamento aceito por uma quantidade limitada de dias após o vencimento.
     *
     * Exemplo:
     * - renovação com tolerância
     * - taxa com janela de regularização
     */
    UNTIL_DAYS_AFTER_DUE_DATE
}