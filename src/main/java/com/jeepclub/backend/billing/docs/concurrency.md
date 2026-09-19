# Concorrência no Billing

## Locks existentes

Os únicos contratos explícitos de lock pessimista do módulo são
`MemberChargeRepository.findByIdForUpdate` e
`MemberPaymentRepository.findByIdForUpdate`. Os adapters JPA usam
`PESSIMISTIC_WRITE`.

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

`ChargeDefinition`, `ChargeAssignment`, `ChargeCycle` e `MemberRefund` não usam
hoje um método `findForUpdate`. Unicidade de definição, atribuição e ciclo também
é apoiada por constraints JPA. Este documento não presume locks ou schedulers
futuros.

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
