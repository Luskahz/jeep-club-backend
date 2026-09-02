# Identity

## Objetivo

O modulo `identity` sera o proprietario da identidade persistente, dos dados
cadastrais e do estado administrativo dos usuarios.

Nesta primeira etapa incremental, o modulo expoe apenas o contrato de leitura
`IdentityQuery`. A implementacao ainda consulta a tabela legada
`authentication_users`, que continua sendo a unica fonte de verdade ate o
corte de dominio e persistencia.

## Fronteira definida

Pertencem a `identity`:

- identificador estavel do usuario;
- nome, data de nascimento, CPF, RG, e-mail, telefone e foto;
- existencia e unicidade cadastral;
- estado administrativo `ACTIVE` ou `DISABLED`.

Nao pertencem a `identity`:

- senha e password hash;
- estado e ciclo de vida da credencial;
- tentativas e bloqueio de login;
- sessions, refresh tokens e JWT;
- roles, permissions e authorities;
- membership e seu futuro estado proprio.

## Semantica de atividade

Uma identidade administrativamente ativa continua ativa para regras como
cobranca mesmo quando a autenticacao esta bloqueada ou a credencial esta
pendente de primeiro acesso ou de troca de senha.

Exclusao logica e tabela de historico nao fazem parte desta etapa. Elas serao
tratadas em uma feature futura, seguindo o modelo adotado em `dependents`.

## Estado transitorio

O adapter legado fica em `authentication.infra.integration.identity` e
implementa `IdentityQuery` sobre a persistencia atual. Nao existe escrita
duplicada nem segunda tabela de identidade nesta etapa.
