# BACK-526 — política numérica homologada em 26/10/2026

## Auditoria anterior à implementação

Base: develop `236c6715c8dbefe262a60d1b927dad26e0ba2909`, incluindo #113.
Branch: feat/back-526-canonical-current-number. #104, #108 e #113 inspecionados;
Story e cinco subtarefas pendentes conferidas no Jira. A solicitação de 26/10
homologa e substitui as regras conservadoras anteriores: número decimal positivo,
canônico, início 1, máximo vigente + 1 e reutilização após correção. Não autoriza
publicação, acesso à VPS, importação ou ativação de flags.

| Componente inspecionado | Diferença objetiva | Alteração necessária |
| --- | --- | --- |
| MembershipNumber | Aceita texto arbitrário e mantém zeros | Canonicalizar dígitos ASCII positivos; manter String HTTP/VARCHAR(64) |
| AdminMemberService / MemberRepositoryAdapter | Contador next_value e assinatura fixam crescimento | Leitura numérica corrente sob mutex, sem contador autoritativo |
| Claims / FKs | Todas as claims antigas bloqueiam para sempre | Remover ocupação anterior na mesma transação da retificação; audit permanece |
| Reservas | Claim sem memberId | Preservar como bloqueio explícito; não criar endpoint redundante de liberação |
| MemberIssuanceProperties | Exige initialNumber/width/prefix | Manter enabled/approved; recusar configuração antiga explícita incompatível |
| Importador / queries / Identity | Comparações literais | Canonicalizar comparação; q textual continua busca textual sem erro |
| Onboarding e BACK-523 | Já emite somente ao concluir primeiro acesso | Preservar serviços e contratos; repetir regressões nos dois métodos |
| Lifecycle guard | Protege Member e referências como ator | Manter; evidências antigas migram para audit e continuam retidas |
| V4–V9 / JPA | VARCHAR(64), claims únicas e audit independente | V10 reconcilia estado legado; não converter PKs nem tipos públicos |
| Testes e documentos | Pressupõem prefixos, zeros preservados e claims eternas | Substituir premissas pela regra homologada, preservando cobertura |

Intervalo suportado: 1 a 10^64−1, decorrente do VARCHAR(64) já existente. A
aritmética usa BigInteger e a consulta MySQL CAST AS DECIMAL(65,0), nunca ordenação
lexicográfica nem AUTO_INCREMENT. Reservas explícitas na posição seguinte são
puladas; números históricos liberados não participam do máximo ou do bloqueio.

V10 fará preflight de Members e reservas vigentes, colisões canônicas e vínculo
de claims. Claims anteriores serão copiadas integralmente como eventos de snapshot
no audit existente antes de liberar claims retiradas. Auditorias antigas não serão
alteradas. Formatos vigentes/reservas inválidos ou duplicidades impedem a migração
antes de mudanças de negócio. Nenhum banco real será acessado nesta rodada.

## Validação e rastreabilidade

| Critério solicitado | Cenário automatizado / evidência |
| --- | --- |
| 1. Vazio inicia em 1 | emptyRegistryStartsAtOneAndIgnoresStaleCounterAndSignature |
| 2. Máximo 200 → 201 | maximumIsNumericAndNotLexicographic |
| 3. 9999→150 volta a 201 | correctingHighest9999To150MakesNext201Again |
| 4–5. 9999 ativo →10000; emitido permanece | active9999Generates10000AndOnlyCorrectingItDoesNotChange10000 |
| 6–7. Libera/reutiliza número | released20CanBelongToAnotherIdentityWithoutRewritingFirstHistory |
| 8. Ocupado conflita | assignmentIsIdempotentAndDoesNotForgeAudit / API 409 |
| 9. Reserva bloqueia | correctedNumberIsReusableAndReservationsStillBlock |
| 10. Duplicidade, versão, permissions | competingCorrectionsHaveOneWinner / isolatedRegistryPermissionsDoNotGrantOtherOperations |
| 11. 027=27 | leadingZerosAreCanonicalAndCannotCreateASeparateNumber / numericAliasIsCanonicalIdempotentAndVersionConflictRemains409 |
| 12–13. Titular vigente e histórico anterior | released20CanBelongToAnotherIdentityWithoutRewritingFirstHistory |
| 14. Duas emissões | concurrentGenerationIsUniqueAndSequential / concurrentRequestsForSameIdentityAreIdempotent |
| 15. Correção×geração | generationAndCorrectionSerializeNumericVariantsAndReleaseOldOccupancy |
| 16. Rollback integral | correctionAuditFailurePreservesCurrentNumberVersionClaimsAndHistory / issuanceFailureRollsBackCounterMemberClaimAndAudit |
| 17–18. Emissão no primeiro acesso, sem aprovação/reenvio | MembershipActivationFlowIntegrationTest (ambos métodos) e repetição MySQL |
| 19–20. Reversão pendente / COMPLETED bloqueado | Corridas e reversões existentes BACK-523; completedOnboardingAllowsHistoricalCorrectionAndReuseWithoutChangingIdentity |
| 21. Upgrade e validate | numericRegistryUpgradePreservesIdentityAndAllLegacyClaimEvidence / numericRegistryPreflightRejectsIncompatibleDataWithoutBusinessMutations |
| 22. Import não contorna unicidade | importCannotBypassCanonicalUniquenessAndCanReuseReleasedNumber / duplicateSourceIsNotSilentlyApplied |
| Snapshot antigo MySQL RR | currentLockingMaximumSeesCommitAfterEarlierRepeatableReadSnapshot, somente MySQL |
| Reserva×atribuição | reservationAndAssignmentForSameNumberHaveOneWinner |
| Intervalo e overflow | supportedRangeIsNotLimitedByLongOrMemberCountAndOverflowDoesNotWrite |
| Legado manual após primeiro acesso | completedOnboardingAllowsHistoricalCorrectionAndReuseWithoutChangingIdentity, duas variantes |

Os nomes herdados conservam rastreabilidade dos testes anteriores, mas suas
premissas foram adaptadas à reutilização/canonicalização. H2 não emula o current
read do MySQL em RR: a prova dessa característica fica na suíte MySQL, sem skip.
O primeiro focal (92 casos) passou; o focal ampliado identificou essa diferença
H2 (1 falha em 70 casos), corrigida pela localização da prova MySQL. O focal
MySQL passou 48 casos (41 Registry e 7 upgrade/preflight), sem falhas/erros/skips.
Os resultados finais dos gates completos estão registrados ao fim deste documento
e na [evidência estruturada](back-526-numeric-policy-evidence.json).

## Entregas por subtask

| Subtask | O que já existia | Resultado desta rodada | Aceite restante |
| --- | --- | --- | --- |
| BACK-527 | ADR e política conservadora | Regras homologadas, transições, unicidade vigente, reservas e reutilização; início 1, sem faixa configurável | Nenhuma decisão de numeração em aberto |
| BACK-532 | Alocador transacional, claims e mutex | Máximo numérico corrente, BigInteger, liberação de ocupação e recusa explícita de configuração antiga | Gates completos registrados abaixo |
| BACK-533 | Emissão no primeiro acesso e reversão BACK-523 | Serviços preservados; regressões link/senha, correção manual posterior e reutilização | Gates completos registrados abaixo |
| BACK-534 | PUT/PATCH, projeções e importador protegido | Fluxo manual homologado, unicidade canônica, reuso e importação opcional compatível | Inventário autorizado da base ativa e evidência representativa de preservação exigidos no refinamento do Jira |
| BACK-536 | Runbook em fases | V10, preflight sanitizado, backup/restauração, recuperação e gates separados | Restauração representativa, preflight/grants reais, release e aceite pós-publicação autorizados |

BACK-528/529/530/531/535 já estavam Concluídas e também foram relidas para
rastreabilidade. Seus exemplos antigos de zeros preservados/não reutilização
são superados pela homologação desta rodada, sem reabrir arquitetura ou duplicar
componentes. A documentação funcional inventaria as superfícies HTTP do frontend.
Testes de contrato não equivalem a smoke visual de um frontend publicado.

## Falhas intermediárias e correções da validação

Os primeiros gates completos MySQL e migration futura executaram 280 casos cada,
com 0 falhas, 1 erro e 0 skips. O erro era a fixture preexistente de lifecycle
que construía `B512-700`, inválida na política numérica. Agora usa `512700` e
preserva a comparação integral de Members, claims e auditorias anteriores,
acrescentando a exigência do snapshot V10; não se removeu cobertura de lifecycle.

Uma consulta constante somente leitura em MySQL 8.0.46 reproduziu que ICU aceita
LF/CR finais com `^[0-9]+$`. V10 e o preflight agora rejeitam qualquer não dígito
e exigem ao menos um 1–9. Dois cenários de upgrade recusam controles em Member e
reserva e comprovam ausência de mutações; o domínio também exige essa rejeição.
Execuções interrompidas para incorporar essa correção não contam como aprovação.

Os harnesses auxiliares de deploy/snapshot foram repetidos em Linux isolado após
limitações de paths Windows, tmpfs noexec e CRLF. Seis testes passaram, assim como
`bash -n` em todos os scripts, sobre `git -c core.autocrlf=false archive` do commit
testado. Repositório somente leitura, rede desabilitada e sem socket Docker nesse
contêiner; o harness usa um double, sem executar deploy real. As duas composições Docker
validaram com `.env.example` e secrets sintéticos. As cinco consultas do preflight
também executaram em schema sintético MySQL 8.0.46, com contadores zero nessa fixture.

## Limites operacionais e critérios GO/NO-GO

As decisões de negócio desta solicitação estão homologadas; não resta exigir
formato, faixa fixa, planilha ou importação em lote para concluir a funcionalidade
manual. Os 22 critérios técnicos acima têm cenários automatizados identificados.
Resultados finais devem ser lidos com o SHA e os gates completos, sem somar
execuções repetidas como casos únicos.

Não foram executados inventário da VPS, restauração de backup produtivo,
reconciliação de cadastros reais, smoke visual publicado, deploy ou ativação de
flags. BACK-534 mantém seu aceite adicional de inventário/preservação real;
BACK-536 e a Story permanecem abertas até evidências operacionais indispensáveis.
Isso não impõe bulk import ao fluxo manual nem constitui decisão de numeração pendente.

Emissão: release autorizada, escritores antigos parados, backup/restauração,
preflight compatível, privilégios de routine, Hibernate validate/health e decisão
operacional explícita para ENABLED/APPROVED. Importação opcional tem janela própria,
fonte conferida, permission, confirmação/sourceReference, dry-run/proof e apply
temporário autorizado. Os três defaults continuam false. Nenhuma flag produtiva
foi alterada nesta rodada. Falha de preflight, backup não restaurável ou autorização
ausente são NO-GO; não usar clean/repair/reset nem downgrade automático da política.

## Gates completos executados

Código/SQL/testes: `1717433397d4e334ebfbf2fbf7b2a8726d7da9b6`.
Java Microsoft 17.0.18+8-LTS e MySQL 8.0.46, somente fixtures sintéticas.
O commit de entrega posterior acrescenta somente documentação/evidências.

| Comando real executado | Testes | Falhas / erros / skips | Tempo Maven | Saída |
| --- | ---: | --- | --- | --- |
| mvnw.cmd --batch-mode --no-transfer-progress verify | 2167 | 0 / 0 / 1 | 06:45 | 0, BUILD SUCCESS |
| mvnw.cmd --batch-mode --no-transfer-progress -Pmysql-migrations -DskipUnitTests=true verify | 282 | 0 / 0 / 0 | 23:41 | 0, BUILD SUCCESS |
| bash deploy/test-future-migration.sh (Git Bash) | 282 | 0 / 0 / 0 | 27:23 | 0, BUILD SUCCESS |

O skip é LocalFileStorageTest.shouldRejectSymbolicLinkInsteadOfFollowingItOutsideRoot,
por indisponibilidade de criação de symlink neste Windows; não foi introduzido
pelo Registry. Não houve skip MySQL. O último gate acrescenta V11 apenas no arquivo
Git temporário: V10 continua sendo a última migration da entrega. Os gates repetem
cenários; seus totais não representam casos únicos somáveis.

MySQL, por gate: DependentPhoto 20, PrincipalLifecycle 30, onboarding 33, Registry
41, MySqlMigration 33, EventOperations 81, VehicleGallery 28 e VehicleMedia 16.
H2/API: Registry 38, HTTP 29 e policy 10, além das regressões completas do projeto.
Os dois gates MySQL validam schema vazio, histórico/upgrade, constraints, Hibernate
validate, rollback, concorrência e compatibilidade. A evidência JSON registra
contagens por suíte, hashes dos relatórios locais, tentativas intermediárias,
blobs imutáveis V1–V9 e inventário dos arquivos afetados.

| Grupo de critérios | Estado final técnico |
| --- | --- |
| 1–16: número, máximo corrente, reuso, reservas, RBAC, concorrência e rollback | Implementado e comprovado |
| 17–20: primeiro acesso e reversão BACK-523/BACK-512 | Implementado e comprovado |
| 21: upgrade incremental e JPA/Flyway MySQL | Implementado e comprovado |
| 22: importador opcional não contorna unicidade | Implementado e comprovado |
| Runbook e preparação operacional | Implementado com lacunas de execução real explicitadas acima |
| Inventário/restauração/rollout e smoke publicado | Bloqueado por autorização e evidências operacionais externas a esta rodada |

Parecer: **READY para revisão técnica**. Produção permanece **NO-GO** até os gates
operacionais. Não há dívida técnica não bloqueante nova demonstrada que exija
ampliar o escopo. Referência do PR, resultado do CI do HEAD publicado e atualização
final de status ficam no próprio PR e no comentário de entrega da
[BACK-526](https://jeep-club-developers.atlassian.net/browse/BACK-526).
CI verde não substitui o aceite operacional nem encerra automaticamente a Story.
