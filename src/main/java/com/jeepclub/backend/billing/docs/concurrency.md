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
| confirmar pagamento | `MemberPayment` -> `MemberCharge` |
| rejeitar pagamento | `MemberPayment` |
| alterar valor final | `MemberCharge` |
| cancelar cobrança individual | `MemberCharge` |

Essa ordem serializa as disputas principais entre membro e administração. Por
exemplo, confirmação e substituição não podem validar simultaneamente uma versão
obsoleta do mesmo pagamento; alteração de valor e submissão também se coordenam
pela cobrança.

O fluxo Event bloqueia a raiz da Publication e depois definições em ordem crescente.
Dentro do Billing, a definição existente é bloqueada antes de consultar/criar
assignment, contexto, ciclo e MemberCharge. Assim há uma linha estável para
serializar inclusive duas primeiras criações. `uk_event_charge_context`,
`uk_event_charge_context_cycle`, `uk_event_charge_context_assignment` e
`uk_billing_member_charges_user_cycle` complementam a proteção em banco.

Cancelamento bloqueia pagamentos existentes por ID e depois cobranças, relendo
seu estado sob lock. A criação de elegibilidade e o request direto também usam
o lock do pagamento. Confirmação tardia preserva essa ordem e garante somente
um refund ativo/concluído. Não existe retry invisível de submissão financeira.

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
