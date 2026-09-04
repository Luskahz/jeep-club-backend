# Identity

## Ownership

`identity` é o bounded context proprietário do recurso/agregado `User`. Sua
fonte de verdade é a tabela `identity_users` e suas responsabilidades são:

- ID persistente e estável;
- nome, nascimento, CPF, RG, e-mail, telefone e foto;
- normalização e unicidade cadastral;
- estado administrativo `UserStatus` (`ACTIVE` ou `DISABLED`);
- cadastro e lifecycle administrativo do usuário.

CPF, RG e telefone são armazenados somente com dígitos. E-mail é normalizado
para lowercase. Exclusão com histórico será uma feature futura.

`authentication` é proprietário de `AuthenticationAccount`, credenciais,
login, lock automático, sessões, refresh tokens e recuperação de senha.
`authorization` é proprietário de roles, permissions e authorities. A futura
entidade `Member` pertence a `memberships` e não é antecipada nesta etapa.

## Contratos de módulo

Consumidores usam somente `identity.api.module`:

- `UserQuery` para dados e atividade administrativa;
- `UserRegistration` para criação;
- `UserAdministration` para o lifecycle administrativo composto;
- exceptions públicas para outcomes que atravessam a fronteira.

As portas consumer-owned `UserAuthenticationProvisioningPort` e
`UserAuthenticationAdministrationPort` descrevem apenas os efeitos exigidos em
Authentication. Seus adapters concretos ficam em
`authentication.infra.integration.identity`; Identity não acessa repositories,
services, models ou hashers internos de Authentication.

## API HTTP

- `POST /identity/register`: cria `User`, provisiona `AuthenticationAccount` e
  devolve os tokens da autenticação inicial;
- `GET /identity/me`: retorna somente dados cadastrais do usuário autenticado;
- `GET /identity/admin/users` e `GET /identity/admin/users/{userId}`: leitura
  administrativa paginada e baseada exclusivamente em `UserEntity`;
- `PATCH /identity/admin/users/{userId}/disable` e `/enable`: lifecycle
  administrativo composto.

O registro aceita CPF com 11 dígitos ou pontuado válido, mas persiste o valor
canônico. O campo de nascimento do contrato é `birthDate`; `birthData` não é
alias suportado.

## Lifecycle administrativo composto

```text
Admin request
    ↓
UserAdministration
    ├── User
    └── UserAuthenticationAdministrationPort
            ├── AuthenticationAccount
            └── revogação de credenciais (somente disable)
```

No disable, a mesma transação altera `UserStatus` para `DISABLED`, desabilita o
acesso da conta e revoga sessões, refresh tokens e challenges ativos. Senha,
histórico, lock automático e estado da credencial são preservados.

No enable, a mesma transação altera `UserStatus` para `ACTIVE` e habilita o
acesso da conta. Não desbloqueia a conta, não torna credencial temporária
permanente e não recria sessões ou tokens. Os locks seguem sempre a ordem
`User` → `AuthenticationAccount`; falhas causam rollback da operação completa.

## Estados independentes

- `UserStatus`: atividade administrativa e elegibilidade de negócio;
- `AuthenticationAccessStatus`: acesso administrativamente habilitado;
- `AuthenticationStatus`: lock automático por segurança;
- `CredentialStatus`: credencial permanente, troca obrigatória ou primeiro acesso.

Um `User ACTIVE` pode ter Authentication bloqueada ou credencial
`PENDING_FIRST_ACCESS`/`CHANGE_REQUIRED` e ainda permanecer elegível para
billing. Billing consulta somente a atividade administrativa de User.

## Persistência

```text
identity_users.id
        1
        │ shared primary key
        0..1
authentication_accounts.identity_id
```

`authentication_accounts.identity_id` é simultaneamente PK e FK. A associação
JPA é unidirecional, lazy, com `@OneToOne`, `@MapsId` e sem cascade de remoção
ou `orphanRemoval`. O domínio Authentication mantém somente o escalar
`Long identityId`. `UserEntity` aparece em Authentication exclusivamente nessa
associação física. O schema segue criado por JPA/Hibernate, sem migrations.

## Integrações

```text
billing        ─┐
authorization  ─┤
dependents     ─┤
memberships    ─┼──> UserQuery / UserRegistration
vehicles       ─┤
authentication─┘

UserAdministration / UserRegistration
    └──> consumer-owned ports
             └──> authentication.infra.integration.identity
```

O read model administrativo pertence integralmente a Identity e consulta
somente `UserEntity`, com paginação, filtros, busca, ordenação e sparse fields
no banco, sem N+1.
