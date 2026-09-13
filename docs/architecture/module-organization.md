# Padrão de organização dos módulos

Este documento registra o padrão estrutural dos bounded contexts do backend.
Leia primeiro o [índice e a governança documental](README.md). A organização é
por recurso de domínio, sem divisão por caso de uso.

## Estrutura base

```text
<module>/
├── api/
│   ├── http/
│   │   ├── controller/
│   │   │   ├── ResourceController.java
│   │   │   └── admin/AdminResourceController.java
│   │   ├── dto/
│   │   └── exception/
│   └── module/                 # API Java exposta a outros módulos
├── core/
│   ├── application/
│   │   ├── query/              # consultas expostas como API de módulo
│   │   ├── result/
│   │   └── service/
│   │       ├── resource/
│   │       │   ├── ResourceService.java
│   │       │   └── AdminResourceService.java
│   │       ├── internal/       # colaboradores sem endpoint próprio
│   │       └── bootstrap/      # inicialização e sincronização
│   ├── domain/
│   │   ├── model/
│   │   ├── enums/
│   │   └── exception/
│   ├── port/
│   └── repository/
├── infra/
│   ├── integration/
│   └── persistence/
│       ├── adapter/
│       ├── entity/
│       ├── jpa/
│       └── mapper/
└── docs/
    └── README.md              # entrada documental obrigatória do módulo
```

`api`, `core` e `infra` existem conforme a capacidade real; não são criadas
classes ou subpastas vazias. `docs/README.md` é a entrada mínima do bounded
context. Notas adicionais são orientadas por necessidade, não por template.

## Controllers e services

- Um recurso exposto por HTTP possui um controller normal e, quando houver
  operações administrativas, um controller `AdminResourceController` no
  subpacote `controller.admin`.
- Cada controller injeta somente o service da mesma superfície:
  `ResourceController` usa `ResourceService` e `AdminResourceController` usa
  `AdminResourceService`.
- Métodos do mesmo recurso ficam agrupados nesses services. Não são criadas
  classes `CreateResourceService`, `UpdateResourceService` ou equivalentes por
  caso de uso.
- Não são criados controllers ou services vazios. Se um recurso não possui
  operação normal ou administrativa, somente a superfície existente é criada.
- Modelos auxiliares sem endpoint próprio podem ser gerenciados por um service
  interno do agregado responsável.

## Limites entre camadas e módulos

- O controller recebe e valida DTOs HTTP, converte seus campos para a chamada
  do service e transforma resultados em DTOs de resposta.
- O core não importa classes de `api.http.dto`, tipos de controller nem classes
  de persistência. Services recebem valores, comandos internos ou interfaces do
  core e retornam modelos ou objetos de `application.result`.
- Transações pertencem aos services, não aos controllers.
- Um módulo não acessa repository, entity, service ou banco interno de outro
  módulo. Relação física entre tabelas não transfere ownership do dado.
- Uma capacidade pública, estável e diretamente compreensível pelo consumidor
  pode ser exposta pelo proprietário em `api.module`, normalmente como uma
  `*Query` somente leitura. O consumidor depende desse contrato, nunca da
  implementação interna.
- Quando o consumidor precisa expressar sua própria necessidade, traduzir o
  modelo externo ou isolar a integração, ele define uma porta no próprio
  `core`. Um adapter em `infra.integration` implementa essa porta usando o
  contrato público do módulo proprietário. `BillingMembershipPort` →
  `IdentityBillingMembershipAdapter` → `UserQuery` é o exemplo vigente.
- Uma porta consumer-owned que precise ser implementada pelo módulo fornecedor
  pode ser publicada como SPI. Identity publica suas necessidades de
  provisionamento em `identity.api.module.spi`, e Authentication fornece os
  adapters em `authentication.infra.integration.identity`.
- Associações JPA legadas entre módulos não devem ser convertidas em IDs
  escalares apenas para satisfazer a organização de pacotes: a remoção precisa
  de uma migração própria que preserve explicitamente as chaves estrangeiras.
- A associação física legada de `AuthenticationAccountEntity` com `UserEntity`
  é uma exceção já existente e não autoriza novos imports cross-module de JPA.
- Helpers compartilhados por services normal e administrativo ficam em
  `service.internal`; rotinas de inicialização ficam em `service.bootstrap`.

## Responsabilidades de `api`, `core`, `infra` e `docs`

- `api.http`: controllers, DTOs, validação de entrada e exception handlers do
  contrato HTTP; delega casos de uso e não concentra regra de negócio.
- `api.module`: contratos Java públicos deliberadamente expostos a outros
  módulos. Tipos internos não se tornam públicos por conveniência.
- `core.application`: orquestra casos de uso e transações; depende de domínio,
  repositories e ports do próprio módulo e, quando apropriado, de APIs públicas
  de outros módulos.
- `core.domain`: modelos, invariantes, estados e exceções de domínio, sem JPA ou
  DTO HTTP.
- `infra.integration`: adapters para contratos externos ou de outros módulos.
- `infra.persistence`: entities JPA, Spring Data repositories, mappers e
  adapters que implementam repositories do core.
- `docs`: conhecimento semântico e decisões específicas do bounded context.

## Bounded context, Shared e Platform

- Um bounded context (`billing`, `vehicles`, `iam.identity` etc.) é proprietário
  de regras, lifecycle e dados de um domínio de negócio.
- `shared` contém contratos pequenos, agnósticos de provider/framework ou
  catálogos realmente consumidos por mais de uma capacidade. Exemplos atuais:
  `FileStorage`, seus tipos neutros e o catálogo de permissions.
- `platform` implementa infraestrutura transversal e composição Spring. São
  exemplos `platform.security`, `platform.openapi`, `platform.web.exception`,
  `platform.time`, `platform.logging` e o provider de storage.

Nem `shared` nem `platform` são depósitos genéricos. Regra de Billing continua
em Billing; validação semântica de comprovante não migra para o storage global.
Uma abstração só vai para `shared` quando permanecer neutra ao domínio e à
infraestrutura. Um mecanismo só vai para `platform` quando sua responsabilidade
for transversal e técnica.

## Persistência

O repository do core pertence ao módulo proprietário. A entity JPA, o Spring
Data repository, o mapper e o adapter ficam na infraestrutura desse módulo; o
domínio não depende de entity JPA. Foreign key, join ou compartilhamento do
mesmo banco não autorizam consultas diretas ao modelo persistente de outro
bounded context.

O estado atual não possui política global de migrations versionadas. A
aplicação usa modelagem por entities/Hibernate (`ddl-auto=update` por padrão),
mantém Flyway desabilitado e não contém scripts de migration. Não introduza
Flyway, Liquibase ou obrigação de migration por inferência documental; uma
mudança dessa natureza exige decisão e escopo próprios.

### Identity, Authentication e Authorization

- `identity` contém o agregado `User`, seus dados cadastrais, estado
  administrativo, registro e lifecycle;
- `authentication` contém `AuthenticationAccount`, credenciais, login, lock,
  sessões, refresh tokens e recuperação de senha;
- `authorization` contém roles, permissions, vínculos e authorities.

As superfícies atuais são `/identity/me`, `/authentication/me` e
`/authorization/me`. Um eventual `/me` agregado deverá ser criado numa camada
de composição/BFF, não em um desses bounded contexts.

## Preservação do contrato HTTP

Mover, renomear ou dividir classes internas não autoriza alterar:

- método HTTP e URL;
- parâmetros de path, query, headers ou multipart;
- formato e validações dos DTOs de request;
- campos e estrutura dos DTOs de response;
- status HTTP, headers e content types;
- códigos de erro e regras de autorização observáveis.

Qualquer melhoria que modifique um desses itens deve ser tratada separadamente
como evolução versionada da API ou ser explicitamente autorizada no escopo da
refatoração. O cutover para `/identity/**` é uma dessas evoluções explícitas.

O contrato HTTP vigente é documentado no OpenAPI, não em um catálogo Markdown
paralelo. Consulte as
[regras globais para features e fixes](feature-development-rules.md#openapi-como-contrato-http).

Os casos encontrados durante esta padronização estão registrados em
[contract-sensitive-findings.md](contract-sensitive-findings.md).
