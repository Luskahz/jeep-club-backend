# Fluxos do Billing

## Objetivo

Este documento descreve os principais fluxos de negócio do módulo `billing`.

Os fluxos aqui documentados devem ser usados como referência para:

* implementação;
* manutenção;
* testes;
* revisão de regras;
* validação de integrações;
* documentação de API.

## Visão geral

O fluxo macro do Billing é:

```text id="xzp9kf"
ChargeDefinition
      ↓
ChargeAssignment
      ↓
ChargeCycle
      ↓
MemberCharge
      ↓
MemberPayment
      ↓
MemberRefund
```

Cada entidade tem uma responsabilidade específica.

```text id="zabha7"
ChargeDefinition = modelo de cobrança
ChargeAssignment = regra de público-alvo
ChargeCycle = lote histórico gerado
MemberCharge = dívida individual do membro
MemberPayment = envio e validação de pagamento
MemberRefund = processo de reembolso
```

## Fluxo 1 — Criar definição de cobrança

### Objetivo

Cadastrar uma definição de cobrança para ser usada em ciclos futuros.

### Ator

Administrador.

### Entrada

Dados principais da cobrança:

* nome;
* descrição;
* valor padrão;
* recorrência;
* obrigatoriedade;
* política de aceitação de pagamento;
* dias de tolerância, quando aplicável.

### Etapas

```text id="38lub9"
1. Administrador envia dados da definição.
2. Sistema normaliza o nome.
3. Sistema verifica se já existe definição com o mesmo nome.
4. Sistema valida a política de pagamento.
5. Sistema cria ChargeDefinition.
6. Sistema retorna a definição criada.
```

### Resultado

Uma `ChargeDefinition` é criada.

### Observações

Criar uma definição não cria cobranças individuais.

A definição apenas fica disponível para atribuições e ciclos futuros.

## Fluxo 2 — Atualizar definição de cobrança

### Objetivo

Alterar uma definição para usos futuros.

### Ator

Administrador.

### Etapas

```text id="az0sgl"
1. Administrador informa o ID da definição.
2. Sistema busca a ChargeDefinition.
3. Sistema valida duplicidade de nome.
4. Sistema atualiza dados da definição.
5. Sistema salva a alteração.
6. Sistema retorna a definição atualizada.
```

### Resultado

A `ChargeDefinition` é atualizada.

### Regra importante

A alteração não afeta:

* ciclos já gerados;
* cobranças já criadas;
* pagamentos já enviados;
* reembolsos já criados.

## Fluxo 3 — Desativar definição de cobrança

### Objetivo

Impedir temporariamente novos usos da definição.

### Ator

Administrador.

### Etapas

```text id="6bs7cj"
1. Administrador solicita desativação.
2. Sistema busca a ChargeDefinition.
3. Sistema altera status para INACTIVE.
4. Sistema salva a alteração.
```

### Resultado

A definição fica inativa.

### Regra importante

Desativar uma definição não altera histórico financeiro.

## Fluxo 4 — Arquivar definição de cobrança

### Objetivo

Remover a definição do fluxo normal de uso, preservando histórico.

### Ator

Administrador.

### Etapas

```text id="cakbne"
1. Administrador solicita arquivamento.
2. Sistema busca a ChargeDefinition.
3. Sistema arquiva a definição.
4. Sistema desativa atribuições ativas vinculadas.
5. Sistema salva as alterações.
```

### Resultado

A definição fica arquivada.

### Regra importante

Uma definição arquivada não deve voltar ao fluxo normal.

Arquivar não altera ciclos ou cobranças já geradas.

## Fluxo 5 — Criar atribuição para todos os membros

### Objetivo

Definir que uma cobrança será aplicada a todos os membros ativos em ciclos futuros.

### Ator

Administrador.

### Etapas

```text id="hi3gup"
1. Administrador informa a ChargeDefinition.
2. Sistema verifica se a definição está ativa.
3. Sistema verifica se ainda não existe atribuição de todos os membros para essa definição.
4. Sistema cria AllMembersChargeAssignment.
5. Sistema retorna a atribuição criada.
```

### Resultado

A definição passa a ter uma regra de cobrança para todos os membros ativos.

## Fluxo 6 — Criar atribuição para usuário específico

### Objetivo

Definir que uma cobrança será aplicada a um usuário específico em ciclos futuros.

### Ator

Administrador.

### Etapas

```text id="pgy1go"
1. Administrador informa ChargeDefinition e userId.
2. Sistema verifica se a definição está ativa.
3. Sistema verifica se existe membro ativo para o userId.
4. Sistema verifica se ainda não existe atribuição igual.
5. Sistema cria UserChargeAssignment.
6. Sistema retorna a atribuição criada.
```

### Resultado

A definição passa a ter uma regra de cobrança para um usuário específico.

## Fluxo 7 — Criar atribuição para role

### Objetivo

Definir que uma cobrança será aplicada a usuários vinculados a uma role.

### Ator

Administrador.

### Etapas

```text id="wj1kvn"
1. Administrador informa ChargeDefinition e roleId.
2. Sistema verifica se a definição está ativa.
3. Sistema verifica se a role está ativa.
4. Sistema verifica se ainda não existe atribuição igual.
5. Sistema cria RoleChargeAssignment.
6. Sistema retorna a atribuição criada.
```

### Resultado

A definição passa a ter uma regra de cobrança para membros associados a uma role.

## Fluxo 8 — Criar atribuição para participantes de evento

### Objetivo

Definir que uma cobrança será aplicada aos participantes confirmados de um evento.

### Ator

Administrador.

### Etapas

```text id="8x9dgc"
1. Administrador informa ChargeDefinition e eventId.
2. Sistema verifica se a definição está ativa.
3. Sistema verifica se o evento existe.
4. Sistema verifica se ainda não existe atribuição igual.
5. Sistema cria EventParticipantsChargeAssignment.
6. Sistema retorna a atribuição criada.
```

### Resultado

A definição passa a ter uma regra de cobrança para participantes confirmados de um evento.

## Fluxo 9 — Ativar atribuição

### Objetivo

Reativar uma regra de atribuição.

### Ator

Administrador.

### Etapas

```text id="nq2moe"
1. Administrador informa assignmentId.
2. Sistema busca a ChargeAssignment.
3. Sistema verifica se a ChargeDefinition vinculada está ativa.
4. Sistema ativa a atribuição.
5. Sistema salva a alteração.
```

### Resultado

A atribuição volta a ser considerada em novos ciclos.

## Fluxo 10 — Desativar atribuição

### Objetivo

Impedir que uma regra de atribuição seja usada em novos ciclos.

### Ator

Administrador.

### Etapas

```text id="pfkzgb"
1. Administrador informa assignmentId.
2. Sistema busca a ChargeAssignment.
3. Sistema verifica se a ChargeDefinition vinculada não está arquivada.
4. Sistema desativa a atribuição.
5. Sistema salva a alteração.
```

### Resultado

A atribuição deixa de ser considerada em novos ciclos.

## Fluxo 11 — Gerar ciclo de cobrança

### Objetivo

Criar um lote de cobrança para uma definição.

### Ator

Administrador ou rotina automática futura.

### Entrada

* ID da `ChargeDefinition`;
* código do ciclo;
* data de vencimento;
* usuário gerador.

### Etapas

```text id="rzwxmo"
1. Sistema busca a ChargeDefinition.
2. Sistema verifica se a definição está ativa.
3. Sistema normaliza o código do ciclo.
4. Sistema verifica se já existe ciclo com mesmo código para a definição.
5. Sistema resolve usuários-alvo pelas atribuições ativas.
6. Sistema valida se existe ao menos um usuário elegível.
7. Sistema cria ChargeCycle com snapshots da definição.
8. Sistema salva o ciclo.
9. Sistema cria MemberCharge para cada usuário elegível.
10. Sistema retorna o ciclo e a quantidade de cobranças criadas.
```

### Resultado

Um `ChargeCycle` é criado.

Uma `MemberCharge` é criada para cada membro elegível.

### Regra importante

O ciclo deve copiar snapshots da definição.

Alterações futuras na `ChargeDefinition` não devem afetar o ciclo gerado.

## Fluxo 12 — Resolver usuários-alvo do ciclo

### Objetivo

Determinar quem receberá cobrança no momento da geração do ciclo.

### Fontes possíveis

```text id="d45dyi"
AllMembersChargeAssignment
UserChargeAssignment
RoleChargeAssignment
EventParticipantsChargeAssignment
```

### Etapas

```text id="67jdx7"
1. Sistema busca atribuições ativas da ChargeDefinition.
2. Para atribuição de todos os membros, busca todos os membros ativos.
3. Para atribuição de usuário, valida se o usuário é membro ativo.
4. Para atribuição de role, busca usuários da role e filtra membros ativos.
5. Para atribuição de evento, busca participantes confirmados.
6. Sistema remove duplicidades.
7. Sistema retorna o conjunto final de userIds.
```

### Resultado

Conjunto único de usuários elegíveis.

## Fluxo 13 — Finalizar ciclo de cobrança

### Objetivo

Encerrar administrativamente o ciclo sem efeito financeiro direto.

### Ator

Administrador.

### Etapas

```text id="67cv6a"
1. Administrador informa cycleId.
2. Sistema busca ChargeCycle.
3. Sistema verifica se o ciclo está GENERATED.
4. Sistema marca o ciclo como FINISHED.
5. Sistema salva a alteração.
```

### Resultado

O ciclo fica finalizado.

### Regra importante

Finalizar o ciclo não deve:

* cancelar cobranças;
* cancelar pagamentos;
* gerar reembolsos;
* impedir pagamento de cobranças abertas;
* alterar status de `MemberCharge`.

## Fluxo 14 — Cancelar ciclo de cobrança

### Objetivo

Cancelar um lote gerado e tratar impactos financeiros.

### Ator

Administrador.

### Etapas

```text id="t0jxvp"
1. Administrador informa cycleId.
2. Sistema busca ChargeCycle.
3. Sistema verifica se o ciclo está GENERATED.
4. Sistema marca o ciclo como CANCELED.
5. Sistema busca MemberCharges abertas do ciclo.
6. Sistema cancela as MemberCharges abertas.
7. Sistema salva o ciclo.
8. Sistema cria elegibilidade de reembolso para pagamentos elegíveis.
```

### Resultado

O ciclo fica cancelado.

Cobranças abertas são canceladas.

Pagamentos confirmados ou pendentes de validação podem gerar reembolso.

### Pagamentos elegíveis para reembolso

```text id="60evi5"
CONFIRMED
PENDING_VALIDATION
```

## Fluxo 15 — Arquivar ciclo

### Objetivo

Organizar histórico de ciclos já encerrados.

### Ator

Administrador.

### Etapas

```text id="6j38ia"
1. Administrador informa cycleId.
2. Sistema busca ChargeCycle.
3. Sistema verifica se o ciclo está FINISHED ou CANCELED.
4. Sistema marca o ciclo como ARCHIVED.
5. Sistema salva a alteração.
```

### Resultado

O ciclo fica arquivado.

### Regra importante

Arquivar não produz efeito financeiro.

## Fluxo 16 — Consultar cobranças de membros

### Objetivo

Permitir consulta administrativa das cobranças individuais.

### Ator

Administrador.

### Filtros

* usuário;
* status persistido;
* paginação.

### Resultado

Lista paginada de `MemberCharge`.

### Observação

O status retornado deve incluir:

* status persistido;
* status efetivo calculado.

## Fluxo 17 — Consultar minhas cobranças

### Objetivo

Permitir que o usuário autenticado consulte suas próprias cobranças.

### Ator

Membro autenticado.

### Etapas

```text id="nfw6e4"
1. Sistema extrai userId do usuário autenticado.
2. Sistema busca cobranças vinculadas ao userId.
3. Sistema calcula effectiveStatus de cada cobrança.
4. Sistema retorna resultado paginado.
```

### Resultado

O membro visualiza suas cobranças.

## Fluxo 18 — Atualizar valor final de cobrança

### Objetivo

Permitir ajuste administrativo do valor final de uma cobrança.

### Ator

Administrador.

### Etapas

```text id="vpduyn"
1. Administrador informa memberChargeId e novo finalAmount.
2. Sistema busca MemberCharge com lock transacional.
3. Sistema verifica se não há MemberPayment PENDING_VALIDATION.
4. Sistema verifica se a cobrança está PENDING.
5. Sistema verifica se a cobrança não está efetivamente expirada.
6. Sistema valida se finalAmount não é maior que originalAmount.
7. Sistema atualiza finalAmount.
8. Sistema salva a cobrança.
```

### Resultado

O valor final da cobrança é atualizado.

### Regra importante

Não é permitido alterar valor final com pagamento pendente de validação.

## Fluxo 19 — Cancelar cobrança individual

### Objetivo

Cancelar uma cobrança individual ainda não paga.

### Ator

Administrador.

### Etapas

```text id="ucztxl"
1. Administrador informa memberChargeId.
2. Sistema busca MemberCharge com lock transacional.
3. Sistema verifica se a cobrança não está PAID.
4. Sistema verifica se a cobrança não está CANCELED.
5. Sistema marca a cobrança como CANCELED.
6. Sistema salva a cobrança.
```

### Resultado

A cobrança individual fica cancelada.

### Regra importante

Cobrança paga não deve ser cancelada.

Se houver devolução de valor, usar fluxo de reembolso.

## Fluxo 20 — Enviar pagamento

### Objetivo

Permitir que o membro envie um pagamento para uma cobrança própria.

### Ator

Membro autenticado.

### Entrada

* valor;
* método de pagamento;
* data do pagamento;
* comprovante;
* observações.

### Etapas

```text id="2d9les"
1. Usuário informa memberChargeId e dados do pagamento.
2. Sistema extrai userId autenticado.
3. Sistema busca MemberCharge com lock transacional.
4. Sistema verifica se a cobrança pertence ao usuário.
5. Sistema verifica se não existe payment editável para a cobrança.
6. Sistema verifica se a cobrança aceita nova submissão na data atual.
7. Sistema valida se amount é igual ao finalAmount da cobrança.
8. Sistema armazena comprovante.
9. Sistema cria MemberPayment com status PENDING_VALIDATION.
10. Sistema retorna o pagamento criado.
```

### Resultado

Um `MemberPayment` pendente de validação é criado.

### Regra importante

Não pode existir mais de um payment editável por cobrança.

## Fluxo 21 — Atualizar pagamento enviado

### Objetivo

Permitir que o membro corrija um pagamento ainda não confirmado.

### Ator

Membro autenticado.

### Permitido quando

O `MemberPayment` está em:

```text id="39v9n7"
PENDING_VALIDATION
REJECTED
```

### Etapas

```text id="fhxtv4"
1. Usuário informa paymentId e novos dados.
2. Sistema extrai userId autenticado.
3. Sistema busca MemberPayment com lock transacional.
4. Sistema busca MemberCharge vinculada com lock transacional.
5. Sistema verifica se a cobrança pertence ao usuário.
6. Sistema verifica se a cobrança ainda está PENDING.
7. Se o payment está REJECTED, sistema valida se a cobrança aceita nova submissão na data atual.
8. Sistema valida se amount é igual ao finalAmount da cobrança.
9. Sistema armazena novo comprovante.
10. Sistema atualiza o MemberPayment.
11. Sistema altera status para PENDING_VALIDATION.
12. Sistema limpa dados de rejeição.
13. Sistema retorna o pagamento atualizado.
```

### Resultado

O pagamento fica novamente pendente de validação.

### Regra importante

Se o pagamento já foi confirmado antes do `PUT`, a atualização deve falhar.

## Fluxo 22 — Confirmar pagamento

### Objetivo

Permitir que o administrador confirme um pagamento enviado pelo membro.

### Ator

Administrador.

### Etapas

```text id="cazdml"
1. Administrador informa paymentId.
2. Sistema busca MemberPayment com lock transacional.
3. Sistema busca MemberCharge vinculada com lock transacional.
4. Sistema verifica se a cobrança ainda está PENDING.
5. Sistema verifica se o valor do pagamento ainda bate com finalAmount.
6. Sistema confirma o MemberPayment.
7. Sistema marca a MemberCharge como PAID.
8. Sistema salva a cobrança e o pagamento.
```

### Resultado

O pagamento fica confirmado.

A cobrança fica paga.

### Regra importante

A confirmação não deve validar a janela atual de pagamento.

Se o payment foi submetido corretamente, ele pode ser confirmado depois do prazo.

## Fluxo 23 — Rejeitar pagamento

### Objetivo

Permitir que o administrador rejeite um pagamento enviado.

### Ator

Administrador.

### Etapas

```text id="df5y4u"
1. Administrador informa paymentId e motivo.
2. Sistema busca MemberPayment com lock transacional.
3. Sistema verifica se o payment está PENDING_VALIDATION.
4. Sistema altera status para REJECTED.
5. Sistema registra rejectedAt, rejectedByUserId e rejectionReason.
6. Sistema salva o pagamento.
```

### Resultado

O pagamento fica rejeitado.

### Regra importante

O membro pode corrigir o pagamento rejeitado via `PUT`, desde que a cobrança ainda aceite nova submissão.

## Fluxo 24 — Solicitar reembolso por pagamento

### Objetivo

Permitir que o membro solicite reembolso de um pagamento próprio.

### Ator

Membro autenticado.

### Etapas

```text id="j40s69"
1. Usuário informa paymentId.
2. Sistema busca MemberPayment.
3. Sistema verifica se o pagamento pode ser reembolsado.
4. Sistema busca MemberCharge vinculada.
5. Sistema verifica se a cobrança pertence ao usuário.
6. Sistema verifica se o pagamento ainda não foi reembolsado.
7. Se já existe refund ativo, sistema solicita o refund existente quando elegível.
8. Se não existe refund ativo, sistema cria novo MemberRefund solicitado pelo membro.
```

### Resultado

Um `MemberRefund` é criado ou atualizado para solicitado.

### Pagamentos aceitos

```text id="a1scqo"
CONFIRMED
PENDING_VALIDATION
```

## Fluxo 25 — Solicitar refund elegível

### Objetivo

Permitir que o membro solicite um reembolso já marcado como elegível.

### Ator

Membro autenticado.

### Etapas

```text id="294u1y"
1. Usuário informa refundId.
2. Sistema busca MemberRefund.
3. Sistema verifica se pertence ao usuário.
4. Sistema verifica se está elegível.
5. Sistema altera status para solicitado.
6. Sistema salva o refund.
```

### Resultado

O refund fica solicitado.

## Fluxo 26 — Aprovar refund

### Objetivo

Permitir que o administrador aprove um reembolso.

### Ator

Administrador.

### Etapas

```text id="0tg7j7"
1. Administrador informa refundId.
2. Sistema busca MemberRefund.
3. Sistema verifica se o status permite aprovação.
4. Sistema marca o refund como aprovado.
5. Sistema registra approvedByUserId e approvedAt.
6. Sistema salva o refund.
```

### Resultado

O refund fica aprovado.

### Regra importante

Aprovar não significa que o valor já foi devolvido.

## Fluxo 27 — Rejeitar refund

### Objetivo

Permitir que o administrador rejeite um reembolso solicitado.

### Ator

Administrador.

### Etapas

```text id="3zokju"
1. Administrador informa refundId e motivo.
2. Sistema busca MemberRefund.
3. Sistema verifica se o status permite rejeição.
4. Sistema marca o refund como rejeitado.
5. Sistema registra rejectedByUserId, rejectedAt e rejectionReason.
6. Sistema salva o refund.
```

### Resultado

O refund fica rejeitado.

## Fluxo 28 — Marcar refund como reembolsado

### Objetivo

Registrar que o valor foi efetivamente devolvido ao membro.

### Ator

Administrador.

### Etapas

```text id="suklex"
1. Administrador informa refundId.
2. Sistema busca MemberRefund.
3. Sistema verifica se o refund está aprovado.
4. Sistema marca como REFUNDED.
5. Sistema registra refundedByUserId e refundedAt.
6. Sistema salva o refund.
```

### Resultado

O valor é considerado reembolsado no sistema.

## Fluxo 29 — Expirar refund

### Objetivo

Expirar elegibilidade de reembolso quando a janela passou.

### Ator

Administrador ou scheduler futuro.

### Etapas

```text id="r9h5x7"
1. Sistema informa refundId.
2. Sistema busca MemberRefund.
3. Sistema verifica se o refund pode expirar.
4. Sistema marca como expirado.
5. Sistema salva o refund.
```

### Resultado

O refund fica expirado.

## Fluxo 30 — Cancelar refund

### Objetivo

Cancelar um processo de reembolso ainda ativo.

### Ator

Administrador.

### Etapas

```text id="it9sp3"
1. Administrador informa refundId.
2. Sistema busca MemberRefund.
3. Sistema verifica se o refund pode ser cancelado.
4. Sistema marca como cancelado.
5. Sistema registra canceledByUserId e canceledAt.
6. Sistema salva o refund.
```

### Resultado

O processo de reembolso fica cancelado.

## Fluxo de concorrência — Usuário atualiza payment enquanto admin confirma

### Cenário A — Admin confirma primeiro

```text id="r8qsy2"
1. Admin chama confirm.
2. Sistema locka MemberPayment.
3. Sistema locka MemberCharge.
4. Payment vira CONFIRMED.
5. Charge vira PAID.
6. Usuário envia PUT depois.
7. Sistema locka MemberPayment.
8. Sistema detecta status CONFIRMED.
9. PUT falha com conflito de estado.
```

### Resultado

O comprovante confirmado não é sobrescrito.

## Fluxo de concorrência — Usuário atualiza payment antes do admin confirmar

### Cenário B — Usuário atualiza primeiro

```text id="at2ctv"
1. Usuário chama PUT.
2. Sistema locka MemberPayment.
3. Sistema locka MemberCharge.
4. Sistema atualiza dados do payment.
5. Status permanece ou volta para PENDING_VALIDATION.
6. Admin chama confirm depois.
7. Sistema confirma a versão atualizada.
```

### Resultado

O administrador confirma os dados mais recentes.

## Fluxo de concorrência — Admin altera finalAmount enquanto usuário envia payment

### Cenário

```text id="px2fbm"
1. Admin tenta alterar finalAmount.
2. Usuário tenta enviar payment.
3. Ambos disputam lock na mesma MemberCharge.
4. Quem obtém o lock primeiro conclui sua operação.
5. O segundo fluxo revalida as regras com o estado atualizado.
```

### Resultado

O sistema evita payment pendente com valor antigo contra cobrança alterada.

## Fluxos que não devem existir

Os seguintes fluxos não fazem parte do desenho atual:

```text id="q2efcj"
- marcar MemberCharge manualmente como OVERDUE;
- marcar MemberCharge manualmente como EXPIRED;
- refresh manual de status de MemberCharge;
- endpoint de lock/deslock de edição no frontend;
- scheduler para atualizar OVERDUE/EXPIRED de MemberCharge;
- alteração retroativa de ChargeCycle após mudança de ChargeDefinition;
- alteração retroativa de MemberCharge após mudança de ChargeAssignment.
```

## Resumo dos principais caminhos

```text id="xvk2u1"
Definição criada
      ↓
Atribuições criadas
      ↓
Ciclo gerado
      ↓
Cobranças individuais criadas
      ↓
Membro envia pagamento
      ↓
Admin confirma
      ↓
Cobrança paga
```

```text id="2bvttk"
Membro envia pagamento
      ↓
Admin rejeita
      ↓
Membro corrige via PUT
      ↓
Payment volta para PENDING_VALIDATION
      ↓
Admin confirma ou rejeita novamente
```

```text id="uu38ls"
Ciclo gerado
      ↓
Ciclo cancelado
      ↓
Cobranças abertas canceladas
      ↓
Pagamentos confirmados ou pendentes geram elegibilidade de refund
      ↓
Refund solicitado/aprovado/reembolsado
```
