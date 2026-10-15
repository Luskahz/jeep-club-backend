# BACK-245 — auditoria técnica de Authentication

Data: 15/10/2026. PR: https://github.com/Luskahz/jeep-club-backend/pull/98.
Base examinada: `develop@b00f492e755f56d9818fda17d8d0a863e9b31216`.
Commit original preservado: `24eff47ba06d26e91b52df1b9d36ba30adc07948`.
Requisitos integrais e subtasks: [snapshot Jira](back-245-jira-requirements.md).

## Diagnóstico inicial

A branch estava 7 commits atrás da develop. A integração da base foi feita por merge,
sem rebase, descarte de commits ou alteração da develop. O checkout original permaneceu
na branch feat/back-522-admin-user-update.

Tiago acrescentou contratos HTTP standalone úteis, invariantes de Session, RefreshToken e
PasswordRecoveryRequest, seis cenários de serviço de refresh e testes de mapeamento/delegação.
Esses testes foram preservados. Não provavam cadeia de segurança, transações, locks ou persistência
real. A suíte anterior já cobria conta, JWT, challenges, revogação em lote, provisioning,
ativação administrativa e concorrência de login; portanto esses fluxos não foram reconstruídos.

Os três testes de save de adaptadores aceitavam qualquer entidade e devolviam uma fixture
independente da entrada: uma regressão no mapeamento de escrita poderia passar. Foram reforçados
com captura de todos os campos persistidos. O teste de refresh unitário prova decisões e
mutações no objeto; a atomicidade passou a ser verificada separadamente com transações reais.

## Matriz de requisitos

Estado indica a cobertura final; a ação explicita a lacuna inicial. Os nomes são classes/métodos
em src/test/java/com/jeepclub/backend; `—` significa ausência de evidência direta nesse conjunto.
Coberto não significa que todos os caminhos possíveis foram enumerados.

| Requisito ou comportamento | Teste preexistente | Teste do Tiago | Estado | Ação necessária / efetuada |
|---|---|---|---|---|
| Login válido, inválido e formato de CPF | LoginControllerTest.shouldReturnTokensOnSuccessfulLogin / shouldReturnUnauthorizedOnInvalidCredentials / shouldRejectCpfFormatsOutsideTheHttpContract | — | Coberto | Reutilizados; HTTP standalone não é prova transacional |
| Login real até /me | AuthenticationSecurityIntegrationTest (JWT real via registration; não login HTTP completo) | — | Coberto | PasswordRecoveryHttpIntegrationTest.emailResetChangesPersistedPasswordRevokesOldCredentialsAndRejectsReuse agora faz login HTTP e usa o JWT retornado em /me |
| JWT: subject/sessão, assinatura, tipo, expiração | JwtServiceImplTest.usesStableIdentityIdAsSubjectWithoutDependingOnUserAggregate / keepsRejectingExpiredAccessToken / keepsRejectingTokenWithInvalidSignature / keepsRejectingNonAccessTokenType | — | Coberto | Não duplicado |
| Bearer inválido, RFC 9457 e localização | AuthenticationSecurityIntegrationTest.invalidBearerTokenUsesLocalizedProblemDetail | — | Coberto | Reutilizado |
| Conta bloqueada, acesso desativado e credencial pendente | AuthenticationAccountTest.fiveFailedLoginsLockAccountWithoutDisablingAccess / disableAndEnableAccessPreserveLockoutAndCredentialState / pendingFirstAccessIsIndependentFromAdministrativeAccess | RefreshTokenServiceTest.shouldThrowWhenAccountIsLocked | Coberto | Papéis distintos: invariantes e rejeição no serviço |
| Troca obrigatória e challenge expirando durante espera por lock | SessionFirstAccessCompletionTest.temporaryPasswordCompletionMakesCredentialPermanentAndCompletesMembership; SessionChallengeLockExpirationTest.expiredWhileWaitingForIdentityLockMustNotReachPasswordMutation | — | Coberto | Reutilizados após merge |
| Criação, expiração e invariantes de sessão | SessionServiceMeTest.currentSessionReturnsOnlyAuthenticationOwnedData | SessionTest.shouldCreateActiveSession / shouldValidateExpirationCorrectly / shouldThrowWhenLoggedOutStateIsInconsistent | Coberto | Domínio complementa serviço, não duplica objetivo |
| Consulta e logout administrativos, 404/400 | AuthenticationSecurityIntegrationTest.positiveAdministrativePathVariableReturnsValidationProblem | AdminSessionControllerTest.shouldReturnNotFoundWhenSessionDoesNotExist / shouldLogoutSessionAdministratively / shouldReturnBadRequestWhenLogoutFailsStateValidation | Coberto | Standalone mantido como contrato; segurança real acrescentada |
| Permissões específicas de sessões, refresh e recuperação | AuthenticationSecurityIntegrationTest cobria proteção geral e um path inválido autorizado | AdminSessionControllerTest / AdminRefreshTokenControllerTest não executam filtros | Coberto | authenticationAdministrationEnforcesEachPermission: 14 endpoints, anônimo 401, sem authority 403, authority alheia 403, permissão exata 200, papel/JWT/serviços/JPA reais |
| HTTP de refresh: 200/400/401/404/409 | — | RefreshTokenControllerTest.shouldReturnOkAndTokensWhenRefreshTokenIsValid / shouldReturnBadRequestWhenRefreshTokenIsBlank / shouldReturnUnauthorizedWhenRefreshTokenIsInvalid / shouldReturnNotFoundWhenRefreshTokenNotFound / shouldReturnConflictWhenRefreshTokenStateIsConflicted | Coberto | São traduções HTTP de exceções; não alegar que todo erro simulado é alcançável no fluxo normal |
| Rotação e rejeição de estado/identidade | — | RefreshTokenServiceTest.shouldRotateTokensSuccessfully / shouldThrowWhenIdentityIsNotAdministrativelyActive / shouldThrowWhenSessionIsNotValid / shouldThrowWhenRefreshTokenAlreadyRotated | Coberto | Unidade mantida; prova transacional adicionada |
| Uso único sob duas rotações simultâneas | — | shouldRotateTokensSuccessfully usa mocks | Coberto | RefreshTokenTransactionIntegrationTest.simultaneousRotationCommitsExactlyOneSuccessorAndRejectsReplay: duas threads, uma vitória, predecessor ROTATED, vínculo com sucessor, duas linhas, replay rejeitado |
| Rotação concorrente com logout | CredentialRevocationServiceTest.revokesEveryCredentialForUser (delegação) | — | Coberto | concurrentLogoutAndRotationLeaveNoUsableRefreshTokenOrSession: nenhum token ACTIVE e /me 401 ao término |
| Rollback após persistir sucessor | — | — | Coberto | failedJwtIssuanceRollsBackSuccessorAndKeepsOriginalUsable: falha de emissão injetada, somente predecessor ACTIVE, nova tentativa funciona |
| Concorrência de login | ConcurrentLoginIntegrationTest.simultaneousLoginsCreateOnlyOneActiveSession | — | Coberto | Não repetido; banco H2 e locks reais |
| Revogação de credenciais e isolamento por usuário | PasswordChangeChallengeJpaRepositoryTest.shouldInvalidateOnlyActiveNonExpiredChallengesForTargetUser / shouldRevokeActiveSessionsAndRefreshTokensForTargetUser | RefreshTokenRepositoryAdapterTest.shouldDelegateRevocations | Coberto | Teste de query real reaproveitado; Mockito caracteriza delegação |
| Recuperação pública sem enumeração de usuários | RequestPasswordRecoveryServiceTest.unknownCpfReturnsSamePublicRepresentationAsExistingUser / userWithoutEmailGetsGenericEmailResultWithoutTokenOrNotification; AuthenticationSecurityIntegrationTest.publicRecoveryRoutesAreNotInterceptedBySecurity | — | Coberto | Não duplicado |
| Redefinição real, senha persistida e sessão antiga revogada | PasswordRecoveryControllerTest.shouldReturnNoContentWhenResetPasswordByToken usa serviço mock | PasswordRecoveryRequestTest.shouldResolveOpenRequest / shouldThrowWhenResolvingAlreadyResolvedRequest | Coberto | PasswordRecoveryHttpIntegrationTest.emailResetChangesPersistedPasswordRevokesOldCredentialsAndRejectsReuse: e-mail capturado, hash persistido, senha alterada, refresh/access antigos rejeitados, token usado 401 |
| Recuperação expirada não altera credencial/sessão | — | PasswordRecoveryRequestTest.shouldThrowWhenChangingMethodOnExpiredRequest (domínio) | Coberto | expiredPersistedRecoveryTokenCannotChangePasswordOrRevokeValidSession testa HTTP e banco; contrato vigente é 401 TOKEN_INVALID, não 410 |
| Recuperação administrativa e autorização | AdminPasswordRecoveryControllerTest.shouldReturnOkWhenAdminGeneratesTemporaryPassword / shouldReturnOkWhenAdminGeneratesResetLink | — | Coberto | Matriz de segurança real ampliada para leitura, cancelamento, senha provisória e reset-link |
| Convite administrativo, hash, rotação e uso único | identity/.../AdminUserCreationIntegrationTest.invitationUsesHashedTokenRotatesAndIsSingleUse / expiredOrBlockedInvitationCannotActivateAccount / simultaneousActivationConsumesInvitationOnlyOnce | AccountActivationControllerTest.shouldValidateInvitationSuccessfully / shouldReturnGoneWhenInvitationIsExpiredOrInvalid / shouldReturnConflictWhenActivationHasConflict | Coberto | Prova integrada anterior preservada; controller cobre 410/409/validação |
| Concorrência de convite, conflitos com Membership e Identity | AdminUserCreationIntegrationTest.simultaneousReissuesLeaveOnlyNewestInvitationValid / simultaneousActivationAndReissueCannotMakePermanentAccountPending / membershipOwnedPendingUserCannotReceiveAdministrativeInvitation | — | Coberto | Não duplicado |
| Persistência de conta, FK e independência de Identity | AuthenticationAccountRepositoryAdapterTest.persistsAccountForExistingIdentityUsingTheSameSharedPrimaryKey / rejectsAuthenticationAccountForNonexistentIdentity / deletingAuthenticationAccountDoesNotDeleteUser | — | Coberto | JPA real preexistente |
| Persistência de sessão/refresh e consultas de recuperação | PasswordChangeChallengeJpaRepositoryTest e AdminUserUpdateTransactionTest.emailCorrectionMustCancelEveryOpenEmailRecoveryToken | SessionRepositoryAdapterTest / RefreshTokenRepositoryAdapterTest / PasswordRecoveryRequestRepositoryAdapterTest | Coberto | Captura de escrita reforçada; novos fluxos reais de refresh e recuperação exercitam adapters/mappers/queries; não chamar testes Mockito de JPA |
| DTO, OpenAPI, 400, 401, 403, 404, 409, 410 | AuthenticationDtoContractCharacterizationTest; AuthenticationSecurityIntegrationTest.openApiKeepsPublishedAuthenticationPaths / openApiDescribesAuthenticationResponsesPrecisely | Controllers e AccountActivationControllerTest.shouldReturnBadRequestWhenPasswordIsTooShort | Coberto | Contratos mantidos e exercitados em níveis complementares |
| Cadastro público pertencente a Authentication | Registro está em Identity e onboarding em Membership | — | N/A | Nenhuma rota de cadastro inventada |
| JaCoCo e PIT focal | JaCoCo global / PIT padrão selecionava Dependents | authentication-report / authentication-mutation | Coberto | Corrigido reportsDirectory do goal mutationCoverage e adicionado namespace iam aos targetTests; escopo de mutação permanece explícito |

## Decisões e limites

- Nenhum arquivo de produção alterado. Nenhum teste original removido.
- Não duplicamos os fluxos de convite concorrente e revogação em lote já presentes.
- Sobreposição de domínio/serviço/HTTP é intencional quando detecta falhas diferentes.
- Testes standalone não provam permissões; a nova matriz usa JWT, roles e banco reais.
- Os novos testes transacionais não usam @Transactional no método: cada serviço faz commit/rollback real.
  Os testes de permissão usam rollback da fixture e IDs específicos; não dependem de dados de outro teste.
- A única substituição no reset é NotificationPort (e-mail externo). A spy de JwtService injeta uma falha
  depois da escrita para provar rollback; os demais caminhos executam seu método real.
- H2 é a infraestrutura definida em BACK-253. Locks e concorrência foram exercitados em H2;
  isso não certifica todas as particularidades de isolamento/deadlock do MySQL.
- Concorrência usa latches e Future com timeout, sem sleeps nem dependência de ordem.
- O perfil PIT seleciona Session, RefreshToken, PasswordRecoveryRequest, AuthenticationAccount e RefreshTokenService.
  Não representa mutação de todo Authentication. Testes vivem principalmente no namespace legado authentication;
  produção vive em iam.authentication. Ambos são considerados pelo perfil de testes.
- As evidências quantitativas finais e sobreviventes estão em [resultados](back-245-validation.md).

## Gaps confirmados pelo PIT

O perfil amplo original também selecionava testes Spring HTTP, levando a minions TIMED_OUT.
A rodada foi interrompida e não é usada como evidência de sucesso. targetTests passou a selecionar
as unidades de domínio e de RefreshTokenService (e o namespace iam correspondente);
os testes Spring continuam na suíte completa test/verify.

A primeira rodada focal terminou com 183 KILLED, 28 SURVIVED e 6 NO_COVERAGE (217 mutantes).
A análise encontrou guards de credential/account e estados persistidos sem asserções proporcionais.
AuthenticationBoundaryInvariantTest acrescenta somente os limites não detectados nessa rodada:

| Requisito ou comportamento | Teste preexistente | Teste do Tiago | Estado | Ação necessária / efetuada |
|---|---|---|---|---|
| Identidade inválida e hash ausente em criação/restauração/mutação | AuthenticationAccountTest cobria sucesso e separação dos estados | — | Coberto | accountCreationAndReconstitutionRejectInvalidIdentity; everyCredentialEntryPointRejectsMissingHashWithoutChangingAccount |
| Conta desabilitada não autentica nem redefine senha | Testes de estados, sem chamadas diretas a todos os guards | — | Coberto | blockedAccountCannotAuthenticateOrRequestPasswordChange |
| Predicados distinguem permanente/provisória/primeiro acesso | Apenas caminhos positivos parciais | — | Coberto | credentialPredicatesDistinguishPermanentTemporaryAndFirstAccess |
| Mutações não antecedem criação e preservam campos após rejeição | — | — | Coberto | mutationBeforeAccountCreationIsRejectedWithoutChangingFields, cinco operações |
| Datas persistidas e limite de tentativas de lock | Reconstituição com estado impossível parcial | — | Coberto | reconstitutionRejectsTimestampsBeforeCreation; persistedLockRequiresFiveAttemptsButEnabledAccountMayHaveZero |
| Troca de método de recovery após consumo/expiração | — | PasswordRecoveryRequestTest cobria apenas parte das trocas | Coberto | administrativeRecoveryMethodCannotChangeAfterResolutionOrExpiration |
| Estados resolvido/cancelado e timestamps contraditórios | — | Reconstituição sobretudo positiva | Coberto | reconstitutionRejectsContradictoryRecoveryLifecycle; cancelledRecoveryRestoresAuditTimestampAndRejectsZeroIdentifier |
| Limites máximos de hash e sucessor com ID zero | — | Testes de valores inválidos sem a borda máxima válida | Coberto | maximumHashLengthsAreAcceptedAndZeroReplacementIdsAreRejected |

Nenhum guard de produção foi relaxado. O resultado da última rodada está na documentação de validação.
