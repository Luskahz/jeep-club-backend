# Rotas da API do Billing

## Objetivo

Este documento registra as rotas HTTP planejadas e implementadas na camada `api` do módulo `billing`.

Ele deve ser usado como referência para:

* documentação OpenAPI;
* testes de controller;
* configuração de permissões;
* revisão de segurança;
* integração com frontend;
* padronização de endpoints.

## Convenções gerais

## Prefixo

Todas as rotas do módulo usam o prefixo:

```text id="ev1de1"
/billing
```

## Rotas administrativas

Rotas administrativas exigem permissões específicas via authority.

Exemplo:

```java id="vs6k37"
// @PreAuthorize("hasAuthority('BILLING_PAYMENT_CONFIRM')")
```

## Rotas do usuário autenticado

Rotas do próprio membro exigem autenticação, mas não necessariamente uma permissão administrativa.

A validação de propriedade do recurso deve acontecer no service.

Exemplo:

```text id="wym3jn"
GET /billing/me/member-charges
PUT /billing/member-payments/{paymentId}
```

## Respostas

Controllers devem retornar DTOs de response.

Services devem retornar results.

O controller deve converter:

```text id="zus9l5"
Result -> Response
```

A regra de negócio não deve ficar no controller.

## ChargeDefinition

## Criar definição de cobrança

```http id="tbaorq"
POST /billing/charge-definitions
```

### Permissão

```text id="f6ke5f"
BILLING_CHARGE_DEFINITION_CREATE
```

### Objetivo

Cria uma definição de cobrança para usos futuros.

### Request

```text id="if4od8"
ChargeDefinitionRequest
```

### Response

```text id="qxrx9t"
201 CREATED
ChargeDefinitionResponse
```

## Atualizar definição de cobrança

```http id="ehbr5k"
PUT /billing/charge-definitions/{id}
```

### Permissão

```text id="o1qz5l"
BILLING_CHARGE_DEFINITION_UPDATE
```

### Objetivo

Atualiza uma definição de cobrança.

A alteração afeta apenas usos futuros.

### Request

```text id="x5dc43"
ChargeDefinitionUpdateRequest
```

### Response

```text id="y1h5yi"
200 OK
ChargeDefinitionResponse
```

## Listar definições de cobrança

```http id="u51eur"
GET /billing/charge-definitions
```

### Permissão

```text id="iapeln"
BILLING_CHARGE_DEFINITION_READ
```

### Objetivo

Lista definições de cobrança de forma paginada.

### Response

```text id="fek2rf"
200 OK
Page<ChargeDefinitionSummaryResponse>
```

## Buscar definição por ID

```http id="cnv4vs"
GET /billing/charge-definitions/{id}
```

### Permissão

```text id="lcqp7i"
BILLING_CHARGE_DEFINITION_READ
```

### Objetivo

Consulta uma definição de cobrança específica.

### Response

```text id="ftrje7"
200 OK
ChargeDefinitionResponse
```

## Ativar definição

```http id="pxq7vm"
PATCH /billing/charge-definitions/{id}/activate
```

### Permissão

```text id="vlfpwl"
BILLING_CHARGE_DEFINITION_UPDATE
```

### Objetivo

Ativa uma definição de cobrança.

### Response

```text id="d3vlyl"
200 OK
ChargeDefinitionResponse
```

## Desativar definição

```http id="z27lm4"
PATCH /billing/charge-definitions/{id}/deactivate
```

### Permissão

```text id="hpdav8"
BILLING_CHARGE_DEFINITION_UPDATE
```

### Objetivo

Desativa uma definição de cobrança sem apagar histórico.

### Response

```text id="u1fqai"
200 OK
ChargeDefinitionResponse
```

## Arquivar definição

```http id="f6ztcz"
PATCH /billing/charge-definitions/{id}/archive
```

### Permissão

```text id="r0081w"
BILLING_CHARGE_DEFINITION_UPDATE
```

### Objetivo

Arquiva uma definição de cobrança.

### Response

```text id="smg9nn"
200 OK
ChargeDefinitionResponse
```

## ChargeAssignment

## Atribuir cobrança a todos os membros

```http id="clv04x"
POST /billing/charge-definitions/{chargeDefinitionId}/assignments/all-members
```

### Permissão

```text id="pij7r8"
BILLING_CHARGE_ASSIGNMENT_CREATE
```

### Objetivo

Cria regra para aplicar a cobrança a todos os membros ativos.

### Response

```text id="uo3m1b"
201 CREATED
ChargeAssignmentResponse
```

## Atribuir cobrança a um usuário

```http id="k4baj5"
POST /billing/charge-definitions/{chargeDefinitionId}/assignments/users/{userId}
```

### Permissão

```text id="adwrn9"
BILLING_CHARGE_ASSIGNMENT_CREATE
```

### Objetivo

Cria regra para aplicar a cobrança a um usuário específico.

### Response

```text id="x2jiiq"
201 CREATED
ChargeAssignmentResponse
```

## Atribuir cobrança a uma role

```http id="lcfdye"
POST /billing/charge-definitions/{chargeDefinitionId}/assignments/roles/{roleId}
```

### Permissão

```text id="zv6h48"
BILLING_CHARGE_ASSIGNMENT_CREATE
```

### Objetivo

Cria regra para aplicar a cobrança a usuários associados a uma role.

### Response

```text id="r1skkx"
201 CREATED
ChargeAssignmentResponse
```

## Atribuir cobrança a participantes de evento

```http id="d3xeao"
POST /billing/charge-definitions/{chargeDefinitionId}/assignments/events/{eventId}/participants
```

### Permissão

```text id="n2cm6n"
BILLING_CHARGE_ASSIGNMENT_CREATE
```

### Objetivo

Cria regra para aplicar a cobrança aos participantes confirmados de um evento.

### Response

```text id="lcmp7h"
201 CREATED
ChargeAssignmentResponse
```

## Listar atribuições de uma definição

```http id="cdbmb2"
GET /billing/charge-definitions/{chargeDefinitionId}/assignments
```

### Permissão

```text id="j1vx47"
BILLING_CHARGE_ASSIGNMENT_READ
```

### Objetivo

Lista regras de atribuição vinculadas a uma definição de cobrança.

### Response

```text id="zy3v0o"
200 OK
Page<ChargeAssignmentResponse>
```

## Buscar atribuição por ID

```http id="jjxcg6"
GET /billing/charge-assignments/{assignmentId}
```

### Permissão

```text id="usobf2"
BILLING_CHARGE_ASSIGNMENT_READ
```

### Objetivo

Consulta uma atribuição específica.

### Response

```text id="copu3z"
200 OK
ChargeAssignmentResponse
```

## Ativar atribuição

```http id="xfbq6x"
PATCH /billing/charge-assignments/{assignmentId}/activate
```

### Permissão

```text id="sft015"
BILLING_CHARGE_ASSIGNMENT_UPDATE
```

### Objetivo

Ativa uma atribuição.

### Response

```text id="iw68g4"
200 OK
ChargeAssignmentResponse
```

## Desativar atribuição

```http id="in4vt4"
PATCH /billing/charge-assignments/{assignmentId}/deactivate
```

### Permissão

```text id="xxpkuc"
BILLING_CHARGE_ASSIGNMENT_UPDATE
```

### Objetivo

Desativa uma atribuição.

### Response

```text id="w2lwmf"
200 OK
ChargeAssignmentResponse
```

## ChargeCycle

## Gerar ciclo de cobrança

```http id="xsp2s6"
POST /billing/charge-definitions/{chargeDefinitionId}/cycles
```

### Permissão

```text id="yznbdw"
BILLING_CHARGE_CYCLE_GENERATE
```

### Objetivo

Gera um ciclo de cobrança e cria cobranças individuais para os membros elegíveis.

### Request

```text id="agk26n"
GenerateChargeCycleRequest
```

### Response

```text id="ngffnz"
201 CREATED
GenerateChargeCycleResponse
```

## Listar ciclos de uma definição

```http id="mn0or5"
GET /billing/charge-definitions/{chargeDefinitionId}/cycles
```

### Permissão

```text id="jki3rb"
BILLING_CHARGE_CYCLE_READ
```

### Objetivo

Lista ciclos gerados para uma definição.

### Response

```text id="uorqau"
200 OK
Page<ChargeCycleSummaryResponse>
```

## Buscar ciclo por ID

```http id="v4d8is"
GET /billing/charge-cycles/{cycleId}
```

### Permissão

```text id="em94fe"
BILLING_CHARGE_CYCLE_READ
```

### Objetivo

Consulta um ciclo específico.

### Response

```text id="d48x3f"
200 OK
ChargeCycleResponse
```

## Cancelar ciclo

```http id="f8lwcl"
PATCH /billing/charge-cycles/{cycleId}/cancel
```

### Permissão

```text id="spce10"
BILLING_CHARGE_CYCLE_CANCEL
```

### Objetivo

Cancela um ciclo gerado, cancela cobranças abertas e cria elegibilidade de reembolso para pagamentos elegíveis.

### Response

```text id="xxw7ez"
200 OK
ChargeCycleResponse
```

## Finalizar ciclo

```http id="jqyof9"
PATCH /billing/charge-cycles/{cycleId}/finish
```

### Permissão

```text id="tbc8by"
BILLING_CHARGE_CYCLE_FINISH
```

### Objetivo

Finaliza administrativamente um ciclo sem efeito financeiro direto.

### Response

```text id="ycyfhp"
200 OK
ChargeCycleResponse
```

## Arquivar ciclo

```http id="k5jcxm"
PATCH /billing/charge-cycles/{cycleId}/archive
```

### Permissão

```text id="u19e3r"
BILLING_CHARGE_CYCLE_ARCHIVE
```

### Objetivo

Arquiva um ciclo finalizado ou cancelado.

### Response

```text id="ocuh29"
200 OK
ChargeCycleResponse
```

## MemberCharge

## Listar cobranças de membros

```http id="gsw5al"
GET /billing/member-charges
```

### Permissão

```text id="snlcth"
BILLING_MEMBER_CHARGE_READ
```

### Objetivo

Lista cobranças individuais de membros de forma administrativa.

### Filtros

```text id="pm8un4"
userId
status
pageable
```

### Response

```text id="vzmm8x"
200 OK
Page<MemberChargeSummaryResponse>
```

## Buscar cobrança por ID

```http id="oz5q5h"
GET /billing/member-charges/{memberChargeId}
```

### Permissão

```text id="dqhsmw"
BILLING_MEMBER_CHARGE_READ
```

### Objetivo

Consulta uma cobrança individual específica.

### Response

```text id="pshqpw"
200 OK
MemberChargeResponse
```

## Listar minhas cobranças

```http id="usqtk7"
GET /billing/me/member-charges
```

### Permissão

```text id="l0z0ye"
Usuário autenticado
```

### Objetivo

Lista cobranças do usuário autenticado.

### Filtros

```text id="kfjxyo"
status
pageable
```

### Response

```text id="g9pv84"
200 OK
Page<MemberChargeSummaryResponse>
```

## Buscar minha cobrança por ID

```http id="kxyf0a"
GET /billing/me/member-charges/{memberChargeId}
```

### Permissão

```text id="q4676q"
Usuário autenticado
```

### Objetivo

Consulta uma cobrança do usuário autenticado, garantindo propriedade do recurso.

### Response

```text id="rvzs3c"
200 OK
MemberChargeResponse
```

## Atualizar valor final da cobrança

```http id="tsgco0"
PATCH /billing/member-charges/{memberChargeId}/final-amount
```

### Permissão

```text id="arv1zx"
BILLING_MEMBER_CHARGE_UPDATE
```

### Objetivo

Atualiza o valor final de uma cobrança pendente.

### Request

```text id="i5e84k"
UpdateMemberChargeFinalAmountRequest
```

### Response

```text id="d8gz4q"
200 OK
MemberChargeResponse
```

## Cancelar cobrança individual

```http id="nqbfgf"
PATCH /billing/member-charges/{memberChargeId}/cancel
```

### Permissão

```text id="puyrn3"
BILLING_MEMBER_CHARGE_CANCEL
```

### Objetivo

Cancela uma cobrança individual ainda não paga.

### Response

```text id="kys555"
200 OK
MemberChargeResponse
```

## MemberPayment

## Enviar pagamento

```http id="yo46x7"
POST /billing/member-charges/{memberChargeId}/payments
```

### Permissão

```text id="yeei24"
Usuário autenticado
```

### Consumes

```text id="g8wup7"
multipart/form-data
```

### Objetivo

Permite que o membro envie comprovante de pagamento para uma cobrança própria.

### Request

```text id="g4b2u9"
SubmitMemberPaymentRequest
```

### Response

```text id="fp2vz8"
201 CREATED
MemberPaymentResponse
```

## Atualizar pagamento enviado

```http id="l5x4nd"
PUT /billing/member-payments/{paymentId}
```

### Permissão

```text id="ba7hw2"
Usuário autenticado
```

### Consumes

```text id="ewh7nw"
multipart/form-data
```

### Objetivo

Permite que o membro atualize um pagamento próprio enquanto ele estiver pendente de validação ou rejeitado.

### Request

```text id="btf7bd"
UpdateMemberPaymentRequest
```

### Response

```text id="sqlsjo"
200 OK
MemberPaymentResponse
```

## Listar pagamentos

```http id="x29r3r"
GET /billing/member-payments
```

### Permissão

```text id="xx4441"
BILLING_PAYMENT_READ
```

### Objetivo

Lista pagamentos de membros de forma administrativa.

### Filtros

```text id="mkhzoc"
status
pageable
```

### Response

```text id="zrwy8u"
200 OK
Page<MemberPaymentSummaryResponse>
```

## Buscar pagamento por ID

```http id="g5h6t7"
GET /billing/member-payments/{paymentId}
```

### Permissão

```text id="qadm17"
BILLING_PAYMENT_READ
```

### Objetivo

Consulta os dados completos de um pagamento.

### Response

```text id="dwv2d3"
200 OK
MemberPaymentResponse
```

## Confirmar pagamento

```http id="rfyb2v"
PATCH /billing/member-payments/{paymentId}/confirm
```

### Permissão

```text id="wnpf1o"
BILLING_PAYMENT_CONFIRM
```

### Objetivo

Confirma um pagamento pendente de validação e marca a cobrança vinculada como paga.

### Response

```text id="d14liq"
200 OK
MemberPaymentResponse
```

## Rejeitar pagamento

```http id="wosda9"
PATCH /billing/member-payments/{paymentId}/reject
```

### Permissão

```text id="zqhcbl"
BILLING_PAYMENT_REJECT
```

### Objetivo

Rejeita um pagamento pendente de validação.

### Request

```text id="bbig86"
RejectMemberPaymentRequest
```

### Response

```text id="g52tom"
200 OK
MemberPaymentResponse
```

## MemberRefund

## Solicitar reembolso por pagamento

```http id="lnkdk4"
POST /billing/member-payments/{paymentId}/refund-request
```

### Permissão

```text id="onoj1u"
Usuário autenticado
```

### Objetivo

Permite que o usuário autenticado solicite reembolso de um pagamento próprio confirmado ou pendente de validação.

### Response

```text id="wxyz3v"
200 OK
MemberRefundResponse
```

## Listar reembolsos

```http id="qdhim4"
GET /billing/member-refunds
```

### Permissão

```text id="y4z6bb"
BILLING_REFUND_READ
```

### Objetivo

Lista reembolsos de membros de forma administrativa.

### Filtros

```text id="hj279d"
status
pageable
```

### Response

```text id="m0ovp7"
200 OK
Page<MemberRefundSummaryResponse>
```

## Buscar reembolso por ID

```http id="ich9fe"
GET /billing/member-refunds/{refundId}
```

### Permissão

```text id="xq9rv1"
BILLING_REFUND_READ
```

### Objetivo

Consulta os dados completos de um reembolso.

### Response

```text id="ee9kt0"
200 OK
MemberRefundResponse
```

## Listar meus reembolsos

```http id="zz6zal"
GET /billing/users/me/member-refunds
```

### Permissão

```text id="qscvaf"
Usuário autenticado
```

### Objetivo

Lista os reembolsos vinculados ao usuário autenticado.

### Response

```text id="y66n6c"
200 OK
Page<MemberRefundSummaryResponse>
```

## Listar reembolsos de um ciclo

```http id="lqkld0"
GET /billing/charge-cycles/{cycleId}/member-refunds
```

### Permissão

```text id="ss7j2l"
BILLING_REFUND_READ
```

### Objetivo

Lista reembolsos vinculados a um ciclo de cobrança.

### Response

```text id="r5gwn3"
200 OK
Page<MemberRefundSummaryResponse>
```

## Solicitar refund elegível

```http id="pq9yqv"
PATCH /billing/member-refunds/{refundId}/request
```

### Permissão

```text id="u9imn1"
Usuário autenticado
```

### Objetivo

Permite que o usuário autenticado solicite um reembolso que já está elegível.

### Response

```text id="u57zt6"
200 OK
MemberRefundResponse
```

## Aprovar reembolso

```http id="q64yxi"
PATCH /billing/member-refunds/{refundId}/approve
```

### Permissão

```text id="eqmby9"
BILLING_REFUND_APPROVE
```

### Objetivo

Aprova um reembolso elegível ou solicitado.

### Response

```text id="orx4sm"
200 OK
MemberRefundResponse
```

## Rejeitar reembolso

```http id="p3hkpc"
PATCH /billing/member-refunds/{refundId}/reject
```

### Permissão

```text id="qvxp8z"
BILLING_REFUND_REJECT
```

### Objetivo

Rejeita um reembolso solicitado.

### Request

```text id="v1l6vm"
RejectMemberRefundRequest
```

### Response

```text id="rsze5y"
200 OK
MemberRefundResponse
```

## Marcar reembolso como realizado

```http id="mu286l"
PATCH /billing/member-refunds/{refundId}/mark-as-refunded
```

### Permissão

```text id="b2trui"
BILLING_REFUND_MARK_AS_REFUNDED
```

### Objetivo

Marca um reembolso aprovado como efetivamente realizado.

### Response

```text id="l5a9mg"
200 OK
MemberRefundResponse
```

## Expirar reembolso

```http id="jt699p"
PATCH /billing/member-refunds/{refundId}/expire
```

### Permissão

```text id="isxrde"
BILLING_REFUND_EXPIRE
```

### Objetivo

Expira manualmente uma elegibilidade de reembolso.

### Response

```text id="zw5i89"
200 OK
MemberRefundResponse
```

## Cancelar reembolso

```http id="etsnwh"
PATCH /billing/member-refunds/{refundId}/cancel
```

### Permissão

```text id="n7w10o"
BILLING_REFUND_CANCEL
```

### Objetivo

Cancela um processo de reembolso ainda ativo.

### Response

```text id="gu5rym"
200 OK
MemberRefundResponse
```

## Rotas que devem ser avaliadas para padronização

## Rota atual

```http id="mgk04y"
GET /billing/users/me/member-refunds
```

## Sugestão futura

```http id="w0ifbs"
GET /billing/me/member-refunds
```

## Motivo

O módulo já usa:

```http id="u5tdiw"
GET /billing/me/member-charges
GET /billing/me/member-charges/{memberChargeId}
```

Padronizar `me` logo após `/billing` melhora consistência da API.

## Resumo por permissão

```text id="zqotva"
BILLING_CHARGE_DEFINITION_CREATE
- POST /billing/charge-definitions

BILLING_CHARGE_DEFINITION_UPDATE
- PUT /billing/charge-definitions/{id}
- PATCH /billing/charge-definitions/{id}/activate
- PATCH /billing/charge-definitions/{id}/deactivate
- PATCH /billing/charge-definitions/{id}/archive

BILLING_CHARGE_DEFINITION_READ
- GET /billing/charge-definitions
- GET /billing/charge-definitions/{id}

BILLING_CHARGE_ASSIGNMENT_CREATE
- POST /billing/charge-definitions/{chargeDefinitionId}/assignments/all-members
- POST /billing/charge-definitions/{chargeDefinitionId}/assignments/users/{userId}
- POST /billing/charge-definitions/{chargeDefinitionId}/assignments/roles/{roleId}
- POST /billing/charge-definitions/{chargeDefinitionId}/assignments/events/{eventId}/participants

BILLING_CHARGE_ASSIGNMENT_READ
- GET /billing/charge-definitions/{chargeDefinitionId}/assignments
- GET /billing/charge-assignments/{assignmentId}

BILLING_CHARGE_ASSIGNMENT_UPDATE
- PATCH /billing/charge-assignments/{assignmentId}/activate
- PATCH /billing/charge-assignments/{assignmentId}/deactivate

BILLING_CHARGE_CYCLE_GENERATE
- POST /billing/charge-definitions/{chargeDefinitionId}/cycles

BILLING_CHARGE_CYCLE_READ
- GET /billing/charge-definitions/{chargeDefinitionId}/cycles
- GET /billing/charge-cycles/{cycleId}

BILLING_CHARGE_CYCLE_CANCEL
- PATCH /billing/charge-cycles/{cycleId}/cancel

BILLING_CHARGE_CYCLE_FINISH
- PATCH /billing/charge-cycles/{cycleId}/finish

BILLING_CHARGE_CYCLE_ARCHIVE
- PATCH /billing/charge-cycles/{cycleId}/archive

BILLING_MEMBER_CHARGE_READ
- GET /billing/member-charges
- GET /billing/member-charges/{memberChargeId}

BILLING_MEMBER_CHARGE_UPDATE
- PATCH /billing/member-charges/{memberChargeId}/final-amount

BILLING_MEMBER_CHARGE_CANCEL
- PATCH /billing/member-charges/{memberChargeId}/cancel

BILLING_PAYMENT_READ
- GET /billing/member-payments
- GET /billing/member-payments/{paymentId}

BILLING_PAYMENT_CONFIRM
- PATCH /billing/member-payments/{paymentId}/confirm

BILLING_PAYMENT_REJECT
- PATCH /billing/member-payments/{paymentId}/reject

BILLING_REFUND_READ
- GET /billing/member-refunds
- GET /billing/member-refunds/{refundId}
- GET /billing/charge-cycles/{cycleId}/member-refunds

BILLING_REFUND_APPROVE
- PATCH /billing/member-refunds/{refundId}/approve

BILLING_REFUND_REJECT
- PATCH /billing/member-refunds/{refundId}/reject

BILLING_REFUND_MARK_AS_REFUNDED
- PATCH /billing/member-refunds/{refundId}/mark-as-refunded

BILLING_REFUND_EXPIRE
- PATCH /billing/member-refunds/{refundId}/expire

BILLING_REFUND_CANCEL
- PATCH /billing/member-refunds/{refundId}/cancel
```

## Rotas autenticadas sem permissão administrativa específica

```text id="m91ovy"
GET /billing/me/member-charges
GET /billing/me/member-charges/{memberChargeId}
POST /billing/member-charges/{memberChargeId}/payments
PUT /billing/member-payments/{paymentId}
POST /billing/member-payments/{paymentId}/refund-request
GET /billing/users/me/member-refunds
PATCH /billing/member-refunds/{refundId}/request
```

Essas rotas devem exigir autenticação.

A autorização de propriedade do recurso deve ser feita na camada de service.
