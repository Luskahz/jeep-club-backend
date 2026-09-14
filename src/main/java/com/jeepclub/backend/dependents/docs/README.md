# Dependents

Leia primeiro a [governança global](../../../../../../../../docs/architecture/README.md), a
[organização dos módulos](../../../../../../../../docs/architecture/module-organization.md)
e as [regras de desenvolvimento](../../../../../../../../docs/architecture/feature-development-rules.md).
Este documento descreve somente o bounded context `dependents`. O OpenAPI
gerado pelos controllers e DTOs é a fonte de verdade do contrato HTTP.

## Responsabilidade e limites

Dependents é proprietário do agregado `Dependent`: seus dados operacionais,
vínculo com o usuário titular, status, transições de atividade e histórico de
exclusão. Não é proprietário do `User`, de credenciais, sessões, tokens, roles
ou authorities.

O titular é identificado por `userId`. Nas rotas do titular, o ID vem do
`UserPrincipal`; o service confirma que o dependente pertence a esse ID. Nas
rotas administrativas, a authority `DEPENDENTS_DEPENDENT_READ` é aplicada por
`@PreAuthorize` e publicada por `@RequiredPermission`. A listagem e a consulta
administrativas não são histórico: elas veem somente registros operacionais.

## Modelo, dados e invariantes

Um `Dependent` tem nome, CPF, data de nascimento, `RelationshipType`, telefone,
`userId`, status e timestamps. `RelationshipType` aceita `SPOUSE`, `CHILD`,
`PARENT`, `SIBLING`, `OTHER` e `GUEST`; não há regra adicional de parentesco
neste módulo.

Nome é aparado. CPF e telefone são normalizados para dígitos; telefone ausente
ou em branco é armazenado como `null`. O domínio exige CPF com onze dígitos e
telefone, quando presente, com dez ou onze. As validações observáveis de body,
formatos e exemplos pertencem aos schemas OpenAPI.

O CPF é único no cadastro operacional de Dependents e não pode coincidir com
um CPF existente em Identity. Um dependente `DISABLED` continua reservando seu
CPF. A exclusão remove o registro operacional; o histórico de exclusão não
participa da consulta de disponibilidade de CPF.

## Status, lifecycle e exclusão

Todo dependente é criado em `ACTIVE`.

```text
create -> ACTIVE --disable()--> DISABLED --enable()--> ACTIVE
               | update (somente ACTIVE)
               +--delete()--> histórico de exclusão + remoção operacional
DISABLED -------+--delete()--> histórico de exclusão + remoção operacional
```

`DISABLED` não significa exclusão. O dependente continua no cadastro
operacional, pode ser lido pela superfície administrativa e permanece com CPF
reservado, mas o agregado bloqueia `update`; a atualização da superfície do
titular procura somente dependentes `ACTIVE`. `enable()` volta o status para
`ACTIVE`. As transições `disable()` e `enable()` existem no domínio e no
`DependentService`, preservam o registro e atualizam `updatedAt`, mas não há
endpoint HTTP que as exponha no estado atual.

`delete()` aceita registro ativo ou desabilitado. A infraestrutura obtém lock
pessimista, grava um snapshot em `dependents_dependent_history` com
`deletedByUserId` e `deletedAt`, e então remove fisicamente a entidade de
`dependents_dependent`. Não há soft delete, não há `deletedAt` no agregado
operacional, não há endpoint de consulta de histórico e não existe reativação
após exclusão. Falha ao persistir o snapshot desfaz a exclusão transacional.

## Integrações e contratos públicos

`DependentUserPort` é uma porta consumer-owned de Dependents. O adapter
`DependentIdentityAdapter` usa `identity.api.module.UserQuery` para confirmar
que o titular existe, está administrativamente ativo e para verificar CPF já
cadastrado em Identity. Dependents não acessa repositório, entity ou service
interno de Identity.

`api.module.DependentsQuery` é a fronteira Java pública de leitura. Health o
consome por adapters para verificar existência, atividade, propriedade e lotes
de IDs ativos de dependentes ao aplicar seu próprio lifecycle de perfis
médicos. Ele não expõe o agregado, entity, repository nem dados pessoais.

## Persistência, concorrência e testes

Entity, repositories JPA, mappers e adapters ficam em `infra.persistence`.
Há constraint única para CPF operacional e uma entrada de histórico por
`dependentId`; o delete usa lock pessimista para serializar a captura do
snapshot e a remoção. `createdAt` representa a criação, `updatedAt` a última
alteração operacional e `deletedAt` pertence exclusivamente ao snapshot de
histórico.

Os testes existentes caracterizam normalização, ownership, atividade,
unicidade de CPF, transições do domínio, persistência de histórico e fluxo de
sistema. Ao alterar o módulo, escolha cobertura proporcional conforme as
[regras globais](../../../../../../../../docs/architecture/feature-development-rules.md#testes-m%C3%ADnimos).
