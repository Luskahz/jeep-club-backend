# BACK-526 — correção de elegibilidade no PR #114

## Defeito reproduzido e causa

Base anterior: `8d9ee7110fc812678a7d66532d4344a34453304c`.
`MemberIdentityAdapter.lockAndIsActive()` comprova somente a atividade administrativa
de Identity. Na aprovação de MembershipApplication, essa Identity já está ACTIVE,
enquanto Authentication continua PENDING_FIRST_ACCESS. PUT/generate e a prévia de
importação aceitavam indevidamente essa conta e podiam impedir a reversão BACK-523.

Antes da correção de serviços, o novo teste parametrizado
`pendingFirstAccessRejectsAllRegistryCreationAndStillAllowsReversal` executou duas
vezes, com duas falhas: esperado HTTP 409, recebido 200. Log local:
`target/back526-eligibility-reproduction.log`. Uma tentativa anterior não compilou
por usar o accessor incorreto do token de dry-run; foi corrigido antes da reprodução.

## Contrato e implementação

O novo port Memberships `MemberAuthenticationEligibility` é implementado por
Authentication em seu adapter de integração, conforme os ports já existentes
CompletePendingFirstAccessPort/PendingMembershipAuthentication. Nenhuma entidade,
tabela ou repository privado de Authentication é importado pelo Memberships.

`hasCompletedFirstAccess` orienta a prévia sem locks. Comandos usam
`lockAndHasCompletedFirstAccess` em transação MANDATORY, após adquirir Identity e
antes do mutex Registry. O repository Authentication existente faz leitura corrente
bloqueante e atualiza a visão após snapshot MySQL RR. Só `PERMANENT` comprova
efetivação para criar novo Member. Conta ausente, pendente ou com credencial
temporária/CHANGE_REQUIRED é inelegível. CHANGE_REQUIRED não é prova suficiente:
o fluxo de recuperação administrativa também pode defini-lo numa conta pendente.
Provisionamento permanente legítimo fora de MembershipApplication continua aceito,
sem exigir aplicação COMPLETED ou login anterior.

PUT e generate retornam MEMBER_REGISTRY_CONFLICT/409 antes de criar Member/claim/
audit de sucesso. Repetição do vínculo vigente e correções históricas permanecem
permitidas; não são novas efetivações. Dry-run informa FIRST_ACCESS_INCOMPLETE;
apply refaz a prévia e assign revalida a conta sob lock na escrita. Mesmo uma
prévia desatualizada não permite contornar essa última verificação.

## Onboarding e integridade

Access link: CompletePendingFirstAccessAdapter salva PERMANENT antes de
MemberActivationTokenService chamar issueOnCompletion. Senha temporária:
SessionService salva PERMANENT antes de chamar MembershipOnboardingCompletion.
Ambos permanecem na transação original. A consulta da conta faz auto-flush das
alterações já salvas, mantendo os locks até commit/rollback. Não existe exceção
por reason/ator nem bypass administrativo; o próprio issueOnCompletion também
precisa comprovar a credencial definitiva quando cria Member.

Numeração por maior vigente, canonicalização, correções, reservas, auditoria,
controle otimista e guards lifecycle/BACK-523 não foram redesenhados. Nenhuma
migration foi criada ou alterada, inclusive V10; nenhum dado/flag produtivo é tocado.

## Regressões

| Grupo | Prova |
| --- | --- |
| Aprovação pelos dois métodos + tentativa antecipada | HTTP 409 em PUT/generate, import sem READY, snapshot integral sem resíduos e reversão permitida |
| Conta ausente/pendente e tentativa pelo próprio issueOnCompletion | Rejeição sem Member, claim, audit ou alteração do coordenador |
| Prévia READY desatualizada | Apply ainda é recusado por assign com leitura bloqueante |
| Vínculo vigente | Retry idempotente, consulta e correção preservadas mesmo sem elegibilidade nova |
| Ativação × geração, dois métodos, commit/rollback | Comando aguarda commit; retorna o único Member ou conflita após rollback, sem emissão prematura |
| MySQL RR com snapshot anterior | Nova leitura bloqueante aceita ativação confirmada e recusa estado pendente atual |
| Authentication | Conta ausente/pendente/CHANGE_REQUIRED recusada; provisionamento permanente externo aceito |
| Suítes existentes | Primeiro acesso, reversão, rollback, lifecycle, numeração, importador e contratos preservados |

A fixture sintética Registry agora cria Authentication permanente junto da Identity;
antes criava só Identity. A primeira execução focal identificou que o repository de
Authentication exige transação; a fixture foi corrigida para criá-la em
TransactionTemplate, mantendo a cobertura existente. Resultados finais, SHAs e CI
serão registrados na evidência específica desta correção e no PR #114.
