# Authorization

Leia primeiro a [governança global](../../../../../../../../../docs/architecture/README.md), a
[organização dos módulos](../../../../../../../../../docs/architecture/module-organization.md)
e as [regras de desenvolvimento](../../../../../../../../../docs/architecture/feature-development-rules.md).
Este documento descreve somente o bounded context `iam.authorization`. O
contrato HTTP detalhado pertence ao OpenAPI gerado pelos controllers e DTOs do
módulo.

## Responsabilidade e limites

Authorization é proprietário do RBAC: roles, permissions, vínculos
`RolePermission` e `UserRole`, e das authorities efetivas derivadas dessas
relações. Ele não possui credenciais, login, sessões, tokens ou senha, que
pertencem a Authentication, nem dados cadastrais e lifecycle administrativo do
usuário, que pertencem a Identity.

Para gerir vínculos com usuários, o módulo consome `identity.api.module.UserQuery`
por `UserIdentityPort` e `AuthorizationIdentityAdapter`; ele consulta apenas a
existência do identificador. A plataforma obtém authorities por
`AuthorizationUserAuthoritiesProvider`, que consulta permissions das roles
ativas no momento em que o filtro JWT monta a autenticação. Authentication não
possui roles nem calcula authorities.

## Modelo RBAC e invariantes

Uma `Permission` é definida pelo catálogo compartilhado `PermissionDefinition`:
tem código técnico único, módulo e descrição. `PermissionSynchronizationRunner`
sincroniza esse catálogo no startup; permissions não são criadas ou editadas
pela API administrativa.

Uma `Role` possui nome único, descrição opcional, `kind`, status e timestamps.
Roles criadas pela API são `CUSTOM` e começam `ACTIVE`. Uma role customizada
pode ser atualizada, ativada, desativada ou excluída logicamente; uma role
`DELETED` não pode voltar a ser usada ou alterada. `RolePermission` e
`UserRole` são vínculos únicos por par de IDs, preservados também por constraints
da persistência. Só roles ativas recebem ou são atribuídas em operações manuais.

Authorities são os códigos de permission distintos de todas as roles ativas do
usuário, ordenados pelo repositório. Roles inativas não contribuem authorities,
mas ainda podem aparecer nas consultas administrativas de roles do usuário.

## `RoleKind.ROOT` e roles estruturais

`RoleKind` possui `ROOT` e `CUSTOM`. A única role estrutural atual é `ROOT`:
ela é criada por `RootRoleBootstrapService` quando ausente, permanece `ACTIVE`,
não pode ter `deletedAt` e recebe todas as permissions sincronizadas. Seu nome
vem de `AdminBootstrapConfig`; sua identidade estrutural é o `RoleKind`, não o
nome visível.

Operações administrativas não podem alterar, ativar, desativar, excluir, nem
atribuir/remover permissions da ROOT. Também não podem atribuir, remover ou
substituir manualmente o vínculo ROOT de um usuário. Ao substituir roles de um
usuário, a implementação remove somente vínculos `CUSTOM` atuais; ROOT já
existente é preservada e não pode ser incluída na lista solicitada.

Essas proteções são sobre a role e seus vínculos. O módulo ainda não expõe uma
consulta pública para responder se um usuário possui ROOT e não impede que
Identity/Authentication desabilite ou bloqueie esse usuário. Essa é a fronteira
da BACK-324, fora deste módulo nesta revisão.

## Contratos e integrações

O único contrato Java em `api.module` é `RoleQuery`, consumido por Billing via
`BillingAuthorizationAdapter`. Ele é somente leitura: informa se uma role existe
e está ativa e lista IDs de usuários vinculados a uma role ativa. Role inexistente,
inativa ou excluída resulta em `false` ou lista vazia; não expõe entity,
repository ou modelo JPA de Authorization.

`UserPermissionQueryService` é colaborador interno do provider de segurança da
plataforma, não uma API Java pública para outros bounded contexts. Bootstrap de
desenvolvimento combina serviços de Identity e Authorization para garantir o
usuário administrativo, a role ROOT e o vínculo entre ambos sem atravessar
repositories de módulo.

## Persistência, erros e testes

Entities, JPA repositories, mappers e adapters ficam em `infra.persistence`.
`UserRole` mantém `userId` escalar, sem entity JPA de Identity. A política de
schema segue a regra global atual, sem migrations versionadas obrigatórias.

Os handlers específicos existem em `api.http.exception`, mas seus
`basePackages` atuais apontam para `com.jeepclub.backend.authorization.api`,
que não cobre os controllers sob `com.jeepclub.backend.iam.authorization.api`.
Assim, exceções de aplicação/domínio de Authorization alcançam hoje o handler
global como `500 INTERNAL_SERVER_ERROR`; o OpenAPI descreve esse comportamento
atual. Violações de `@Positive` e constraints equivalentes em parâmetros HTTP
também resultam em `500` até a BACK-373. Validação de body continua usando o
contrato global `400` RFC 9457.

Os testes cobrem rotas, verbs, authorities documentadas, respostas de sucesso,
consulta da autorização atual e sincronização do catálogo. Alterações neste
contexto devem selecionar testes conforme as [regras globais](../../../../../../../../../docs/architecture/feature-development-rules.md#testes-m%C3%ADnimos).
