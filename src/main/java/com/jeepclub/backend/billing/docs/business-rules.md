# Regras de Negócio do Billing

## Objetivo

Este documento registra as regras de negócio centrais do módulo `billing`.

As regras descritas aqui devem orientar alterações futuras em:

* domínio;
* services de aplicação;
* DTOs;
* controllers;
* adapters;
* migrations;
* testes;
* documentação de API.

## Regra 1 — ChargeDefinition é configuração futura

A `ChargeDefinition` representa um modelo de cobrança.

Ela não representa uma dívida individual e não representa um evento financeiro já ocorrido.

Alterações em uma `ChargeDefinition` afetam apenas usos futuros.

Alterar uma definição não deve modificar:

* `ChargeCycle` já gerado;
* `MemberCharge` já criada;
* `MemberPayment` já enviado;
* `MemberRefund` já criado.

## Regra 2 — ChargeDefinition deve preservar histórico por snapshot

Quando um `ChargeCycle` é gerado, ele deve copiar os dados relevantes da `ChargeDefinition`.

Esses dados copiados são snapshots.

Snapshots existem para garantir que o histórico financeiro continue correto mesmo que a definição seja alterada no futuro.

Exemplo:

```text
ChargeDefinition no momento da geração:
- name = mensalidade
- defaultAmount = 150.00
- paymentAcceptancePolicy = AFTER_DUE_DATE

ChargeCycle gerado:
- chargeDefinitionNameSnapshot = mensalidade
- chargeDefinitionDefaultAmountSnapshot = 150.00
- chargeDefinitionPaymentAcceptancePolicySnapshot = AFTER_DUE_DATE
```

Se depois a definição mudar para `200.00`, o ciclo antigo continua com snapshot de `150.00`.

## Regra 3 — ChargeDefinition arquivada não deve ser reutilizada

Uma `ChargeDefinition` arquivada deve permanecer disponível para histórico, mas não deve voltar ao fluxo normal.

Ela não deve receber novas atribuições e não deve gerar novos ciclos.

Arquivar uma definição não apaga nem altera histórico financeiro.

## Regra 4 — ChargeAssignment define público-alvo futuro

A `ChargeAssignment` define quem será cobrado em ciclos futuros.

Ela não cria cobrança imediatamente.

Ela é usada no momento da geração de um `ChargeCycle`.

## Regra 5 — ChargeAssignment não reescreve histórico

Ativar, desativar ou alterar atribuições não deve alterar ciclos já gerados.

A alteração afeta apenas novas gerações de ciclo.

## Regra 6 — ChargeAssignment depende de ChargeDefinition ativa

Uma atribuição só pode ser criada ou ativada se a `ChargeDefinition` estiver ativa.

Se a definição estiver inativa ou arquivada, ela não deve receber novas atribuições ativas.

## Regra 7 — ChargeCycle é um lote histórico

O `ChargeCycle` representa um lote de cobrança gerado.

Ele é histórico.

Ele contém snapshots da definição e serve como origem das `MemberCharge`.

## Regra 8 — ChargeCycle não decide se o membro pode pagar

O ciclo não deve controlar diretamente se um membro pode pagar.

Essa responsabilidade pertence à `MemberCharge`.

O ciclo pode ser finalizado, cancelado ou arquivado, mas a regra de aceitação de pagamento é da cobrança individual.

## Regra 9 — Finalizar ChargeCycle não altera cobranças individuais

Finalizar um ciclo é uma ação administrativa.

Ela não deve:

* cancelar cobranças;
* marcar cobranças como pagas;
* cancelar pagamentos;
* gerar reembolsos;
* impedir automaticamente pagamento de cobranças abertas.

Uma cobrança pode continuar pendente, vencida ou expirada mesmo após o ciclo ser finalizado.

## Regra 10 — Cancelar ChargeCycle é evento financeiro

Cancelar um ciclo é diferente de finalizar.

O cancelamento é um evento financeiro.

Ao cancelar um ciclo:

* o ciclo passa para `CANCELED`;
* cobranças abertas vinculadas ao ciclo devem ser canceladas;
* pagamentos confirmados ou pendentes de validação podem gerar elegibilidade de reembolso.

## Regra 11 — ChargeCycle cancelado pode gerar refund eligibility

Quando um ciclo é cancelado, o sistema deve buscar pagamentos elegíveis vinculados às cobranças do ciclo.

São elegíveis:

```text
CONFIRMED
PENDING_VALIDATION
```

Esses pagamentos podem gerar `MemberRefund`.

## Regra 12 — ChargeCycle arquivado é apenas organização histórica

Arquivar um ciclo não deve produzir efeito financeiro.

Apenas ciclos finalizados ou cancelados podem ser arquivados.

Arquivar um ciclo não deve:

* cancelar cobranças;
* cancelar pagamentos;
* gerar reembolsos;
* alterar cobranças individuais.

## Regra 13 — MemberCharge representa a dívida individual

A `MemberCharge` é a entidade que representa a dívida de um membro.

Ela pertence a:

* um usuário;
* uma definição de cobrança;
* um ciclo de cobrança.

Ela possui valor, vencimento, política de pagamento e status próprio.

## Regra 14 — MemberCharge controla a aceitação de pagamento

A `MemberCharge` deve decidir se aceita pagamento.

Essa decisão considera:

* status persistido;
* data atual;
* data de vencimento;
* política de aceitação de pagamento;
* data limite para pagamento.

## Regra 15 — MemberCharge possui status persistido simples

O status persistido da `MemberCharge` deve representar apenas fatos reais.

Valores permitidos:

```text
PENDING
PAID
CANCELED
```

## Regra 16 — OVERDUE e EXPIRED não são status persistidos

`OVERDUE` e `EXPIRED` não devem ser persistidos no banco como status da `MemberCharge`.

Eles são estados calculados.

Motivo:

* dependem da data atual;
* mudam naturalmente com o tempo;
* não representam necessariamente um evento gravado;
* não devem depender de scheduler para se manterem corretos.

## Regra 17 — MemberChargeEffectiveStatus é calculado em runtime

O status efetivo da cobrança deve ser calculado com base no estado persistido e na data de referência.

Valores possíveis:

```text
PENDING
OVERDUE
EXPIRED
PAID
CANCELED
```

Regras:

```text
Se status = PAID:
    effectiveStatus = PAID

Se status = CANCELED:
    effectiveStatus = CANCELED

Se status = PENDING e hoje passou de paymentAllowedUntil:
    effectiveStatus = EXPIRED

Se status = PENDING e hoje passou de dueDate e ainda aceita pagamento:
    effectiveStatus = OVERDUE

Caso contrário:
    effectiveStatus = PENDING
```

## Regra 18 — Cobrança paga não pode ser cancelada

Uma `MemberCharge` com status `PAID` não deve ser cancelada pelo fluxo normal.

Se houver necessidade de devolver valor, o fluxo correto é reembolso, não cancelamento da cobrança paga.

## Regra 19 — Cobrança cancelada não pode ser paga

Uma `MemberCharge` com status `CANCELED` não deve aceitar pagamento.

## Regra 20 — finalAmount não pode ser maior que originalAmount

O valor final da cobrança pode ser igual ou menor que o valor original.

Ele não deve ser maior.

## Regra 21 — finalAmount não deve mudar com payment pendente de validação

Se uma `MemberCharge` possui `MemberPayment` com status `PENDING_VALIDATION`, o `finalAmount` não deve ser alterado.

Motivo:

* o membro enviou comprovante com base no valor vigente;
* alterar o valor durante a validação pode invalidar o pagamento;
* a regra evita inconsistência financeira.

## Regra 22 — Alterar finalAmount deve usar lock na MemberCharge

Operações que alteram `finalAmount` devem buscar a `MemberCharge` com lock transacional.

Isso evita concorrência com:

* submissão de pagamento;
* atualização de pagamento;
* confirmação de pagamento;
* cancelamento de cobrança.

## Regra 23 — PaymentAcceptancePolicy define janela de pagamento

A política de aceitação de pagamento define até quando uma `MemberCharge` aceita pagamento.

Valores:

```text
UNTIL_DUE_DATE
AFTER_DUE_DATE
UNTIL_DAYS_AFTER_DUE_DATE
```

## Regra 24 — UNTIL_DUE_DATE

Quando a política for `UNTIL_DUE_DATE`, a cobrança aceita pagamento até a data de vencimento.

Após essa data, a cobrança fica efetivamente expirada.

## Regra 25 — AFTER_DUE_DATE

Quando a política for `AFTER_DUE_DATE`, a cobrança aceita pagamento mesmo após o vencimento.

Após o vencimento, ela fica efetivamente vencida, mas não expirada.

Nesse caso, `paymentAllowedUntil` deve ser nulo.

## Regra 26 — UNTIL_DAYS_AFTER_DUE_DATE

Quando a política for `UNTIL_DAYS_AFTER_DUE_DATE`, a cobrança aceita pagamento até uma quantidade limitada de dias após o vencimento.

Nesse caso, `latePaymentGraceDays` é obrigatório e deve ser maior que zero.

## Regra 27 — MemberPayment representa envio de pagamento

O `MemberPayment` representa o envio de um pagamento pelo membro.

Ele não quita automaticamente a cobrança.

A quitação só ocorre quando o pagamento é confirmado por um administrador.

## Regra 28 — MemberPayment nasce pendente de validação

Todo pagamento enviado pelo membro deve nascer como:

```text
PENDING_VALIDATION
```

## Regra 29 — MemberPayment confirmado marca MemberCharge como paga

Quando um `MemberPayment` é confirmado:

* o payment passa para `CONFIRMED`;
* a `MemberCharge` vinculada passa para `PAID`;
* `paidAt` da cobrança deve usar a data informada no pagamento.

## Regra 30 — Confirmação de payment não valida prazo atual da cobrança

O administrador pode confirmar um pagamento depois do fim da janela da cobrança.

O que importa é que o pagamento tenha entrado corretamente no fluxo quando foi submetido ou ressubmetido.

O prazo deve ser validado no envio do payment, não na confirmação administrativa.

## Regra 31 — Payment rejeitado não quita cobrança

Quando um pagamento é rejeitado:

* o payment passa para `REJECTED`;
* a cobrança permanece aberta se ainda estiver `PENDING`;
* o membro pode tentar corrigir o envio via `PUT`, desde que a cobrança ainda aceite nova submissão.

## Regra 32 — Payment rejeitado pode voltar para pendente

Um `MemberPayment` com status `REJECTED` pode ser atualizado pelo membro.

Ao ser atualizado:

* status volta para `PENDING_VALIDATION`;
* dados de rejeição são limpos;
* novo comprovante e novos dados de pagamento são registrados.

## Regra 33 — Payment pendente pode ser corrigido

Um `MemberPayment` com status `PENDING_VALIDATION` pode ser atualizado pelo membro.

Isso permite corrigir:

* valor;
* método;
* data de pagamento;
* comprovante;
* observações.

## Regra 34 — Payment confirmado não pode ser atualizado

Um `MemberPayment` com status `CONFIRMED` não pode ser alterado pelo membro.

Se o administrador confirmou, o pagamento já produziu efeito financeiro na cobrança.

## Regra 35 — Payment cancelado não pode ser atualizado

Um `MemberPayment` com status `CANCELED` não pode ser alterado pelo membro.

## Regra 36 — Não pode existir mais de um payment editável por charge

Para uma mesma `MemberCharge`, não pode existir mais de um pagamento editável.

São pagamentos editáveis:

```text
PENDING_VALIDATION
REJECTED
```

Se já existir um pagamento editável, o membro deve usar `PUT` para atualizar o existente, não criar outro com `POST`.

## Regra 37 — Payment submit valida janela da cobrança

Ao criar um novo `MemberPayment`, o sistema deve validar se a `MemberCharge` aceita pagamento na data atual.

Se a cobrança estiver efetivamente expirada, o envio deve ser bloqueado.

## Regra 38 — PUT em payment rejeitado valida janela da cobrança

Quando um pagamento rejeitado é atualizado, ele representa uma nova submissão.

Nesse caso, a cobrança deve aceitar pagamento na data atual.

Se a cobrança já expirou, o `PUT` deve ser bloqueado.

## Regra 39 — PUT em payment pendente não revalida janela da cobrança

Quando um pagamento pendente é atualizado, ele ainda representa uma submissão que já entrou no fluxo.

Nesse caso, o sistema não precisa revalidar a janela da cobrança.

Se o administrador confirmar antes do `PUT`, o lock transacional garante que o `PUT` falhe.

## Regra 40 — Payment submit/update/confirm deve usar lock

Operações críticas de payment devem usar lock transacional.

Devem ser protegidos:

* `MemberPayment`;
* `MemberCharge`.

Isso evita concorrência entre:

* membro atualizando pagamento;
* administrador confirmando pagamento;
* administrador rejeitando pagamento;
* administrador alterando valor final da cobrança;
* administrador cancelando cobrança.

## Regra 41 — MemberRefund representa processo de reembolso

O `MemberRefund` representa o processo de devolução de valor ao membro.

Ele não é o pagamento original.

Ele referencia:

* `memberChargeId`;
* `memberPaymentId`;
* `chargeCycleId`;
* `userId`;
* valor a reembolsar.

## Regra 42 — Refund pode nascer de cancelamento de ciclo

Quando um ciclo é cancelado, o sistema pode criar elegibilidade de reembolso para pagamentos vinculados às cobranças do ciclo.

## Regra 43 — Refund pode ser solicitado pelo membro

O membro pode solicitar reembolso de pagamento próprio quando a regra de negócio permitir.

O sistema deve garantir que o payment pertence ao usuário autenticado por meio da `MemberCharge`.

## Regra 44 — Payment já reembolsado não pode gerar novo refund

Se um pagamento já foi marcado como reembolsado, ele não deve gerar novo processo de reembolso.

## Regra 45 — Não deve existir refund ativo duplicado para o mesmo payment

Antes de criar novo `MemberRefund`, o sistema deve verificar se já existe refund ativo para o mesmo `MemberPayment`.

## Regra 46 — Refund aprovado ainda não significa valor devolvido

Aprovar um reembolso significa que o processo foi autorizado.

O valor só é considerado efetivamente devolvido quando o refund for marcado como reembolsado.

## Regra 47 — Scheduler não mantém status de MemberCharge

O sistema não deve depender de scheduler para atualizar `OVERDUE` ou `EXPIRED`.

Esses estados são calculados em runtime.

## Regra 48 — Scheduler pode automatizar tarefas operacionais

Schedulers futuros podem ser usados para:

* gerar ciclos recorrentes;
* expirar elegibilidades de reembolso;
* executar rotinas administrativas.

Schedulers não devem corrigir regras essenciais que precisam ser garantidas pelos métodos de domínio e services.

## Regra 49 — API não deve conter regra de negócio

Controllers devem apenas:

* receber entrada;
* validar dados básicos;
* extrair usuário autenticado;
* montar objetos simples;
* chamar services;
* converter result em response.

A regra de negócio deve ficar no `core`.

## Regra 50 — CORE não deve depender de infraestrutura

O `core` não deve conhecer:

* JPA;
* banco de dados concreto;
* storage físico;
* controllers;
* HTTP;
* detalhes de framework externo.

O `core` declara contratos por meio de repositories e ports.

## Regra 51 — Locks reais pertencem à infraestrutura

O `core` declara métodos como:

```text
findByIdForUpdate
```

A infraestrutura deve implementar esses contratos com lock real no banco de dados.

Exemplo esperado em JPA:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
```

## Regra 52 — Erros de arquivo devem virar erro de aplicação

Falhas ao ler comprovante de pagamento não devem vazar como `IOException` no controller.

Elas devem ser convertidas para uma exception de aplicação apropriada, como:

```text
InvalidPaymentReceiptException
```

## Regra 53 — Erros de estado devem retornar conflito

Quando uma operação viola o estado atual da entidade, o erro deve representar conflito de estado.

Exemplos:

* confirmar payment já confirmado;
* atualizar payment confirmado;
* pagar charge cancelada;
* cancelar charge paga;
* arquivar cycle ainda gerado.

Esses casos devem retornar erro equivalente a `409 CONFLICT`.

## Regra 54 — Erros de acesso devem retornar proibido

Quando um usuário tenta acessar ou alterar recurso que não pertence a ele, o erro deve representar acesso negado.

Esses casos devem retornar erro equivalente a `403 FORBIDDEN`.

## Regra 55 — Erros de recurso inexistente devem retornar não encontrado

Quando uma entidade não existe, o erro deve representar recurso não encontrado.

Esses casos devem retornar erro equivalente a `404 NOT_FOUND`.
