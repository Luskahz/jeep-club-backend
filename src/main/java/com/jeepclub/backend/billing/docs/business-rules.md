# Regras de negócio do Billing

## Configuração e fatos históricos

1. `ChargeDefinition` nasce `ACTIVE`, exige valor padrão positivo e possui nome
   normalizado. A política `UNTIL_DAYS_AFTER_DUE_DATE` exige tolerância positiva;
   as demais políticas não aceitam esse campo.
2. Definições `ACTIVE` e `INACTIVE` podem ser atualizadas. `ARCHIVED` é terminal:
   não pode ser atualizada, ativada ou desativada.
3. Atualizar a definição afeta somente o futuro. `ChargeCycle` guarda snapshots
   de nome, descrição, valor, recorrência, obrigatoriedade e política.
4. Atribuições só são criadas para definição `ACTIVE`. Ativar também exige
   definição `ACTIVE`; desativar é permitido enquanto a definição não estiver
   arquivada. Ativar uma atribuição ativa ou desativar uma inativa gera conflito.
5. O alvo `USER` exige usuário administrativamente ativo; `ROLE` exige role
   ativa. Alvos resolvidos na geração são deduplicados.
6. `EVENT_PARTICIPANTS` valida Event pelo contrato público de Publications.
   A geração administrativa continua resolvendo todos os assignments; o comando
   específico de Event resolve somente seu contexto e recebe o inscrito diretamente.

## Ciclos e cobranças

7. Um ciclo só é gerado para definição ativa, código único nessa definição e ao
   menos um usuário elegível. Ele nasce `GENERATED` e cria uma cobrança por alvo.
8. Finalizar um ciclo muda apenas seu estado; não quita nem cancela cobranças.
9. Cancelar um ciclo `GENERATED` cancela suas cobranças ainda abertas e cria
   elegibilidades de reembolso por 30 dias para pagamentos `CONFIRMED`
   que ainda não possuam refund ativo ou concluído. `PENDING_VALIDATION` continua
   analisável: confirmação posterior garante elegibilidade com a mesma janela,
   ancorada no cancelamento; rejeição não gera refund.
10. Somente ciclos `FINISHED` ou `CANCELED` podem ser arquivados. Arquivamento é
    organização histórica e não produz novo efeito financeiro.
11. `MemberCharge` persiste `PENDING`, `PAID` ou `CANCELED`. `OVERDUE` e
    `EXPIRED` são calculados no momento da consulta e nunca persistidos.
12. O valor original e o valor final são positivos; `finalAmount` não pode ser
    maior que `originalAmount`.
13. Somente cobrança `PENDING` pode ter valor final alterado, ser paga ou ser
    cancelada. O valor não muda enquanto existir pagamento
    `PENDING_VALIDATION`.
14. `UNTIL_DUE_DATE` aceita submissão até o vencimento;
    `UNTIL_DAYS_AFTER_DUE_DATE` até a data calculada de tolerância;
    `AFTER_DUE_DATE` não possui limite final.

No vínculo de Event, `participationCutoff` é um `Instant` de Publications e
controla confirmação da inscrição. `financialDueDate` é um `LocalDate` opcional
da regra do Event; quando informado, vira `ChargeCycle.dueDate` e
`MemberCharge.dueDate`. Quando omitido, o ciclo usa a data UTC de `startsAt`
como vencimento de referência. `paymentAllowedUntil` é derivado da política
financeira: coincide com o vencimento para `UNTIL_DUE_DATE`, soma a tolerância
para `UNTIL_DAYS_AFTER_DUE_DATE` e fica sem limite para `AFTER_DUE_DATE`.
Essas datas não alteram retroativamente uma inscrição já `CONFIRMED`.

### Consulta pública para Membership

`MembershipChargeQuery` recebe definição e usuário e resolve primeiro o ciclo
não arquivado aplicável ao período atual: mês para `MONTHLY`, ano para `YEARLY`
e ciclo não arquivado para `ONE_TIME`. Entre múltiplos candidatos, prefere o
vencimento mais recente já alcançado; se todos forem futuros, o mais próximo.
Depois localiza a cobrança daquele usuário pelo ciclo e calcula o estado efetivo
com o `Clock`. Ciclo recorrente atual ausente ou cobrança ausente nesse ciclo
resultam em `CHARGE_NOT_FOUND`; `OVERDUE` e `EXPIRED` não são persistidos.

## Pagamentos

15. A submissão exige cobrança própria, aberta na data atual, valor exatamente
    igual ao `finalAmount` e ausência de outro pagamento editável
    (`PENDING_VALIDATION` ou `REJECTED`).
16. O pagamento nasce `PENDING_VALIDATION`. Apenas esse estado pode ser
    confirmado ou rejeitado.
17. Confirmação marca o pagamento `CONFIRMED` e a cobrança aberta `PAID` na mesma
    transação. Se o ciclo já foi cancelado, preserva a cobrança cancelada e garante
    elegibilidade de refund. A janela é validada na submissão, não na confirmação.
18. Rejeição mantém a cobrança aberta. Pagamento `REJECTED` pode ser reenviado e
    volta a `PENDING_VALIDATION`, revalidando a janela da cobrança.
19. Pagamento já `PENDING_VALIDATION` também pode ser substituído; nessa
    atualização a implementação não revalida a janela, mas exige cobrança ainda
    `PENDING`.
20. Pagamentos `CONFIRMED` ou `CANCELED` não podem ser atualizados.

## Reembolsos

21. Um membro pode solicitar reembolso apenas de pagamento próprio `CONFIRMED`
    que ainda não tenha sido reembolsado. Comprovante não analisado não comprova dinheiro recebido.
22. Solicitação direta nasce `REQUESTED`. Uma elegibilidade de cancelamento de
    ciclo nasce `ELIGIBLE` com janela de 30 dias e pode ser solicitada ou aprovada
    enquanto não expirada.
23. `REQUESTED` pode ser aprovada ou rejeitada; `ELIGIBLE` também pode ser
    aprovada diretamente. Apenas `APPROVED` pode virar `REFUNDED`.
24. Expiração é operação administrativa manual e só ocorre em `ELIGIBLE` após o
    fim da janela. Não há scheduler implementado.
25. `ELIGIBLE`, `REQUESTED` e `APPROVED` são ativos e podem ser cancelados.
    `REJECTED`, `REFUNDED`, `EXPIRED` e `CANCELED` são terminais no fluxo atual.

## Comprovantes

`submittedAt` registra a submissão corrente e muda no reenvio/substituição.
Confirmar ou rejeitar não altera esse instante. Dados legados sem a coluna
preenchida usam `createdAt`; não há backfill que invente timestamps antigos.
Publications compara esse instante com seu cutoff; Billing não decide participação.

26. O arquivo é obrigatório em submissão e atualização; são aceitos PDF, JPEG,
    PNG e WebP de até 10 MB, com extensão e MIME coerentes.
27. Billing persiste somente `receiptStorageKey`, nunca path, provider ou URL
    física. A resposta deriva `receiptUrl` pelo `paymentId`.
28. A autorização ocorre antes de `FileStorage.load`: dono da cobrança ou
    `BILLING_PAYMENT_READ`. Conhecer uma key não concede acesso.
29. Substituição armazena o novo arquivo antes da persistência. Commit remove o
    anterior; rollback remove o novo. Falha de cleanup é registrada e não muda o
    resultado financeiro já concluído.
