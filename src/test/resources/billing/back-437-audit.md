# BACK-437 — auditoria e evidências

## Baseline e escopo

- Repositório `Luskahz/jeep-club-backend`.
- `git fetch origin` executado; `origin/develop` confirmado em
  `0d8542fb3d90768573dc738cea85b23c1701b44c` no início da implementação, na branch
  `test/back-437-billing-hardening`.
- BACK-437 lida diretamente no Jira. O SHA `80ddc7d` da análise original não foi
  utilizado. A descrição pede PR; a instrução explícita desta execução pede
  somente branch remota, sem PR, merge ou encerramento da issue.
- Leitura: `docs/architecture/{README,module-organization,feature-development-rules}.md`
  e todos os arquivos atuais de `billing/docs/`.
- Suíte baseline: `mvnw.cmd test` e `mvnw.cmd verify` passaram, ambos com
  **1.109 testes, zero failures/errors, um skipped**. Logs locais em `target/`.

## Matriz produção → testes → gaps → ação

Os nomes abaixo identificam classes reais de teste. A classificação se refere
à cobertura comportamental anterior; não é inferida somente do JaCoCo.

| Superfície/comportamento | Evidência preexistente | Antes | Ação implementada |
| --- | --- | --- | --- |
| ChargeDefinition: normalização, valor, política, ativo/inativo/arquivado | geração em EventOperationsIntegrationTest | Parcial | BillingDomainLifecycleTest, BillingConfigurationServiceTest: unicidade, lifecycle, arquivamento de assignments e isolamento histórico |
| ChargeAssignment e quatro especializações | administrativeAudiencesAndMembershipRemainIndependentOfEventCycle | Parcial | transições/repetição, referências inválidas, targets e duplicatas por tipo; persistência de todas as especializações |
| ChargeCycle: snapshots, geração, deduplicação, cancel/finish/archive | EventOperationsIntegrationTest | Parcial | deduplicação das audiences e filtro de ativos, sem público/duplicado/inativo, efeitos separados de finish/archive, releitura após lock |
| MemberCharge: dinheiro, janela inclusiva e estados efetivos | MembershipChargeQueryServiceTest, PaidMembershipFlowIntegrationTest | Parcial | limites e desconto/restauração, estados terminais, datas persistidas incoerentes; administração e ownership |
| MemberPayment: submissão, revisão, reenvio, substituição | MemberPaymentServiceReceiptTest, EventOperationsIntegrationTest | Parcial | conflitos antes de storage, amount, charge fechada, confirmação sem recalcular janela, timestamps, rejeição e terminais |
| MemberRefund: estados, prazo e decisões | realPaymentSubmissionRejectionResubmissionAndRefundLifecycle, concurrentLateConfirmCreatesSingleRefund | Parcial | lifecycle completo, instante exato de expiração, atores, request/approve após janela, idempotência, ownership e filtros |
| AdminChargeDefinitionService | criação indireta de Event | Ausente na superfície geral | canonicalização de nome, duplicata, update/lifecycle, arquiva somente assignments ativos |
| AdminChargeAssignmentService | geração lê assignments criados diretamente | Ausente | USER/ROLE/EVENT/ALL_MEMBERS, targets ausentes, definição inativa/arquivada, duplicata e ativação/desativação |
| AdminChargeCycleService | geração/cancelamento de Event | Parcial | audiences deduplicadas, conflito de geração, reconsulta sob lock e efeitos financeiros de cada comando |
| MemberChargeService / AdminMemberChargeService | leitura de Membership, exports | Parcial | escopo member, filtros administrativos, alteração impedida por payment pendente, cancel sob lock |
| MemberPaymentService / AdminMemberPaymentService | receipt tests e Event flows | Parcial | validação financeira anterior a I/O, lock payment→charge, charge fechada, amount mismatch e confirmação tardia |
| MemberRefundService / AdminMemberRefundService | cancelamento de Event e um lifecycle | Parcial | request direto/eligibility/reuso de ativo/refunded, guards financeiros e todos os comandos administrativos |
| EventBillingService | EventOperationsIntegrationTest: vários Events/mesma definição, criação concorrente, cutoff, datas e snapshots | Relevante já coberta | BillingEventBoundaryTest acrescenta falhas de catálogo/contexto fechado/assignment inativo, parent lock e preservação de FINISHED (BACK-464) |
| MembershipChargeQuery | MembershipChargeQueryServiceTest, MemberChargeMembershipQueryAdapterTest, PaidMembershipFlowIntegrationTest | Coberta | preservados seleção por período, isolamento Event e avaliação pública; não reescrever esses testes |
| ChargeDefinitionQuery / EventFinanceReportQuery | Membership e exports | Parcial | IDs inválidos/ausentes/inativos, limite de lote 500/501 e delegação pública |
| Identity boundary | IdentityBillingMembershipAdapterTest e PaidMembershipFlowIntegrationTest | Coberta | preservados; mocks dos services novos permanecem no port consumidor |
| Authorization/Event adapters | flows reais de Publications | Parcial | exercício por geração e contexto de Event; catálogo/targets controlados pelos ports |
| Persistência/adapters/mappers | schema/payment mapper, MemberChargeMembershipQueryAdapterTest, EventOperationsIntegrationTest | Parcial | round-trip com flush/clear de assignments, ciclo histórico e todos os estados de refund; estados financeiros impossíveis rejeitados |
| Pessimistic locks / concorrência | concurrentCycleAndMemberChargeCreationIsIdempotent, concurrentLateConfirmCreatesSingleRefund | Parcial | disputa de duas confirmações reais em transações separadas; ordem payment→charge e releitura de charge nos testes focais |
| Receipt/storage, rollback/cleanup | PaymentReceiptValidatorTest, PaymentReceiptServiceTest, PaymentReceiptLifecycleTest, PaymentReceiptLifecycleTransactionIntegrationTest, PaymentReceiptLocalStorageIntegrationTest | Coberta | preservados; sem duplicar testes do provider; fluxo HTTP exerce a integração pública |
| HTTP member/admin | PaymentReceiptSecurityIntegrationTest, BillingOpenApiIntegrationTest | Parcial | BillingHttpWorkflowIntegrationTest: services reais para geração, revisão/reenvio/payment/refund, principal, ownership, permissions e paginação |
| Bean Validation / RFC 9457 / handlers | OpenAPI, HttpParameterValidationTest, receipt security | Parcial | payload ausente/inválido, enum inválido, ID zero/missing, multipart inválido e conflitos de domínio no HTTP real |
| Dados financeiros / internals | receipt tests, exports | Parcial | owner-scoped charges/refunds; receiptUrl público, sem receiptStorageKey nos detalhes/listagens de payment; erros estruturados |
| OpenAPI | BillingOpenApiIntegrationTest | Coberta | preservado teste focal completo; não alterar contrato para adequar aos testes |
| Exportações de Billing | ExportHttpIntegrationTest: período, contexto estruturado, último pagamento, cinco produtos após hard delete; ExportSecurity/OpenAPI tests | Relevante já coberta | BillingExportPreconditionsTest: status incompatível, entidade financeira inexistente, filtro de cobrança em produto de configuração |

## Decisões semânticas e dívida

Pesquisa de todo o projeto BACK paginada antes de criar tickets; conferidos
summary e description para evitar duplicação. Não alterar status/Sprint da BACK-437.

- **BACK-489 (nova, Task):** transições de MemberRefund não têm lock/version.
  `requestByMemberPaymentId` bloqueia payment; `request(refundId)` e comandos
  administrativos carregam refund por `findById`, sem `@Version` na entity.
  Decisões simultâneas podem se sobrescrever. Requer estratégia de concorrência
  coerente com os locks existentes, e não refatoração estética.
- **BACK-490 (nova, Task):** cancel interno de payment REJECTED mantém dados da
  rejeição, mas reconstitution de CANCELED os proíbe. Reproduzido por
  `BillingReconstitutionTest.unusedRejectedPaymentCancellationCurrentlyCannotBeReconstituted`.
  Método não é usado por nenhum service/endpoint: dívida latente e decisão de
  histórico, não bug de endpoint que justifique alterar política silenciosamente.
- As duas issues foram criadas sem Sprint, movidas explicitamente ao backlog do
  **board 34**, e relidas. Permanecem `Tarefas pendentes`, sem campo Sprint,
  fora de **Sprint Final — V2 e Produção**.
- **BACK-460 / BACK-464 (reutilizadas):** Event cancela apenas ciclos GENERATED;
  FINISHED permanece. O teste focal caracteriza o comportamento documentado.
- **BACK-465 (reutilizada):** confirmação além de 30 dias produz eligibility já
  expirada, ancorada no cancelamento; nenhuma alteração da política neste trabalho.
- **BACK-466 (reutilizada):** efeitos financeiros de cancelamento individual de
  Registration pertencem à decisão futura de Event/Billing.
- **BACK-471 (reutilizada):** `EventBillingService.findByEvent` materializa listas
  e faz filtros repetidos; não abrir outro ticket para os mesmos scans.
- Reenvio de REJECTED revalida prazo; replacement de PENDING_VALIDATION permite
  complementar comprovante após prazo se a cobrança segue PENDING. É comportamento
  deliberadamente documentado; teste focal preserva a assimetria. Nenhum bug presumido.
- `MemberCharge.isDue` expressa atraso de cobrança aberta; regra efetiva continua
  centralizada em `effectiveStatusAt`. Não abrir ticket de naming cosmético.
- `AdminMemberChargeService` e `MemberPaymentService` fazem mais de uma leitura de
  Clock. Observação de desenho temporal, sem reprodução de falha financeira neste
  escopo; não inventar bug nem alterar produção por conveniência de teste.

## Não aplicáveis e limites

- Não existe endpoint/app service para cancel interno de MemberPayment nem scheduler
  de expiração de refunds. Testar somente o domínio e o comando manual existente.
- Estado OVERDUE/EXPIRED de charge não é persistido; tests não inventam transição JPA.
- Cutoff de participação pertence a Publications; tests existentes são a evidência.
- FileStorage provider/path traversal pertence a Platform e já tem testes próprios.
- Concorrência exercitada com H2, não confirmação de semântica MySQL/PostgreSQL em
  produção. BACK-475 continua responsável pela validação no banco alvo de Publications.
- DataJpaTest usa o datasource H2 configurado no perfil test, com pool. O datasource
  substituto sem pool produziu check evaluation com `database has been closed`;
  a suíte usa `replace=NONE`, sem remover constraints ou alterar schema de produção.
- Bean Validation recebe o mesmo Clock fixo via customizer no contexto HTTP de teste.
- Testes de domínio/application isolam invariantes e interações de lock/storage que
  flows maiores apenas percorriam; não repetem os cenários cross-module completos.
- Nenhuma regra ou classe de produção foi excluída do JaCoCo. Nenhuma mudança de
  produção foi feita para aumentar cobertura.

## Validação final da árvore

Execução final em 02/10/2026 na branch existente `test/back-437-billing-hardening`,
descendente de `887860e3b6ad62b95037a9abf292edc25d1037ed`, após fetch/pull --ff-only.
Baseline histórica: **develop@0d8542fb3d90768573dc738cea85b23c1701b44c**,
**1.109 testes**. O commit de implementação acima recebeu somente os testes
identificados pela análise de mutações e esta atualização de evidências.

| Comando sobre os fontes/testes finais | Total | Failures | Errors | Skipped | Resultado |
| --- | ---: | ---: | ---: | ---: | --- |
| `./mvnw test` | 1.331 | 0 | 0 | 1 | BUILD SUCCESS |
| `./mvnw verify` | 1.331 | 0 | 0 | 1 | BUILD SUCCESS |

São **222 execuções adicionais à baseline**, incluindo os três testes HTTP que
não estavam na execução intermediária de 1.256. Os resultados de 1.256, 1.259
e os testes focais anteriores não são usados como validação final desta árvore.
Logs: `target/back-437-final-test.log` e `target/back-437-final-verify.log`.

## JaCoCo final

Fonte: `target/site/jacoco/jacoco.xml`, JaCoCo 0.8.15. Somente os contadores
diretos dos packages de prefixo `com/jeepclub/backend/billing` são somados.
`target/jacoco.exec` foi removido imediatamente antes do verify final para não
acumular cobertura de execuções anteriores. Sem exclusões novas de produção.

| Métrica | Inicial covered | Inicial missed | Inicial % | Final covered | Final missed | Final % |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| LINE | 1556 | 1210 | 56,25% | 2612 | 154 | 94,43% |
| BRANCH | 458 | 427 | 51,75% | 760 | 125 | 85,88% |
| INSTRUCTION | 8629 | 5508 | 61,04% | 13260 | 877 | 93,80% |

**LINE >= 80% satisfeita.** O relatório cobre Billing inteiro, distinto da
cobertura de linha de 1422/1444 (98,48%) das classes selecionadas pelo PIT.
BillingAuthorizationAdapter e os demais adapters não foram adicionados ao alvo
de mutation nem receberam testes artificiais para perseguir 100%.

## PIT final e análise completa

Comando efetivamente executado, sem sobrescrever os targets/mutators do pom:
`./mvnw -Pbilling-mutation org.pitest:pitest-maven:mutationCoverage`.
PIT 1.19.0; 20 classes alvo, 200 classes de teste examinadas.
Perfil focal: cinco agregados, assignments e serviços de configuração, ciclos,
charges, payments, refunds e Event. Controllers, DTOs e infraestrutura ficam
fora desse alvo; a suíte completa continua obrigatória.

Apesar de `outputDirectory` no perfil indicar `target/pit-reports/billing`,
esta versão do plugin escreveu o relatório em **target/pit-reports/**.
Análise feita sobre todo `mutations.xml`, conferida com `index.html` e o log,
não somente com o resumo do console. Todos os mutantes pertencem aos targets
de Billing. Arquivos locais: `target/pit-reports/{index.html,mutations.xml}`
e `target/back-437-final-pit.log`; término 02/10/2026 00:45:23 -03:00.

| Resultado final | Quantidade |
| --- | ---: |
| Geradas | 617 |
| KILLED | 611 |
| SURVIVED | 6 |
| NO_COVERAGE | 0 |
| TIMED_OUT / NON_VIABLE / MEMORY_ERROR / RUN_ERROR / NOT_STARTED / STARTED | 0 |
| Mutation score | 99,03% (611/617) |
| Test strength | 99,03% (611/(617 - 0)) |
| Execuções de teste durante mutation | 2117 |
| Build | BUILD SUCCESS |

A primeira execução produziu 513 KILLED, 81 SURVIVED e 23 NO_COVERAGE.
Esses valores são diagnósticos intermediários, não resultados finais.
Após testes comportamentais, a segunda execução produziu 600 KILLED,
17 SURVIVED e zero NO_COVERAGE. A terceira execução acima é a final.
Dos 104 mutantes inicialmente sobreviventes/sem cobertura, **98 passaram a
KILLED e seis são equivalentes**. Nenhuma exclusão ou troca de mutators foi
usada para aumentar o score.

### Gaps comportamentais implementados nesta validação

- Reenvio de payment revalida ownership, charge fechada e valor antes de
  storage/lifecycle; referências ausentes continuam com exceção controlada.
- Refund ativo de outro owner não pode ser reutilizado a partir de um payment
  da charge consultada; nenhum save pode ocorrer.
- Cancel de ciclo retorna CANCELED com timestamp; cancel de Event diferencia
  IDs reais de ciclos GENERATED/FINISHED, preservando BACK-460/BACK-464.
- Geração de dívida de Event salva contexto e charge, reutiliza assignment do
  Event correto e cria um quando ausente. Catálogo mantém os dados retornados.
- Consulta financeira de Event separa ciclos/owners, seleciona o último payment
  e mantém linhas sem payment/metadados, sem alterar os scans de BACK-471.
- Listagem/generation/assignment com definição ausente preserva erro de domínio;
  resultados dos quatro assignments conservam audienceType e target.
- `BillingMutationHistoryTest`: reconstrução via mappers rejeita decisões
  incompatíveis de refunds/ciclos, falta de aprovação/transferência, janelas
  ausentes, timestamps inválidos, IDs zero e atores históricos inválidos.
  Round-trip mantém histórico e estado observável; null em descrição opcional
  é preservado; submittedAt de payment mantém o valor ou o fallback legado.
  ReflectionTestUtils corrompe apenas entidades de fixture para simular dados
  persistidos inválidos, nunca objetos de produção durante a execução real.

### Seis sobreviventes finais: tratamento individual

Classes de domínio abaixo pertencem a `com.jeepclub.backend.billing.core.domain.model`.
O nome do mutator tem prefixo `org.pitest.mutationtest.engine.gregor.mutators`;
NullReturnValsMutator pertence ao subpackage `returns`.

| Classe | Método | Linha | Mutator | Comportamento afetado e justificativa |
| --- | --- | ---: | --- | --- |
| MemberRefund | validateStatusConsistency | 397 | VoidMethodCallMutator | Remove validateOptionalId(rejectedByUserId). REJECTED valida o mesmo ID em requireRejectionData; todos os outros estados proíbem rejection data. A remoção não aceita nenhum estado novo. Equivalente. |
| MemberRefund | validateStatusConsistency | 398 | VoidMethodCallMutator | Remove validateOptionalId(refundedByUserId). REFUNDED valida em requireRefundedData; todos os demais estados proíbem refunded data. Equivalente. |
| MemberRefund | validateStatusConsistency | 399 | VoidMethodCallMutator | Remove validateOptionalId(canceledByUserId). CANCELED valida em requireCancellationData; os demais estados proíbem cancellation data. Equivalente. |
| ChargeCycle | validateStatusConsistency | 281 | VoidMethodCallMutator | Remove validateOptionalId(archivedByUserId). ARCHIVED valida em requireArchiveData; GENERATED/CANCELED/FINISHED proíbem archive data. Equivalente. Os atores históricos canceled/finished em ARCHIVED são distintos e agora têm teste de ID zero. |
| MemberPayment | updateSubmission | 232 | VoidMethodCallMutator | Remove validateStatusConsistency após reenvio. Só entra de PENDING_VALIDATION/REJECTED reconstruídos válidos; muda para PENDING_VALIDATION e zera todos os campos de rejeição/cancelamento. Campos de confirmação já eram null, e novos valores são validados antes. Nenhum estado inválido pode ser produzido pela API pública; checagem final redundante. Equivalente, sem forçar corrupção de campos privados para matar o mutante. |
| com.jeepclub.backend.billing.core.application.service.chargeassignment.AdminChargeAssignmentService | findActiveChargeDefinitionOrThrow | 178 | returns.NullReturnValsMutator | Troca somente o retorno por null após executar lookup e guards. Os cinco chamadores (quatro assign e activate) descartam o retorno; validações e efeitos permanecem. Equivalente; não alterar produção para obter 100%. |

Não existem SURVIVED sem triagem nem NO_COVERAGE finais.

## Dívidas, limites e conclusão

BACK-489 e BACK-490 foram relidas no Jira nesta execução: ambas continuam
**Tarefas pendentes, sem Sprint, no backlog**. Nenhuma delas foi corrigida.
Permanecem as associações com **BACK-460, BACK-464, BACK-465, BACK-466 e BACK-471**
descritas acima. Os limites de H2 e as decisões semânticas da matriz continuam
válidos. Nenhum arquivo em `src/main/` mudou desde a baseline: **ausência de
alteração funcional de produção**; somente testes, fixtures, configuração PIT
e evidência de auditoria.

Os critérios técnicos restantes da BACK-437 estão satisfeitos: test, verify,
JaCoCo e PIT foram efetivamente executados após o último ajuste de teste;
mutantes relevantes receberam teste ou justificativa técnica. Pronta para
encerramento técnico, sem encerrar a issue nesta execução. Publicação somente
na mesma branch remota, sem PR ou merge e sem mudança no Jira.

### Integridade dos artefatos locais

Os relatórios/logs completos permanecem em `target/`, sem versionamento.
Hashes SHA-256 identificam exatamente os artefatos usados nesta evidência:

- `target/back-437-final-test.log`: `9dd8795ff2fa7b6074ea0b15b7d1591367ad6fbe4453e33a029160924dd50221`
- `target/back-437-final-verify.log`: `c67844c701f64f560e7525ad4eaf5ce6d4f3130d5a8a030139d61e0383c2c4c7`
- `target/site/jacoco/jacoco.xml`: `c1688e3fc2483488eef2e4eebce1a9a2646f2ea68324c8a3cce930c42f2e9bc6`
- `target/back-437-final-pit.log`: `c476dd783aae6f92454e58fd8d846131c3f76f8f254d1df43130cf299ee8bd47`
- `target/pit-reports/mutations.xml`: `faed2ac91b151bb46b81fa4ffa726f9980628e47d49ff24557862537de44d421`

### Inventário dos 104 mutantes inicialmente sobreviventes/sem cobertura

Cada linha relaciona classe, método, linha e mutator do XML inicial ao status
no XML final. `KILLED` indica o tratamento pelos testes acima; `SURVIVED` remete
à justificativa individual da tabela anterior. O prefixo das classes é
`com.jeepclub.backend.billing.`; mutators são exibidos pelo nome curto.

| Classe (sob Billing) | Método | Linha | Mutator | Comportamento mutado | Inicial | Final |
| --- | --- | ---: | --- | --- | --- | --- |
| core.domain.model.MemberRefund | `isApproved` | 366 | BooleanTrueReturnValsMutator | replaced boolean return with true for com/jeepclub/backend/billing/core/domain/model/MemberRefund::isApproved | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `isCanceled` | 378 | BooleanTrueReturnValsMutator | replaced boolean return with true for com/jeepclub/backend/billing/core/domain/model/MemberRefund::isCanceled | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `isExpired` | 374 | BooleanTrueReturnValsMutator | replaced boolean return with true for com/jeepclub/backend/billing/core/domain/model/MemberRefund::isExpired | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `isRefunded` | 370 | BooleanTrueReturnValsMutator | replaced boolean return with true for com/jeepclub/backend/billing/core/domain/model/MemberRefund::isRefunded | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `isRequested` | 362 | BooleanTrueReturnValsMutator | replaced boolean return with true for com/jeepclub/backend/billing/core/domain/model/MemberRefund::isRequested | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `normalizeNullableText` | 601 | EmptyObjectReturnValsMutator | replaced return value with "" for com/jeepclub/backend/billing/core/domain/model/MemberRefund::normalizeNullableText | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateId` | 563 | ConditionalsBoundaryMutator | changed conditional boundary | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 394 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::validateOptionalId | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 395 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::validateOptionalId | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 396 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::validateOptionalId | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 397 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::validateOptionalId | SURVIVED | SURVIVED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 398 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::validateOptionalId | SURVIVED | SURVIVED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 399 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::validateOptionalId | SURVIVED | SURVIVED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 404 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::requireEligibilityWindow | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 409 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::requireEligibilityWindow | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 412 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoRejectionData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 413 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoRefundedData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 414 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoCancellationData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 419 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoApprovalData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 420 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoRejectionData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 421 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoRefundedData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 422 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoCancellationData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 427 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoRejectionData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 428 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoRefundedData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 429 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoCancellationData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 434 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoApprovalData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 435 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoRefundedData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 436 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoCancellationData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 440 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::requireApprovalData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 441 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::requireRefundedData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 442 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoRejectionData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 443 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoCancellationData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 447 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::requireEligibilityWindow | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 449 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoApprovalData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 450 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoRejectionData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 451 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoRefundedData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 452 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoCancellationData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 457 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoRejectionData | SURVIVED | KILLED |
| core.domain.model.MemberRefund | `validateStatusConsistency` | 458 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberRefund::ensureNoRefundedData | SURVIVED | KILLED |
| core.domain.model.ChargeCycle | `<init>` | 108 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/ChargeCycle::validatePaymentAcceptancePolicyConsistency | SURVIVED | KILLED |
| core.domain.model.ChargeCycle | `isArchived` | 267 | BooleanTrueReturnValsMutator | replaced boolean return with true for com/jeepclub/backend/billing/core/domain/model/ChargeCycle::isArchived | SURVIVED | KILLED |
| core.domain.model.ChargeCycle | `isCanceled` | 259 | BooleanTrueReturnValsMutator | replaced boolean return with true for com/jeepclub/backend/billing/core/domain/model/ChargeCycle::isCanceled | SURVIVED | KILLED |
| core.domain.model.ChargeCycle | `isFinished` | 263 | BooleanTrueReturnValsMutator | replaced boolean return with true for com/jeepclub/backend/billing/core/domain/model/ChargeCycle::isFinished | SURVIVED | KILLED |
| core.domain.model.ChargeCycle | `normalizeNullableText` | 455 | EmptyObjectReturnValsMutator | replaced return value with "" for com/jeepclub/backend/billing/core/domain/model/ChargeCycle::normalizeNullableText | SURVIVED | KILLED |
| core.domain.model.ChargeCycle | `validateAmount` | 438 | ConditionalsBoundaryMutator | changed conditional boundary | SURVIVED | KILLED |
| core.domain.model.ChargeCycle | `validateId` | 420 | ConditionalsBoundaryMutator | changed conditional boundary | SURVIVED | KILLED |
| core.domain.model.ChargeCycle | `validateOptionalId` | 412 | ConditionalsBoundaryMutator | changed conditional boundary | SURVIVED | KILLED |
| core.domain.model.ChargeCycle | `validatePaymentAcceptancePolicyConsistency` | 393 | ConditionalsBoundaryMutator | changed conditional boundary | SURVIVED | KILLED |
| core.domain.model.ChargeCycle | `validateStatusConsistency` | 275 | NegateConditionalsMutator | negated conditional | SURVIVED | KILLED |
| core.domain.model.ChargeCycle | `validateStatusConsistency` | 279 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/ChargeCycle::validateOptionalId | SURVIVED | KILLED |
| core.domain.model.ChargeCycle | `validateStatusConsistency` | 280 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/ChargeCycle::validateOptionalId | SURVIVED | KILLED |
| core.domain.model.ChargeCycle | `validateStatusConsistency` | 281 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/ChargeCycle::validateOptionalId | SURVIVED | SURVIVED |
| core.domain.model.ChargeCycle | `validateStatusConsistency` | 291 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/ChargeCycle::ensureNoFinishData | NO_COVERAGE | KILLED |
| core.domain.model.ChargeCycle | `validateStatusConsistency` | 292 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/ChargeCycle::ensureNoArchiveData | NO_COVERAGE | KILLED |
| core.domain.model.ChargeCycle | `validateStatusConsistency` | 297 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/ChargeCycle::ensureNoCancellationData | SURVIVED | KILLED |
| core.domain.model.ChargeCycle | `validateStatusConsistency` | 298 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/ChargeCycle::ensureNoArchiveData | SURVIVED | KILLED |
| core.domain.model.ChargeCycle | `validateStatusConsistency` | 302 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/ChargeCycle::requireArchiveData | SURVIVED | KILLED |
| core.domain.model.MemberCharge | `validateId` | 343 | ConditionalsBoundaryMutator | changed conditional boundary | SURVIVED | KILLED |
| core.domain.model.MemberPayment | `isCanceled` | 254 | BooleanTrueReturnValsMutator | replaced boolean return with true for com/jeepclub/backend/billing/core/domain/model/MemberPayment::isCanceled | SURVIVED | KILLED |
| core.domain.model.MemberPayment | `isPendingValidation` | 236 | BooleanTrueReturnValsMutator | replaced boolean return with true for com/jeepclub/backend/billing/core/domain/model/MemberPayment::isPendingValidation | SURVIVED | KILLED |
| core.domain.model.MemberPayment | `reconstitute` | 241 | NegateConditionalsMutator | negated conditional | SURVIVED | KILLED |
| core.domain.model.MemberPayment | `updateSubmission` | 232 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/MemberPayment::validateStatusConsistency | SURVIVED | SURVIVED |
| core.domain.model.MemberPayment | `validateId` | 314 | ConditionalsBoundaryMutator | changed conditional boundary | SURVIVED | KILLED |
| core.application.service.event.EventBillingService | `assignment` | 122 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/application/service/event/EventBillingService::assignment | NO_COVERAGE | KILLED |
| core.application.service.event.EventBillingService | `assignment` | 124 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/application/service/event/EventBillingService::assignment | SURVIVED | KILLED |
| core.application.service.event.EventBillingService | `cancelEventCycles` | 85 | NegateConditionalsMutator | negated conditional | SURVIVED | KILLED |
| core.application.service.event.EventBillingService | `eligibleLocked` | 115 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/application/service/event/EventBillingService::eligibleLocked | SURVIVED | KILLED |
| core.application.service.event.EventBillingService | `findByEvent` | 102 | NegateConditionalsMutator | negated conditional | NO_COVERAGE | KILLED |
| core.application.service.event.EventBillingService | `findByEvent` | 103 | NegateConditionalsMutator | negated conditional | NO_COVERAGE | KILLED |
| core.application.service.event.EventBillingService | `findByEvent` | 104 | NegateConditionalsMutator | negated conditional | NO_COVERAGE | KILLED |
| core.application.service.event.EventBillingService | `findByEvent` | 108 | EmptyObjectReturnValsMutator | replaced return value with Collections.emptyList for com/jeepclub/backend/billing/core/application/service/event/EventBillingService::findByEvent | NO_COVERAGE | KILLED |
| core.application.service.event.EventBillingService | `lambda$assignment$8` | 119 | BooleanTrueReturnValsMutator | replaced boolean return with true for com/jeepclub/backend/billing/core/application/service/event/EventBillingService::lambda$assignment$8 | SURVIVED | KILLED |
| core.application.service.event.EventBillingService | `lambda$eligibleLocked$7` | 112 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/application/service/event/EventBillingService::lambda$eligibleLocked$7 | NO_COVERAGE | KILLED |
| core.application.service.event.EventBillingService | `lambda$ensureEventMemberCharge$2` | 69 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/repository/EventChargeContextRepository::save | NO_COVERAGE | KILLED |
| core.application.service.event.EventBillingService | `lambda$ensureEventMemberCharge$2` | 70 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/application/service/event/EventBillingService::lambda$ensureEventMemberCharge$2 | NO_COVERAGE | KILLED |
| core.application.service.event.EventBillingService | `lambda$ensureEventMemberCharge$3` | 74 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/application/service/event/EventBillingService::lambda$ensureEventMemberCharge$3 | NO_COVERAGE | KILLED |
| core.application.service.event.EventBillingService | `lambda$findByEvent$4` | 96 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/application/service/event/EventBillingService::lambda$findByEvent$4 | NO_COVERAGE | KILLED |
| core.application.service.event.EventBillingService | `lambda$findByEvent$5` | 98 | BooleanFalseReturnValsMutator | replaced boolean return with false for com/jeepclub/backend/billing/core/application/service/event/EventBillingService::lambda$findByEvent$5 | NO_COVERAGE | KILLED |
| core.application.service.event.EventBillingService | `lambda$findByEvent$5` | 98 | BooleanTrueReturnValsMutator | replaced boolean return with true for com/jeepclub/backend/billing/core/application/service/event/EventBillingService::lambda$findByEvent$5 | NO_COVERAGE | KILLED |
| core.application.service.event.EventBillingService | `lambda$findByEvent$6` | 99 | BooleanFalseReturnValsMutator | replaced boolean return with false for com/jeepclub/backend/billing/core/application/service/event/EventBillingService::lambda$findByEvent$6 | NO_COVERAGE | KILLED |
| core.application.service.event.EventBillingService | `lambda$findByEvent$6` | 99 | BooleanTrueReturnValsMutator | replaced boolean return with true for com/jeepclub/backend/billing/core/application/service/event/EventBillingService::lambda$findByEvent$6 | NO_COVERAGE | KILLED |
| core.application.service.event.EventBillingService | `lambda$findEligible$0` | 33 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/application/service/event/EventBillingService::lambda$findEligible$0 | SURVIVED | KILLED |
| core.application.service.chargecycle.AdminChargeCycleService | `cancel` | 141 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/ChargeCycle::cancel | SURVIVED | KILLED |
| core.application.service.chargecycle.AdminChargeCycleService | `cancel` | 159 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/application/service/chargecycle/AdminChargeCycleService::cancel | SURVIVED | KILLED |
| core.application.service.chargecycle.AdminChargeCycleService | `findByChargeDefinitionId` | 118 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/application/service/chargecycle/AdminChargeCycleService::ensureChargeDefinitionExists | SURVIVED | KILLED |
| core.application.service.chargecycle.AdminChargeCycleService | `lambda$findChargeDefinitionOrThrow$1` | 305 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/application/service/chargecycle/AdminChargeCycleService::lambda$findChargeDefinitionOrThrow$1 | NO_COVERAGE | KILLED |
| core.application.service.chargeassignment.AdminChargeAssignmentService | `findActiveChargeDefinitionOrThrow` | 178 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/application/service/chargeassignment/AdminChargeAssignmentService::findActiveChargeDefinitionOrThrow | SURVIVED | SURVIVED |
| core.application.service.chargeassignment.AdminChargeAssignmentService | `findByChargeDefinitionId` | 124 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/application/service/chargeassignment/AdminChargeAssignmentService::ensureChargeDefinitionExists | SURVIVED | KILLED |
| core.application.service.chargeassignment.AdminChargeAssignmentService | `lambda$ensureChargeDefinitionExists$2` | 280 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/application/service/chargeassignment/AdminChargeAssignmentService::lambda$ensureChargeDefinitionExists$2 | NO_COVERAGE | KILLED |
| core.application.service.chargeassignment.AdminChargeAssignmentService | `lambda$ensureChargeDefinitionIsNotArchived$3` | 289 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/application/service/chargeassignment/AdminChargeAssignmentService::lambda$ensureChargeDefinitionIsNotArchived$3 | NO_COVERAGE | KILLED |
| core.application.service.chargeassignment.AdminChargeAssignmentService | `lambda$findActiveChargeDefinitionOrThrow$0` | 168 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/application/service/chargeassignment/AdminChargeAssignmentService::lambda$findActiveChargeDefinitionOrThrow$0 | NO_COVERAGE | KILLED |
| core.domain.model.ChargeDefinition | `normalizeNullableText` | 250 | EmptyObjectReturnValsMutator | replaced return value with "" for com/jeepclub/backend/billing/core/domain/model/ChargeDefinition::normalizeNullableText | SURVIVED | KILLED |
| core.domain.model.ChargeDefinition | `update` | 147 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/domain/model/ChargeDefinition::validatePaymentAcceptancePolicyConsistency | SURVIVED | KILLED |
| core.application.service.memberrefund.MemberRefundService | `requestExistingRefundIfEligible` | 68 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/application/service/memberrefund/MemberRefundService::ensureRefundBelongsToUser | SURVIVED | KILLED |
| core.application.service.memberpayment.MemberPaymentService | `lambda$findMemberChargeForUpdateOrThrow$1` | 198 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/application/service/memberpayment/MemberPaymentService::lambda$findMemberChargeForUpdateOrThrow$1 | NO_COVERAGE | KILLED |
| core.application.service.memberpayment.MemberPaymentService | `lambda$findMemberPaymentForUpdateOrThrow$0` | 192 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/application/service/memberpayment/MemberPaymentService::lambda$findMemberPaymentForUpdateOrThrow$0 | NO_COVERAGE | KILLED |
| core.application.service.memberpayment.MemberPaymentService | `updateSubmission` | 106 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/application/service/memberpayment/MemberPaymentService::ensureChargeBelongsToAuthenticatedUser | SURVIVED | KILLED |
| core.application.service.memberpayment.MemberPaymentService | `updateSubmission` | 107 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/application/service/memberpayment/MemberPaymentService::ensureChargeCanHavePaymentUpdated | SURVIVED | KILLED |
| core.application.service.memberpayment.MemberPaymentService | `updateSubmission` | 111 | VoidMethodCallMutator | removed call to com/jeepclub/backend/billing/core/application/service/memberpayment/MemberPaymentService::ensurePaymentAmountMatchesCharge | SURVIVED | KILLED |
| core.application.service.membercharge.AdminMemberChargeService | `lambda$findMemberChargeOrThrow$0` | 99 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/application/service/membercharge/AdminMemberChargeService::lambda$findMemberChargeOrThrow$0 | NO_COVERAGE | KILLED |
| core.domain.model.assignment.RoleChargeAssignment | `audienceType` | 60 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/domain/model/assignment/RoleChargeAssignment::audienceType | SURVIVED | KILLED |
| core.domain.model.assignment.UserChargeAssignment | `audienceType` | 60 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/domain/model/assignment/UserChargeAssignment::audienceType | SURVIVED | KILLED |
| core.domain.model.assignment.EventParticipantsChargeAssignment | `audienceType` | 60 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/domain/model/assignment/EventParticipantsChargeAssignment::audienceType | SURVIVED | KILLED |
| core.domain.model.assignment.AllMembersChargeAssignment | `audienceType` | 50 | NullReturnValsMutator | replaced return value with null for com/jeepclub/backend/billing/core/domain/model/assignment/AllMembersChargeAssignment::audienceType | SURVIVED | KILLED |
