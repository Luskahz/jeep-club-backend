# Concorrência no Billing

## Locks existentes

Os contratos de lock pessimista financeiro incluem
`MemberChargeRepository.findByIdForUpdate` e
`MemberPaymentRepository.findByIdForUpdate`. Os adapters JPA usam
`PESSIMISTIC_WRITE`. `ChargeDefinitionRepository.findByIdForUpdate` protege
a criação/incremento de contextos financeiros de Event.

| Operação | Locks e ordem |
|---|---|
| enviar pagamento | `MemberCharge` |
| substituir pagamento | `MemberPayment` -> `MemberCharge` |
| confirmar pagamento | `ChargeCycle` -> `MemberPayment` -> `MemberCharge` |
| rejeitar pagamento | `MemberPayment` |
| alterar valor final | `MemberCharge` |
| cancelar cobrança individual | `MemberCharge` |

Essa ordem serializa as disputas principais entre membro e administração. Por
exemplo, confirmação e substituição não podem validar simultaneamente uma versão
obsoleta do mesmo pagamento; alteração de valor e submissão também se coordenam
pela cobrança.

A inscrição no Event bloqueia a raiz da Publication e depois definições em ordem crescente.
Dentro do Billing, a definição existente é bloqueada antes de consultar/criar
assignment, contexto, ciclo e MemberCharge. Assim há uma linha estável para
serializar inclusive duas primeiras criações. `uk_event_charge_context`,
`uk_event_charge_context_cycle`, `uk_event_charge_context_assignment` e
`uk_billing_member_charges_user_cycle` complementam a proteção em banco.

Cancelamento e finish/archive bloqueiam o ciclo. Cancelamento bloqueia pagamentos
por cobrança/ID e depois cobranças, relendo
seu estado sob lock. A criação de elegibilidade e o request direto também usam
o lock do pagamento. Confirmação tardia preserva essa ordem e garante somente
um refund ativo/concluído. Não existe retry invisível de submissão financeira.
Os pagamentos e refunds são verificados por consultas com lock (current reads),
evitando snapshot obsoleto no MySQL REPEATABLE READ após espera por lock.

## Transições de MemberRefund — BACK-489

`approve`, `reject`, `markAsRefunded`, `expire`, `cancel` e `request(refundId)`
carregam o refund com `PESSIMISTIC_WRITE` e `jakarta.persistence.lock.timeout=0`
(`FOR UPDATE NOWAIT` no MySQL 8). O lock dura até o término da transação do
service. A leitura ocorre antes da validação e da mutação. Leituras de listagem
e consulta por ID continuam sem lock.

A consulta existente por pagamento também usa NOWAIT, inclusive nos checks de
refund ativo/concluído. `requestByMemberPaymentId` mantém o lock do pagamento
antes de consultar ou criar o refund; a reutilização de REQUESTED/APPROVED
continua idempotente. Criação de elegibilidade permanece protegida pelo pagamento.
Não há retry automático. Disputa por um refund ocupado é traduzida pelo adapter
para `InvalidMemberRefundStateException` e pelo handler existente para HTTP 409,
código `INVALID_MEMBER_REFUND_STATE`, sem expor SQL.

NOWAIT evita que comandos sobrepostos como approve/cancel se tornem duas decisões
bem-sucedidas após uma espera: cancelar um APPROVED continua válido quando
solicitado depois do commit da aprovação. Uma nova chamada explícita sempre
valida o estado vigente. A contenção se limita ao refund consultado, sem locks
em páginas/listagens e sem versionamento ou migration.

| Fluxo | Ordem de aquisição |
|---|---|
| comandos de refund por ID | apenas `MemberRefund`; nunca adquire payment/charge depois |
| request por pagamento | `MemberPayment` -> `MemberRefund`; charge é somente leitura |
| confirmação tardia | `ChargeCycle` -> `MemberPayment` -> `MemberCharge` -> `MemberRefund` |
| cancelamento de ciclo | `ChargeCycle` -> pagamentos por cobrança/ID -> cobranças -> refunds |

O refund é sempre o último nível de lock. Nenhum comando bloqueia refund e
posteriormente tenta payment/charge/cycle. Os locks existentes nos níveis
anteriores são mantidos; não há ampliação de lock para operações administrativas
que só alteram refund. NOWAIT também impede espera circular nesse último nível.
A falha de aquisição aborta toda a transação, incluindo alterações financeiras
anteriores no mesmo fluxo. O banco deve suportar NOWAIT (MySQL 8); os cenários
rodam em H2 e são repetidos em MySQL pelo perfil `mysql-migrations`.

## Comprovante e transação

`PaymentReceiptLifecycle.register` exige transação e sincronização ativas.

```text
store novo arquivo
  -> persistir nova receiptStorageKey
  -> commit: excluir arquivo anterior, se diferente
  -> rollback: excluir arquivo novo
```

Se o lifecycle for chamado sem transação, ele tenta compensar o novo arquivo e
falha o caso de uso. Falha de delete no callback é capturada e registrada; não
reverte um commit financeiro concluído. Provider, path e I/O continuam em
`platform.storage`.
