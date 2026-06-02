# Modelos de Estado do Billing

## Objetivo

Este documento descreve os estados das principais entidades do módulo `billing`.

Ele deve ser usado como referência para:

* regras de domínio;
* transições permitidas;
* validações de service;
* testes;
* documentação de API;
* implementação de persistência.

## Visão geral

As principais entidades com estado no Billing são:

```text id="ly9ca7"
ChargeDefinition
ChargeAssignment
ChargeCycle
MemberCharge
MemberPayment
MemberRefund
```

Cada entidade possui um modelo de estado próprio.

Alguns estados são persistidos no banco.

Outros são calculados em runtime.

## ChargeDefinitionStatus

## Estados

```text id="5mle0h"
ACTIVE
INACTIVE
ARCHIVED
```

## Significado dos estados

### ACTIVE

A definição está ativa.

Ela pode:

* receber atribuições;
* gerar ciclos;
* ser usada em novas cobranças futuras.

### INACTIVE

A definição está temporariamente desativada.

Ela não deve ser usada para novos ciclos.

Ela continua disponível para consulta e histórico.

### ARCHIVED

A definição foi arquivada.

Ela permanece disponível para histórico, mas não deve voltar ao fluxo normal de uso.

## Transições permitidas

```text id="na1k57"
ACTIVE -> INACTIVE
INACTIVE -> ACTIVE

ACTIVE -> ARCHIVED
INACTIVE -> ARCHIVED
```

## Transições não permitidas

```text id="s2rp0q"
ARCHIVED -> ACTIVE
ARCHIVED -> INACTIVE
```

## Observações

Arquivar uma definição não altera:

* ciclos já gerados;
* cobranças já criadas;
* pagamentos já enviados;
* reembolsos já criados.

## ChargeAssignmentStatus

## Estados

```text id="fz54mr"
ACTIVE
INACTIVE
```

## Significado dos estados

### ACTIVE

A atribuição está ativa e será considerada na geração de novos ciclos.

### INACTIVE

A atribuição está desativada e não será considerada na geração de novos ciclos.

## Transições permitidas

```text id="md36pe"
ACTIVE -> INACTIVE
INACTIVE -> ACTIVE
```

## Regras de transição

Uma atribuição só pode ser ativada se a `ChargeDefinition` vinculada estiver ativa.

Uma atribuição não deve ser alterada quando a `ChargeDefinition` vinculada estiver arquivada.

## Observações

Ativar ou desativar uma atribuição não altera ciclos já gerados.

A atribuição só influencia novos ciclos.

## ChargeCycleStatus

## Estados

```text id="qzd4re"
GENERATED
FINISHED
CANCELED
ARCHIVED
```

## Significado dos estados

### GENERATED

O ciclo foi gerado.

As cobranças individuais foram criadas.

O ciclo ainda pode ser:

* finalizado;
* cancelado.

### FINISHED

O ciclo foi finalizado administrativamente.

Finalizar um ciclo não produz efeito financeiro nas cobranças individuais.

### CANCELED

O ciclo foi cancelado.

Cancelar um ciclo é um evento financeiro.

O cancelamento deve:

* cancelar cobranças abertas vinculadas;
* gerar elegibilidade de reembolso para pagamentos elegíveis.

### ARCHIVED

O ciclo foi arquivado para organização histórica.

Arquivar não produz efeito financeiro.

## Transições permitidas

```text id="ww7l6r"
GENERATED -> FINISHED
GENERATED -> CANCELED

FINISHED -> ARCHIVED
CANCELED -> ARCHIVED
```

## Transições não permitidas

```text id="n6nqcj"
FINISHED -> CANCELED
CANCELED -> FINISHED
ARCHIVED -> GENERATED
ARCHIVED -> FINISHED
ARCHIVED -> CANCELED
```

## Diagrama

```text id="r86cua"
             ┌───────────┐
             │ GENERATED │
             └─────┬─────┘
                   │
        ┌──────────┴──────────┐
        │                     │
        ▼                     ▼
   ┌──────────┐          ┌──────────┐
   │ FINISHED │          │ CANCELED │
   └────┬─────┘          └────┬─────┘
        │                     │
        └──────────┬──────────┘
                   ▼
             ┌──────────┐
             │ ARCHIVED │
             └──────────┘
```

## Observações

Finalizar ciclo não altera `MemberCharge`.

Cancelar ciclo altera cobranças abertas e pode gerar `MemberRefund`.

Arquivar ciclo não altera cobranças, pagamentos ou reembolsos.

## MemberChargeStatus

## Tipo

Status persistido.

## Estados

```text id="x75n0z"
PENDING
PAID
CANCELED
```

## Significado dos estados

### PENDING

A cobrança está aberta.

Ela ainda não foi paga nem cancelada.

Uma cobrança `PENDING` pode estar efetivamente:

* pendente;
* vencida;
* expirada.

Essa variação não é persistida no status principal.

### PAID

A cobrança foi paga.

Esse estado é alcançado quando um `MemberPayment` é confirmado.

### CANCELED

A cobrança foi cancelada.

Cobrança cancelada não pode ser paga.

## Transições permitidas

```text id="d60gmb"
PENDING -> PAID
PENDING -> CANCELED
```

## Transições não permitidas

```text id="v3r2c5"
PAID -> PENDING
PAID -> CANCELED

CANCELED -> PENDING
CANCELED -> PAID
```

## Diagrama

```text id="o0lpqf"
           ┌─────────┐
           │ PENDING │
           └────┬────┘
                │
        ┌───────┴───────┐
        │               │
        ▼               ▼
   ┌────────┐      ┌──────────┐
   │  PAID  │      │ CANCELED │
   └────────┘      └──────────┘
```

## Observações

`OVERDUE` e `EXPIRED` não fazem parte de `MemberChargeStatus`.

Eles são calculados em `MemberChargeEffectiveStatus`.

## MemberChargeEffectiveStatus

## Tipo

Status calculado.

## Estados

```text id="xb8p12"
PENDING
OVERDUE
EXPIRED
PAID
CANCELED
```

## Significado dos estados

### PENDING

A cobrança está aberta e ainda não venceu.

### OVERDUE

A cobrança está aberta, passou da data de vencimento, mas ainda aceita pagamento.

### EXPIRED

A cobrança está aberta, mas não aceita mais pagamento.

### PAID

A cobrança foi paga.

### CANCELED

A cobrança foi cancelada.

## Regras de cálculo

```text id="puzw82"
Se MemberChargeStatus = PAID:
    effectiveStatus = PAID

Se MemberChargeStatus = CANCELED:
    effectiveStatus = CANCELED

Se MemberChargeStatus = PENDING
e paymentAllowedUntil não é nulo
e data atual passou de paymentAllowedUntil:
    effectiveStatus = EXPIRED

Se MemberChargeStatus = PENDING
e data atual passou de dueDate
e a cobrança ainda aceita pagamento:
    effectiveStatus = OVERDUE

Caso contrário:
    effectiveStatus = PENDING
```

## Observações

O status efetivo deve ser calculado no momento da consulta ou da execução de regra que precise dele.

Ele não deve ser persistido.

Ele não deve depender de scheduler para ficar correto.

## MemberPaymentStatus

## Estados

```text id="tv1b84"
PENDING_VALIDATION
CONFIRMED
REJECTED
CANCELED
```

## Significado dos estados

### PENDING_VALIDATION

O membro enviou um pagamento e ele aguarda validação administrativa.

### CONFIRMED

O pagamento foi confirmado por um administrador.

Quando isso acontece, a `MemberCharge` vinculada deve ser marcada como `PAID`.

### REJECTED

O pagamento foi rejeitado por um administrador.

O membro pode corrigir esse pagamento via `PUT`, desde que a cobrança ainda aceite nova submissão.

### CANCELED

O pagamento foi cancelado.

Pagamentos confirmados não podem ser cancelados pelo fluxo normal.

## Transições permitidas

```text id="v9ng39"
PENDING_VALIDATION -> CONFIRMED
PENDING_VALIDATION -> REJECTED
PENDING_VALIDATION -> CANCELED

REJECTED -> PENDING_VALIDATION
REJECTED -> CANCELED
```

## Transições não permitidas

```text id="dj8asa"
CONFIRMED -> PENDING_VALIDATION
CONFIRMED -> REJECTED
CONFIRMED -> CANCELED

CANCELED -> PENDING_VALIDATION
CANCELED -> CONFIRMED
CANCELED -> REJECTED
```

## Diagrama

```text id="ojyk91"
                 ┌────────────────────┐
                 │ PENDING_VALIDATION │
                 └───────┬─────┬──────┘
                         │     │
             ┌───────────┘     └───────────┐
             ▼                             ▼
       ┌───────────┐                 ┌──────────┐
       │ CONFIRMED │                 │ REJECTED │
       └───────────┘                 └────┬─────┘
                                          │
                                          ▼
                                ┌────────────────────┐
                                │ PENDING_VALIDATION │
                                └────────────────────┘
```

## Observações

A transição `REJECTED -> PENDING_VALIDATION` acontece quando o membro atualiza o envio pelo endpoint `PUT`.

Quando isso ocorre:

* novo comprovante é registrado;
* valor pode ser corrigido;
* método pode ser corrigido;
* data de pagamento pode ser corrigida;
* observações podem ser corrigidas;
* dados de rejeição são limpos.

## MemberRefundStatus

## Estados

Os valores exatos devem permanecer sincronizados com o enum `MemberRefundStatus`.

O modelo conceitual do refund considera os seguintes estados principais:

```text id="cxyb2d"
ELIGIBLE
REQUESTED
APPROVED
REJECTED
REFUNDED
EXPIRED
CANCELED
```

## Significado dos estados

### ELIGIBLE

O pagamento é elegível para reembolso, mas o membro ainda não solicitou ou o processo ainda não avançou.

### REQUESTED

O membro solicitou o reembolso.

### APPROVED

O administrador aprovou o reembolso.

Esse estado ainda não significa que o valor foi devolvido.

### REJECTED

O administrador rejeitou a solicitação de reembolso.

### REFUNDED

O valor foi efetivamente devolvido ao membro.

### EXPIRED

A janela de elegibilidade expirou.

### CANCELED

O processo de reembolso foi cancelado.

## Transições conceituais permitidas

```text id="u34r64"
ELIGIBLE -> REQUESTED
ELIGIBLE -> APPROVED
ELIGIBLE -> EXPIRED
ELIGIBLE -> CANCELED

REQUESTED -> APPROVED
REQUESTED -> REJECTED
REQUESTED -> CANCELED

APPROVED -> REFUNDED
APPROVED -> CANCELED
```

## Transições conceituais não permitidas

```text id="do8tpm"
REFUNDED -> REQUESTED
REFUNDED -> APPROVED
REFUNDED -> CANCELED

EXPIRED -> REQUESTED
EXPIRED -> APPROVED
EXPIRED -> REFUNDED

CANCELED -> REQUESTED
CANCELED -> APPROVED
CANCELED -> REFUNDED
```

## Diagrama

```text id="dnkkak"
             ┌──────────┐
             │ ELIGIBLE │
             └────┬─────┘
                  │
        ┌─────────┼─────────┐
        │         │         │
        ▼         ▼         ▼
  ┌───────────┐ ┌──────────┐ ┌──────────┐
  │ REQUESTED │ │ APPROVED │ │ EXPIRED  │
  └─────┬─────┘ └────┬─────┘ └──────────┘
        │            │
   ┌────┴────┐       ▼
   ▼         ▼  ┌──────────┐
┌────────┐ ┌──────────┐
│REJECTED│ │ REFUNDED │
└────────┘ └──────────┘
```

## Observações

Um refund `APPROVED` não significa que o dinheiro foi devolvido.

O valor só é considerado devolvido no estado `REFUNDED`.

## PaymentAcceptancePolicy e efeito nos estados

A política de aceitação de pagamento influencia o cálculo do `MemberChargeEffectiveStatus`.

## UNTIL_DUE_DATE

```text id="pj9b1y"
paymentAllowedUntil = dueDate
```

Antes ou na data de vencimento:

```text id="ckbsqq"
effectiveStatus = PENDING
```

Depois da data de vencimento:

```text id="rg834y"
effectiveStatus = EXPIRED
```

## AFTER_DUE_DATE

```text id="7oe31m"
paymentAllowedUntil = null
```

Antes ou na data de vencimento:

```text id="nxbr9m"
effectiveStatus = PENDING
```

Depois da data de vencimento:

```text id="v7nr6n"
effectiveStatus = OVERDUE
```

## UNTIL_DAYS_AFTER_DUE_DATE

```text id="4egll9"
paymentAllowedUntil = dueDate + latePaymentGraceDays
```

Antes ou na data de vencimento:

```text id="pfp425"
effectiveStatus = PENDING
```

Depois da data de vencimento e dentro da tolerância:

```text id="c6h70v"
effectiveStatus = OVERDUE
```

Depois da tolerância:

```text id="7x4zmb"
effectiveStatus = EXPIRED
```

## Estados finais

Estados considerados finais ou terminais no fluxo normal:

```text id="vjnbyb"
ChargeDefinition:
- ARCHIVED

ChargeCycle:
- ARCHIVED

MemberCharge:
- PAID
- CANCELED

MemberPayment:
- CONFIRMED
- CANCELED

MemberRefund:
- REFUNDED
- EXPIRED
- CANCELED
```

## Estados editáveis

## ChargeDefinition

Editável enquanto não estiver arquivada.

## ChargeAssignment

Ativável/desativável enquanto a definição vinculada permitir.

## ChargeCycle

Editável apenas por transições controladas de estado.

## MemberCharge

Pode ter valor final atualizado quando:

```text id="e24fvg"
status = PENDING
effectiveStatus != EXPIRED
não existe MemberPayment PENDING_VALIDATION
```

## MemberPayment

Pode ser atualizado pelo membro quando:

```text id="s3w4w5"
status = PENDING_VALIDATION
ou
status = REJECTED
```

## MemberRefund

Pode mudar de estado conforme regras específicas do domínio.

## Resumo

```text id="xve57h"
ChargeDefinition:
ACTIVE -> INACTIVE -> ACTIVE
ACTIVE/INACTIVE -> ARCHIVED

ChargeAssignment:
ACTIVE -> INACTIVE -> ACTIVE

ChargeCycle:
GENERATED -> FINISHED -> ARCHIVED
GENERATED -> CANCELED -> ARCHIVED

MemberCharge:
PENDING -> PAID
PENDING -> CANCELED

MemberPayment:
PENDING_VALIDATION -> CONFIRMED
PENDING_VALIDATION -> REJECTED
REJECTED -> PENDING_VALIDATION

MemberRefund:
ELIGIBLE -> REQUESTED -> APPROVED -> REFUNDED
ELIGIBLE -> EXPIRED
REQUESTED -> REJECTED
```
