# Identity

## Objetivo

O modulo `identity` sera o proprietario da identidade persistente, dos dados
cadastrais e do estado administrativo dos usuarios.

O modulo ja possui dominio e persistencia proprios. Durante a migracao
incremental, o contrato de leitura `IdentityQuery` ainda consulta a tabela
legada `authentication_users`, que continua sendo a unica fonte de verdade
utilizada pelos fluxos da aplicacao ate o cutover transacional.

## Fronteira definida

Pertencem a `identity`:

- identificador estavel do usuario;
- nome, data de nascimento, CPF, RG, e-mail, telefone e foto;
- existencia e unicidade cadastral;
- estado administrativo `ACTIVE` ou `DISABLED`.

Os dados cadastrais sao normalizados no dominio:

- nome recebe `trim`, preservando capitalizacao;
- CPF e RG armazenam apenas digitos;
- e-mail recebe `trim` e lowercase;
- telefone armazena apenas digitos;
- textos opcionais vazios sao convertidos para `null`.

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

## Persistencia

`identity_users` e a tabela definitiva do modulo. Ela usa ID gerado por
`IDENTITY`, constraints unicas nomeadas para CPF, e-mail e RG e indice para o
status administrativo.

Enquanto os fluxos de escrita ainda nao foram migrados, essa tabela permanece
sem dados funcionais e nao e consultada pelos demais modulos. Ela nao constitui
uma segunda fonte de verdade neste estado intermediario.

## Estado transitorio

O adapter legado fica em `authentication.infra.integration.identity` e
implementa `IdentityQuery` sobre a persistencia atual. Nao existe escrita
duplicada. O proximo cutover deve criar primeiro a identidade, reutilizar o ID
na conta de autenticacao e somente entao substituir o adapter legado.
