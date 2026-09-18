# Identity

Leia primeiro a [governança global](../../../../../../../../../docs/architecture/README.md), a
[organização dos módulos](../../../../../../../../../docs/architecture/module-organization.md)
e as [regras de desenvolvimento](../../../../../../../../../docs/architecture/feature-development-rules.md).
Este documento descreve somente o bounded context `iam.identity`. O OpenAPI
gerado pelos controllers e DTOs do módulo é a fonte de verdade do contrato HTTP.

## Responsabilidade e limites

Identity é proprietário do agregado `User`: identificador estável, nome,
`birthDate`, e-mail, CPF, RG, telefone, URL de foto de perfil, estado
administrativo e timestamps de criação, desativação e atualização. Também é
proprietário do cadastro, das consultas cadastrais e do lifecycle administrativo
do usuário.

Identity não é proprietário de senha, hash, login, lock, sessões, access token
ou refresh token: esses conceitos pertencem a Authentication. Tampouco possui
roles, permissions ou authorities, que pertencem a Authorization. Para proteger o
sistema contra lockout administrativo, o lifecycle administrativo de Identity consulta
a porta consumer-owned `UserAuthorizationProtectionPort` (adaptada via `RoleQuery`
de Authorization) e impede a desativação de usuário portador da role estrutural
ROOT, retornando 409 CONFLICT com código `USER_ROOT_CANNOT_BE_DISABLED` sem produzir
efeitos em Identity ou Authentication. O `enable` administrativo continua permitido
para recuperação de estado.

`User` é criado com `UserStatus.ACTIVE`. O único outro estado é `DISABLED`:
`disable` define `disabledAt` e `updatedAt`; `enable` remove `disabledAt` e
atualiza `updatedAt`. Desabilitar um usuário já desabilitado e habilitar um
usuário ativo são conflitos. O agregado não conhece lock de autenticação nem
estado de credencial.

## Dados cadastrais e normalização

Nome e textos opcionais são aparados; e-mail é aparado e convertido para
minúsculas. CPF, RG e telefone são persistidos somente com dígitos. CPF tem
exatamente 11 dígitos e CPF, e-mail e RG são únicos na persistência. Campos
opcionais em branco se tornam ausentes.

O nome do campo HTTP de nascimento é `birthDate`. Não há alias `birthData` no
DTO ou na serialização atual. Formatos aceitos, limites e exemplos do request,
bem como o formato canônico retornado, pertencem aos schemas OpenAPI.

## Lifecycle composto com Authentication

Identity define as necessidades de Authentication em
`api.module.spi.UserAuthenticationProvisioningPort` e
`UserAuthenticationAdministrationPort`. Os adapters de Authentication em
`authentication.infra.integration.identity` implementam essas portas; Identity
não acessa service, repository, entity ou hash interno daquele módulo.

- `UserRegistration` cria o User e solicita provisionamento de credencial.
  Registro HTTP provisiona e autentica; os fluxos internos também podem criar
  credencial permanente ou pendente de primeiro acesso.
- `UserAdministration.disable` bloqueia o User e solicita que Authentication
  desabilite o acesso e revogue credenciais ativas. A operação é transacional:
  falha no efeito de Authentication desfaz a transição de Identity.
- `UserAdministration.enable` reativa o User e solicita habilitação de acesso.
  Não desbloqueia a conta, não zera tentativas, não altera senha ou estado da
  credencial e não recria sessões/tokens.

O acesso administrativo de Authentication, lock e `CredentialStatus` continuam
estados independentes. Um User administrativamente ativo pode estar bloqueado
ou pendente de primeiro acesso e ainda ser ativo para os consumidores de
`UserQuery`.

## Contratos e integrações públicas

`api.module` é a fronteira Java pública. Suas assinaturas continuam no código;
os contratos são:

- `UserQuery`: consultas somente leitura de User, existência por ID/CPF/e-mail
  e atividade administrativa, inclusive consultas em lote. É consumido por
  Authentication e por adapters de Authorization, Billing, Memberships,
  Dependents, Vehicles e Health.
- `UserRegistration` e `UserRegistrationData`: criação composta de User com
  credencial permanente, pendente de primeiro acesso ou já autenticada. O
  bootstrap de desenvolvimento o consome diretamente; o fluxo de Memberships
  chega a ele pelo adapter de Authentication que implementa o contrato do
  consumidor.
- `UserAdministration`: transições administrativas compostas de disable/enable
  e seus efeitos em Authentication; o controller administrativo o usa por meio
  de `AdminUserService`.
- `UserDetails`, `UserAuthenticationTokens` e `UserStatus`: valores de
  transporte dos contratos acima, sem expor entity, repository ou modelo JPA.
- `UserAuthenticationProvisioningPort` e
  `UserAuthenticationAdministrationPort`: SPIs proprietários de Identity que
  Authentication implementa para provisionamento e administração de acesso.

`DevelopmentAdminUserBootstrapService` usa `UserQuery` e `UserRegistration`
para assegurar o usuário administrativo configurado. O bootstrap de role ROOT
é responsabilidade de Authorization; essa composição não muda o ownership de
User, conta ou role.

## Consulta administrativa e contrato HTTP

O OpenAPI descreve registro público, `/identity/me` autenticado e a superfície
administrativa de usuários. As operações administrativas usam `@PreAuthorize`
para enforcement e `@RequiredPermission` apenas para publicar a mesma authority
no OpenAPI: `IDENTITY_USER_READ`, `IDENTITY_USER_DISABLE` e
`IDENTITY_USER_ENABLE`.

A listagem administrativa retorna o envelope paginado transversal
`PageResponse<AdminUserResponseDTO>`. Aceita
paginação zero-based (`page`, `size`; padrão 20 e máximo 50), `sort` com os
campos permitidos, filtros cadastrais, busca `q` e `fields`. Quando `fields`
é omitido ou vazio, todos os campos de `AdminUserField` são selecionados;
campos não solicitados podem ser omitidos do item retornado. Filtros, seleção,
ordenação e paginação são executados na consulta de Identity, não por leituras
de Authentication ou outros contextos.

`/identity/me` usa o `UserPrincipal` já autenticado para obter o ID e consulta
Identity somente para os dados cadastrais. A documentação detalhada de paths,
parâmetros, schemas, validações, respostas e erros RFC 9457 está no OpenAPI;
não há catálogo Markdown concorrente.

## Persistência e concorrência

`UserEntity`, repository JPA, mapper, adapter e read model administrativo
ficam em `infra.persistence`; o domínio não depende de JPA. `identity_users`
aplica unicidade a CPF, e-mail e RG. Alterações de lifecycle tomam lock
pessimista em User e participam da mesma transação do efeito em Authentication.

Há uma associação física legada de chave compartilhada entre User e
`AuthenticationAccount`, necessária à PK/FK existente. Ela não permite acesso
cross-module a modelos internos. A política de schema é a global atual: entities
e Hibernate representam mudanças e não há migrations versionadas obrigatórias.

## Testes

Os testes do módulo caracterizam serialização de `birthDate`, registro,
normalização, lifecycle transacional, read model administrativo, paginação,
filtros, sparse fields, sort e persistência. Alterações futuras devem escolher
cobertura proporcional conforme as
[regras globais](../../../../../../../../../docs/architecture/feature-development-rules.md#testes-m%C3%ADnimos).
