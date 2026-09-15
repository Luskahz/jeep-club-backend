# Modelos de estado do Billing

## ChargeDefinition

```text
create -> ACTIVE <-> INACTIVE
             \         /
              -> ARCHIVED (terminal)
```

Ativar/desativar no mesmo estado é aceito e apenas reafirma o estado. Operações
de atualização ou lifecycle sobre `ARCHIVED` geram conflito.

## ChargeAssignment

A atribuição usa o booleano persistido `active`, não um enum.

```text
create -> active=true --deactivate--> active=false --activate--> active=true
```

Repetir a transição para o mesmo valor gera conflito. A definição limita as
transições conforme descrito em [Regras de negócio](business-rules.md).

## ChargeCycle

```text
generate -> GENERATED --finish--> FINISHED --archive--> ARCHIVED
                     \--cancel--> CANCELED --archive--> ARCHIVED
```

Cancelar um ciclo já cancelado tem erro específico; qualquer transição fora dos
caminhos acima é conflito.

## MemberCharge

Estados persistidos:

```text
create -> PENDING --confirm payment--> PAID
                 \--cancel----------> CANCELED
```

Estados efetivos de leitura:

- `PENDING`: aberta e ainda no prazo;
- `OVERDUE`: aberta, após o vencimento e ainda aceitável;
- `EXPIRED`: aberta, depois de `paymentAllowedUntil`;
- `PAID` e `CANCELED`: refletem o estado persistido.

`AFTER_DUE_DATE` deixa `paymentAllowedUntil` nulo e, portanto, não produz
`EXPIRED`.

## MemberPayment

```text
submit -> PENDING_VALIDATION --confirm--> CONFIRMED
                           \--reject---> REJECTED --resubmit--> PENDING_VALIDATION
PENDING_VALIDATION ou REJECTED --cancel (interno)--> CANCELED
```

O agregado possui `cancel`, mas nenhum application service ou endpoint o invoca
no estado atual.

## MemberRefund

```text
cycle cancel -> ELIGIBLE --request--> REQUESTED --approve--> APPROVED --mark--> REFUNDED
                    |                    \--reject--> REJECTED
                    \--approve---------> APPROVED
                    \--expire----------> EXPIRED

member request ------------------------> REQUESTED
ELIGIBLE | REQUESTED | APPROVED --cancel--> CANCELED
```

`OVERDUE` e `EXPIRED` de cobrança são calculados. `EXPIRED` de refund é
persistido após comando administrativo explícito.
