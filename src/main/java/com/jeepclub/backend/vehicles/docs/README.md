# Vehicles

Leia primeiro a [governança global](../../../../../../../../docs/architecture/README.md), a
[organização dos módulos](../../../../../../../../docs/architecture/module-organization.md)
e as [regras de desenvolvimento](../../../../../../../../docs/architecture/feature-development-rules.md).
Este documento descreve somente o bounded context `vehicles`. O OpenAPI gerado
pelos controllers e DTOs é a fonte de verdade de paths, payloads, paginação,
responses, permissions e erros HTTP.

## Responsabilidade e ownership

Vehicles é proprietário do cadastro operacional de `Vehicle`, dos seus
identificadores, dados descritivos, vínculo escalar com o proprietário e do
snapshot histórico gerado na exclusão. Não é proprietário do `User`, de
credenciais ou de roles e permissions.

Nas rotas de membro, o `ownerId` vem do `UserPrincipal`. Consulta, edição e
exclusão procuram o veículo por `(vehicleId, ownerId)` e exigem status `ACTIVE`;
veículo inexistente, não ativo ou de outro usuário produz o mesmo `404`, sem
revelar ownership. As rotas administrativas operam sobre qualquer veículo
ativo e usam as authorities `VEHICLES_VEHICLE_CREATE`,
`VEHICLES_VEHICLE_READ`, `VEHICLES_VEHICLE_UPDATE` e
`VEHICLES_VEHICLE_DELETE` conforme a operação.

## Modelo e identificadores

`Vehicle` contém apelido, referência de foto, placa, RENAVAM, marca, modelo,
anos de fabricação e modelo, cor, capacidade, combustível, cilindrada,
capacidade de reboque, `ownerId`, status e timestamps. `FuelType` possui
`GASOLINE`, `ETHANOL`, `FLEX`, `DIESEL`, `ELECTRIC` e `HYBRID`.

O domínio ainda não normaliza a maioria dos seus argumentos, mas placa e
RENAVAM são exceção: `Vehicle.normalizePlate` (trim + uppercase) e
`Vehicle.normalizeRenavam` (somente dígitos) são chamados internamente por
`create`, `update` e `reconstitute`, então a forma persistida é sempre
canônica independentemente do que o chamador enviou. `RenavamValidator` aceita
somente 11 dígitos ou o formato documentado `###.###.###-##` antes de
reutilizar `Vehicle.normalizeRenavam` para calcular o checksum, então validação
e persistência nunca divergem sobre o que conta como dígito. No cadastro e na
edição, a placa aceita espaços nas bordas e minúsculas no DTO (o `@Pattern` é
case-insensitive e tolera espaços); o RENAVAM aceita apenas a pontuação do
formato documentado. Dados legados lidos via `reconstitute` recebem a forma
canônica apenas em memória; esta Story não executa migração nem grava
automaticamente essa canonicalização. As garantias de formato continuam vindo
principalmente dos DTOs HTTP mais essa canonicalização de domínio.

Antes de criar ou trocar identificadores, os services canonicalizam o valor
recebido e só então fazem a consulta de existência (`existsByPlate`/
`existsByRenavam`) — a checagem nunca usa o valor bruto. A tabela operacional
tem as constraints nomeadas `uk_vehicle_plate` e `uk_vehicle_renavam`
(`VehicleEntity`), e essa checagem prévia continua sem serializar duas
criações/edições concorrentes: `VehicleRepositoryAdapter.save` usa
`saveAndFlush` dentro de um try/catch que inspeciona a cadeia de causas de
`DataIntegrityViolationException` por esses nomes de constraint e traduz uma
violação concorrente para `VehiclePlateAlreadyExistsException`/
`VehicleRenavamAlreadyExistsException` (mesmo 409 RFC 9457 da pré-checagem),
em vez de vazar `DataIntegrityViolationException`/SQL para o cliente.

## Criação e edição atuais

Todo veículo criado começa `ACTIVE`. A rota de membro usa diretamente o
`userId` já autenticado e não consulta `UserPort`; a validação do token vigente
já exige User administrativamente ativo, mas Vehicles não repete essa consulta
no caso de uso. A criação administrativa usa `UserPort.existsById` para
rejeitar proprietário inexistente e `UserPort.existsActiveById` para rejeitar
proprietário existente porém administrativamente `DISABLED`.

A edição HTTP continua um `PUT`, agora com semântica de atualização parcial:
campo omitido do JSON preserva o valor atual do veículo; campo presente com
valor aplica esse valor após validação de formato; campo presente como `null`
só é aceito para os campos realmente anuláveis (`nickname`, `photo`, `color`)
e limpa o valor, sendo rejeitado com `400` para os demais campos
(`plate`, `renavam`, `brand`, `model`, `manufacturingYear`, `modelYear`,
`seatingCapacity`, `fuelType`, `engineDisplacement`, `towing`). O controller lê o corpo
como `JsonNode` (não mais `@Valid EditRequestDTO` direto) via
`EditRequestFieldReader`, que converte e valida os valores presentes contra
`EditRequestDTO` e monta um `VehicleEditFields` com um `FieldUpdate<T>` por
campo. `VehicleEditResolver` (`service.internal`), compartilhado pelas edições
de membro e administrador, resolve esse feixe contra o `Vehicle` atual antes
de chamar `Vehicle.update`, que segue sem qualquer dependência de Jackson ou
DTO HTTP.

## Status, exclusão e histórico

Existe uma única política operacional de exclusão: hard delete transacional
com snapshot histórico. Nenhum outro lifecycle é suportado.

```text
create -> ACTIVE --update--> ACTIVE
                  |
                  +--delete HTTP--> snapshot histórico + remoção operacional
```

Dentro da transação do service, o adapter obtém lock pessimista pela ID, cria
um snapshot completo em `vehicles_vehicle_history` com `deletedByUserId` e
`deletedAt`, e remove a linha de `vehicles_vehicle`. Falha no snapshot desfaz
a remoção. O histórico não tem API de consulta e não reserva placa ou
RENAVAM; após a remoção operacional, os identificadores podem ser usados por
um novo veículo. Uma segunda exclusão que perde a corrida pelo lock recebe
conflito de veículo já removido (`VehicleAlreadyDeletedException`, 409). Os
services e listagens só expõem `ACTIVE`; `DetailResponseDTO.status` documenta
`ACTIVE` como único valor no contrato público.

`VehicleStatus` ainda declara `SOFT_DELETED`, mas exclusivamente como legado
de compatibilidade de leitura: nenhum caso de uso grava esse valor (o método
de domínio que o produzia, `Vehicle.softDelete(Instant)`, foi removido por
não ter mais nenhum caller), e não existe campo `deletedAt`/`disabledAt` na
operação atual — esses campos foram removidos do domínio (`Vehicle`), da
entidade operacional (`VehicleEntity`) e da entidade de histórico
(`VehicleHistoryEntity`, que já tinha seu próprio `deletedAt` do hard delete,
correto e preservado; o `disabledAt` copiado da entidade operacional era
puro peso morto). A única razão para manter o valor no enum é não quebrar a
leitura de uma linha pré-existente que porventura ainda tenha
`status = 'SOFT_DELETED'` no banco: `@Enumerated(EnumType.STRING)` falharia
ao desserializar essa linha se o valor fosse removido do enum, e uma consulta
por ID que a alcançasse quebraria com erro não controlado em vez do 404 atual.
Como hoje nenhuma constraint impede a leitura de uma linha assim por ID,
`findActiveVehicle` (member e admin) já trata qualquer status diferente de
`ACTIVE` — incluindo esse legado — como veículo não encontrado, então o
registro nunca fica visível, editável ou reativável pelas rotas atuais.
`VehicleRepositoryAdapterTest.legacySoftDeletedRowIsReadWithoutMappingFailure`
e `AdminVehicleServiceTest.hidesLegacySoftDeletedVehicleAsNotFoundWithoutFailingOnRead`
caracterizam essa compatibilidade explicitamente. Como o projeto não usa
migrations versionadas, não há cleanup automático de linhas legadas; o banco
de desenvolvimento pode ser recriado quando necessário, e a estratégia acima
cobre o caso de uma instância que não seja recriada.

## Integração e persistência

`UserPort` é uma porta consumer-owned de Vehicles. O `VehicleIdentityAdapter`
a implementa por meio de `identity.api.module.UserQuery.existsById` e
`UserQuery.isAdministrativelyActive`; hoje ela é consumida somente na criação
administrativa. Não existe contrato Java público em `vehicles.api.module` nem
acesso direto a repository, entity ou service interno de Identity.

`VehicleEntity`, `VehicleHistoryEntity`, repositories JPA, mappers e o adapter
de persistência ficam em `infra.persistence`. A entidade operacional mantém
placa e RENAVAM únicos com constraints nomeadas (`uk_vehicle_plate`,
`uk_vehicle_renavam`) e campos obrigatórios para marca, modelo, combustível,
status, owner e criação, mas ainda sem índices explícitos além dessas
constraints. O histórico tem unicidade por `vehicleId` e índices por owner,
placa, RENAVAM e data de exclusão. O schema atual continua representado pelas
entities/Hibernate, sem migration versionada introduzida por este módulo; não
há backfill de dados legados eventualmente não canônicos, consistente com a
política atual do projeto de recriar o banco de desenvolvimento quando
necessário em vez de introduzir migrations.

## Limitações funcionais já rastreadas

- BACK-329: mover e reforçar invariantes no domínio;
- BACK-331: endurecer constraints, tamanhos e índices JPA;
- BACK-332: ampliar a matriz funcional, MVC, segurança, rollback e concorrência.

Essas limitações descrevem o estado atual; os tickets não são regras já
implementadas.

## Testes

Os testes existentes cobrem services de membro/admin e o adapter de exclusão
histórica. `VehicleEditResolverTest` cobre, campo a campo, ausência, valor
presente, `false`/`0` explícitos e null permitido/proibido na resolução da
edição parcial. `VehicleTest` cobre a canonicalização de placa/RENAVAM em
`create`/`update`/`reconstitute`. `VehicleRepositoryAdapterTest` simula
duplicidade concorrente chamando `save` duas vezes com o mesmo valor canônico
sem pré-checagem entre as chamadas, caracterizando a tradução de
`DataIntegrityViolationException` para os erros de negócio de placa/RENAVAM, e
`legacySoftDeletedRowIsReadWithoutMappingFailure` prova que uma linha
pré-existente com `status = SOFT_DELETED` inserida diretamente via JPA (sem
passar por `Vehicle.create`) ainda é lida sem falha de mapeamento;
`AdminVehicleServiceTest.hidesLegacySoftDeletedVehicleAsNotFoundWithoutFailingOnRead`
e `VehicleServiceTest.hidesSoftDeletedVehicle` provam que esse registro é
tratado como 404 pelos dois services. Testes de contrato caracterizam o
binding do `PUT` via `JsonNode` e o OpenAPI. Ampliações funcionais permanecem
na BACK-332 e devem seguir os
[critérios globais](../../../../../../../../docs/architecture/feature-development-rules.md#testes-m%C3%ADnimos).
