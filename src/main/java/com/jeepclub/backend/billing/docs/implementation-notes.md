# Notas de Implementação do Billing

## Objetivo

Este documento registra decisões técnicas e pendências de implementação do módulo `billing`.

Ele deve ser usado como referência para as próximas etapas após o fechamento do desenho de API + CORE.

## Status atual

O desenho de `api` e `core` do módulo Billing está definido.

As próximas etapas pertencem principalmente a:

* infraestrutura;
* persistência;
* mapeamento;
* migrations;
* permissões;
* testes;
* schedulers futuros.

## Estrutura esperada

```text id="ko6rqt"
billing/
├── api/
├── core/
├── docs/
└── infra/
```

## Camada api

A camada `api` contém:

* controllers;
* DTOs de request;
* DTOs de response;
* exception handlers.

A camada `api` deve apenas coordenar entrada e saída HTTP.

Ela não deve conter regra de negócio.

## Camada core

A camada `core` contém:

* entidades de domínio;
* enums;
* services de aplicação;
* results;
* ports;
* repositories;
* exceptions.

A camada `core` deve ser independente de infraestrutura.

Ela não deve depender de:

* JPA;
* banco de dados;
* storage físico;
* HTTP;
* controllers;
* entidades de persistência.

## Camada infra

A camada `infra` deve implementar os detalhes técnicos necessários para executar os contratos definidos pelo core.

Exemplos:

* JPA entities;
* Spring Data repositories;
* mappers;
* adapters;
* migrations;
* storage de comprovantes;
* integrações com outros módulos.

## Pendência 1 — Entities JPA

Devem ser criadas entities JPA para persistir os agregados e modelos do módulo.

Entidades esperadas:

```text id="0actza"
ChargeDefinitionEntity
ChargeAssignmentEntity
ChargeCycleEntity
MemberChargeEntity
MemberPaymentEntity
MemberRefundEntity
```

A estratégia exata de herança para `ChargeAssignment` deve ser definida na infraestrutura.

Possíveis abordagens:

```text id="5hebrz"
1. SINGLE_TABLE
2. JOINED
3. Uma tabela única manual com targetType e targetId
```

A escolha deve preservar simplicidade e clareza de consulta.

## Pendência 2 — Mappers

Devem ser criados mappers entre domínio e persistência.

Exemplos:

```text id="xigkd0"
ChargeDefinitionMapper
ChargeAssignmentMapper
ChargeCycleMapper
MemberChargeMapper
MemberPaymentMapper
MemberRefundMapper
```

Os mappers devem:

* converter entity para domínio;
* converter domínio para entity;
* preservar IDs;
* preservar datas;
* preservar snapshots;
* preservar status;
* evitar regra de negócio.

## Pendência 3 — Adapters de repository

Os repositories do core devem ser implementados por adapters na infra.

Exemplo conceitual:

```text id="078gs0"
core/repository/MemberPaymentRepository
        ↓
infra/persistence/MemberPaymentRepositoryAdapter
        ↓
infra/persistence/jpa/MemberPaymentJpaRepository
```

Os adapters devem ser responsáveis por:

* chamar Spring Data JPA;
* aplicar mappers;
* cumprir contratos do core;
* esconder detalhes de persistência.

## Pendência 4 — Locks reais

O core declara métodos com intenção de lock:

```text id="luqq2j"
MemberChargeRepository.findByIdForUpdate
MemberPaymentRepository.findByIdForUpdate
```

Na infra, esses métodos devem ser implementados com lock pessimista real.

Exemplo:

```java id="ih13bq"
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select c from MemberChargeEntity c where c.id = :id")
Optional<MemberChargeEntity> findByIdForUpdate(@Param("id") Long id);
```

Sem essa implementação, a regra de concorrência existe no core, mas não é garantida no banco.

## Pendência 5 — Avaliar locks futuros

Pode ser avaliado futuramente adicionar locks em:

```text id="xr0k36"
ChargeCycleRepository.findByIdForUpdate
MemberRefundRepository.findByIdForUpdate
```

Esses locks podem fortalecer fluxos como:

* cancelamento de ciclo;
* aprovação de refund;
* marcação de refund como reembolsado;
* expiração de refund;
* cancelamento de refund.

Não são bloqueantes para o fechamento de API + CORE, mas devem ser considerados na infraestrutura.

## Pendência 6 — Migrations

Devem ser criadas migrations para as tabelas do Billing.

Tabelas esperadas conceitualmente:

```text id="wggu7o"
billing_charge_definitions
billing_charge_assignments
billing_charge_cycles
billing_member_charges
billing_member_payments
billing_member_refunds
```

Também devem ser criados índices para consultas frequentes.

## Índices sugeridos

## ChargeDefinition

```text id="ca2g2z"
unique(name)
index(status)
```

## ChargeAssignment

```text id="hmo1pb"
index(charge_definition_id)
index(status)
index(target_type)
index(target_id)
unique(charge_definition_id, target_type, target_id)
```

A unicidade exata depende da estratégia de persistência escolhida.

## ChargeCycle

```text id="d58za0"
unique(charge_definition_id, code)
index(charge_definition_id)
index(status)
index(due_date)
```

## MemberCharge

```text id="qz2j4k"
index(user_id)
index(charge_definition_id)
index(charge_cycle_id)
index(status)
unique(user_id, charge_cycle_id)
```

## MemberPayment

```text id="e7ag9w"
index(member_charge_id)
index(status)
index(paid_at)
```

Pode ser avaliado reforço para impedir múltiplos payments editáveis por charge.

Como nem todo banco suporta índice parcial da mesma forma, a estratégia deve ser definida conforme o banco usado.

## MemberRefund

```text id="g4jj95"
index(member_charge_id)
index(member_payment_id)
index(charge_cycle_id)
index(user_id)
index(status)
```

Pode ser avaliado reforço para impedir múltiplos refunds ativos por payment.

## Pendência 7 — Ports externos

O Billing depende de ports para consultar informações de outros módulos.

Ports esperados:

```text id="d9jf3w"
BillingMembershipPort
BillingAuthorizationPort
BillingEventPort
PaymentReceiptStoragePort
```

## BillingMembershipPort

Responsável por consultar membros ativos.

Usado para:

* validar se existe membro ativo por userId;
* buscar todos os membros ativos;
* filtrar usuários elegíveis para cobrança.

## BillingAuthorizationPort

Responsável por consultar roles e usuários associados a roles.

Usado para:

* validar se uma role existe e está ativa;
* buscar usuários vinculados a uma role.

## BillingEventPort

Responsável por consultar eventos e participantes confirmados.

Usado para:

* validar se um evento existe;
* buscar participantes confirmados de um evento.

## PaymentReceiptStoragePort

Responsável por armazenar comprovantes de pagamento.

Usado no envio e atualização de `MemberPayment`.

## Pendência 8 — Storage de comprovantes

O storage de comprovantes deve validar e armazenar arquivos de forma segura.

Validações recomendadas:

* arquivo obrigatório;
* arquivo não vazio;
* tamanho máximo;
* content type permitido;
* extensão permitida;
* nome seguro;
* geração de storage key interna;
* URL controlada.

O controller não deve salvar arquivo diretamente.

O controller apenas converte `MultipartFile` para `PaymentReceiptFile` e delega ao core.

O core delega ao `PaymentReceiptStoragePort`.

## Pendência 9 — Exposição de receiptStorageKey

Avaliar se `receiptStorageKey` deve ser exposto na API.

Em geral:

```text id="3n34gr"
receiptUrl = dado útil para cliente/admin
receiptStorageKey = detalhe interno de storage
```

Pode ser melhor expor apenas `receiptUrl` nos responses públicos.

Se `receiptStorageKey` for necessário para auditoria administrativa, avaliar DTO separado para admin.

## Pendência 10 — Permissões BILLING

As permissões usadas nos controllers devem ser registradas no módulo de authorization.

Permissões observadas no Billing:

```text id="75d5hb"
BILLING_CHARGE_DEFINITION_CREATE
BILLING_CHARGE_DEFINITION_UPDATE
BILLING_CHARGE_DEFINITION_READ

BILLING_CHARGE_ASSIGNMENT_CREATE
BILLING_CHARGE_ASSIGNMENT_READ
BILLING_CHARGE_ASSIGNMENT_UPDATE

BILLING_CHARGE_CYCLE_GENERATE
BILLING_CHARGE_CYCLE_READ
BILLING_CHARGE_CYCLE_CANCEL
BILLING_CHARGE_CYCLE_FINISH
BILLING_CHARGE_CYCLE_ARCHIVE

BILLING_MEMBER_CHARGE_READ
BILLING_MEMBER_CHARGE_UPDATE
BILLING_MEMBER_CHARGE_CANCEL

BILLING_PAYMENT_READ
BILLING_PAYMENT_CONFIRM
BILLING_PAYMENT_REJECT

BILLING_REFUND_READ
BILLING_REFUND_APPROVE
BILLING_REFUND_REJECT
BILLING_REFUND_MARK_AS_REFUNDED
BILLING_REFUND_EXPIRE
BILLING_REFUND_CANCEL
```

Essas permissões devem ser adicionadas em:

```text id="b6a7zb"
PermissionCode
PermissionDefinition
ModuleCode, se necessário
```

## Pendência 11 — Segurança dos endpoints do membro

Alguns endpoints são do próprio usuário autenticado e não usam permissões administrativas.

Exemplos:

```text id="2n14y3"
GET /billing/me/member-charges
GET /billing/me/member-charges/{memberChargeId}
POST /billing/member-charges/{memberChargeId}/payments
PUT /billing/member-payments/{paymentId}
POST /billing/member-payments/{paymentId}/refund-request
GET /billing/users/me/member-refunds
PATCH /billing/member-refunds/{refundId}/request
```

Esses endpoints devem exigir autenticação, mas não necessariamente authority administrativa.

A validação de propriedade do recurso deve ser feita no service.

## Pendência 12 — Padronização de rotas "me"

Há uma pequena diferença de padrão entre rotas do próprio usuário:

```text id="3240kq"
GET /billing/me/member-charges
GET /billing/users/me/member-refunds
```

Avaliar padronização futura.

Sugestão:

```text id="z0iw6z"
GET /billing/me/member-charges
GET /billing/me/member-refunds
```

Isso melhora consistência da API.

## Pendência 13 — Testes de domínio

Criar testes unitários para entidades de domínio.

Entidades prioritárias:

```text id="x2qp2o"
ChargeDefinition
ChargeAssignment
ChargeCycle
MemberCharge
MemberPayment
MemberRefund
```

Cenários importantes:

* transições válidas;
* transições inválidas;
* validações de amount;
* validações de policy;
* cálculo de effectiveStatus;
* atualização de payment rejeitado;
* confirmação de payment;
* cancelamento de cycle;
* criação de refund eligibility.

## Pendência 14 — Testes de services

Criar testes para os services de aplicação.

Services prioritários:

```text id="qcnbb4"
AdminChargeDefinitionService
AdminChargeAssignmentService
AdminChargeCycleService
MemberChargeService
AdminMemberChargeService
MemberPaymentService
AdminMemberPaymentService
MemberRefundService
AdminMemberRefundService
```

Cenários importantes:

* gerar cycle com assignments;
* bloquear cycle sem alvo elegível;
* cancelar cycle e gerar refunds;
* bloquear finalAmount com payment pendente;
* bloquear múltiplos payments editáveis;
* permitir PUT em payment rejeitado;
* bloquear PUT em payment confirmado;
* confirmar payment depois do prazo;
* bloquear nova submissão após expiração;
* solicitar refund de payment próprio.

## Pendência 15 — Testes de controller

Criar testes para controllers com MockMvc.

Validar:

* status HTTP;
* payload de request;
* payload de response;
* validações Jakarta;
* permissões;
* extração de usuário autenticado;
* multipart em payment;
* responses de erro.

## Pendência 16 — Testes de concorrência

Criar testes de integração para validar locks reais.

Cenários importantes:

```text id="d0gq1b"
PUT payment x confirm payment
updateFinalAmount x submit payment
cancel charge x submit payment
confirm payment x cancel charge
```

Esses testes dependem da infraestrutura e do banco real ou ambiente compatível.

## Pendência 17 — Schedulers futuros

Schedulers podem ser implementados futuramente para:

* gerar ciclos recorrentes;
* expirar elegibilidade de refund;
* executar rotinas administrativas.

Schedulers não devem ser criados para atualizar status `OVERDUE` ou `EXPIRED` de `MemberCharge`.

Esses estados são calculados em runtime.

## Pendência 18 — Geração automática de ciclos

A geração automática de ciclos deve usar o mesmo fluxo de `AdminChargeCycleService.generate`.

Não deve existir uma regra paralela para scheduler.

O scheduler deve apenas chamar o caso de uso existente.

## Pendência 19 — Expiração de refund eligibility

A expiração automática de refund eligibility pode ser feita futuramente por scheduler.

Esse scheduler deve:

* buscar refunds elegíveis com janela expirada;
* chamar método de domínio apropriado;
* salvar alteração.

## Pendência 20 — Auditoria futura

O módulo já registra alguns usuários responsáveis por ações, como:

* generatedByUserId;
* canceledByUserId;
* finishedByUserId;
* archivedByUserId;
* confirmedByUserId;
* rejectedByUserId;
* requestedByUserId;
* approvedByUserId;
* refundedByUserId.

Pode ser avaliado futuramente um modelo de auditoria mais amplo.

## Pendência 21 — Observabilidade

Avaliar logs de eventos financeiros relevantes:

* geração de cycle;
* cancelamento de cycle;
* criação de member charges;
* envio de payment;
* confirmação de payment;
* rejeição de payment;
* criação de refund;
* aprovação de refund;
* marcação como refunded.

Logs não devem substituir persistência de estado.

## Pendência 22 — Documentação OpenAPI

Os controllers já possuem anotações de documentação.

Após estabilizar DTOs e rotas, revisar:

* summaries;
* descriptions;
* exemplos;
* responses de erro;
* consumes multipart;
* permissões necessárias.

## Pendência 23 — REST Docs

Pode ser criado Spring REST Docs para fluxos críticos.

Fluxos recomendados:

```text id="1ci37u"
criar ChargeDefinition
criar ChargeAssignment
gerar ChargeCycle
consultar minhas MemberCharges
enviar MemberPayment
atualizar MemberPayment
confirmar MemberPayment
rejeitar MemberPayment
cancelar ChargeCycle
solicitar MemberRefund
aprovar MemberRefund
```

## Pendência 24 — Consistência de nomenclatura

Manter os nomes conceituais consistentes:

```text id="vke84h"
ChargeDefinition = definição/modelo
ChargeAssignment = regra de atribuição
ChargeCycle = lote histórico
MemberCharge = cobrança/dívida individual
MemberPayment = envio de pagamento
MemberRefund = processo de reembolso
```

Evitar misturar:

```text id="ycutux"
ChargeCycle como responsável por pagamento individual
MemberPayment como quitação automática
Refund aprovado como dinheiro já devolvido
```

## Pendência 25 — Regras que não devem ser implementadas

Não implementar:

```text id="6y50xc"
endpoint de lock/deslock de edição
scheduler para OVERDUE/EXPIRED de MemberCharge
status persistido OVERDUE
status persistido EXPIRED
refresh manual de status de MemberCharge
reescrita de histórico após mudar ChargeDefinition
reescrita de histórico após mudar ChargeAssignment
```

## Checklist antes de fechar infra

Antes de considerar a infra do Billing fechada, validar:

```text id="1j8ckr"
[ ] Todas as entities JPA criadas.
[ ] Todos os mappers criados.
[ ] Todos os repositories do core implementados.
[ ] Todos os ports externos implementados.
[ ] Locks pessimistas reais implementados.
[ ] Migrations criadas.
[ ] Índices criados.
[ ] Permissões BILLING_* registradas.
[ ] Storage de comprovante implementado.
[ ] Testes unitários de domínio criados.
[ ] Testes de service criados.
[ ] Testes de controller criados.
[ ] Testes de concorrência planejados ou implementados.
[ ] Swagger revisado.
```

## Decisão final

O desenho de API + CORE do Billing está fechado.

As próximas alterações devem preservar as regras documentadas nos arquivos:

```text id="7x8frp"
README.md
glossary.md
business-rules.md
flows.md
state-models.md
concurrency.md
implementation-notes.md
```
