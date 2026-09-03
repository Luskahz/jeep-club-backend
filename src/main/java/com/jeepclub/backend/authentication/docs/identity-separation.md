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
de autenticacao na mesma transacao. `IdentityAdministration` e o owner do caso
de uso composto e chama `IdentityAuthenticationAdministrationPort`; o adapter
fica em `authentication.infra.integration.identity`. A ordem de lock e sempre
Identity antes de AuthenticationAccount. No disable, o adapter tambem revoga
sessions, refresh tokens e challenges. Reativar nao remove bloqueio automatico,
nao torna permanente uma credencial temporaria e nao recria tokens.

`authentication_accounts.identity_id` e uma chave primaria atribuida com o ID
de `identity_users` e tambem uma chave estrangeira para essa tabela. A
infraestrutura usa uma associacao unidirecional lazy com `@OneToOne`, `@MapsId`
e sem cascade de remocao. Nao existe ID independente. O dominio continua usando
somente `Long identityId`. O schema e gerado pelo JPA/Hibernate e nenhuma
migration foi adicionada nesta etapa.

Login, cadastro, recuperacao de senha, refresh, bootstrap, membership e
administracao usam as duas novas fontes. A antiga tabela unificada
`authentication_users` foi removida; nao existe dual write.

JWT e sessions continuam usando o identificador estavel como `userId` no
contrato externo. `/me` e as consultas cadastrais leem `IdentityQuery`, sem
replicar dados pessoais dentro de autenticacao.

O antigo contrato genérico de consulta de usuário foi removido. Dados pessoais,
existência e atividade administrativa são consultados diretamente em
`IdentityQuery`. Não foi criado
um `AuthenticationQuery` especulativo; esse contrato so deve existir quando
houver um consumidor real de informacoes pertencentes a autenticacao.

`AdminUserJpaQueryRepository` e explicitamente um read model composto. Ele
parte de `AuthenticationAccountEntity` e navega pela associacao formal ate
Identity em um unico join, mantendo paginação, filtros, busca, ordenacao e
sparse field selection no banco e sem N+1. Essa leitura nao transfere ownership
de dados cadastrais para Authentication.
