# BACK-585 / BACK-574 / BACK-463 / BACK-573

## Audit before implementation

Baseline: origin/develop `f76541692c6c25bc082264bd63cc62ae9b365f48`.
Branch: `feature/back-585-574-463-573-admin-hardening`.
Jira full descriptions, acceptance criteria, links and comments inspected for all four stories,
their twelve actual subtasks, and related BACK-455/456/457/464. GitHub connector reports no
open PRs and no matching feature branch. PR #125 is merged into develop; its reported CI
is historical evidence only. Current workflow requires Maven verify and MySQL migration tests.
Local untracked output/ and tmp/ predate this task and are preserved.

BACK-463's real subtasks are BACK-473, BACK-474, BACK-475. BACK-464 is an unrelated,
completed Event/Billing cancellation task. BACK-455/456/457 were closed because they were
replaced by real subtasks, not because their acceptance criteria were implemented.

## Traceability and planned verification

| Story / subtask | Existing code | Necessary change | Verification |
| --- | --- | --- | --- |
| BACK-585 / BACK-586 | AdminUserJpaQueryRepository uses unescaped LIKE | Literal %, _, escape in every partial predicate | Persistence regression with literal metacharacters |
| BACK-585 / BACK-587 | UserSortMapper allowlist; no total order | Append id when absent, preserve explicit id direction | Sort unit tests and repeated-name pages |
| BACK-585 / BACK-588 | Tuple projection, Membership port, registry API tests | Strengthen combined filters, q OR, count, fields, later pages | Identity persistence/HTTP, registry integration, SQL inspection |
| BACK-574 / BACK-579 | name/status and full Tool summary | Add typed q/filter/fields contract with existing default | HTTP/OpenAPI/invalid parameters/security |
| BACK-574 / BACK-580 | ToolSpecifications/JpaSpecificationExecutor | Reuse predicates for count and selective DB projection | Persistence/count/pages/SQL columns |
| BACK-574 / BACK-581 | Tools contain owner userId; summary has no owner name | Assess need for owner search; bound integration if included | Compatibility and fixed query count |
| BACK-463 / BACK-473 | PublicationComment has no payload caps | Define technical text/image caps before external work | Exact limit, +1, empty, text/images, zero side effects |
| BACK-463 / BACK-474 | Event catches NPE/all DataAccessException; social labels all integrity failures as like conflict | Restrict functional conflicts to recognized failures | HTTP RFC 9457 for NPE/SQL/known constraints, no internal details |
| BACK-463 / BACK-475 | H2 and real MySQL suites, no PostgreSQL suite | Isolated PostgreSQL validation without production DB changes | Real PostgreSQL schema/constraints/concurrency/history/Billing |
| BACK-573 / BACK-576 | status/page/sort and batch owner presentation | Typed q/plate/ownerId/nickname/model/fields contract | HTTP/OpenAPI/security/normalization |
| BACK-573 / BACK-577 | Entity page then owner-name batch | DB predicates/count and selective projection, stable id | Persistence/count/later pages/selected columns |
| BACK-573 / BACK-578 | Public Identity names-by-IDs API | Minimal bounded name-search contract | Cardinality limit, no N+1, owner-name integration |

## Risks and boundaries

* Preserve default JSON fields/nulls, default status behavior and existing permissions.
* q OR and specific filters AND must share count predicates; never filter a page in memory.
* Owner names belong to Identity; no cross-module entity/repository imports or unbounded IDs.
* Membership projection remains batched; do not add per-row reads.
* Contains LIKE generally scans; no index/performance claim without representative measurement.
* Current production database is MySQL/Flyway, although BACK-475 still names PostgreSQL.
  PostgreSQL validation must be isolated and cannot establish production migration readiness.
* New payload limits apply to input; preserve readability of existing persisted comments.
* Unexpected infrastructure failures must remain sanitized server errors, not known conflicts.
* No merge, deploy, master modification, production data operation or automatic Jira closure.

## Execution evidence

Identity: literal LIKE, stable sort and Membership batching are implemented. The focal
Identity set passed 77 tests (24 HTTP, 8 persistence, 16 sort, 29 Registry API) in
`output/admin-hardening-a-b.log`; that combined run exposed a Tools invalid-sort mapping
failure, so it is not a passing combined build. The subsequent corrected focal build
passed 60/0/0/0, including Identity and Tools HTTP/persistence (`output/admin-hardening-a-b-c-compile.log`).
The first Identity-only run failed because a new SQL assertion expected ESCAPE without a
LIKE filter; the fixture was corrected and then passed.

Tools deliberately searches only name with q; name/status remain compatible and userId is
an additive exact filter. Neither the current administrative summary nor the Jira requires
owner-name presentation/search in Tools. No owner-name or description expansion is justified
by its current screen contract. Thus no Identity call is added. Fields are limited to the
existing four summary fields; persistence tests observe two statements and only requested
SELECT columns. Contains search is not claimed to be index optimized.

Full test/verify and PostgreSQL are now verified on implementation HEAD `1053be9dfa07eecbc4d980ed5c8172a0cb6ed9a9`.
MySQL is verified on detached HEAD `d625d24e9160b5fe1e1fbda54cf1f516f294be8e`.
`git diff d625d24e 1053be9d -- src/main pom.xml .github/workflows` is empty; the
only intervening source change is the corrected Vehicles security-test mock.

| Verificação final local | Comando Maven (Java 17.0.18) | Descobertos / falhas / erros / skips | Log em output/ |
| --- | --- | --- | --- |
| Test | `mvnw.cmd test` | 2545 / 0 / 0 / 1 | admin-hardening-test-final.log |
| Verify / build | `mvnw.cmd verify` | 2545 / 0 / 0 / 1 | admin-hardening-verify.log |
| MySQL 8.0.46 migrado | `mvnw.cmd -Pmysql-migrations -DskipUnitTests=true verify` | 492 / 0 / 0 / 0 | admin-hardening-mysql.log |
| PostgreSQL 17.6 real | `mvnw.cmd -Ppublications-postgresql -DskipUnitTests=true verify` | 110 / 0 / 0 / 0 | admin-hardening-postgres-final.log |

Todos os comandos finais concluíram com BUILD SUCCESS. O único skip local pertence a
`LocalFileStorageTest.shouldRejectSymbolicLinkInsteadOfFollowingItOutsideRoot`, por
indisponibilidade de symlink no Windows. Não foi removido nem enfraquecido; CI Linux
executa a mesma suíte. Repetições em MySQL/PostgreSQL são execuções adicionais de cenários,
não contagens de requisitos ou de testes únicos novos.

Relatórios XML locais foram preservados em `output/admin-hardening-validation-1053be9d/`
com identificação dos fontes. O commit final de entrega acrescenta somente documentação
e evidências. Os checks e artifacts do CI do HEAD de entrega são vinculados no PR;
a aprovação deles é requisito adicional para declarar pronto para revisão. Esta validação
não representa merge, deploy, ativação ou aceite operacional de produção.

## Evidências por subtarefa

| História / subtarefa real | Resultado implementado | Testes correspondentes |
| --- | --- | --- |
| BACK-585 / BACK-586 | LIKE literal em name/email/phone e q, com escape explícito de %, _ e !. | AdminUserSearchPersistenceTest.partialFiltersAndQAreLiteral; AdminUserSearchMySqlIT (mesmos cenários em MySQL real) |
| BACK-585 / BACK-587 | Allowlist preservada, id como desempate, direção explícita preservada e matrícula não ordenável. | UserSortMapperTest.appendsIdWithoutOverridingAnExplicitDirection; membershipNumberRemainsUnsortable; AdminUserSearchPersistenceTest.qOrSpecificAndAndCountApplyBeforeStablePagination |
| BACK-585 / BACK-588 | Projeção SQL, count/páginas, q OR + filtros AND e matrícula corrente com batch; sem índice ou promessa de desempenho sem medição representativa. | AdminUserControllerTest.membershipProjectionUsesOneBatchForThePageAndNoBatchWhenOmitted; MemberRegistryApiIntegrationTest; AdminUserSearchPersistenceTest.projectionSelectsOnlyRequestedColumnsWithTwoQueriesRegardlessOfPageSize; missingMembershipMatchDoesNotSuppressTextualQOr |
| BACK-574 / BACK-579 | q no nome; name/status preservados; userId exato; fields=ID,NAME,STATUS,USER_ID; sort/validação e OpenAPI. | ToolsOpenApiIntegrationTest.administrativeSearchParametersArePublished; ToolsHttpIntegrationTest.adminRequiresPermissionAndCanManageOtherOwnersTools |
| BACK-574 / BACK-580 | Specifications reutilizadas em conteúdo/count, paginação no banco, fields com seleção real e duas consultas. | ToolRepositoryAdapterTest.administrativeSearchFiltersBeforeCountAndProjectsOnlyRequestedColumns; administrativeNameAndQEscapeLiteralCharacters |
| BACK-574 / BACK-581 | Avaliada integração: busca nominal de proprietário/descrição não é necessária ao resumo atual nem obrigatória no Jira. q fica no nome, userId atende filtro exato, sem consulta Identity adicional. | ToolsHttpIntegrationTest; ToolsSearchMySqlIT; ToolRepositoryAdapterTest (SQL e query count) |
| BACK-463 / BACK-473 | Limites de entrada: 10000 unidades UTF-16 e 10 imagens; limite/+1/vazio, antes de storage/persistência; histórico maior continua legível. | PublicationSocialWorkflowTest.commentLimitsAcceptExactBoundariesAndRejectBeforeAnyExternalWork; PublicationHardeningHttpTest.oversizedTextAndImagesAreRejectedBeforeServiceWork; PublicationSocialFeedOpenApiIntegrationTest |
| BACK-463 / BACK-474 | NPE/SQL inesperada usam 500 sanitizado/global; somente unicidades reconhecidas e locks esperados viram conflito. Integridade de comentário não vira conflito de like. | PublicationHardeningHttpTest (4 cenários HTTP incluindo NPE, SQL genérica, integridade desconhecida, locks, constraints conhecidas e nomes qualificados por dialeto) |
| BACK-463 / BACK-475 | 110 cenários em PostgreSQL 17.6 real: schema halt-on-error, likes, guest/última vaga, ride selection, Service concorrente, idempotência Billing, nullable unique e hard delete/histórico. Override PostgreSQL de default boolean, sem alterar migrations/MySQL. | EventOperationsPostgresIT (85); PublicationPersistencePostgresIT (19); ServiceChangePersistencePostgresIT (6) |
| BACK-573 / BACK-576 | Contrato q/plate/ownerId/nickname/model/status/fields com defaults existentes e erros 400, autorização preservada. | VehiclesOpenApiIntegrationTest.administrativeSearchDocumentsAllFiltersAndProjection; AdminVehicleSearchHttpIntegrationTest |
| BACK-573 / BACK-577 | Placa normalizada sem máscara, predicados/count antes da paginação, campos selecionados no SQL e desempate id. Sem carregar galeria nem filtrar página em memória. | VehicleRepositoryAdapterTest.selectiveAdministrativeQueryUsesTwoStatementsWithoutLoadingGalleryOrUnrequestedColumns; AdminVehicleSearchHttpIntegrationTest.ownerNameIsAppliedBeforePageCountAndSelectedFieldsDoNotLeakAuxiliaryId; qUsesOrAndPlatesIgnoreMaskWhileFiltersUseAnd |
| BACK-573 / BACK-578 | Extensão mínima UserQuery.findIdsByName: SELECT de até 501 IDs, retorna até 500 ou 400 para refinar termo. Consumer usa porta pública e apresentação batelada; sem N+1 nem import cross-module. | UserQueryServiceTest.nameSearchHasAHardLimitAndNeverSilentlyTruncates; AdminVehicleServiceTest.ownerNameLookupIsBatchedAndRunsOnlyWhenSelected; AdminVehicleSearchHttpIntegrationTest.ownerCardinalityNeverSilentlyTruncatesAndInvalidParametersAre400; AdminVehicleSearchMySqlIT; VehicleAdminSecurityIntegrationTest |

## Arquivos, compatibilidade e limites

O [manifesto de arquivos e commits por história](admin-hardening-files.md) lista todos os
arquivos de implementação/teste e os commits correspondentes. Os README dos quatro
módulos e o OpenAPI documentam os contratos finais.

* Identity mantém os campos/defaults, matrícula vigente e allowlist existentes.
* Tools mantém id/name/status/userId e name/status; a avaliação não justificou busca
  nominal do proprietário nem descrição. Não há materialização/call de Identity em Tools.
* Vehicles mantém o resumo completo, seus nulls, status ACTIVE e size 10 como defaults.
  fields seleciona somente colunas necessárias (e ownerId auxiliar interno para OWNER_NAME).
  A busca nominal retorna no máximo 500 IDs ou 400 solicitando refinar o termo; o limite
  também se aplica com filtros específicos. Não há truncamento silencioso.
* Os SELECT de Identity/Tools/Vehicles usam paginação e count no banco; testes inspecionam
  duas instruções e as colunas selecionadas. Em Vehicles, lookup nominal e apresentação
  de nomes acrescentam chamadas bateladas constantes quando necessários, nunca por linha.
* Contains LIKE pode fazer scan. Não foi executado benchmark/EXPLAIN em uma população
  representativa de produção; não há promessa de otimização nem criação especulativa de índice.
* Nenhuma migration, configuração de produção, permission ou flag foi modificada.
  O override boolean é específico ao DDL PostgreSQL; schema MySQL continua sob Flyway.
* Os limites de comentário são de entrada, não de leitura/reconstituição histórica.
  PostgreSQL é validação adicional em containers, não uma migração/ativação do banco de produção.

## Execuções intermediárias não aprovadas

Foram preservados os logs em output/. Falhas intermediárias foram corrigidas: asserção
SQL que esperava ESCAPE sem LIKE; tipagem Criteria multiselect/import de teste na compilação;
reconfiguração de mock com stub já lançando exceção; mock de segurança de Vehicles ainda
apontando para a antiga listagem. A primeira tentativa Tools devolvia 500 para sort inválido
por tradução de IllegalArgumentException em @Repository; uma exceção tipada preserva 400.
A tentativa PostgreSQL inicialmente foi bloqueada pela proteção de DDL (mantida em produção);
a configuração isolada de teste então detectou boolean default 0, corrigido por override de dialeto.
A primeira suíte completa descobriu 2545 testes, com uma falha no mock antigo de segurança
(mais o skip Windows); ela não é evidência de aprovação. A reexecução final é registrada separadamente.
