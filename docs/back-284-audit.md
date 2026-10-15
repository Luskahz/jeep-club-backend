# BACK-284 — auditoria técnica de Membership

Data: 15/10/2026. PR: https://github.com/Luskahz/jeep-club-backend/pull/99.
Base examinada: `develop@b00f492e755f56d9818fda17d8d0a863e9b31216`.
Commit original preservado: `8d9eca3f740b846f62bb40e2f95d4f900888b8a9`.
Requisitos e subtasks integrais: [snapshot Jira](back-284-jira-requirements.md).

## Diagnóstico inicial

Tiago ampliou adequadamente as regras de domínio, controllers públicos/administrativos,
erros RFC 9457, conflitos no serviço e permissões positivas. A nova classe de segurança
já executava a cadeia Spring, embora substituísse parser JWT, validação da sessão,
authorities e serviços; ela caracteriza composição de segurança, não onboarding real.

MembershipActivationFlowIntegrationTest começava com aplicação aprovada e simulava
CompletePendingFirstAccessPort e ActivationRecipientPort. Não provava provisioning,
mudança de credencial ou consistência transacional com Identity/Authentication.
Foi ampliado na mesma classe, preservando o objetivo original de validar, consumir e rejeitar replay.
A branch foi atualizada por merge da develop (7 commits de atraso), preservando todo o histórico.

A suíte anterior já tinha integração real parcial na área Identity. Em especial,
AdminUserCreationIntegrationTest.membershipOwnedPendingUserCannotReceiveAdministrativeInvitation
completa Membership por senha provisória, e
AdminUserUpdateTransactionTest.membershipExpiryAfterAuthenticationRollsBackCredentialChange
prova rollback de expiração após mudança real da credencial. Essas evidências foram reaproveitadas.
PaidMembershipFlowIntegrationTest cobre cobrança/acesso pago; não cobre solicitação → aprovação → ativação.

## Matriz de requisitos

Estado é o resultado final de cobertura; a ação indica a lacuna inicial e sua resolução.
Os métodos estão sob src/test/java/com/jeepclub/backend. `—` = sem evidência direta nesse conjunto.
As linhas cobertas não alegam enumerar toda combinação de estados.

| Requisito ou comportamento | Teste preexistente | Teste do Tiago | Estado | Ação necessária / efetuada |
|---|---|---|---|---|
| Criação pública válida, normalização e resposta sem PII | MembershipApplicationServiceTest.rejectedApplicationAllowsNewApplication; MembershipOpenApiIntegrationTest.publicMembershipErrorsUseRfc9457 | MembershipApplicationControllerTest.shouldReturn201WhenCreated | Coberto | Reaproveitado; onboarding novo submete pelo HTTP real |
| Solicitação duplicada/idempotência | MembershipApplicationServiceTest.repeatedPendingApplicationReturnsExistingApplication; MembershipOpenApiIntegrationTest.repeatedPendingRequestIsIdempotentWithoutLeakingStoredPersonalData | MembershipApplicationControllerTest.shouldReturn200WhenExistingPending | Coberto | Repetição no onboarding comprova mesmo ID antes do provisioning |
| Payload inválido, CPF bloqueado | MembershipApplicationServiceTest.activeBlockIsCheckedBeforePendingApplicationAndDoesNotExposeReason | MembershipApplicationControllerTest.shouldReturn400WhenPayloadInvalid / shouldReturn403WhenApplicantBlocked | Coberto | Controller valida 400/403; regra existente reutilizada |
| Conflitos de CPF/e-mail cadastrado/pendente | — | MembershipApplicationServiceTest.existingUserWithSameCpfThrowsMembershipCpfAlreadyRegisteredException / existingPendingApplicationWithSameEmailThrowsMembershipEmailAlreadyInUseException / existingUserWithSameEmailThrowsMembershipEmailAlreadyRegisteredException; controllers 409 | Coberto | Preservado; não adicionar regras de idempotência não existentes |
| Listagem, filtro, paginação e consulta admin | MembershipApplicationRepository e teste prévio do 404 HTTP | AdminMembershipApplicationControllerTest.listAllReturnsPageOfApplications / listByStatusReturnsFilteredApplications / getByIdReturnsApplicationWhenExists; AdminMembershipApplicationServiceTest.listAllDelegatesToRepository / listByStatusDelegatesToRepository | Coberto | Mockito caracteriza contrato/encaminhamento; não alegar integração de filtros SQL |
| Aprovação por senha provisória, sem e-mail | AdminMembershipApplicationServiceTest.temporaryPasswordApprovalWorksWithoutEmail | AdminMembershipApplicationControllerTest.approveWithTemporaryPasswordReturnsSuccess | Coberto | Novo temporaryPasswordFirstAccessAlsoCompletesMembershipThroughRealBoundary valida contrato HTTP do challenge; prova de boundary de serviço já existe em Identity |
| Aprovação por link exige e-mail | AdminMembershipApplicationServiceTest.accessLinkApprovalWithoutEmailFailsBeforeCreatingUser / approveWithAccessLinkBelongsToMembershipApplicationService | AdminMembershipApplicationControllerTest.approveWithAccessLinkReturnsSuccess | Coberto | Onboarding novo testa aprovação com banco/Identity/Authentication reais |
| Provisioning real em Identity e Authentication | Identity: AdminUserCreationIntegrationTest e UserRegistrationTransactionTest | MembershipActivationFlowIntegrationTest começava com application.approve e IDs inventados | Coberto | completeDeterministicOnboardingFlow começa no POST público, aprova via admin com JWT/permissão reais e consulta usuário/conta persistidos |
| Disponibilização de link e somente hash no banco | MemberActivationTokenServiceTest.issuancePersistsOnlyHashAndInvalidatesPreviousTokens | Fluxo anterior capturava envio, com recipient mock | Coberto | Captura somente e-mail externo; recipient e token generator/hash/JPA reais; applicationId e hash conferidos |
| Validação de ativação é somente leitura | MemberActivationTokenServiceTest.repeatedValidationIsReadOnly | Fluxo anterior e MemberActivationTokenControllerTest | Coberto | Fluxo real verifica usedAt e finishedAt ainda nulos |
| Consumo único, senha definitiva e COMPLETED | MemberActivationTokenServiceTest.completionConsumesTokenAndApplicationOnlyAfterAuthenticationSucceeds | Fluxo anterior verificava chamada ao mock de primeiro acesso | Coberto | Novo fluxo verifica credencial PERMANENT, hash compatível com senha, token consumido, Membership concluído e login → /me real |
| Reutilização após conclusão | MemberActivationTokenServiceTest.validationRejectsUsedTokenWithoutWritingState | Fluxo anterior esperava ACTIVATION_TOKEN_ALREADY_USED nos dois métodos | Coberto | Atualizado ao contrato BACK-522: GET 409 ACTIVATION_TOKEN_ALREADY_USED; POST 409 MEMBERSHIP_FIRST_ACCESS_CONFLICT, pois valida application antes do token |
| Expiração e token desconhecido | MemberActivationTokenServiceTest.validationRejectsExpiredTokenWithoutWritingState / validationRejectsUnknownTokenWithoutWritingState | MemberActivationTokenControllerTest.shouldReturn410WhenTokenExpired / shouldReturn404WhenTokenNotFound | Coberto | expiredAndUnknownTokensDoNotChangeCredentialsOrMembership exercita 410/404 reais, sem consumir ou promover credencial |
| Reenvio invalida o token anterior sem novo usuário | AdminMembershipApplicationServiceTest.resendUsesExistingUserAndDoesNotProvisionAnotherAccount | AdminMembershipApplicationControllerTest.resendActivationLinkReturnsNoContent | Coberto | resendInvalidatesPreviousPersistedTokenAndLeavesFirstAccessPending verifica dois tokens reais e rejeição do anterior |
| Falha externa durante aprovação | Identity: AdminUserCreationIntegrationTest.provisioningFailureRollsBackIdentity | — | Coberto | deliveryFailureRollsBackApprovalIdentityAccountAndTokenThenAllowsRetry prova rollback das três áreas e retry |
| Falha depois da mudança de senha/token, antes da escrita final | AdminUserUpdateTransactionTest.membershipExpiryAfterAuthenticationRollsBackCredentialChange cobre expiração | — | Coberto | finalMembershipWriteFailureRollsBackRealCredentialChangeAndTokenConsumption injeta falha final de persistência; senha/status/token originais restaurados, retry funciona |
| Rejeição, rejeição com bloqueio e motivo | AdminMembershipApplicationServiceTest.normalRejectDoesNotBlockCpf / rejectAndBlockUsesSameTimestampAndApplicationCpf | AdminMembershipApplicationControllerTest.rejectReturnsNoContent / rejectAndBlockReturnsNoContent / rejectAndBlockReturns400WhenReasonBlank | Coberto | Objetivos complementares mantidos |
| Bloquear/desbloquear e auditoria | AdminMembershipApplicantBlockServiceTest.createsBlockWithNormalizedCpfAndAuditData / activeBlockPreventsAnotherBlock / unblockPreservesBlockAndRecordsAuditFields | MembershipApplicantBlockTest.shouldUnblockActiveBlock / shouldFailWhenUnblockingAlreadyInactiveBlock; AdminMembershipApplicantBlockControllerTest.shouldReturn404WhenBlockNotFound | Coberto | Não criar uma rota isolada de block: contrato vigente usa reject-and-block |
| Estado e operações proibidas | — | MembershipApplicationTest.shouldRejectInvalidStateTransitions; AdminMembershipApplicationServiceTest.operationsOnNonPendingApplicationThrowAlreadyProcessedException; controller operationsOnProcessedApplicationReturn409 | Coberto | Mantidos; nova repetição de approve após conclusão retorna 409 |
| Anônimo 401 em rotas administrativas | — | MembershipAdminSecurityIntegrationTest.unauthenticatedAdminAccessReturns401 | Coberto | Cadeia Spring real preexistente no PR, preservada |
| Permissão específica positiva | — | MembershipAdminSecurityIntegrationTest.readAuthorityAllowsAccess / approveAuthorityAllowsApprovalEndpoints / rejectAuthorityAllowsRejectEndpoint / blockAuthorityAllowsRejectAndBlockEndpoint / unblockAuthorityAllowsUnblockEndpoint / exportAuthorityAllowsExportEndpoints | Coberto | Mocks justificáveis para composição de segurança, não usados como prova do onboarding |
| Negativas de approve/reject/block/unblock/export | — | missingReadAuthorityReturns403 só verificava consulta | Coberto | mutationAndExportEndpointsRejectReadOnlyAuthority: 8 endpoints com authority de consulta, 403 RFC 9457 e zero chamadas aos serviços protegidos |
| Público: submit e validação de token | AuthenticationSecurityIntegrationTest.membershipActivationCompletionRouteIsNotInterceptedBySecurity | MembershipAdminSecurityIntegrationTest.publicRoutesAllowAnonymousAccess | Coberto | POST de conclusão anônimo agora exercitado em fluxo real |
| Persistência, unicidade e histórico de bloqueio | MembershipApplicantBlockJpaRepositoryTest.findsOnlyActiveBlockAndPreservesUnblockedHistory / databasePreventsTwoActiveBlocksForSameCpf / rejectedApplicationDoesNotPreventAnotherApplicationForSameCpf / membershipApplicationPersistsWithoutEmail | Domínio adicional | Coberto | JPA real existente mantido; activation mapper/repository agora exercitados por fluxo real |
| OpenAPI e RFC 9457 | MembershipOpenApiIntegrationTest.openApiDescribesMembershipContractsPrecisely / publicMembershipErrorsUseRfc9457 | MembershipExceptionHandlerTest ampliado e controllers | Coberto | Preservados após atualização de base |
| Cobrança, enforcement e status pago | PaidMembershipFlowIntegrationTest.shouldAllowPersistedPendingPaidAndCanceledStates / shouldReturnPaymentRequiredForEffectiveBlockingStates / shouldEnforceAdministrativeConfigurationAuthorities | — | Coberto | Não confundir com ativação/provisioning; reutilizado |
| Registro pertencente a Authentication ou bloqueio de conta Identity por Membership | Boundaries atuais delegam primeiro acesso à Authentication e identidade à Identity | — | N/A | Não inventar nova decisão de negócio nem duplicar testes exclusivos dos outros módulos |
| JaCoCo/PIT focal de Membership | JaCoCo global; sem perfil PIT de Membership | — | Coberto | Adicionados membership-report e membership-mutation, sem modificar escopo dos outros módulos |

## Qualidade, isolamento e limites

- Nenhuma classe de produção foi alterada; todos os commits do PR foram preservados.
- O fluxo anterior foi ampliado na mesma classe. As antigas asserções de estado/validação
  permanecem com dados reais; a verificação de chamada ao mock foi substituída por efeitos persistidos.
- O cenário de sucesso usa Spring Boot, MockMvc, JWT, roles, Identity, Authentication,
  geração/hash e persistência reais. Somente o e-mail externo é simulado.
- Uma spy de MembershipApplicationRepository executa o adapter real; somente no cenário de
  rollback interrompe a última escrita, após a execução real do primeiro acesso.
- Não existe @Transactional externo nos cenários de onboarding: requisições terminam suas próprias
  transações, e as leituras seguintes observam commit/rollback. @DirtiesContext isola a base desse
  conjunto do restante da suíte; CPFs diferentes isolam os cenários e não há deleteAll global.
- Datas de expiração são persistidas explicitamente relativas ao Clock; não há sleeps ou espera
  por expiração real. E-mails e CPFs de teste são fixos; os IDs são obtidos das respostas.
- A semântica atual de MemberActivationToken considera expiresAt igual a now ainda válido
  (isAfter). O teste de domínio de Tiago caracteriza essa borda; não alteramos produção.
- O teste de segurança com mocks continua sendo apenas composição HTTP/security.
  O fluxo real cobre também autenticação/autorização positiva da aprovação.
- A cobertura é H2, conforme BACK-253; não prova todos os comportamentos específicos do MySQL.
- O perfil PIT seleciona três modelos e três serviços (application, admin application e activation).
  Não representa mutação completa de Billing, Identity ou Authentication.
- Os percentuais históricos de BACK-285/288 (77,51% linha / 62,09% branch) são de outra baseline;
  não são apresentados como medição da develop atual.
- Evidências finais e mutantes: [resultados](back-284-validation.md).

## Gaps confirmados pelo PIT

A primeira rodada focal terminou com 93 KILLED, 6 SURVIVED e 7 NO_COVERAGE (106 mutantes).
Além da integração HTTP, a inspeção justificou as seguintes correções unitárias:

| Requisito ou comportamento | Teste preexistente | Teste do Tiago | Estado | Ação necessária / efetuada |
|---|---|---|---|---|
| Notificação de rejeição contém destinatário e motivo normalizado | normalRejectDoesNotBlockCpf verificava estado, não entrega | — | Coberto | rejectionNotificationUsesPersistedRecipientAndTrimmedReason e rejectionWithAbsentReasonDoesNotSendMail |
| Consulta existente e erro de application ausente em approve/resend | Apenas findById inexistente | Delegação de paginação e operações processadas | Coberto | findByIdReturnsExistingApplicationAndMissingApprovalOrResendDoesNotProvision |
| Token expirado/usado não alcança Authentication | validationRejectsExpiredTokenWithoutWritingState cobria GET/validate | — | Coberto | completionRejectsExpiredOrUsedTokenBeforeCallingAuthentication verifica ausência da chamada central |
| Application/recipient inexistentes | — | — | Coberto | missingApplicationOrIdentityCannotValidateCompleteOrIssueInvitation |
| Mensagem opcional não perde conteúdo útil; telefone normalizado | Helpers enviavam texto fixo sem conferir o resultado | — | Coberto | creationNormalizesOptionalMessageWithoutLosingNonBlankText |

A rodada seguinte eliminou 105/106 mutantes, sem NO_COVERAGE. O sobrevivente em
AdminMembershipApplicationService.rejectApplication, linha 148, troca por null o retorno de
um método privado cujo chamador ignora o resultado. As mutações observáveis de envio, estado e guards
foram eliminadas; não modificamos produção nem adicionamos testes reflexivos para esse retorno equivalente.

## Persistência administrativa real

JaCoCo mostrou consultas de MembershipApplicationRepositoryAdapter que só eram chamadas por
mocks em controllers/services. Foi acrescentado
MembershipApplicantBlockJpaRepositoryTest.applicationQueriesPreserveStatusSortAndPaginationAfterDatabaseRoundTrip.
O cenário persiste duas solicitações, aprova uma, executa flush/clear e verifica contagem,
ordenação, páginas distintas, filtro APPROVED/PENDING, vínculo do usuário/revisor,
mensagem e consultas de existência de CPF/e-mail. É um slice JPA com rollback automático,
sem mocks de repositório e sem dependência dos dados dos cenários HTTP.

Na linha de listagem/paginação da matriz, essa evidência complementa os contratos HTTP e
substitui a limitação inicial de não haver execução das consultas SQL correspondentes.
