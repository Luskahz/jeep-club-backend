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
TransactionTemplate, mantendo a cobertura existente.

## Corrida adicional de importação corrigida

O CI do HEAD documental ddaf6d2b (run 40410277078) expôs uma corrida em
`concurrentImportsOfSameBindingAreIdempotentAndAuditedOnce`: Member ausente na
primeira leitura, mas claim já confirmada na seguinte, gerando falsamente
NUMBER_UNAVAILABLE. A prévia agora relê o titular quando a claim está ocupada.
Se o vínculo vigente já é exatamente o solicitado, retorna ALREADY_LINKED;
reservas/outros titulares continuam bloqueados. Assign mantém a revalidação
bloqueante de Authentication e unicidade antes de qualquer escrita.

O cenário determinístico `importRecognizesBindingCommittedBetweenMemberAndClaimPreviewReads`
intercala o commit concorrente e exige exatamente um Member, uma claim e um audit.
O assert concorrente original foi preservado. Os gates foram repetidos após o ajuste.

## Validação do código final

Código/testes: `e80d543fd296a854d09c837fc8e24634ce00aeea`.
[CI 40411327180](https://github.com/Luskahz/jeep-club-backend/actions/runs/40411327180)
concluído com sucesso nesse SHA; deploy skipped. O commit inicial 07601103 contém
os guards de elegibilidade e e80d543f também corrige a corrida de prévia.
Nenhuma execução do antigo HEAD 8d9ee711 é usada para aprovar esta correção.

| Gate validado | Testes | Falhas / erros / skips |
| --- | ---: | --- |
| CI Maven verify | 2181 | 0 / 0 / 0 |
| CI MySQL/Flyway | 294 | 0 / 0 / 0 |
| CI migration futura | 294 | 0 / 0 / 0 |
| Focal ampliado local | 134 | 0 / 0 / 0 |
| Maven verify local | 2181 | 0 / 0 / 1 |

Verify local: 06:12; focal: 01:45. O skip local é o teste preexistente de symlink
no Windows. O focal inclui Identity, Authentication, Registry API/policy/JPA e
onboarding. Os totais se sobrepõem. Registry H2 tem 42 casos; HTTP 29; policy 10;
Registry MySQL 47; onboarding H2/MySQL 39 em cada; migrations MySQL 33.
MySQL 8.0.46 usa somente fixtures sintéticas; V11 existe apenas como probe efêmero
da migration futura, sem modificar as migrations V1–V10 da entrega.

A evidência congela esses resultados do código final. Os gates MySQL locais
complementares e os checks do HEAD documental posterior são registrados no
[PR #114](https://github.com/Luskahz/jeep-club-backend/pull/114), referência final
de entrega. O último commit documental não altera Java, testes, SQL ou POM.
Detalhes, hashes e inventário de arquivos:
[evidência estruturada](back-526-authentication-eligibility-evidence.json).

Histórico de validação preservado: reprodução inicial 2 falhas HTTP; erro inicial
de compilação do accessor de dry-run corrigido antes da reprodução; primeira
fixture focal sem transação produziu 41 erros e foi corrigida; CI 07601103 passou,
mas o seguinte ddaf6d2b executou 2180 testes com 1 falha de idempotência. Os gates
MySQL locais dessa primeira versão foram interrompidos e não aprovam o código
final. Após corrigir a corrida, o verify local e todos os gates CI acima passaram.
