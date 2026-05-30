# Glossário do Billing

## Objetivo

Este documento define os principais termos usados no módulo `billing`.

O objetivo é manter uma linguagem única para regras de negócio, código, documentação e futuras implementações de infraestrutura.

## Billing

Módulo responsável pelo controle financeiro interno do Jeep Club.

Ele gerencia:

* definições de cobrança;
* regras de atribuição;
* ciclos;
* cobranças individuais;
* pagamentos;
* reembolsos.

## ChargeDefinition

Representa uma definição de cobrança.

É o modelo usado para gerar cobranças futuras.

Exemplos:

* mensalidade;
* taxa anual;
* taxa de evento;
* taxa extraordinária;
* cobrança administrativa.

A `ChargeDefinition` não representa uma dívida individual. Ela é apenas a configuração base.

## ChargeAssignment

Representa uma regra de atribuição de cobrança.

Ela define quais membros serão cobrados quando uma `ChargeDefinition` for usada para gerar um `ChargeCycle`.

Uma cobrança pode ser atribuída para:

* todos os membros;
* um usuário específico;
* usuários vinculados a uma role;
* participantes confirmados de um evento.

A `ChargeAssignment` não cria dívida imediatamente. Ela apenas define o público-alvo para ciclos futuros.

## ChargeCycle

Representa um ciclo ou lote de cobrança.

Um `ChargeCycle` é gerado a partir de uma `ChargeDefinition`.

Quando um ciclo é gerado, o sistema cria `MemberCharge` para os membros elegíveis conforme as `ChargeAssignment` ativas.

O ciclo mantém snapshots da definição usada no momento da geração.

Esses snapshots preservam o histórico mesmo que a `ChargeDefinition` seja alterada depois.

## Snapshot

Snapshot é uma cópia dos dados relevantes de uma entidade no momento em que um evento financeiro acontece.

No Billing, snapshots são usados para preservar histórico.

Exemplo:

```text id="1gxa3o"
ChargeDefinition atual:
- nome = Mensalidade 2026
- valor = 150.00

ChargeCycle gerado:
- chargeDefinitionNameSnapshot = Mensalidade 2026
- chargeDefinitionDefaultAmountSnapshot = 150.00
```

Se a `ChargeDefinition` for alterada depois, o ciclo continua preservando os valores originais usados na geração.

## MemberCharge

Representa uma cobrança individual de um membro.

Ela é a dívida individual gerada dentro de um `ChargeCycle`.

A `MemberCharge` contém:

* usuário cobrado;
* definição de origem;
* ciclo de origem;
* valor original;
* valor final;
* vencimento;
* política de aceitação de pagamento;
* data limite para pagamento;
* status persistido;
* status efetivo calculado.

A `MemberCharge` é quem controla se a cobrança ainda aceita pagamento.

## MemberPayment

Representa um pagamento enviado por um membro para uma `MemberCharge`.

O pagamento não quita automaticamente a cobrança.

Ele nasce como pendente de validação e precisa ser confirmado por um administrador.

Um `MemberPayment` pode conter:

* valor informado;
* método de pagamento;
* data em que o membro realizou o pagamento;
* comprovante;
* status;
* dados de confirmação;
* dados de rejeição;
* observações.

## MemberRefund

Representa um processo de reembolso.

Um `MemberRefund` pode ser criado quando um pagamento se torna elegível para reembolso.

Exemplos:

* cancelamento de um ciclo de cobrança;
* solicitação manual de reembolso de um pagamento próprio;
* análise administrativa de um pagamento pendente ou confirmado.

## Status persistido

Status persistido é o status salvo no banco de dados.

Ele representa fatos reais do ciclo de vida da entidade.

Exemplo em `MemberCharge`:

```text id="1yh8jk"
PENDING
PAID
CANCELED
```

Esses status representam eventos reais:

* ainda está pendente;
* foi paga;
* foi cancelada.

## Status efetivo

Status efetivo é um status calculado em runtime.

Ele não precisa ser salvo no banco.

Exemplo em `MemberCharge`:

```text id="jqla4n"
PENDING
OVERDUE
EXPIRED
PAID
CANCELED
```

`OVERDUE` e `EXPIRED` são calculados com base na data atual, no vencimento e na data limite de pagamento.

## ChargeDefinitionStatus

Status da definição de cobrança.

Valores:

```text id="gwzzyv"
ACTIVE
INACTIVE
ARCHIVED
```

### ACTIVE

A definição pode ser usada para criar atribuições e gerar ciclos.

### INACTIVE

A definição fica desativada para novos usos, mas permanece no histórico.

### ARCHIVED

A definição é arquivada e não deve voltar ao fluxo normal de uso.

Arquivar uma definição não altera ciclos, cobranças, pagamentos ou reembolsos já existentes.

## ChargeCycleStatus

Status do ciclo de cobrança.

Valores:

```text id="8je66q"
GENERATED
FINISHED
CANCELED
ARCHIVED
```

### GENERATED

O ciclo foi gerado e suas cobranças individuais foram criadas.

### FINISHED

O ciclo foi finalizado administrativamente.

Finalizar um ciclo não cancela cobranças, não cancela pagamentos e não gera reembolsos.

### CANCELED

O ciclo foi cancelado.

Cancelar um ciclo é um evento financeiro.

O cancelamento cancela cobranças abertas e pode gerar elegibilidade de reembolso para pagamentos confirmados ou pendentes de validação.

### ARCHIVED

O ciclo foi arquivado para organização histórica.

Apenas ciclos finalizados ou cancelados podem ser arquivados.

## MemberChargeStatus

Status persistido da cobrança individual.

Valores:

```text id="tllsnm"
PENDING
PAID
CANCELED
```

### PENDING

A cobrança ainda está aberta.

Ela pode estar efetivamente:

* pendente;
* vencida;
* expirada.

Essa variação é calculada pelo status efetivo.

### PAID

A cobrança foi paga.

Esse status é persistido quando um `MemberPayment` é confirmado.

### CANCELED

A cobrança foi cancelada.

Uma cobrança cancelada não pode ser paga.

## MemberChargeEffectiveStatus

Status calculado da cobrança individual.

Valores:

```text id="o1yplq"
PENDING
OVERDUE
EXPIRED
PAID
CANCELED
```

### PENDING

A cobrança está aberta e ainda não venceu.

### OVERDUE

A cobrança está aberta, passou da data de vencimento, mas ainda aceita pagamento.

### EXPIRED

A cobrança está aberta, mas não aceita mais pagamento porque a janela de pagamento expirou.

### PAID

A cobrança foi paga.

### CANCELED

A cobrança foi cancelada.

## MemberPaymentStatus

Status do pagamento enviado pelo membro.

Valores:

```text id="racr2s"
PENDING_VALIDATION
CONFIRMED
REJECTED
CANCELED
```

### PENDING_VALIDATION

O membro enviou o pagamento e ele aguarda validação administrativa.

### CONFIRMED

O pagamento foi confirmado por um administrador.

Quando um pagamento é confirmado, a `MemberCharge` vinculada é marcada como `PAID`.

### REJECTED

O pagamento foi rejeitado por um administrador.

O membro pode corrigir um pagamento rejeitado via `PUT`, desde que a `MemberCharge` ainda aceite nova submissão.

### CANCELED

O pagamento foi cancelado.

Pagamentos confirmados não podem ser cancelados pelo fluxo normal.

## MemberRefundStatus

Status do processo de reembolso.

Representa a situação do reembolso desde a elegibilidade até a conclusão ou cancelamento.

Os status exatos devem ser mantidos em sincronia com o enum `MemberRefundStatus`.

## PaymentAcceptancePolicy

Política de aceitação de pagamento de uma cobrança.

Ela define até quando uma `MemberCharge` aceita pagamento.

Valores principais:

```text id="4ts6ma"
UNTIL_DUE_DATE
AFTER_DUE_DATE
UNTIL_DAYS_AFTER_DUE_DATE
```

### UNTIL_DUE_DATE

A cobrança aceita pagamento até a data de vencimento.

Depois disso, a cobrança fica efetivamente expirada.

### AFTER_DUE_DATE

A cobrança aceita pagamento mesmo após o vencimento.

Depois do vencimento, ela fica efetivamente vencida, mas ainda pagável.

### UNTIL_DAYS_AFTER_DUE_DATE

A cobrança aceita pagamento por uma quantidade limitada de dias após o vencimento.

Depois desse período, a cobrança fica efetivamente expirada.

## dueDate

Data de vencimento da cobrança.

Ela é usada para calcular se a cobrança está vencida.

## paymentAllowedUntil

Última data em que uma `MemberCharge` aceita pagamento.

Quando esse campo é nulo, significa que não existe limite de pagamento definido após o vencimento.

Exemplo:

```text id="x32tdo"
PaymentAcceptancePolicy = AFTER_DUE_DATE
paymentAllowedUntil = null
```

## latePaymentGraceDays

Quantidade de dias de tolerância após o vencimento.

Esse campo só deve ser usado quando a política de aceitação for:

```text id="xekell"
UNTIL_DAYS_AFTER_DUE_DATE
```

## originalAmount

Valor original da cobrança no momento em que a `MemberCharge` foi criada.

Esse valor representa o valor base gerado pelo ciclo.

## finalAmount

Valor final cobrado do membro.

Ele pode ser menor ou igual ao valor original.

O `finalAmount` não pode ser maior que o `originalAmount`.

O `finalAmount` não deve ser alterado se já existir pagamento pendente de validação para a cobrança.

## Editable payment

Pagamento editável é um `MemberPayment` que ainda pode ser atualizado pelo membro.

São considerados editáveis:

```text id="r0iq97"
PENDING_VALIDATION
REJECTED
```

Não pode existir mais de um pagamento editável para a mesma `MemberCharge`.

## Refund eligibility

Elegibilidade de reembolso.

Representa que um pagamento pode ser reembolsado, mas ainda depende de solicitação ou aprovação conforme o fluxo.

Um ciclo cancelado pode gerar elegibilidade de reembolso para pagamentos:

```text id="yz2xcg"
CONFIRMED
PENDING_VALIDATION
```

## Lock transacional

Mecanismo de proteção contra concorrência no backend.

O Billing não usa lock de tela nem lock manual acionado pelo frontend.

Operações críticas usam locks transacionais, declarados no core por métodos como:

```text id="n9nhpc"
findByIdForUpdate
```

Na infraestrutura, esses métodos devem ser implementados com lock pessimista real no banco de dados.

## Scheduler

Processo automático executado em intervalos definidos.

No Billing, schedulers podem ser usados futuramente para:

* gerar ciclos recorrentes;
* expirar elegibilidades de reembolso;
* executar rotinas administrativas.

Schedulers não devem ser necessários para atualizar `OVERDUE` ou `EXPIRED` de `MemberCharge`, pois esses estados são calculados em runtime.
