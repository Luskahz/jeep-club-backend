# Fluxos do Billing

## Configuração até a dívida individual

```text
ChargeDefinition ACTIVE
  -> ChargeAssignment ativa
  -> geração do ChargeCycle
  -> resolução e deduplicação de usuários ativos
  -> snapshots da configuração
  -> uma MemberCharge PENDING por usuário
```

`ALL_MEMBERS` consulta Identity; `USER` valida um usuário ativo; `ROLE` consulta
Authorization e filtra usuários ativos. O adapter de eventos ainda retorna
nenhum evento/participante, portanto o alvo de evento não opera atualmente.

## Encerramento do ciclo

- `finish`: altera somente o ciclo para `FINISHED`.
- `cancel`: cancela cobranças abertas, preserva pagamentos e cria, quando
  aplicável, refunds `ELIGIBLE` por 30 dias.
- `archive`: parte de `FINISHED` ou `CANCELED` e não altera cobranças, pagamentos
  ou refunds.

## Submissão e substituição de pagamento

Na submissão, o serviço bloqueia a cobrança, valida ownership, janela, valor e
unicidade do pagamento editável; depois valida e armazena o comprovante, registra
a compensação e persiste `MemberPayment(PENDING_VALIDATION)`.

Na substituição, bloqueia pagamento e cobrança nessa ordem, valida estado e
ownership, armazena o novo arquivo e persiste a nova key. Se houver commit, o
arquivo anterior é removido; em rollback, o novo é compensado.

O download recebe `paymentId`, carrega pagamento e cobrança, autoriza owner ou
`BILLING_PAYMENT_READ` e somente então usa a key interna em `FileStorage.load`.

## Validação administrativa

```text
PENDING_VALIDATION --confirm--> CONFIRMED + MemberCharge PAID
PENDING_VALIDATION --reject---> REJECTED + MemberCharge permanece PENDING
REJECTED --member resubmits---> PENDING_VALIDATION
```

Pagamento enviado dentro da janela pode ser confirmado depois porque a
confirmação valida estado, não recalcula a janela.

## Reembolso

O membro pode criar `REQUESTED` a partir de pagamento próprio confirmado ou
pendente de validação. Se já houver refund ativo de cancelamento de ciclo, a
solicitação converte `ELIGIBLE` em `REQUESTED` em vez de criar outro registro.

O administrador aprova, rejeita, marca como devolvido, expira ou cancela conforme
a máquina de estados. O código não executa expiração automática.
