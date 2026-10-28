# BACK-526 — implementação e evidência para revisão

> Registro histórico anterior à homologação de 26/10/2026. As regras antigas
> de claims permanentes, formato opaco e contador crescente foram substituídas.
> Política e resultados atuais: [BACK-526 numérica](back-526-numeric-policy.md).

> Registro histórico da implementação e revisão de 17–18/10/2026. PR #104 já
> integrado e release #112 publicada. Referências abaixo a V4 futura, PR aberto,
> baseline V3 e BACK-512/523 pendentes descrevem aquela data, não o estado atual.
> A auditoria corrente, matriz de requisitos e novos resultados estão em
> [back-526-finalization.md](back-526-finalization.md); operação atual em
> [back-526-runbook.md](back-526-runbook.md). V1–V9 permanecem imutáveis.

## Identificação e escopo

- Base remota verificada: `ccca1f48126ef5bbdd8aacbc38cca8503a101feb` (`origin/develop`).
- Branch: `feat/back-526-membership-registry`. Target: `develop`.
- Jira lido integralmente: BACK-526 e BACK-527 a BACK-536, em 17/10/2026.
- PR #103 inspecionado, sem incorporação de seu diagnóstico da VPS.
- O HTML preexistente foi preservado por cherry-pick e acompanha este PR.
- Nenhum push em develop/master, merge, deploy, SSH ou acesso ao banco produtivo.
- Nenhuma transição de status ou declaração de aceite foi feita no Jira.

As regras e decisões pertencem ao [documento do módulo](../src/main/java/com/jeepclub/backend/memberships/docs/member-registry.md).
Este arquivo registra a entrega e suas evidências, sem substituir o OpenAPI.

## Subtarefas e arquivos principais

| Ticket | Estado técnico | Entrega técnica | Arquivos / testes principais | Pendência |
| --- | --- | --- | --- | --- |
| BACK-527 | Parcial: homologação pendente | Modelo, fronteiras, política conservadora e matriz de conflitos | `memberships/docs/member-registry.md`; `MembershipNumber`; `MemberRegistryPolicyTest` | Homologação do formato, zeros, reservas, início e momento formal de associação pelo clube |
| BACK-528 | Implementada para revisão | Agregado, quatro entities JPA, repositories, mapper, adapter e V4 | `Member`, `MemberEntity`, `MemberRepositoryAdapter`, `V4__membership_member_registry.sql`; `MySqlMigrationIT`, `MemberRegistryMySqlIT` | Revisão do DDL e ensaio operacional sobre cópia autorizada da VPS |
| BACK-529 | Implementada para revisão | Consulta atual, reversa e em lote sem cache | `MemberRegistrationQuery`, `MemberRegistrationQueryService`; cenário de 100 identidades com uma consulta | Nenhuma cópia de matrícula em Identity |
| BACK-530 | Implementada para revisão | Administração, RBAC, versão e audit atômico | `AdminMemberController`, `AdminMemberService`, handlers, permission catalog; `MemberRegistryApiIntegrationTest` | Concessão da permission por role em fase operacional |
| BACK-531 | Implementada para revisão | Projeção dinâmica em cadastro, perfis, páginas e CSV/PDF | DTOs de Identity, `UserMembershipNumberProjection`, `MembershipNumberProjectionAdapter`, `UserExportService` | Adaptação da UI para exibir matrícula e tratar ausência |
| BACK-532 | Implementada para revisão | Contador persistido, mutex e configuração obrigatória | `MemberIssuanceProperties`, `MemberRegistrySequenceEntity`, `AdminMemberService`; testes concorrentes nos dois bancos | Emissão desabilitada até homologação e conciliação |
| BACK-533 | Implementada para revisão | Emissão somente no primeiro acesso concluído, quando habilitada | `MemberActivationTokenService`, `MembershipOnboardingCompletionService`; `MembershipActivationFlowIntegrationTest` | BACK-523 evoluirá a rejeição após aprovação |
| BACK-534 | Implementada para revisão | Preview sem escrita, comprovante temporário, importação por unidade e retry | `MemberRegistryImportService`, `MemberImportProof`, `AdminMemberImportController`; cenários de importação | Fonte oficial, inventário e aplicação real são fases separadas |
| BACK-535 | Implementada para revisão | Domínio, JPA, concorrência, API, schema e regressões | `memberships/registry/*Test`, `*MySqlIT`, testes existentes de Identity/Authentication/Billing | Resultados finais registrados abaixo |
| BACK-536 | Implementada para revisão | Contratos, frontend, rollout em fases e recuperação de DDL | Docs de Memberships/Identity/Authorization, `database/09-membership-registry.md`, este relatório | Backup/restauração de produção e aprovação da release pelo futuro agente DevOps |

BACK-527 não representa aprovação de regras de negócio. A implementação das
demais partes é segura sem essa aprovação porque não habilita emissão nem importa
associados no startup. Revisão de código não é aceite funcional ou autorização de release.

## Banco e persistência

| Tabela | Campos e controles |
| --- | --- |
| `membership_members` | `id` PK técnica, `identity_user_id` único com FK física, matrícula vigente única, created/updated timestamps, `version` otimista |
| `membership_number_claims` | claim SHA-256 como PK, número literal único, Member opcional para reserva, ator, instante e justificativa; nenhuma liberação de número |
| `membership_member_audits` | ID do evento, Member, matrícula anterior/nova, ator, instante, motivo, operação e outcome; campos imutáveis pela aplicação/JPA |
| `membership_registry_sequence` | singleton, próximo valor nullable, assinatura da configuração e version; inicia sem faixa |

A V4 também estende o ENUM de `authorization_permissions.code` com a nova
permission, preservando a ordem dos valores antigos. Não modifica V1–V3, baseline,
Identity IDs, dados de usuários/aplicações, credenciais ou vínculos financeiros.
Os quatro componentes persistidos usam JPA; o mapper/adapter é proprietário de cada
tradução. As referências entre contextos são escalares e usam contratos públicos.

Todas as escritas seguem a ordem Identity → mutex do registry. Leituras com lock
renovam o estado JPA para não reutilizar snapshots antigos de uma transação externa
ou do OpenEntityManagerInView. Constraints e versionamento são proteções adicionais.
Claims usam INSERT JPA, preservando sua natureza permanente. Cadastro, claim,
avanço de contador e audit confirmam juntos ou são revertidos juntos.

O suite de migrations cobre banco vazio, baselines históricas 1/2/3, preservação
integral das linhas sintéticas anteriores de Identity e MembershipApplication,
Hibernate validate, reexecução, checksums, drift e DDL parcial. O cenário MySQL do
registry cobre persistência real, constraints, FK, rollback e concorrência.
Esse ensaio não substitui restauração de backup produtivo em ambiente isolado.

## Snapshot do contrato HTTP para revisão

As permissions são independentes por operação. Todos os paths são
completos. Erros usam `application/problem+json` e não incluem SQL ou dados pessoais.

| Método | Path | Permission | Request | Response / sucesso | Finalidade |
| --- | --- | --- | --- | --- | --- |
| GET | `/admin/members` | MEMBERSHIP_MEMBER_READ | page, size, sort | PageResponse de Member, 200 | Listar registros, padrão 20/máximo 50 |
| GET | `/admin/members/by-number` | MEMBERSHIP_MEMBER_READ | membershipNumber | Member, 200 | Pesquisa literal na matrícula vigente |
| GET | `/admin/members/by-identity/{userId}` | MEMBERSHIP_MEMBER_READ | ID técnico no path | Member, 200 | Consultar vínculo, matrícula e version |
| PUT | `/admin/members/by-identity/{userId}` | MEMBERSHIP_MEMBER_REGISTRATION_MANAGE | membershipNumber, reason | Member, 200 | Atribuição histórica ou repetição idempotente |
| PATCH | `/admin/members/by-identity/{userId}/membership-number` | MEMBERSHIP_MEMBER_REGISTRATION_MANAGE | membershipNumber, expectedVersion, reason | Member, 200 | Retificação auditada |
| GET | `/admin/members/by-identity/{userId}/history` | MEMBERSHIP_MEMBER_AUDIT_READ | page, size | PageResponse de audit, 200 | Histórico confirmado em ordem decrescente de evento |
| POST | `/admin/members/by-identity/{userId}/generate` | MEMBERSHIP_MEMBER_NUMBER_ISSUE | reason | Member, 200 | Emissão explicitamente habilitada ou vínculo existente |
| POST | `/admin/members/reservations` | MEMBERSHIP_MEMBER_NUMBER_RESERVE | membershipNumber, reason | Vazio, 201 | Proteger número sem associá-lo |
| POST | `/admin/members/imports/dry-run` | MEMBERSHIP_MEMBER_IMPORT | rows, reason | Comprovante e outcomes por índice, 200 | Simulação sem escrita |
| POST | `/admin/members/imports/apply` | MEMBERSHIP_MEMBER_IMPORT | rows, reason, dryRunToken | Outcomes por índice, 200 | Aplicação parcial por transações independentes |

As rotas protegidas recusam ausência de autenticação com 401 e ausência da permission da operação com
403. Entradas inválidas retornam 400; recurso individual inexistente retorna 404;
colisão, reserva, vínculo, versão ou emissão indisponível retorna 409. A importação
retorna erros de linhas em relatório sanitizado, sem transformar a aplicação
parcial em sucesso de todas as linhas. O token da simulação não substitui RBAC.

| Superfície existente | Mudança aditiva | Permission preservada / autenticação |
| --- | --- | --- |
| GET/PATCH `/identity/me`; PATCH `/identity/me/email` e `/identity/me/photo` | `membershipNumber` vigente ou null | Principal autenticado |
| GET/PUT `/identity/me/profile` | `membershipNumber` vigente ou null | Principal autenticado |
| GET `/identity/admin/users` | Matrículas por lote quando MEMBERSHIP_NUMBER selecionado, incluído por padrão; ID auxiliar não é exposto sem seleção | IDENTITY_USER_READ |
| GET `/identity/admin/users/{userId}` | Campo vigente, omitido quando null | IDENTITY_USER_READ |
| PATCH `/identity/admin/users/{userId}` | Campo vigente no retorno | IDENTITY_USER_UPDATE |
| PATCH `/identity/admin/users/{userId}/disable` e `/enable` | Campo vigente no retorno | IDENTITY_USER_DISABLE / IDENTITY_USER_ENABLE |
| GET/PUT `/identity/admin/users/{userId}/profile` | Matrícula no perfil complementar | IDENTITY_USER_READ / IDENTITY_USER_UPDATE |
| GET `/identity/admin/users/export` | Matrícula primeira no layout MEMBER; TECHNICAL preserva ordem/cabeçalhos anteriores; consulta por chunk | IDENTITY_USER_EXPORT |
| POST `/membership-applications/activate` | Emissão condicional na mesma transação, sem mudar payload/204 | Token de ativação do fluxo existente |
| POST `/authentication/login/password-change` | Callback Membership na conclusão, emissão condicional | Challenge do fluxo existente |

IDs técnicos continuam presentes e aceitos. Campos técnicos de MembershipApplication
identificam a solicitação, não uma matrícula. Nenhum novo gate de Member foi
introduzido em autenticação, autorização, membritude paga ou Billing.

## Validação da implementação inicial (histórico)

Código funcional validado: `6fe33a41e17ce2d11ec1167bc9b2b2114d52aa7b`.
O verify final usou `d052aad7fd5dea5f75463ba6434d4871596decf4`; a diferença entre esses
commits é somente documentação OpenAPI do primeiro acesso. Commits posteriores
registram docs, HTML, evidências e o ajuste determinístico de uma fixture temporal,
sem alterar o comportamento de produção validado.

| Comando / verificação | Resultado local |
| --- | --- |
| `./mvnw --batch-mode --no-transfer-progress verify` | 1.913 testes, 0 falhas, 0 erros, 1 skip |
| `./mvnw --batch-mode --no-transfer-progress -Pmysql-migrations -DskipUnitTests=true verify` | 45 testes MySQL, 0 falhas, 0 erros, 0 skips |
| `bash deploy/test-future-migration.sh` | 45 testes, 0 falhas, 0 erros, 0 skips, com V5 somente no arquivo Git isolado |
| Focais: `-Dtest=MemberRegistryIntegrationTest,MemberRegistryApiIntegrationTest,MemberRegistryPolicyTest,MembershipActivationFlowIntegrationTest test` | 43 testes passaram na rodada inicial; os complementos de policy (8) e onboarding (8) também passaram. O verify final inclui os 46 casos atuais dessas quatro suítes |
| `git diff --check` e diff da branch | Sem erros de whitespace; V1–V3, workflows e scripts de deploy intactos |
| Compose principal/local (`config --quiet`) e `bash -n` dos scripts | Passaram, usando somente valores sintéticos no ambiente local |
| HTML e documentação | 37 slides, JSON válido, IDs únicos, referências existentes, UTF-8 e links verificados |

Os comandos Maven foram executados pelo wrapper Windows `mvnw.cmd` com Java 17
apenas no processo. Git Bash executou o script POSIX. A primeira rodada MySQL
encontrou um caminho de Bash fixo inexistente; o teste agora localiza a instalação
por usuário ou aceita `-Dtest.bash`, sem omitir o coletor. As rodadas finais passaram.

O primeiro CI [39085850356](https://github.com/Luskahz/jeep-club-backend/actions/runs/39085850356)
aprovou Maven verify e encontrou uma falha na fixture MySQL de usuário desativado:
CURRENT_TIMESTAMP sem fração podia gerar disabled_at anterior a created_at no
runner mais rápido. A fixture agora usa created_at como instante determinístico
para disabled_at/updated_at, mantendo a validação de Identity. O caso foi repetido
isoladamente com sucesso em H2 (1 teste) e MySQL (1 teste). Nenhuma regra de
produção, constraint ou gate foi enfraquecido. O CI completo é reexecutado no HEAD
da correção, e seu estado corrente consta do PR.

O único skip é preexistente: `LocalFileStorageTest.shouldRejectSymbolicLinkInsteadOfFollowingItOutsideRoot`,
porque o ambiente Windows não permite criar o symlink de teste. Nenhum teste novo
foi desabilitado. Os logs de erro dos cenários de falha injetada, schema drift e
migration inválida são esperados e não representam falhas remanescentes das suítes.

A evidência resumida e sanitizada está em [back-526-test-evidence.json](back-526-test-evidence.json).
Logs brutos, env, tokens e dados pessoais não são versionados. O estado corrente
dos jobs obrigatórios Maven verify e MySQL migrations deve ser conferido no PR.
Não existe job de deploy autorizado por este PR para develop.

## Pendências e limites para rollout

1. Homologar formato, equivalência de zeros/case, reservas, política de lacunas,
   início da sequência e momento formal de associação. Não há último número real
   presumido no código.
2. Revisar a fonte histórica, classificar identidades, simular e autorizar a
   conciliação em fase separada. Uma matrícula anterior permanece protegida e
   não pode ser transferida ou reutilizada nesta versão.
3. BACK-512 deve resolver lifecycle/arquivamento antes de remoção de Identity
   vinculada. BACK-523 trata rejeição após aprovação pendente; a emissão não ocorre
   na aprovação e não introduz matrícula prematura nesse fluxo.
4. O comprovante de dry-run dura 15 minutos e é local ao processo; restart ou
   outra instância exige nova simulação. A autorização continua sendo a permission.
5. O agente DevOps deverá comprovar backup/restauração, equivalência da cópia
   produtiva, tempo/locks do DDL, migration history, validate/health e regressões
   antes de autorizar a futura master. Falha de DDL pode deixar schema parcial;
   não há promessa de rollback ACID, clean/repair ou downgrade automático.

O código é proposto para revisão e integração em develop. Merge, deploy, importação
real e habilitação de emissão dependem de autorizações posteriores distintas.


## Revisão corretiva pré-merge — 18/10/2026

HEAD inicial e remoto confirmado: `dd0c7cc168451ef5f2c15923ec9209adaf8d0055`.
PR #104 aberto para develop, sem conflitos. Fetch confirmou develop e master
sem V4; nenhum commit de outro autor foi substituído. Toda a especificação Jira
BACK-526/527–536 e o diff foram revisitados. Sem alteração de workflow/deploy,
V1–V3, baseline, dados reais ou operação na VPS.

| Finding | Causa e correção | Arquivos principais | Evidência dirigida | Risco residual |
| --- | --- | --- | --- | --- |
| Fields sem ID | Matrícula dependia da seleção ID. MEMBERSHIP_NUMBER agora seleciona chave auxiliar interna; DTO omite id não solicitado | AdminUserField, AdminUserJpaQueryRepository, AdminUserController, AdminUserResponseDTO | JSON padrão, matrícula isolada, nome+matrícula, ID sem matrícula, ausência e retificação; 100 linhas em três statements | HTTP mantém máximo 50; cenário interno de 100 verifica lote |
| Pesquisa cadastral | Só havia pesquisa no registry. Adapter de Identity usa SPI composta da query pública Membership, adiciona predicado ID antes de count/paginação e usa FALSE para número inexistente, sem ID sentinela | AdminUserFilter/DTO, AdminUserQueryRepositoryAdapter, MembershipNumberProjectionAdapter | Exata, inexistente, histórica, q, nome/status/ID, segunda página e RBAC | Sort matrícula em Identity explicitamente 400; registry suporta sort global |
| Exportações | ID ocupava primeira coluna. MEMBER prioriza Matrícula e deixa ID técnico no final; TECHNICAL mantém contrato anterior | UserExportService, UserExportLayout, AdminUserExportController | CSV/PDF, ausência, retificação e 100 linhas em um lookup | Consumidor posicional antigo precisa solicitar TECHNICAL |
| Privilégios amplos | MANAGE global autorizava tudo. Seis permissions independentes, metadata igual ao enforcement | PermissionCode/Definition, AdminMemberController, AdminMemberImportController, V4 | Seis authorities isoladas, ROOT estrutural, CUSTOM sem grants e OpenAPI de todas as rotas | Operação futura deve conceder apenas grants necessários |
| Importação | Declaração podia sugerir verificação externa e apply não tinha gate operacional. Novo contrato administrativelyConfirmed/sourceReference, HMAC exato e flag apply-enabled=false | MemberRegistryRequests, MemberImportRow, MemberImportProof, MemberRegistryImportService, MemberImportProperties, Compose | Default bloqueado mesmo com IMPORT; dry-run sem escrita; enable explícito, expiração, ator/fonte/motivo/linhas, unconfirmed, retry/concorrência/partial/audit | Conferência é humana; proof local ao processo expira em 15min; outra instância requer nova simulação |
| Lifecycle | FK nova exige retenção. Mantida restritiva, sem cascade/delete/transferência; não há endpoint físico atual de Identity | V4, testes MemberRegistryApiIntegrationTest e MemberRegistryMySqlIT; docs | Disable/enable e nome/e-mail/CPF preservam Member; FK recusa delete e mantém claims/audit | BACK-512 deverá definir arquivamento e erro 409 controlado antes de expor exclusão |

### Contratos finais de frontend

A matrícula é o identificador principal cadastral e visual do associado. IDs são
preservados para necessidades técnicas, não são matrícula nem fallback visual.

- `GET /identity/admin/users?fields=NAME,MEMBERSHIP_NUMBER&membershipNumber=027`
  retorna nome e matrícula, sem `id`. Sem fields inclui matrícula e campos anteriores.
- `fields=ID,NAME` omite matrícula de forma deliberada.
- `membershipNumber` pesquisa somente vigente, exata, combinável com filtros.
  `q` inclui OR com matrícula vigente exata. Histórico não participa.
- Paginação/filtros são aplicados globalmente no banco. Lookup Membership em lote.
- Export padrão `layout=MEMBER`: Matrícula primeiro, ID técnico por último.
  `layout=TECHNICAL`: `ID;Matrícula;Nome;...` na ordem anterior, ID técnico explícito
  pela finalidade desse layout. CSV e PDF compartilham o mesmo documento.
- Atribuição manual continua PUT com número/motivo, exige MANAGE; retificação
  exige versão esperada, registra auditoria e preserva IDs/relações/JWT/Billing.
- Import request: rows com identityUserId, membershipNumber,
  administrativelyConfirmed e sourceReference opaca sem PII; reason e dryRunToken.
  Backend não comprova correspondência com fonte oficial externa.
- Apply exige IMPORT **e** `memberships.registry.imports.apply-enabled=true`
  (`MEMBERSHIPS_REGISTRY_IMPORTS_APPLY_ENABLED` no Compose), além da conferência e
  comprovante válidos. Default false: 409 MEMBER_REGISTRY_IMPORT_DISABLED sem escrita.
  Dry-run não depende dessa habilitação. Emissão automática continua false.

### Gates da revisão

Código final revisado: `e49dd22fa4e015de2d971572999f8bbf21ba0872`. `mvnw.cmd --batch-mode --no-transfer-progress verify`
passou com 1.933 testes, 0 falhas/erros e 1 skip preexistente de symlink Windows.
As cinco suítes focais da revisão somam 81 casos incluídos nesse verify.
Os dois gates MySQL passaram no mesmo código, com MySQL 8.0.46:

| Comando | Testes | Falhas/erros/skips |
| --- | --- | --- |
| `mvnw.cmd --batch-mode --no-transfer-progress verify` | 1.933 | 0 / 0 / 1 (symlink Windows preexistente) |
| `mvnw.cmd --batch-mode --no-transfer-progress -Pmysql-migrations -DskipUnitTests=true verify` | 50 | 0 / 0 / 0 |
| `bash deploy/test-future-migration.sh` | 50 | 0 / 0 / 0 |

O último comando usa V5 no arquivo Git isolado; a release mantém somente V1–V4.
As 50 integrações são MySqlMigrationIT (14), MemberRegistryMySqlIT (28) e
MembershipRegistryOnboardingMySqlIT (8). Baseline V3, igualdade de schema,
constraints, catálogo, rollback, concorrência e preservação sintética passaram.
Compose principal/local foram validados com env exclusivamente sintético,
ambas as flags false e ddl-auto=validate; UTF-8, links, HTML e diff check passaram.
CI do HEAD publicado deve ser lido no PR #104, sem confundir este snapshot local
com execução futura. Commits posteriores ao SHA de código acima contêm somente
documentação, HTML e evidências.

As rodadas de construção dos novos testes corrigiram assertions/fixtures de
precisão temporal, metadados versus cabeçalho PDF, célula CSV vazia entre aspas e
principal exigido pela auditoria de exportação. Nenhuma proteção foi removida.
O caso de declaração ausente foi confirmado como 400 pelo contrato JSON; false
é UNVERIFIED e true permite aplicação com permission/flag/proof válidos. Todos
os casos foram repetidos no verify final. Não há falha remanescente conhecida.
Os números da seção histórica acima descrevem a implementação inicial, não estes commits.

### Pendências preservadas

Não há presunção de último número ou formato real. Clube homologa zeros/case,
reservas, faixa inicial, lacunas e momento formal. BACK-512 define lifecycle
futuro; BACK-523 define rejeição de aprovação pendente. O registry não emite na
aprovação e não introduz novo gate de autenticação/Billing.

DevOps futuro confere SHA, backup/restauração, base real equivalente, baseline V3,
V4, catálogo, tempo/locks de DDL, validate/health/login/Billing. Mantém emissão e
apply false até autorizações distintas. Não foi feito merge, deploy ou importação real.


### Procedência da V4 e limites da verificação

A V4 foi ajustada nesta revisão seguindo a autorização explícita do pedido e o
estado remoto confirmado por fetch: master/develop contêm somente V1–V3, PR #104
segue aberto e o deploy anterior do PR foi skipped. Preservaram-se as 128 posições
do ENUM proposto anteriormente; cinco códigos novos foram acrescentados ao final.
Não se consultou nenhum banco compartilhado ou produtivo. Eventual aplicação
manual fora desse fluxo deve ser informada antes do merge; nesse cenário deve-se
preservar o SQL aplicado e preparar incremento posterior, sem repair silencioso.
