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

`authentication_accounts` reutiliza `identity_users.id` como chave primaria
atribuida, sem gerar um segundo identificador para a mesma pessoa. A ligacao e
mantida como identificador escalar entre modulos, sem relacionamento JPA: cada
modulo continua responsavel pelo proprio agregado e pelo proprio ciclo de vida.

## Estado transitorio

O adapter legado fica em `authentication.infra.integration.identity` e
implementa `IdentityQuery` sobre a persistencia atual. Nao existe escrita
duplicada. Tanto `identity_users` quanto `authentication_accounts` permanecem
dormentes no runtime atual. O proximo cutover deve criar primeiro a identidade,
reutilizar o ID na conta de autenticacao e somente entao substituir o adapter
legado.

## Criacao coordenada

`IdentityRegistration` e a API de escrita minima exposta pelo modulo. Ela cria
e normaliza a identidade dentro do proprio dominio e devolve somente o ID
gerado.

`AuthenticationAccountProvisioningService` coordena essa operacao com a criacao
da conta de autenticacao em uma unica transacao Spring. Uma falha na segunda
escrita desfaz a identidade, evitando registros parciais. O coordenador ainda
nao esta conectado aos fluxos HTTP, de memberships ou de bootstrap porque eles
continuam lendo `authentication_users`; ativa-lo isoladamente tornaria o novo
usuario invisivel ao runtime legado.
