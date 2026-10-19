# BACK-512 — Lifecycle dos seis cadastros (em implementação)

Base temporária: BACK-510, PR #106, `86573ea6d0803c7407e739c1b271fc636047eb38`. Cadeia: develop → BACK-460 → BACK-510 → BACK-512 → BACK-523 → BACK-524 → BACK-525. Este documento ainda não constitui evidência de conclusão; os gates e a matriz de aceite serão registrados após execução.

## Decisões e matriz de transições

Cada contexto conserva seu enum e suas invariantes. ACTIVE é o estado inicial. INACTIVE suspende disponibilidade; ARCHIVED retira de circulação sem apagar dados. DELETE é operação transacional com histórico segregado, nunca um estado operacional.

| Agregado | Inativar/ativar | Arquivar | Desarquivar | Exclusão |
| --- | --- | --- | --- | --- |
| Identity.User | ACTIVE ↔ INACTIVE; paths disable/enable preservados | ACTIVE/INACTIVE → ARCHIVED; revogação real de Authentication | ARCHIVED → INACTIVE; acesso continua desabilitado | Restrita a vínculos comprovadamente seguros; ROOT/Member/financeiro/retenção impedem |
| Dependent | ACTIVE ↔ INACTIVE | ACTIVE/INACTIVE → ARCHIVED | ARCHIVED → INACTIVE | Snapshot existente; Health/Event podem impedir |
| Role CUSTOM | ACTIVE ↔ INACTIVE | ACTIVE/INACTIVE → ARCHIVED | ARCHIVED → INACTIVE | Remoção operacional com histórico; vínculos impeditivos retornam conflito; ROOT protegido |
| Tool | ACTIVE ↔ INACTIVE | ACTIVE/INACTIVE → ARCHIVED | ARCHIVED → INACTIVE | Snapshot existente, sem remover mídia compartilhada |
| Vehicle | ACTIVE ↔ INACTIVE | ACTIVE/INACTIVE → ARCHIVED | ARCHIVED → INACTIVE, exceto exclusão legada | Snapshot existente; referências de Event podem impedir |
| ChargeDefinition | ACTIVE ↔ INACTIVE | ACTIVE/INACTIVE → ARCHIVED | Terminal, conforme regra financeira existente | Apenas configuração sem uso; nunca eliminar fatos financeiros para permitir DELETE |

Edição de arquivados é bloqueada. Leitura autorizada de cadastro arquivado continua possível nas superfícies cadastrais apropriadas; elegibilidade operacional exige ACTIVE. Repetições não devem produzir histórico duplicado nem avançar timestamps sem transição. Os detalhes de conflitos das rotas existentes serão preservados/documentados com os testes.

## Fronteiras e concorrência

Reutilizar `UserIdentityLock`, já existente e usado por Authentication e Registry, antes de locks da conta/convites. Guardas de exclusão devem consultar somente os repositórios do próprio contexto ou contratos públicos. Escritas de referências lógicas precisam compartilhar o lock de elegibilidade; uma consulta de existência sem serialização não prova exclusão segura. A FK restritiva da V4 permanece proteção adicional para User associado a Member.

`SystemLogService.recordInTransaction` já fornece auditoria síncrona atômica com o banco. As transições reutilizam esse contrato, sem criar outro sistema de logs; fatos de exclusão permanecem nos históricos próprios. Não registrar PII, chaves de mídia ou credenciais nos eventos. Retenção ampla continua sob BACK-487/409/481.

## Migração e compatibilidade

A próxima versão disponível é V5. V1–V4 permanecem imutáveis. DISABLED de User/Dependent vira INACTIVE, incluindo históricos aplicáveis. Role DELETED precisa ser capturada com suas associações em histórico antes da remoção operacional. Vehicle SOFT_DELETED não pode voltar a ACTIVE: a conversão conservadora para ARCHIVED deve preservar um marcador legado que impeça ressurreição. URLs/fotos e vínculos financeiros não são apagados.

Hibernate continua em validate e Flyway governa o schema. A evolução deve ampliar a representação persistida antes da transformação de dados e só depois restringir os estados finais. O catálogo de permissions deve preservar a sequência existente do ENUM físico e acrescentar novas autoridades sem concedê-las automaticamente a roles CUSTOM.

Não há alteração de vocabulário de MembershipApplication, Authentication, Credential, Session, RefreshToken, MemberCharge, MemberPayment, Refund, ChargeCycle, Event ou Publication.

## Validação e rollout pendentes

Antes da publicação: testes dos seis agregados, HTTP/permissions/ownership/ROOT, auditoria/rollback, leitura após arquivamento, exclusões admissíveis e impeditivas, dados legados/upgrade/reexecução controlada, concorrência real em MySQL, OpenAPI/filtros/exports e os três gates completos. A interpretação adotada é conservadora para retenção e integridade; impedimentos não autorizam cascata destrutiva.

O futuro rollout exige parada coordenada de escrita, backup/restauração verificada, frontend adaptado aos novos status, migrations antes do código incompatível com valores legados e startup com validate. Downgrade pode exigir restauração/forward fix após uso dos novos estados/históricos; não usar repair/clean. Preservar instruções de #103/#104, emissão/importação Registry desabilitadas e nenhuma operação na VPS nesta missão.

## Exclusão e retenção adotadas

User DELETE só remove Identity ainda sem primeiro acesso concluído: Authentication ausente ou PENDING_FIRST_ACCESS sem passwordChangedAt/lastLoginAt e sem qualquer fato de Session. Uma credencial PERMANENT/CHANGE_REQUIRED, sessão histórica, perfil complementar, Member, vínculos financeiros, registros clínicos, cadastros dependentes ou participação/publicação operacional impedem a remoção. Não há limpeza em cascata dessas relações. Uma aplicação APPROVED/COMPLETED também impede DELETE administrativo direto; BACK-523 poderá orquestrar sua própria reversão antes de invocar este contrato, na mesma transação.

Sob lock de User, os guards de cada bounded context fazem consultas correntes apenas às próprias tabelas. A conta pendente, quando elegível, é removida após revogação de sessões/refresh/challenges, invalidação de convites e cancelamento de recoveries abertos. Vínculos UserRole de onboarding recebem snapshot em `authorization_user_role_removal_history` antes da remoção operacional. Role e permissions não são apagadas por essa limpeza. Falha de guard, FK, snapshot ou auditoria desfaz a operação inteira. Histórico mínimo de User conserva ID, estados, timestamps, ator, motivo e versão; não copia CPF, senha, token ou imagem.

Role DELETE recusa ROOT, usuários atribuídos e referências financeiras. Preserva configuração e RolePermissions em tabelas próprias. Migração de Role DELETED também captura UserRoles legados; essas linhas históricas nunca concedem authorities. Consultas de apresentação de nomes incluem o histórico para relatórios que já referenciam IDs removidos.

ChargeDefinition DELETE recusa qualquer ciclo, cobrança, assignment, contexto de Event, regra de Event ou configuração de cobrança de Membership. O snapshot preserva a configuração monetária completa. Nada remove MemberCharge, MemberPayment ou MemberRefund. ARCHIVED continua terminal, conforme a regra financeira existente; não foi acrescentada restauração financeira implícita.

Dependent DELETE recusa referências clínicas atuais/históricas e alocações/inscrições de Event. Vehicle DELETE recusa alocações, ocupantes, solicitações de convidados e ofertas de carona. Tool preserva o mecanismo existente de histórico independente. Nenhum DELETE apaga arquivo de mídia.

## Contratos HTTP e frontend

As rotas existentes de inclusão, edição e exclusão mantêm paths e payloads não afetados. O frontend deve aceitar os três estados e substituir DISABLED de User/Dependent por INACTIVE. As respostas de detalhes e listas de Vehicle incluem o status real. Erros de transição/retenção usam 409 e problemas RFC 9457; cadastros inexistentes continuam 404 e ownership continua obrigatório.

| Contexto | Comandos PATCH | Permissão administrativa |
| --- | --- | --- |
| Identity | `/identity/admin/users/{id}/archive`, `/unarchive`; disable/enable existentes | IDENTITY_USER_ARCHIVE / IDENTITY_USER_UNARCHIVE; autoridades anteriores para disable/enable |
| Dependents member | `/dependents/{id}/disable`, `/enable`, `/archive`, `/unarchive` | Principal proprietário autenticado |
| Dependents admin | `/users/{userId}/dependents/{id}/disable`, `/enable`, `/archive`, `/unarchive` | DEPENDENTS_DEPENDENT_DEACTIVATE / ACTIVATE / ARCHIVE / UNARCHIVE |
| Authorization | `/authorization/roles/{id}/archive`, `/unarchive`; activate/deactivate existentes | AUTHORIZATION_ROLE_ARCHIVE / UNARCHIVE; autoridades anteriores preservadas |
| Tools member/admin | `/tools/{id}/archive`, `/unarchive`; `/admin/tools/{id}/archive`, `/unarchive` | Proprietário; TOOLS_TOOL_ARCHIVE / UNARCHIVE no admin |
| Vehicles member/admin | `/vehicles/{action}/member/{id}`, `/vehicles/{action}/admin/{id}`; action = activate, deactivate, archive, unarchive | Proprietário; VEHICLES_VEHICLE_ACTIVATE / DEACTIVATE / ARCHIVE / UNARCHIVE no admin |

DELETE `/identity/admin/users/{id}` exige IDENTITY_USER_DELETE. DELETE `/users/{userId}/dependents/{id}` exige DEPENDENTS_DEPENDENT_DELETE. DELETE `/billing/charge-definitions/{id}` exige BILLING_CHARGE_DEFINITION_DELETE. Role DELETE preserva AUTHORIZATION_ROLE_DELETE. Resposta 204; vínculo impeditivo 409. Não enviar um status de exclusão em DTO.

GET `/{id}/deletion-history`, sob os prefixos `/identity/admin/users`, `/authorization/roles` e `/billing/charge-definitions`, exige respectivamente IDENTITY_USER_HISTORY_READ, AUTHORIZATION_ROLE_HISTORY_READ e BILLING_CHARGE_DEFINITION_HISTORY_READ. São leituras administrativas auditadas com Cache-Control: no-store e 404 quando não há snapshot. O link de ativação/token não faz parte dessas respostas.

Listagens de Dependents e Vehicles aceitam `?status=ACTIVE|INACTIVE|ARCHIVED`; o default continua ACTIVE e o filtro de Vehicle ocorre antes da paginação. Inventário de Tools conserva seus filtros existentes e aceita ARCHIVED. Exemplo: `GET /vehicles/list/member?status=ARCHIVED&page=0&size=20`. Detalhe cadastral de arquivado permanece autorizado; editar/ativar diretamente requer antes unarchive. Exemplo: PATCH archive → ARCHIVED; PATCH unarchive → INACTIVE; PATCH activate/enable → ACTIVE. Desarquivar nunca restaura automaticamente sessão, refresh token ou permissão operacional.

## Locks e auditoria

UserIdentityLock é o ponto comum de Identity, Authentication, Authorization, Registry e dos escritores administrativos de dependentes, veículos, ferramentas e perfis clínicos. A geração administrativa de ciclos e a atribuição USER de Billing tomam o lock de elegibilidade de Identity antes de gravar seus targets por User ID. Recursos Dependent/Vehicle têm contratos públicos de leitura com lock, consumidos por Health e Events ao criar referências. Events ordena locks de Vehicles e depois Dependents por ID. Definitions e Roles são bloqueadas por seus escritores e comandos de lifecycle. Os adapters descartam snapshots ORM anteriores ao lock e releem sob o mesmo lock, necessário no REPEATABLE READ do MySQL.

As novas transições e os comandos HTTP administrativos de disponibilidade registram ator real em SystemLog na transação de negócio. Repetições idempotentes não avançam o timestamp de arquivamento. Disable/enable de User conservam seus conflitos históricos para repetição sem transição. Os overloads antigos de serviços internos continuam disponíveis por compatibilidade; não inventam ator administrativo. SystemLog segue a retenção técnica existente, enquanto snapshots de exclusão possuem tabelas próprias. Uma política geral de retenção de PII não foi inventada nesta entrega.

## Migration V5 e checklist pré-deploy

`V5__principal_entity_lifecycle.sql` acrescenta archived_at a User, Role, Dependent, Tool, Vehicle e históricos existentes aplicáveis, legacy_deleted aos registros atuais/históricos de Vehicle e seis tabelas de histórico: User, Role, RolePermission, UserRole da exclusão de Role, remoção de UserRole por exclusão de Identity e ChargeDefinition. V1–V4 e os fixtures congelados permanecem intactos.

O preflight recusa ROOT não ativo, timestamps contraditórios de Role e User inativo sem disabled_at. Em seguida amplia os ENUMs, captura e confere históricos de Role DELETED, remove somente suas associações operacionais já preservadas, transforma DISABLED e SOFT_DELETED e restringe os ENUMs finais. O catálogo físico de permissions acrescenta 20 códigos ao fim da sequência V4, sem reordenar valores nem conceder permissões a roles CUSTOM. Reexecução controlada não duplica snapshots; Flyway normal continua executando cada versão uma vez.

Antes da release futura: interromper escritores; verificar backup e restauração; examinar preflight e vínculos legados; estimar locks/tempo de ALTER TABLE no volume real; aplicar migrations via Flyway; subir aplicação com Hibernate validate; conferir enums, históricos, V4/FKs, permissions ROOT e concessões CUSTOM deliberadas; exercitar login/revogação, consulta de arquivados, paginação/exports e fluxos de Event/Billing/Health. Nenhuma variável obrigatória, volume, provider de storage ou flag de produção nova. Manter importação e emissão do Registry desabilitadas até homologação/decisão operacional das PRs #103/#104.

MySQL DDL não é atômico. Se a V5 parar, preservar o diagnóstico, reconciliar/restaurar o banco e corrigir a causa; não mascarar com repair/clean. O código antigo não interpreta os novos estados; rollback de código após transformação exige análise e possível restauração ou correção para frente. Consultas de retenção por vínculos de auditoria podem custar varreduras; DELETE de Identity é uma operação administrativa restrita, não uma rotina em lote.

## Pontos prioritários de revisão

Confirmar a política conservadora de retenção: Identity com credencial definitiva ou perfil complementar não é fisicamente removida; Role ainda atribuída retorna conflito; ChargeDefinition arquivada permanece terminal. Conferir os guards dos módulos quando novos tipos de referência forem adicionados. BACK-523 deve usar o lock de Identity e a operação composta de exclusão, sem apagar Member/claims/financeiro. BACK-524/525 devem preservar archived_at e os snapshots ao acrescentar mídia. A homologação do cadastro oficial, a faixa inicial, a importação histórica e a ativação operacional do Registry continuam pendentes e não foram executadas.

Eventos com publicação operacional ou referência órfã sem snapshot impedem DELETE de Dependent/Vehicle. Depois de Event excluído com snapshot, os cadastros podem ser removidos com seus próprios históricos: a apresentação pós-evento continua usando esses históricos e não perde nomes, placa ou capacidade. Esta distinção preserva o contrato de exportação já existente.

## Rastreabilidade das subtarefas

Estado de publicação ainda pendente dos gates finais e do CI. A coluna abaixo registra o código e as verificações correspondentes, sem substituir a evidência de execução final.

| Jira | Implementação e evidência correspondente |
| --- | --- |
| [BACK-513](https://jeep-club-developers.atlassian.net/browse/BACK-513) | Matriz local dos seis enums e transições; domínio sem GlobalStatus; PrincipalLifecycleDomainTest |
| [BACK-514](https://jeep-club-developers.atlassian.net/browse/BACK-514) | Identity ACTIVE/INACTIVE/ARCHIVED; revogação composta, ROOT, DELETE pendente, histórico e guards; testes transacionais/HTTP/MySQL |
| [BACK-515](https://jeep-club-developers.atlassian.net/browse/BACK-515) | Dependent archive/unarchive/enable/disable, ownership, filtros e histórico; retenção Health/Event e locks dos escritores |
| [BACK-516](https://jeep-club-developers.atlassian.net/browse/BACK-516) | Role archive/unarchive, authorities somente ACTIVE, ROOT, exclusão com histórico e captura dos legados DELETED |
| [BACK-517](https://jeep-club-developers.atlassian.net/browse/BACK-517) | Tool archive/unarchive, comandos member/admin auditados e preservação do histórico existente |
| [BACK-518](https://jeep-club-developers.atlassian.net/browse/BACK-518) | Vehicle ACTIVE/INACTIVE/ARCHIVED, filtro antes da paginação, legado não reativável, histórico e referências de Event |
| [BACK-519](https://jeep-club-developers.atlassian.net/browse/BACK-519) | Exclusão restrita de ChargeDefinition com snapshot completo, guards de configuração/ciclos/assignments e locks dos escritores; estados financeiros preservados |
| [BACK-520](https://jeep-club-developers.atlassian.net/browse/BACK-520) | Novos comandos e consultas de histórico, 20 permissions sem grants CUSTOM, RFC 9457, OpenAPI, ownership e orientação frontend |
| [BACK-521](https://jeep-club-developers.atlassian.net/browse/BACK-521) | V5 incremental, preflight e dados legados, reexecução controlada, Hibernate validate, compatibilidade V4/Registry, testes e checklist de rollout |
