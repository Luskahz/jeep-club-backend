# Glossário do Billing

| Termo | Significado atual |
|---|---|
| `ChargeDefinition` | Configuração reutilizável para cobranças futuras. |
| `ChargeAssignment` | Regra ativa/inativa que seleciona o público de uma definição. |
| `ChargeCycle` | Lote histórico gerado com snapshots da definição. |
| `MemberCharge` | Dívida individual criada para um usuário em um ciclo. |
| `MemberPayment` | Submissão de pagamento e comprovante vinculada a uma cobrança. |
| `MemberRefund` | Processo de devolução vinculado a pagamento, cobrança e ciclo. |
| snapshot | Valor copiado no momento da geração e imune a edições posteriores da definição. |
| status persistido | Estado gravado diretamente (`PENDING`, `PAID`, `CANCELED` na cobrança). |
| status efetivo | Estado calculado na consulta (`OVERDUE` e `EXPIRED`, além dos persistidos). |
| `dueDate` | Data de vencimento copiada do ciclo para a cobrança. |
| `paymentAllowedUntil` | Último dia aceito; nulo em `AFTER_DUE_DATE`. |
| editable payment | Pagamento `PENDING_VALIDATION` ou `REJECTED`. |
| refund ativo | Refund `ELIGIBLE`, `REQUESTED` ou `APPROVED`. |
| `receiptStorageKey` | Identidade técnica interna do comprovante no `FileStorage`. |
| `receiptUrl` | Rota HTTP lógica derivada do `paymentId`, não uma URL física. |

## Vocabulários fechados

- definição: `ACTIVE`, `INACTIVE`, `ARCHIVED`;
- audience: `ALL_MEMBERS`, `USER`, `ROLE`, `EVENT_PARTICIPANTS`;
- ciclo: `GENERATED`, `CANCELED`, `FINISHED`, `ARCHIVED`;
- cobrança persistida: `PENDING`, `PAID`, `CANCELED`;
- cobrança efetiva: `PENDING`, `OVERDUE`, `EXPIRED`, `PAID`, `CANCELED`;
- pagamento: `PENDING_VALIDATION`, `CONFIRMED`, `REJECTED`, `CANCELED`;
- refund: `ELIGIBLE`, `REQUESTED`, `APPROVED`, `REJECTED`, `REFUNDED`,
  `EXPIRED`, `CANCELED`;
- recorrência: `ONE_TIME`, `MONTHLY`, `YEARLY`;
- aceitação: `UNTIL_DUE_DATE`, `AFTER_DUE_DATE`,
  `UNTIL_DAYS_AFTER_DUE_DATE`.
