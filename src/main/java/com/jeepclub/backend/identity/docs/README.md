# Identity

O modulo `identity` e o proprietario da identidade persistente, dos dados
cadastrais e do estado administrativo dos usuarios.

Pertencem a `identity`: identificador estavel, nome, data de nascimento, CPF,
RG, e-mail, telefone, foto, unicidade cadastral e o estado `ACTIVE` ou
`DISABLED`. CPF, RG e telefone sao armazenados somente com digitos; e-mail e
normalizado para lowercase.

Senha, credencial, bloqueio de login, sessions, tokens, roles e permissions nao
pertencem a este modulo. Membership e sua futura entidade `Member` tambem ficam
fora desta fronteira.

Uma identidade ativa continua elegivel para regras administrativas, inclusive
cobranca, ainda que a autenticacao esteja bloqueada ou pendente de primeiro
acesso. Exclusao com historico sera uma feature futura.

`identity_users` e a fonte de verdade cadastral. `IdentityQuery` expoe leituras
sem vazar estado de autenticacao e `IdentityAdministration` concentra a
ativacao e desativacao. A criacao coordenada com `authentication_accounts` usa
uma unica transacao e o mesmo ID escalar, sem relacionamento JPA entre modulos.
