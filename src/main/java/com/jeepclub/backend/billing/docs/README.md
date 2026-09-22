# Billing

Leia primeiro a [governança global](../../../../../../../../docs/architecture/README.md), a
[organização dos módulos](../../../../../../../../docs/architecture/module-organization.md)
e as [regras de desenvolvimento](../../../../../../../../docs/architecture/feature-development-rules.md).

Billing é o bounded context responsável por configurar cobranças, gerar débitos
individuais, receber e validar pagamentos e controlar processos de reembolso. O
módulo preserva o fato financeiro gerado mesmo quando a configuração que o
originou muda.

## Responsabilidades e fronteiras

- `ChargeDefinition` e `ChargeAssignment` configuram cobranças futuras.
- `ChargeCycle`, `MemberCharge`, `MemberPayment` e `MemberRefund` registram fatos
  financeiros históricos e seus próprios estados.
- Identity informa usuários administrativamente ativos; Authorization informa
  roles e seus usuários. Billing consome esses contratos por portas próprias e
  adapters em `infra.integration`.
- A integração de eventos ainda é um adapter indisponível: atribuições para
  participantes de evento não podem ser criadas no estado atual.
- Credenciais, usuários, roles e o provider físico de arquivos não pertencem a
  Billing.

O módulo expõe dois contratos Java read-only mínimos em `api.module` para a
política de membritude: `ChargeDefinitionQuery`, que informa se uma definição
está ativa, e `MembershipChargeQuery`, que recebe somente definição e usuário e
devolve um resultado financeiro sem expor entities, repositories, valores ou
ciclos. A superfície externa de administração financeira continua HTTP; as
integrações consumidas estão descritas em [Fluxos](flows.md).

## Modelo conceitual

`ChargeDefinition` concentra nome, valor padrão, recorrência, obrigatoriedade e
política de aceitação de pagamento. Uma `ChargeAssignment` ativa seleciona o
público de uma definição ativa (`ALL_MEMBERS`, `USER`, `ROLE` ou
`EVENT_PARTICIPANTS`).

A geração de `ChargeCycle` resolve e deduplica os usuários elegíveis, copia a
configuração da definição para snapshots e cria uma `MemberCharge` por usuário.
Por isso, alterações posteriores na definição ou nas atribuições não reescrevem
ciclos e cobranças existentes.

`MemberCharge` guarda somente `PENDING`, `PAID` ou `CANCELED`; `OVERDUE` e
`EXPIRED` são estados efetivos calculados com `Clock`, vencimento e política do
snapshot. `MemberPayment` representa uma submissão com comprovante. Um
`MemberRefund` representa o processo separado de devolução.

Consulte:

- [Regras de negócio](business-rules.md) para invariantes financeiras;
- [Modelos de estado](state-models.md) para estados persistidos e transições;
- [Fluxos](flows.md) para geração, pagamento, reembolso e comprovante;
- [Concorrência](concurrency.md) para locks e compensação transacional;
- [Glossário](glossary.md) para a linguagem do contexto.

Para `MembershipChargeQuery`, Billing resolve primeiro o ciclo aplicável da
definição. Ciclos `ARCHIVED` são históricos; ciclos mensais precisam pertencer
ao mês corrente e ciclos anuais ao ano corrente. Um ciclo `ONE_TIME` permanece
aplicável enquanto não for arquivado. Se houver mais de um ciclo no período,
vence o vencimento mais recente já alcançado; sem ciclo vencido, vence o futuro
mais próximo. Só então Billing procura a cobrança do usuário por `chargeCycleId`
e usa a mesma data de referência capturada para a seleção do ciclo ao calcular
`MemberCharge.effectiveStatusAt(referenceDate)`.
O resultado público distingue prazo vigente, obrigação satisfeita, cancelamento,
pagamento necessário e cobrança inexistente. A ausência do ciclo recorrente do
período atual ou da cobrança do usuário nesse ciclo resulta em
`CHARGE_NOT_FOUND`. Membership decide a política de acesso; Billing não conhece
`@RequiresMembership`.

## Comprovantes

Billing valida arquivo, ownership e lifecycle; o contrato compartilhado
`FileStorage` armazena, carrega e remove bytes no namespace
`billing/payment-receipts`. Provider, filesystem, root, path security e I/O são
responsabilidade de `platform.storage`.

`receiptStorageKey` é referência interna persistida. O frontend recebe somente a
rota lógica `/billing/member-payments/{paymentId}/receipt`, autorizada pelo dono
da cobrança ou pela authority `BILLING_PAYMENT_READ`. Nenhuma rota pública aceita
storage key ou path físico.

## HTTP e persistência

Métodos, paths, parâmetros, multipart, schemas, permissions, paginação e erros
HTTP têm uma única fonte: `/v3/api-docs` e Swagger UI. As listagens usam o
envelope transversal `PageResponse<T>` na fronteira HTTP.

As Entities JPA representam o schema nesta fase. Não há Flyway, Liquibase nem
política de migrations versionadas; nenhuma mudança de schema deve inferir essa
obrigação sem decisão arquitetural própria.

## Testes relevantes

Os testes de domínio e aplicação caracterizam snapshots, estados, locks e regras
financeiras. Os testes de comprovante cobrem validação, storage, compensação,
persistência da key interna e autorização por `paymentId`. O contrato OpenAPI é
protegido por teste focal em `/v3/api-docs`.
