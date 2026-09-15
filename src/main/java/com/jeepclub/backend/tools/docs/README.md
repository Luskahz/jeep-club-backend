# Tools

Leia primeiro a [governança global](../../../../../../../../docs/architecture/README.md), a
[organização dos módulos](../../../../../../../../docs/architecture/module-organization.md)
e as [regras de desenvolvimento](../../../../../../../../docs/architecture/feature-development-rules.md).
Este documento descreve somente o bounded context `tools`. O OpenAPI gerado
pelos controllers e DTOs é a fonte de verdade de paths, payloads, paginação,
responses, permissions e erros HTTP.

## Responsabilidade e ownership

Tools é proprietário do cadastro operacional de `Tool`, de seu status, vínculo
escalar com `userId` e snapshot histórico de exclusão. Não é proprietário do
cadastro, atividade administrativa ou lifecycle do `User`, nem de credenciais,
roles ou permissions.

Nas rotas de membro, o `userId` vem do `UserPrincipal`. Operações individuais
carregam a ferramenta por ID e `Tool.assertBelongsTo` rejeita outro proprietário
com `403`; ID inexistente produz `404`. Não há filtro de status nesse
carregamento: ferramentas `ACTIVE` e `INACTIVE` podem ser consultadas,
atualizadas, ativadas, desativadas ou excluídas pelo proprietário. A listagem
do membro também usa somente `userId`, portanto inclui ambos os status.

As rotas administrativas operam sobre ferramentas de qualquer proprietário e
requerem uma authority específica: `TOOLS_TOOL_READ`, `TOOLS_TOOL_CREATE`,
`TOOLS_TOOL_UPDATE`, `TOOLS_TOOL_ACTIVATE`, `TOOLS_TOOL_DEACTIVATE` ou
`TOOLS_TOOL_DELETE`, conforme a operação.

## Modelo, criação e atualização parcial

`Tool` contém nome, descrição, `ToolStatus`, `userId`, `createdAt` e
`updatedAt`. Os timestamps usam `LocalDateTime` derivado do `Clock` injetado
nos services; `createdAt` é definido na criação e não muda, enquanto mutações
efetivas avançam `updatedAt`.

A criação sempre inicia em `ACTIVE`. O nome é obrigatório no request, porém a
criação atual o encaminha sem `trim` adicional no domínio. A descrição é
opcional e também é armazenada conforme recebida na criação.

O `PUT` atual possui semântica parcial, apesar de manter esse verbo:

- `name` nulo preserva o valor; vazio ou somente espaços também preserva o
  nome; texto não vazio é aparado e substitui;
- `description` nula preserva o valor; qualquer texto não nulo é aparado e
  substitui, inclusive texto vazio;
- a operação atualiza `updatedAt` e persiste o agregado mesmo quando nenhum
  campo altera seu conteúdo.

## Status e lifecycle

`ToolStatus` possui apenas `ACTIVE` e `INACTIVE`; não existe `DELETED` nem
soft delete operacional.

```text
create -> ACTIVE --deactivate()--> INACTIVE --activate()--> ACTIVE
                    ^                                |
                    +--------------------------------+

ACTIVE ou INACTIVE --delete HTTP--> snapshot histórico + remoção operacional
```

Ativar uma ferramenta já `ACTIVE` ou desativar uma já `INACTIVE` é idempotente:
o service retorna a própria ferramenta, não muda `updatedAt` e não chama
persistência de mutação. `InvalidToolStatusException` permanece mapeada pelo
handler, mas não há fluxo atual de `Tool`, `ToolService` ou `AdminToolService`
que a lance.

## Exclusão e histórico

O delete não é soft delete. O service obtém o instante pelo `Clock` e informa o
usuário executor; o adapter toma lock pessimista por ID, cria
`ToolHistoryEntity` com o snapshot completo, `deletedByUserId` e `deletedAt`,
e remove fisicamente `ToolEntity` na mesma transação. Falha no snapshot desfaz a
remoção. A tabela operacional deixa de conter a ferramenta e o histórico não
possui endpoint de consulta.

Uma segunda exclusão que chega ao adapter após a remoção encontra ausência sob
lock e resulta em `ToolAlreadyDeletedException`/`409`; se a ferramenta já não
for encontrada no carregamento anterior, o resultado é `404`. O histórico tem
unicidade por `toolId`, preserva o status no momento da exclusão e não é um
estado operacional reativável.

## Administração, filtros e paginação

A criação administrativa para `/admin/tools/users/{userId}` grava o `userId`
recebido como escalar. Tools não possui `api.module`, `core.port` ou
`infra.integration`, não consulta Identity e não valida se o User existe ou
está `ACTIVE`/`DISABLED`. Também não há FK declarada para Identity neste módulo.

A listagem administrativa aceita `name` e `status`. `name` não vazio aplica
busca por trecho case-insensitive; `status` compara exatamente `ACTIVE` ou
`INACTIVE`; os filtros podem ser combinados. As listagens de membro e admin
retornam diretamente `Page<T>` do Spring Data. A página é zero-based, o tamanho
padrão global é 20, o máximo global é 50 e não há ordenação padrão específica
de Tools. A estabilização transversal da forma JSON de `Page` pertence à
BACK-377; este módulo apenas documenta o runtime atual.

## Persistência, limites e testes

`ToolEntity`, `ToolHistoryEntity`, repositories JPA, specifications, mappers e
o adapter ficam em `infra.persistence`. A entidade operacional exige nome,
status, userId e `createdAt`; o histórico tem unicidade por `toolId` e índices
por `userId` e `deletedAt`. O schema segue a política global vigente de
entities/Hibernate, sem migration versionada introduzida por este módulo.

Os testes existentes cobrem service e adapter de exclusão histórica. Testes
focais de contrato caracterizam OpenAPI, update parcial, visibilidade de
`INACTIVE` e ciclo idempotente. A suíte ampla de unidade, MVC, segurança,
persistência, sistema, JaCoCo e PIT continua fora desta documentação nas
BACK-302 até BACK-309.

## Terminologia histórica

Textos antigos de BACK-301 e BACK-302 até BACK-308 ainda citam soft delete ou
estado de exclusão. Eles não descrevem o runtime atual: exclusão é snapshot
histórico seguido de hard delete e `ToolStatus` contém somente `ACTIVE` e
`INACTIVE`. Essas tarefas continuam inalteradas e precisam ser revalidadas
antes de uma futura suíte definitiva.
