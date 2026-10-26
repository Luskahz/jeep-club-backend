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

## Validação inicial e corrida adicional encontrada no CI

Código/testes: `07601103f98456012e64de933de4754e68684da3`.
[CI 40408479741](https://github.com/Luskahz/jeep-club-backend/actions/runs/40408479741)
concluído com sucesso nesse SHA; deploy skipped. Não se utiliza a execução antiga
do HEAD 8d9ee711 como aprovação da correção.

| Gate do código corrigido | Testes | Falhas / erros / skips |
| --- | ---: | --- |
| CI Maven verify | 2180 | 0 / 0 / 0 |
| CI MySQL/Flyway | 293 | 0 / 0 / 0 |
| CI migration futura | 293 | 0 / 0 / 0 |
| Focal ampliado local | 133 | 0 / 0 / 0 |
| Maven verify local | 2180 | 0 / 0 / 1 |

O skip local é o teste preexistente de symlink no Windows. Focal inclui Identity,
Authentication, Registry API/policy/persistência e onboarding. O verify completo
revalidou também a versão final da sincronização dos testes concorrentes e OpenAPI.
Os totais dos gates se sobrepõem. MySQL 8.0.46 usa somente fixtures sintéticas;
V11 existe apenas como probe efêmero da migration futura, sem alterar V10.

Esta evidência congela os resultados acima; os gates MySQL locais complementares
e os checks do HEAD documental posterior são registrados no
[PR #114](https://github.com/Luskahz/jeep-club-backend/pull/114), que é a referência
final de entrega. O commit ddaf6d2b altera somente evidências; a correção adicional
descrita abaixo altera o importador e seus testes. Ver detalhes na
[evidência estruturada](back-526-authentication-eligibility-evidence.json).

O CI seguinte do HEAD documental ddaf6d2b (run 40410277078) expôs uma corrida
em `concurrentImportsOfSameBindingAreIdempotentAndAuditedOnce`: Member ausente na
primeira leitura, mas claim já confirmada na leitura seguinte, gerando falsamente
NUMBER_UNAVAILABLE. O verify executou 2180 testes com uma falha. Esses resultados
iniciais não certificam a correção adicional descrita a seguir.

A prévia passa a reler o titular quando encontra a claim ocupada. Se o vínculo
vigente já é exatamente o solicitado, informa ALREADY_LINKED; reservas e outros
titulares continuam NUMBER_UNAVAILABLE. Assign mantém a última revalidação
bloqueante, inclusive de Authentication. Novo cenário determinístico confirma uma
atribuição concorrente entre as duas leituras e exige um único Member/claim/audit.
O assert concorrente anterior permanece inalterado; os gates são repetidos.
Execuções locais MySQL/futura do código inicial foram interrompidas para essa
correção e não contam como aprovação da versão final.
