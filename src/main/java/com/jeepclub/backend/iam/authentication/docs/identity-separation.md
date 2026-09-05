# Separação entre Identity, Authentication e Authorization

O bounded context `identity` contém o agregado cadastral `User`.
`AuthenticationAccount` representa exclusivamente o acesso e a segurança desse
usuário: `identityId`, hash, estado da credencial, bloqueio por tentativas,
habilitação administrativa do acesso e timestamps de autenticação.

Os estados são independentes:

- `UserStatus`: atividade administrativa e elegibilidade de negócio;
- `AuthenticationAccessStatus`: acesso administrativamente habilitado;
- `AuthenticationStatus`: bloqueio automático;
- `CredentialStatus`: senha permanente, troca obrigatória ou primeiro acesso.

Identity coordena registro e disable/enable por portas consumer-owned. As
implementações em `authentication.infra.integration.identity` provisionam a
conta, emitem tokens ou aplicam os efeitos de lifecycle. No disable são
revogados sessions, refresh tokens e challenges; no enable não são alterados
lock, tentativas, senha nem `CredentialStatus`. A ordem de lock é sempre User e
depois AuthenticationAccount, dentro da mesma transação.

`authentication_accounts.identity_id` é PK e FK para `identity_users.id`. A
infraestrutura usa associação unidirecional lazy com `@OneToOne`, `@MapsId`,
sem cascade de remoção e sem `orphanRemoval`. O domínio Authentication armazena
somente `Long identityId`; `UserEntity` é usado neste módulo apenas para mapear
essa associação física.

## Superfícies HTTP

- `/identity/me`: dados cadastrais e estado administrativo do User;
- `/authentication/me`: identificadores e validade da sessão atual;
- `/authorization/me`: `userId` e authorities correntes.

Não existe endpoint agregador `/me`. Se necessário no futuro, ele deverá ficar
em uma camada de composição/BFF, fora dos três bounded contexts.

O registro público é `POST /identity/register`. Identity cria o User e delega
hash, criação da AuthenticationAccount, tokens e login inicial à porta de
Authentication. Membership e o bootstrap de desenvolvimento reutilizam esse
mesmo fluxo. Os endpoints legados `/authentication/register` e
`/authentication/admin/users/**` não existem mais.

Authentication consulta `identity.api.module.UserQuery` somente para resolver
CPF, validar existência/atividade e obter dados necessários em autenticação ou
recuperação. Não acessa repository, service ou domínio interno de Identity.
Identity, por sua vez, não acessa AuthenticationAccountRepository,
PasswordHasher, Session ou RefreshToken diretamente.
