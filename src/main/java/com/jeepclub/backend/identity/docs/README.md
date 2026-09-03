# Identity

## Ownership

O módulo `identity` é o proprietário da identidade persistente. Sua fonte de
verdade é `identity_users` e suas responsabilidades são:

- ID persistente e estável;
- nome, nascimento, CPF, RG, e-mail, telefone e foto;
- normalização e unicidade cadastral;
- estado administrativo `ACTIVE` ou `DISABLED`.

CPF, RG e telefone são armazenados somente com dígitos. E-mail é normalizado
para lowercase. Exclusão com histórico será uma feature futura.

O módulo `authentication` é o proprietário de senha e credenciais, login,
habilitação do acesso, lock automático, sessões, refresh tokens e challenges.
Roles e permissions pertencem a `authorization`. A futura entidade `Member` e
seu lifecycle pertencem a `memberships` e não são antecipados por este módulo.

## Contratos

Consumidores usam apenas contratos de `identity.api.module`:

- `IdentityQuery` para dados e atividade administrativa;
- `IdentityRegistration` para criação;
- `IdentityAdministration` para o lifecycle administrativo composto;
- exceptions públicas para outcomes que precisam atravessar a fronteira.

Exceptions de domínio e persistência permanecem internas. A porta SPI
`IdentityAuthenticationAdministrationPort` descreve somente os efeitos de
autenticação exigidos pelo lifecycle. Seu adapter concreto fica em
`authentication.infra.integration.identity`.

## Lifecycle administrativo composto

Disable e enable são operações sensíveis e atômicas:

```text
Admin request
    ↓
IdentityAdministration
    ├── Identity
    └── IdentityAuthenticationAdministrationPort
            ├── AuthenticationAccount
            └── credential revocation (apenas disable)
```

No disable, a mesma transação altera `IdentityStatus` para `DISABLED`, altera
`AuthenticationAccessStatus` para `DISABLED` e revoga sessões, refresh tokens e
challenges ativos. Senha, histórico, lock automático e estado da credencial são
preservados.

No enable, a mesma transação altera `IdentityStatus` para `ACTIVE` e
`AuthenticationAccessStatus` para `ENABLED`. Não desbloqueia a conta, não torna
credencial temporária permanente e não recria sessões ou tokens.

Os locks seguem sempre a ordem `Identity` → `AuthenticationAccount`. Qualquer
falha causa rollback de ambos os agregados e das revogações.

## Estados independentes

- `IdentityStatus`: atividade administrativa e elegibilidade de negócio;
- `AuthenticationAccessStatus`: acesso administrativamente habilitado ou desabilitado;
- `AuthenticationStatus`: lock automático por segurança;
- `CredentialStatus`: credencial permanente, troca obrigatória ou primeiro acesso.

São estados válidos:

```text
Identity ACTIVE + Authentication LOCKED
Identity ACTIVE + Credential PENDING_FIRST_ACCESS
Identity ACTIVE + Credential CHANGE_REQUIRED
```

Em todos esses casos a identidade continua elegível para billing. Billing olha
somente a atividade administrativa da Identity, nunca o estado de autenticação.

## Persistência

```text
identity_users.id
        1
        │
        │ shared primary key
        0..1
authentication_accounts.identity_id
```

`authentication_accounts.identity_id` é simultaneamente PK e FK para
`identity_users.id`. A infraestrutura de autenticação declara uma associação
JPA unidirecional, lazy, com `@OneToOne`, `@MapsId` e sem cascade de remoção.
O domínio de autenticação continua usando apenas o `Long identityId`.

O schema atual é criado pelo JPA/Hibernate. Não foram adicionadas migrations
porque não há dados existentes que precisem de preservação ou transformação
nesta etapa.

O uso de `IdentityEntity` pela infraestrutura de autenticação é uma exceção
consciente e limitada à associação física e ao read model administrativo. Ele
não autoriza acesso aos repositories ou services internos de Identity.

## Integrações

As integrações cadastrais e de atividade administrativa são:

```text
billing        ─┐
authorization  ─┤
dependents     ─┤
memberships    ─┼──> IdentityQuery
vehicles       ─┤
authentication─┘

IdentityAdministration
    └──> IdentityAuthenticationAdministrationPort
             └──> authentication.infra.integration.identity
```

O read model administrativo combina Identity e Authentication por join na
associação formal, mantendo paginação, filtro, busca, ordenação e sparse fields
no banco, sem atribuir a Authentication ownership dos dados cadastrais.
