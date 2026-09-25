# Memberships

Memberships é o bounded context do processo de admissão e da política de acesso
de membros do Jeep Club. Ele é proprietário da `MembershipApplication`, da
decisão administrativa sobre a solicitação, dos bloqueios de novos pedidos por
CPF e da configuração que indica qual cobrança de Billing representa a
membritude. Não é proprietário do cadastro do `User`, de credenciais, sessões,
permissions ou fatos financeiros.

Antes de alterar este módulo, leia a [governança global](../../../../../../../../docs/architecture/README.md), a
[organização dos módulos](../../../../../../../../docs/architecture/module-organization.md) e as
[regras de desenvolvimento](../../../../../../../../docs/architecture/feature-development-rules.md).
O Swagger/OpenAPI é a fonte de verdade do contrato HTTP detalhado; este documento
descreve o domínio e as integrações, não um catálogo concorrente de rotas.

## Limites

- **Identity** é proprietário do `User` cadastral. Memberships consulta a
  existência de CPF/e-mail por uma porta consumidora e, na aprovação, solicita a
  criação do usuário de primeiro acesso por uma porta própria.
- **Authentication** é proprietário de credenciais e do fluxo de primeiro
  acesso. Seu adapter implementa a porta de criação consumida por Memberships e
  usa os contratos públicos necessários de Identity e Authentication; o núcleo
  de Memberships não acessa serviços, repositórios ou entidades internas desses
  contextos.
- **Authorization** é proprietário de roles, permissions e authorities. As
  operações administrativas usam as authorities aplicadas por `@PreAuthorize`;
  a metadata `@RequiredPermission` apenas as publica no OpenAPI.

## Solicitação de adesão

Uma `MembershipApplication` contém nome, CPF, e-mail opcional, celular, mensagem e dados
de revisão. O domínio normaliza CPF para dígitos, e-mail para texto aparado em
minúsculas, celular para dígitos e campos opcionais vazios para `null`.

O pedido público começa em `PENDING`, com `requestedAt` e `updatedAt`. Ao criar
um pedido, o serviço verifica primeiro um bloqueio ativo do CPF. Sem bloqueio,
uma solicitação `PENDING` existente para o mesmo CPF é devolvida em vez de criar
outra. Para um novo pedido, CPF não pode já estar cadastrado em Identity; quando
o e-mail existir, ele não pode estar em Identity nem em outra solicitação pendente. Uma solicitação
rejeitada não impede um novo pedido por si só.

O agregado possui os seguintes estados e transições:

```text
PENDING --aprovar--> APPROVED --complete() do agregado--> COMPLETED
   |
   +--rejeitar ou rejeitar-e-bloquear--> REJECTED
```

As operações de aplicação/HTTP executam as transições a partir de `PENDING`
para `APPROVED` ou `REJECTED`. A conclusão bem-sucedida do primeiro acesso,
por senha provisória ou token de ativação, leva a solicitação a `COMPLETED`.
Rejeitar preenche `reviewedAt`, `finishedAt` e `updatedAt`; aprovar preenche
`reviewedAt`, `updatedAt`, o administrador revisor e o `User` criado. Os
instantes vêm do `Clock` injetado conforme a regra global.

## Aprovação e rejeição

As duas formas de aprovação criam um `User` em `PENDING_FIRST_ACCESS` por
`CreateUserWithPendingFirstAccessPort` e só depois registram a aprovação com o
identificador retornado. Falha nessa integração impede a aprovação da
solicitação na transação atual. A opção de senha temporária funciona com ou sem
e-mail e devolve a senha uma única vez. A opção por e-mail exige endereço,
persiste somente o hash do token e envia o segredo sem devolvê-lo ao admin.

A rejeição simples não cria bloqueio. O motivo é opcional e, quando presente,
é enviado ao mail sender configurado. Rejeição e aprovação só aceitam
solicitações `PENDING`; tentativas em outro estado são recusadas pelo serviço.

## Bloqueio de applicant

`MembershipApplicantBlock` é uma entidade independente da solicitação. Ela
bloqueia o CPF normalizado, exige motivo, registra quem e quando bloqueou e
preserva histórico ao ser desbloqueada (`unblockedAt` e `unblockedByUserId`). Há
no máximo um bloqueio ativo por CPF; o histórico de bloqueios inativos permanece.

Hoje o bloqueio é criado apenas pelo fluxo **rejeitar e bloquear**. Esse fluxo
cria o bloqueio e rejeita a solicitação pendente usando o mesmo instante dentro
da mesma transação Spring. Se o bloqueio não puder ser criado, a rejeição não é
confirmada. O bloqueio não altera solicitações antigas além da solicitação alvo
rejeitada; ele impede novos pedidos enquanto ativo. Desbloquear encerra somente
o bloqueio ativo e permite novos pedidos sujeitos às demais regras de
elegibilidade.

## Integrações e persistência

Memberships publica apenas `MembershipOnboardingCompletion` para Authentication
notificar a conclusão do primeiro acesso por senha provisória. Seus demais
contratos consumidores são definidos no próprio módulo:

- `UserExistencePort` é implementada por `IdentityUserExistenceAdapter`, que
  consulta `identity.api.module.UserQuery` para CPF e e-mail.
- `CreateUserWithPendingFirstAccessPort` é implementada no adapter de
  Authentication para provisionar o `User` e o primeiro acesso.
- `MemberActivationTokenHashPort` tem adapter em Authentication; a validação de
  token consulta apenas o repositório de token de Memberships.
- `CompletePendingFirstAccessPort` delega a definição da senha e a transição da
  credencial a Authentication.
- `MemberActivationMailSender` possui dummy restrito a dev/test e implementação
  SMTP para os demais ambientes; nenhum deles registra token ou link.
- `MembershipAccessQuery` é o contrato público read-only de decisão de acesso.
  A implementação consulta `MembershipChargeQuery`, contrato público de
  Billing, sem acessar ciclos, cobranças, repositories ou entities desse módulo.

As entidades JPA e mappers permanecem em `infra.persistence`. A aplicação usa
versionamento otimista em `MembershipApplication`. O bloqueio ativo tem
unicidade persistida por CPF; a solicitação pendente é protegida atualmente por
consulta e fluxo transacional, sem uma constraint de unicidade equivalente.

## Membritude paga

`MembershipBillingConfiguration` é uma configuração singleton opcional com
`chargeDefinitionId`, `enforcementEnabled`, `createdAt` e `updatedAt`. Membership
não copia valor, recorrência, vencimento, tolerância ou lifecycle da cobrança.
A definição só pode ser configurada enquanto estiver ativa em Billing; nenhuma
recorrência, flag `required` ou política de pagamento específica é imposta.

Zero configurações é um estado válido e libera recursos protegidos. Desabilitar
o enforcement também libera acesso, preservando a referência configurada para
reativação posterior. A superfície administrativa permite consultar, criar ou
substituir a configuração e alternar o enforcement; seus paths, payloads,
permissions e respostas são documentados no OpenAPI.

Quando o enforcement está ativo, Membership envia somente
`chargeDefinitionId` e o `UserPrincipal.userId` para Billing. A matriz de
decisão é:

| Resultado financeiro | Decisão de Membership |
| --- | --- |
| `WITHIN_PAYMENT_PERIOD` (`PENDING` válido) | permitir |
| `SATISFIED` (`PAID`) | permitir |
| `CANCELED` | permitir intencionalmente nesta versão |
| `PAYMENT_REQUIRED` (`OVERDUE` ou `EXPIRED`) | bloquear com `402 MEMBERSHIP_PAYMENT_REQUIRED` |
| `CHARGE_NOT_FOUND` | bloquear com `503 MEMBERSHIP_CHARGE_UNAVAILABLE` e emitir warning operacional |

Falhas inesperadas ao consultar Billing também usam o erro operacional 503,
sem serem apresentadas como dívida. O log de cobrança ausente contém somente
identificadores técnicos (`chargeDefinitionId` e `userId`) e herda a correlação
do MDC global.

`@RequiresMembership` declara essa exigência em métodos ou classes. Um advisor
dedicado de Spring Method Security obtém o principal já autenticado e chama
`MembershipAccessQuery`; ele executa cumulativamente com `@PreAuthorize`.
Assim, ausência de autenticação permanece 401, bloqueio financeiro permanece
402, indisponibilidade operacional permanece 503 e permission ausente após uma
membritude válida permanece 403. Endpoints não anotados não executam essa regra.

## Token de ativação

O módulo emite `MemberActivationToken` criptograficamente aleatório e persiste
somente seu hash. O GET público valida existência, expiração, uso e estado da
solicitação sem alterar o banco. O POST de conclusão usa o token bruto para
solicitar a senha definitiva a Authentication; somente depois do sucesso marca
o token usado e a solicitação `COMPLETED`. O reenvio invalida tokens ativos e
não recria User nem AuthenticationAccount.

## Testes

Os testes de domínio/aplicação cobrem elegibilidade, aprovação, rejeição e o
histórico de bloqueio. Testes de contrato HTTP/OpenAPI devem caracterizar a
superfície pública, permissions administrativas, paginação e respostas RFC 9457
sem duplicar o contrato detalhado do Swagger.
