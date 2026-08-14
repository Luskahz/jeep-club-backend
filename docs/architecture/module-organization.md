# Padrão de organização dos módulos

Este documento registra o padrão adotado a partir dos módulos `authentication`
e `billing`. A organização é por recurso de domínio, sem divisão por caso de
uso.

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
└── infra/
    ├── integration/
    └── persistence/
        ├── adapter/
        ├── entity/
        ├── jpa/
        └── mapper/
```

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
- Um módulo não acessa repository, entity ou service interno de outro módulo.
  A comunicação ocorre por uma API em `api.module` ou por uma porta definida no
  módulo consumidor e implementada na infraestrutura.
- Associações JPA legadas entre módulos não devem ser convertidas em IDs
  escalares apenas para satisfazer a organização de pacotes: a remoção precisa
  de uma migração própria que preserve explicitamente as chaves estrangeiras.
- Helpers compartilhados por services normal e administrativo ficam em
  `service.internal`; rotinas de inicialização ficam em `service.bootstrap`.

## Preservação do contrato HTTP

Mover, renomear ou dividir classes internas não autoriza alterar:

- método HTTP e URL;
- parâmetros de path, query, headers ou multipart;
- formato e validações dos DTOs de request;
- campos e estrutura dos DTOs de response;
- status HTTP, headers e content types;
- códigos de erro e regras de autorização observáveis.

Qualquer melhoria que modifique um desses itens deve ser tratada separadamente
como evolução versionada da API.

Os casos encontrados durante esta padronização estão registrados em
[contract-sensitive-findings.md](contract-sensitive-findings.md).
