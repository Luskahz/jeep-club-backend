# BACK-526 — auditoria de finalização

> Registro histórico anterior à homologação de 26/10/2026. As regras antigas
> de claims permanentes, formato opaco e contador crescente foram substituídas.
> Política e resultados atuais: [BACK-526 numérica](back-526-numeric-policy.md).

## Baseline e método

Auditoria de 25/10/2026, antes de alterações de código: `origin/master` e
`origin/develop` em `9ad04a71e50917e0a53c1c4855ea6770730bd8b9` (release #112).
Branch `feat/back-526-finalization`, criada de develop. O commit exclusivo da
develop local foi preservado em `backup/develop-before-back-526-finalization-20261025`;
diretórios locais não rastreados `output/` e `tmp/` preservados e fora da entrega.

Lidas integralmente a Story e as dez subtarefas BACK-527–536 no Jira atual,
incluindo refinamentos. O Rovo Legacy conectado não autoriza o site Jeep Club;
a leitura foi feita pelo conector Atlassian atual. Consultados PR #104 (merged,
`7c8df2e39dd218d5cbc6eb6042d3151b511840b9`) e #112 (merged no baseline acima),
descrições e discussões. A descrição antiga da release não comprova o estado
operacional de hoje; não houve acesso à VPS nesta auditoria.

Inspecionados: serviços/ports de Registry, importação e onboarding; contratos
públicos de Identity, Authentication e Authorization e seus adapters; guards de
lifecycle; projeções e exports; V1–V9; documentação e suítes H2/MySQL existentes.
As evidências de 17–18/10 permanecem históricas em
[back-526-validation.md](back-526-validation.md); os resultados desta revisão
serão registrados separadamente abaixo.

## Matriz anterior às alterações

Estados: **IC** = implementado e comprovado por cenário existente inspecionado
(a execução atual será discriminada); **IL** = implementado com lacunas;
**NI** = não implementado; **BE** = bloqueado por decisão/evidência externa.
Não se interpreta a existência de um teste como execução aprovada nesta revisão.

| Requisito | Subtask | Estado inicial | Evidência / lacuna |
| --- | --- | --- | --- |
| User.id, Member.id e matrícula independentes; sem alteração de PK/FK/JWT | 527/528 | IC | Member, V4, MemberIdentityAdapter, dynamicCorrectionPreservesBothIdsIdentityAndAudit |
| String literal, zeros preservados, unicidade e colisão numérica conservadora | 527 | IC | MembershipNumber, leadingZerosArePreservedAndAmbiguousVariantsAreRejected |
| Claims permanentes, reservas e números retirados sem reutilização/transferência | 527/532 | IC | historicNumbersAndReservationsCannotBeReused |
| ADR formal, transições e governança atualizadas | 527 | IL | Regras dispersas; referências a lifecycle futuro obsoletas |
| Formato oficial, equivalência, reservas, lacunas, início e marco formal aprovados | 527 | BE | Sem homologação apresentada; não inventar valores |
| Inventário e validação das telas do frontend | 527/531 | BE | Contratos/backend documentados; homologação da UI depende de seu responsável |
| JPA, constraints e Flyway incremental com validate | 528/535 | IC | MemberRegistryMySqlIT, MySqlMigrationIT; V4 integrada e V1–V9 preservadas |
| Query vigente, reversa e em lote; ausência sem fallback; sem N+1 | 529/531 | IC | batchOfOneHundredUsesOneQuery, hundredUserProjectionUsesOnlyAuxiliaryIdsAndOneMembershipLookup |
| Atribuição/retificação auditada, versão, HTTP e permissions explícitas | 530 | IC | MemberRegistryApiIntegrationTest e competingCorrectionsHaveOneWinner |
| Projeção após retificação em perfis/listas/pesquisa/CSV/PDF | 531 | IC | MemberRegistryApiIntegrationTest, hundredMemberExportResolvesBusinessNumbersOncePerChunk |
| Contador independente, lock Identity→registry, constraints e idempotência | 532 | IC | concurrentGenerationIsUniqueAndSequential, concurrentRequestsForSameIdentityAreIdempotent |
| Rollback de Member/claims/audit/contador | 532 | IL | Há falha de audit na emissão; reforçar snapshot completo e falha de retificação |
| Geração concorrente com retificação e número retirado com zeros | 532 | IL | Há corrida com atribuição; falta cenário específico de retificação |
| Emissão false e homologação obrigatória de configuração | 532 | IC | MemberRegistryPolicyTest, .env.example e Compose |
| Aprovação não emite; ambos primeiros acessos emitem quando habilitados | 533 | IC | MembershipActivationFlowIntegrationTest, repetido em MySQL |
| Reenvio/expiração/retries/rejeição/concorrência sem emissão prematura | 533 | IL | Fluxos testados; reforçar invariantes de claims/contador/audit |
| Rollback no primeiro acesso por senha temporária após falha do Registry | 533 | IL | Falha no access link testada; falta equivalente temporária |
| Ambos primeiros acessos com emissão false | 533 | IL | Default testado, falta fluxo completo sem emissão |
| ROOT/técnicos e legados autenticam sem Member; sem gate novo | 533 | IC | Login administrativo real sem Member; callback só atua em aplicação APPROVED; policy disabled |
| BACK-512 retém Member e BACK-523 serializa rejeição/primeiro acesso | 527/533 | IC | MembershipsUserDeletionGuard, PrincipalLifecycleIntegrationTest e races do onboarding |
| Reconciliação por identityUserId existente, sem criar User/inferir matrícula | 534 | IC | MemberRegistryImportService reutiliza admin.assign |
| Dry-run obrigatório sem escrita; proof por ator/fonte/motivo/linhas/tempo | 534 | IC | dryRunNeverWritesAndReportsMissingAndUnverifiedIdentities, MemberRegistryPolicyTest |
| Confirmação administrativa e referência; conflitos/duplicidades/ausências/reservas | 534 | IC | administrativelyConfirmed/sourceReference; preview sanitizado por índice/outcome |
| Transação/idempotência por unidade, revalidação, aplicação parcial | 534 | IL | Casos de conflito/retry existem; falta falha intermediária de uma unidade seguida de sucesso |
| Auditoria persistida e projeção atual; sem PII nos relatórios | 534 | IC | audit por atribuição, resultado sem eco de linha; fonte/motivo devem ser sanitizados pelo operador |
| Fonte oficial, inventário ativo e autorização de aplicação real | 534 | BE | Não apresentados; nenhuma operação real autorizada |
| Imports apply false por padrão | 534 | IC | MemberRegistryImportDisabledIntegrationTest |
| Runbook atual em três fases, recuperação e GO/NO-GO | 536 | IL | Documentos ainda falam baseline V3/V4 futura e BACK-512/523 futuras |
| Backup real, restauração isolada representativa e assinatura das fases | 536 | BE | Não executados nesta missão; teste sintético não substitui estes aceites |

Nenhum componente de produção ausente foi demonstrado (**NI**). A revisão
acrescenta provas e corrige documentação, preservando implementação e políticas.

## Alterações e rastreabilidade do aceite técnico

Não foi reproduzido defeito de produção que justificasse alterar serviços ou
contratos. Mantidos o monólito modular, JPA, locks existentes, RBAC explícito,
projeção dinâmica e default false das flags. Nenhum endpoint/entity/repository,
abstração, campo de schema ou migration adicional foi criado.

| Lacuna inicial | Complemento nesta revisão | Verificação |
| --- | --- | --- |
| Governança dispersa | ADR com identificadores, transições, política conservadora, permissions e decisões externas | docs/adr/back-526-member-registry.md |
| Corrida retificação × geração | Disputa por variantes numéricas com zeros, preservando PK e claim retirada | generationAndCorrectionSerializeNumericVariantsWithoutReusingClaims, H2 e MySQL |
| Número retirado equivalente ao próximo | Gerador pula claim histórica mesmo após retificação | generatorSkipsRetiredNumericVariantWithoutChangingIdentity, H2 e MySQL |
| Falha de retificação | Snapshot de Member/versão/claims/audit permanece íntegro | correctionAuditFailurePreservesCurrentNumberVersionClaimsAndHistory, H2 e MySQL |
| Falha de emissão | Assertions adicionais de assinatura do contador, Member/claims/audit | issuanceFailureRollsBackCounterMemberClaimAndAudit, H2 e MySQL |
| Falha de uma unidade de importação | Audit falha na primeira linha; próxima confirma; retry não duplica | failedImportUnitRollsBackAndDoesNotPreventNextUnitOrSafeRetry, H2 e MySQL |
| Senha temporária e falha tardia de Registry | Credencial, sessão, aplicação e sequência revertem; mesmo challenge permite retry e depois é recusado | temporaryPasswordRegistryFailureRollsBackCredentialsSessionChallengeAndSequenceThenRetries, H2 e MySQL |
| Emissão false nos dois métodos | COMPLETED e login real sem Member nem consumo; usuário administrativo legado preservado | disabledIssuanceAllowsBothFirstAccessMethodsAndLegacyLoginWithoutRegistryWrites, duas variantes H2 e MySQL |
| Reenvio/expiração sem consumo | Snapshot de Member/claims/audit/contador antes/depois | Assertions nos dois cenários HTTP existentes, H2 e MySQL |
| Procedimento antigo de implantação | Runbook em três fases com backup/restore, GO/NO-GO e recuperação; docs de V4 e lifecycle atualizadas | docs/back-526-runbook.md e documento do módulo |
| Gate completo dependia de data de calendário | Fixture preexistente de RefreshTokenTransactionIntegrationTest criava sessão/token em 15/10/2026 com TTL 7d; agora usa Instant.now(clock) | Duas falhas reproduzidas isoladamente antes da correção; sem alterar expiração, assertions ou código de produção |

As novas provas reutilizam MemberRegistryScenarios e
MembershipActivationFlowIntegrationTest, herdados pelas suítes MySQL existentes.
São sete casos adicionais por banco (a variante enabled=false conta duas vezes),
sem criar uma segunda implementação dos fluxos. As suítes existentes continuam
cobrindo 400/401/403/404/409, OpenAPI/permissions, ROOT/CUSTOM, lote/exports sem N+1,
Billing, access link, rejeição e lifecycle. A recusa de challenge consumido é 401
conforme contrato de Authentication; não foi alterada para atender uma expectativa
errada do novo teste.

A correção da fixture de Authentication é separada das novas provas do Registry.
A primeira execução completa encontrou 2.149 testes, duas falhas nessa fixture,
zero erros e um skip de symlink Windows. A reprodução isolada repetiu duas falhas
em três casos, confirmando que os tokens já expirados impediam alcançar os passos
de rotação/rollback. Nenhuma validação de validade/expiração foi relaxada.

## Estado das dez subtarefas e bloqueios preservados

| Subtask | Entrega técnica auditada | Aceite externo / estado Jira |
| --- | --- | --- |
| BACK-527 | Regras, ADR e conflitos consolidados | Permanece pendente: aprovação da ADR, formato/equivalência/reservas/faixa e marco formal |
| BACK-528 | JPA/V4/constraints existentes preservados | Já Concluído; sem alteração de status |
| BACK-529 | Queries atuais/reversas/lote existentes preservadas | Já Concluído; sem alteração de status |
| BACK-530 | Administração/audit/RBAC/HTTP existentes preservados | Já Concluído; sem alteração de status |
| BACK-531 | Projeções/pesquisa/exports existentes preservados | Já Concluído; homologação visual do frontend não presumida |
| BACK-532 | Alocador existente auditado e provas complementadas | Permanece pendente: homologação da política/faixa e reconciliação histórica antes de ativação |
| BACK-533 | Ambos primeiros acessos, rollback, flags e compatibilidade BACK-512/523 | Permanece pendente: aprovação do marco formal e homologação funcional do clube |
| BACK-534 | Importador existente, dry-run/proof, partial/rollback/retry comprováveis | Permanece pendente: fonte oficial, inventário e conferência/autorização real |
| BACK-535 | Suítes existentes reutilizadas e complementadas | Já Concluído; resultados atuais devem ser lidos abaixo |
| BACK-536 | Runbook atual com três fases independentes e critérios de recuperação | Permanece pendente: backup real/restauração isolada representativa e assinatura das fases |

Nenhuma aprovação externa foi inferida do CI. Story e cinco subtarefas pendentes
não devem ser encerradas nesta entrega. Emissão exige configuração explicitamente
homologada, legado/reservas revisados e autorização de ativação. Importação exige
fonte oficial conferida, permissão, autorização de janela, dry-run exato/proof
válido, confirmação humana e apply habilitado só durante a janela. Ambas flags
permanecem false até essas decisões, sem catch-up automático para contas antigas.

## Resultados executados — 25/10/2026

Código testado: `36c70e13a1d2cd874e4772a1e0b7d752ed18a09e`. Commits posteriores
acrescentam somente documentação/evidência; não alteram fonte Java/testes ou SQL.
Java Microsoft OpenJDK 17.0.18+8-LTS e Testcontainers MySQL 8.0.46.

| Comando solicitado | Testes | Falhas | Erros | Skips | Tempo Maven |
| --- | ---: | ---: | ---: | ---: | --- |
| ./mvnw --batch-mode --no-transfer-progress verify | 2149 | 0 | 0 | 1 | 05:34 |
| ./mvnw --batch-mode --no-transfer-progress -Pmysql-migrations -DskipUnitTests=true verify | 261 | 0 | 0 | 0 | 18:55 |
| bash deploy/test-future-migration.sh | 261 | 0 | 0 | 0 | 19:20 |

No Windows, os dois primeiros comandos foram executados por `mvnw.cmd`, com
JAVA_HOME do Java 17 somente no processo. O terceiro usou Git Bash e Git archive
do mesmo código: V10 temporária apenas no arquivo isolado, nunca na release.
MySQL e arquivo isolado usaram builds e bancos distintos em paralelo; ambos
terminaram com exit code 0. Os resultados de gates sobrepostos não são casos únicos.

O único skip é preexistente: LocalFileStorageTest.
shouldRejectSymbolicLinkInsteadOfFollowingItOutsideRoot, cujo teste faz assumption
quando o ambiente não permite criar symlink. Nenhum teste foi desabilitado nesta
revisão. A correção de fixture de refresh e a expectativa 401 do novo teste estão
descritas acima, incluindo as execuções intermediárias que falharam.

As oito suítes MySQL, tanto na release V9 quanto no arquivo com V10, passaram:
DependentPhotoMySqlIT (20), PrincipalLifecycleMySqlIT (30),
MembershipRegistryOnboardingMySqlIT (31), MemberRegistryMySqlIT (32),
MySqlMigrationIT (23), EventOperationsMySqlIT (81), VehicleGalleryMySqlIT (28)
e VehicleMediaOrchestrationMySqlIT (16). A evidência estruturada inclui totais,
comandos, casos intermediários, suítes pertinentes e blobs Git das nove migrations
em [back-526-finalization-evidence.json](back-526-finalization-evidence.json).
Os logs brutos ficam fora do versionamento; resultados de CI devem ser conferidos
no HEAD publicado do PR, inclusive mergeability e job de deploy skipped.

## Matriz final de critérios

| Critérios | Estado técnico após os gates locais | Pendência de aceite |
| --- | --- | --- |
| Identificadores, PK/FK/JWT/contratos financeiros preservados | Implementado e comprovado | Nenhuma mudança de produção nesta revisão |
| JPA, constraints, Flyway e Hibernate validate | Implementado e comprovado | Comprovação operacional na instância real é independente |
| Política conservadora de zeros, colisões, claims/reservas e não reutilização | Implementado e comprovado | Regra oficial alternativa depende do clube |
| Administração, audit, HTTP e RBAC sem privilégios CUSTOM implícitos | Implementado e comprovado | Designação/grants operacionais pelo responsável |
| Queries, projeções, pesquisa/exports e ausência de N+1/fallback | Implementado e comprovado | Homologação visual da UI externa a este repo |
| Emissão concorrente, idempotência e rollback completo | Implementado e comprovado | Formato/faixa/reservas/legado aprovados antes de habilitar |
| Primeiro acesso nos dois métodos, disabled, retries e BACK-512/523 | Implementado e comprovado | Aprovação formal do marco e homologação funcional |
| Dry-run/proof, confirmação, partial/rollback e retry da importação | Implementado e comprovado | Fonte oficial e conferência humana/autorização real |
| ADR, transições e runbook em três fases | Implementado e comprovado como documentação | Aprovação da ADR e assinatura das fases |
| Backup real, restauração representativa e liberação operacional | Bloqueado por decisão/evidência externa | Não executados nem autorizados nesta tarefa |

As lacunas técnicas IL da matriz inicial foram complementadas pelas provas e
documentos identificados acima. Não há componente funcional novo NI demonstrado.
Os itens BE continuam bloqueados; CI verde não os converte em aceite.

**Parecer técnico: READY WITH BUSINESS BLOCKERS.** Os gates locais estão aprovados;
a implementação pode seguir revisão do PR para develop, com flags false e sem
schema novo. A Story e BACK-527/532/533/534/536 permanecem abertas por homologação,
fonte oficial e evidências operacionais indispensáveis. Não houve merge, deploy,
acesso à VPS, importação real, concessão de grants ou ativação de flags.

## Referências

- [ADR e matriz de transições](adr/back-526-member-registry.md).
- [Runbook de três fases](back-526-runbook.md).
- [Contrato proprietário do módulo](../src/main/java/com/jeepclub/backend/memberships/docs/member-registry.md).
- [Jira BACK-526](https://jeep-club-developers.atlassian.net/browse/BACK-526).
- [Implementação #104](https://github.com/Luskahz/jeep-club-backend/pull/104).
- [Release #112](https://github.com/Luskahz/jeep-club-backend/pull/112).
