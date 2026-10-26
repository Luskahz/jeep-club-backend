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
Resultados dos gates completos e PR serão registrados ao terminar a execução.
