# Separacao entre identidade e autenticacao

`AuthenticationAccount` representa o acesso e a seguranca de uma identidade.
Ela armazena somente `identityId`, hash, estado da credencial, bloqueio por
tentativas, habilitacao administrativa do acesso e timestamps de autenticacao.

Os estados permanecem independentes:

- `IdentityStatus` define atividade administrativa e elegibilidade de negocio;
- `AuthenticationAccessStatus` permite ou revoga acesso administrativamente;
- `AuthenticationStatus` representa bloqueio automatico;
- `CredentialStatus` representa senha permanente, troca obrigatoria ou primeiro acesso.

Desativar ou reativar um usuario pela administracao altera identidade e acesso
de autenticacao na mesma transacao. Reativar nao remove bloqueio automatico nem
torna permanente uma credencial temporaria.

`authentication_accounts.identity_id` e uma chave primaria atribuida com o ID
de `identity_users`. Nao existe relacionamento JPA entre as entidades. O
`AuthenticationAccountProvisioningService` cria ambas as partes atomicamente.

Login, cadastro, recuperacao de senha, refresh, bootstrap, membership e
administracao usam as duas novas fontes. A antiga tabela unificada
`authentication_users` foi removida; nao existe dual write.

JWT e sessions continuam usando o identificador estavel como `userId` no
contrato externo. `/me` e as consultas cadastrais leem `IdentityQuery`, sem
replicar dados pessoais dentro de autenticacao.
