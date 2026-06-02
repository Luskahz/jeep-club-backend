# Billing

## Objetivo

O módulo `billing` é responsável por controlar o ciclo financeiro interno do Jeep Club.

Ele concentra as regras de negócio relacionadas a:

* definições de cobrança;
* regras de atribuição de cobrança;
* ciclos de cobrança;
* cobranças individuais dos membros;
* envio e validação de pagamentos;
* solicitação e controle de reembolsos.

O módulo foi desenhado para separar claramente o que é configuração futura, o que é histórico financeiro e o que é evento financeiro individual.

## Estrutura do módulo

```text
billing/
├── api/
├── core/
├── docs/
└── infra/
```

## Responsabilidades por camada

### api

A camada `api` expõe os endpoints HTTP do módulo.

Ela contém:

* controllers;
* DTOs de request;
* DTOs de response;
* exception handlers específicos do módulo.

A camada `api` não deve conter regra de negócio. Ela apenas recebe a requisição, valida entrada básica, extrai o usuário autenticado quando necessário e delega para os services da camada `core`.

### core

A camada `core` contém o coração do módulo.

Ela contém:

* entidades de domínio;
* enums;
* services de aplicação;
* results;
* ports;
* interfaces de repository;
* exceptions de aplicação e domínio.

A camada `core` deve ser independente de detalhes de banco, JPA, storage físico, frameworks externos ou integrações concretas.

### infra

A camada `infra` será responsável por implementar detalhes técnicos.

Exemplos:

* entities JPA;
* repositories JPA;
* mappers;
* adapters de ports;
* storage real de comprovantes;
* locks pessimistas reais;
* migrations.

## Principais conceitos

### ChargeDefinition

Representa uma definição de cobrança.

É um modelo reutilizável para gerar cobranças futuras.

Exemplos:

* mensalidade;
* taxa anual;
* taxa de evento;
* taxa administrativa;
* taxa extraordinária.

Alterar uma `ChargeDefinition` não altera cobranças, ciclos ou pagamentos já gerados.

### ChargeAssignment

Representa uma regra de atribuição de cobrança.

Ela define quem será cobrado quando uma `ChargeDefinition` gerar um ciclo.

A atribuição pode ser feita para:

* todos os membros;
* um usuário específico;
* uma role;
* participantes confirmados de um evento.

### ChargeCycle

Representa um lote histórico de cobrança gerado a partir de uma `ChargeDefinition`.

O ciclo registra snapshots da definição usada no momento da geração.

O ciclo não controla diretamente se um membro pode pagar. Essa responsabilidade pertence à `MemberCharge`.

### MemberCharge

Representa a dívida individual de um membro dentro de um ciclo.

Ela controla:

* usuário cobrado;
* ciclo de origem;
* valor original;
* valor final;
* data de vencimento;
* política de aceitação de pagamento;
* última data permitida para pagamento;
* status persistido;
* status efetivo calculado.

### MemberPayment

Representa um envio de pagamento feito pelo membro.

O pagamento passa por validação administrativa.

O membro pode corrigir o envio enquanto o pagamento estiver pendente de validação ou rejeitado.

### MemberRefund

Representa um processo de reembolso.

Ele pode surgir a partir de um pagamento confirmado ou pendente de validação, especialmente quando um ciclo de cobrança é cancelado.

## Separação entre configuração e histórico

O módulo diferencia objetos de configuração e objetos históricos.

### Objetos de configuração

São objetos usados para gerar cobranças futuras:

* `ChargeDefinition`;
* `ChargeAssignment`.

Alterações nesses objetos afetam apenas usos futuros.

### Objetos históricos

São objetos que representam fatos financeiros já gerados:

* `ChargeCycle`;
* `MemberCharge`;
* `MemberPayment`;
* `MemberRefund`.

Esses objetos não devem ser reescritos automaticamente quando uma configuração futura muda.

## Princípios importantes

## 1. ChargeDefinition não reescreve histórico

Editar, desativar ou arquivar uma `ChargeDefinition` não altera ciclos já gerados, cobranças individuais já criadas, pagamentos ou reembolsos.

## 2. ChargeAssignment não reescreve histórico

Editar, ativar ou desativar uma atribuição não altera ciclos já gerados.

A atribuição é usada para resolver o público-alvo no momento da geração de um novo ciclo.

## 3. ChargeCycle é lote histórico

O ciclo representa um lote gerado.

Ele pode ser:

* gerado;
* finalizado;
* cancelado;
* arquivado.

Finalizar um ciclo não cancela cobranças, não cancela pagamentos e não gera reembolsos.

Cancelar um ciclo é um evento financeiro e pode gerar elegibilidade de reembolso.

## 4. MemberCharge controla a cobrança individual

A `MemberCharge` é quem define se uma cobrança ainda aceita pagamento.

Essa decisão é baseada em:

* status persistido;
* data de vencimento;
* política de aceitação de pagamento;
* data limite de pagamento.

O ciclo não deve decidir se o membro pode pagar.

## 5. OVERDUE e EXPIRED não são persistidos

`OVERDUE` e `EXPIRED` são estados calculados em runtime.

Eles não devem ser salvos como status da `MemberCharge`.

O status persistido da `MemberCharge` deve representar apenas eventos reais de ciclo de vida.

## 6. Payment pode ser confirmado após o prazo

Se o membro enviou o pagamento corretamente enquanto a cobrança aceitava pagamento, o administrador pode confirmar esse pagamento posteriormente, mesmo que a janela da cobrança tenha expirado depois.

O prazo é validado na submissão ou ressubmissão do pagamento, não na confirmação administrativa.

## 7. Não existe lock de tela

O módulo não usa bloqueio no frontend para impedir edição simultânea.

A proteção contra concorrência deve acontecer no backend por transação e lock pessimista nas operações críticas.

## 8. Scheduler não corrige regra de negócio

Schedulers podem existir para automatizar tarefas operacionais, como gerar ciclos ou expirar elegibilidades de reembolso.

Eles não devem ser necessários para manter `OVERDUE` ou `EXPIRED` de `MemberCharge`, pois esses estados são calculados em tempo de consulta.

## Fluxo principal

```text
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

## Fluxo resumido

1. O administrador cria uma `ChargeDefinition`.
2. O administrador cria uma ou mais `ChargeAssignment`.
3. Um `ChargeCycle` é gerado a partir da definição.
4. O ciclo cria `MemberCharge` para os membros elegíveis.
5. O membro consulta suas cobranças.
6. O membro envia um `MemberPayment`.
7. O administrador confirma ou rejeita o pagamento.
8. Se o pagamento for confirmado, a `MemberCharge` é marcada como paga.
9. Se o ciclo for cancelado, cobranças abertas são canceladas e pagamentos elegíveis podem gerar `MemberRefund`.

## Status geral dos fluxos

A documentação formal das regras está dividida nos seguintes arquivos:

```text
README.md
glossary.md
business-rules.md
flows.md
state-models.md
concurrency.md
```

## Pendências fora do API + CORE

O desenho de API + CORE do módulo está definido.

As próximas etapas pertencem principalmente à infraestrutura e testes:

* implementar entities JPA;
* implementar mappers;
* implementar adapters dos repositories;
* implementar adapters dos ports externos;
* implementar storage real de comprovantes;
* implementar locks pessimistas reais nos métodos `findByIdForUpdate`;
* criar migrations;
* registrar permissões `BILLING_*`;
* criar testes de unidade e integração;
* implementar schedulers futuros quando necessário.
