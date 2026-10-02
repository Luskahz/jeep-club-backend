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
- A integração de eventos usa `publications.api.module.EventQuery`. A geração
  incremental de dívida não exige confirmação prévia da inscrição.
- Credenciais, usuários, roles e o provider físico de arquivos não pertencem a
  Billing.

O módulo expõe contratos Java read-only mínimos em `api.module` para a
política de membritude: `ChargeDefinitionQuery`, que informa se uma definição
está ativa, e `MembershipChargeQuery`, que recebe somente definição e usuário e
devolve um resultado financeiro sem expor entities, repositories, valores ou
ciclos. A superfície externa de administração financeira continua HTTP; as
integrações consumidas estão descritas em [Fluxos](flows.md).

Para Event, `EventChargeCatalogQuery` consulta definições ACTIVE/ONE_TIME,
`EventBillingCommand` cria definição inline, garante assignment/ciclo/cobrança
e cancela os ciclos ainda `GENERATED` daquele Event; `EventFinancialQuery` consulta
estado efetivo, estado de pagamento e instante da submissão, em lote.
`evaluate` distingue também `CHARGE_NOT_FOUND`. Nenhum desses contratos expõe
entity ou repository. Criação inline força ONE_TIME e AFTER_DUE_DATE.

`EventChargeContext` mantém a identidade estruturada Event + definição +
assignment + ciclo. A tabela retida tem unicidade de Event/definição,
assignment e ciclo. O código textual do ciclo não é sua chave de idempotência.
MemberCharges usam os snapshots do ciclo, inclusive para inscrições posteriores.
Ciclos com contexto Event são excluídos da seleção de `MembershipChargeQuery`;
a reutilização da definição não substitui a obrigação normal de membership.

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


## Exportações CSV/PDF — BACK-410

Cinco produtos: definições × atribuições (inclui sem atribuição), ciclos históricos, cobranças, pagamentos e reembolsos. Filtros de ano/mês derivam do vencimento do ciclo; EventChargeContext fornece o vínculo de evento. Cobranças por chargeCycleId formam o relatório do ciclo e incluem o último pagamento, sem multiplicar cobranças. Situação efetiva usa o domínio existente. EventFinancialQuery acrescenta snapshots e vencimento mantendo o construtor anterior; EventFinanceReportQuery fornece definições e totais por status, com limites antes da composição. Nome/CPF, papel e evento são resolvidos por contratos públicos em lote.

Contratos HTTP, limites, segurança e evidências estão em `docs/exports/` na raiz do repositório. As exportações são administrativas, sem paginação HTTP, com auditoria síncrona e `Cache-Control: no-store`.
