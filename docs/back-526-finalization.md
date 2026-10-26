# BACK-526 — auditoria de finalização

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

## Referências atuais

- [ADR e matriz de transições](adr/back-526-member-registry.md).
- [Runbook de três fases](back-526-runbook.md).
- [Contrato proprietário do módulo](../src/main/java/com/jeepclub/backend/memberships/docs/member-registry.md).
- [Jira BACK-526](https://jeep-club-developers.atlassian.net/browse/BACK-526).
- [Implementação #104](https://github.com/Luskahz/jeep-club-backend/pull/104).
- [Release #112](https://github.com/Luskahz/jeep-club-backend/pull/112).
