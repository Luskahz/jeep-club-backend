# Authentication

Leia primeiro a [governança global](../../../../../../../../docs/architecture/README.md), a
[organização dos módulos](../../../../../../../../docs/architecture/module-organization.md)
e as [regras de desenvolvimento](../../../../../../../../docs/architecture/feature-development-rules.md).
Este documento descreve somente o bounded context `iam.authentication`. O
contrato HTTP detalhado pertence ao OpenAPI gerado pelos controllers e DTOs do
módulo.

## Responsabilidade e limites

Authentication é proprietário de credenciais, `AuthenticationAccount`, hash de
senha, tentativas inválidas, lock, sessões, access tokens, refresh tokens,
desafios de troca obrigatória de senha, recuperação e revogação de credenciais.
Ele não é proprietário dos dados cadastrais nem do estado administrativo do
usuário, que pertencem a Identity, nem de roles, permissions ou authorities,
que pertencem a Authorization.

`AuthenticationAccount` usa o `identityId` como chave e mantém somente esse
identificador no domínio. A infraestrutura mantém a associação JPA legada com
`UserEntity` exclusivamente para a PK/FK compartilhada; isso não autoriza
acesso ao repositório, entidade ou service interno de Identity.

Para os limites conceituais entre os três bounded contexts, consulte também
[identity-separation.md](identity-separation.md). A documentação de Identity
descreve o lifecycle administrativo composto que chama as portas de
provisionamento e administração implementadas aqui.

## Modelo e estados

Os estados da conta são independentes:

- `AuthenticationAccessStatus`: `ENABLED` ou `DISABLED`, controlado pelo
  lifecycle administrativo de Identity;
- `AuthenticationStatus`: `ENABLED` ou `LOCKED`, usado para o lock automático
  após cinco tentativas de login inválidas;
- `CredentialStatus`: `PERMANENT`, `PENDING_FIRST_ACCESS` ou
  `CHANGE_REQUIRED`, que determina se a autenticação final pode emitir tokens.

Desabilitar e habilitar o acesso não altera lock, contador de tentativas,
senha nem estado da credencial. Uma troca de senha válida limpa o lock e torna
a credencial permanente, mas não reabilita acesso administrativamente
desabilitado. Não existe nesta implementação uma regra especial para impedir
lockout do usuário com role ROOT; essa proteção pertence ao escopo da BACK-324.

Uma `Session` inicia `ACTIVE`, pode passar a `LOGGED_OUT` no logout do dono ou
na operação administrativa, e pode ser `REVOKED` por revogação de
credenciais. Um `RefreshToken` inicia `ACTIVE`, é `ROTATED` quando usado para
renovação ou `REVOKED` por logout/revogação. Solicitações de recuperação ficam
`OPEN`, `RESOLVED` ou `CANCELLED`; o método registra se o fluxo é por e-mail,
link administrativo ou senha provisória. Desafios de troca obrigatória são
temporários e de uso único.

## Fluxos e invariantes

- Login resolve CPF por `identity.api.module.UserQuery`, trava a conta para
  atualização e registra tentativa inválida antes de rejeitar credencial
  incorreta. Credencial definitiva gera ou reutiliza uma sessão ativa e emite
  access/refresh token; credencial provisória retorna somente o desafio de
  troca obrigatória.
- A conclusão da troca obrigatória valida e consome o desafio, atualiza a
  senha, resolve a solicitação de senha provisória quando aplicável, revoga
  credenciais anteriores e emite novos tokens.
- A renovação valida token, sessão, conta e atividade administrativa do User;
  ela emite um novo refresh token e marca o anterior como `ROTATED` na mesma
  transação.
- Logout do dono encerra somente a sessão indicada pelo `UserPrincipal` e
  revoga seus refresh tokens ativos. A leitura de sessão atual também usa o
  `UserPrincipal`, sem reprocessar JWT no controller.
- A solicitação pública de recuperação devolve representação genérica mesmo
  quando CPF, conta ou e-mail não podem ser usados. Isso reduz revelação de
  existência de conta. O reset por token, senha provisória administrativa e
  link administrativo alteram senha conforme seus fluxos próprios.
- Revogação de credenciais revoga sessões e refresh tokens ativos e invalida
  desafios ativos do usuário. É usada no disable administrativo e nas trocas
  de senha que exigem encerramento de credenciais anteriores.

As decisões temporais recebem `Clock` UTC por injeção; TTLs de sessão, refresh
token, recuperação e desafio são configurados em `security.auth`.

## Integrações e contratos públicos

Authentication consome `UserQuery` de Identity para CPF, nome, e-mail,
existência e atividade administrativa. Identity publica os SPIs
`UserAuthenticationProvisioningPort` e `UserAuthenticationAdministrationPort`;
os adapters em `infra.integration.identity` criam contas, emitem tokens e
aplicam disable/enable sem expor internals de Authentication a Identity.

O adapter de Memberships é consumidor de `UserRegistration` e de serviços de
recuperação para criar usuário pendente de primeiro acesso com senha provisória
ou link. Authentication não possui `api.module` próprio nesta revisão; suas
integrações publicadas são implementações dos contratos proprietários de
Identity e Memberships. A validação de access token é usada pela infraestrutura
de segurança da plataforma para montar o `UserPrincipal`; authorities são
obtidas por Authorization fora deste módulo.

## HTTP, autorização e erros

Os controllers separam operações públicas, de sessão autenticada e
administrativas. O OpenAPI é a fonte de verdade de métodos, paths, payloads,
validações, schemas, exemplos, status e authorities. Rotas públicas removem a
segurança bearer global; rotas administrativas usam `@PreAuthorize` para
enforcement e `@RequiredPermission` apenas para publicar a mesma authority no
OpenAPI. Não há ownership administrativo adicional além da authority; operações
da sessão corrente são limitadas pelo `UserPrincipal` e pelas verificações do
service.

Erros do módulo usam `ApiErrorResponse` no formato RFC 9457 com media type
`application/problem+json`. Os handlers em `api.http.exception` traduzem
outcomes de conta, sessão, refresh token, recuperação, desafio e token; 401 e
403 da infraestrutura de segurança usam o mesmo contrato global.

## Persistência, concorrência e testes

Repositórios, entities, JPA queries, mappers e adapters ficam em
`infra.persistence`. Consultas que mudam conta, sessão, refresh token,
solicitação ou desafio usam variantes com lock pessimista quando necessário;
o teste de concorrência caracteriza que logins simultâneos deixam uma única
sessão ativa. O schema continua governado pelas entities/JPA na política
global atual, sem migrations versionadas obrigatórias.

Os testes existentes cobrem contrato DTO, controllers, segurança HTTP, estados
da conta, revogação, login concorrente, adapters de persistência, JWT e
integrações de provisionamento. Ao alterar este contexto, escolha os testes
afetados conforme as [regras globais](../../../../../../../../docs/architecture/feature-development-rules.md#testes-m%C3%ADnimos).
