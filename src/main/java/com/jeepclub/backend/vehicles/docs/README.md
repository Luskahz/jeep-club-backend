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

## Invariantes do agregado

`Vehicle.create`, `Vehicle.reconstitute` e `Vehicle.update` protegem, no próprio
domínio, as invariantes já sustentadas pelo contrato HTTP/JPA atual,
independentemente de quem chama (service, adapter, teste ou integração
futura): `ownerId` obrigatório e positivo; `id` reconstituído obrigatório e
positivo; `plate`/`renavam` obrigatórios (a checagem usa o mesmo
`normalizePlate`/`normalizeRenavam` já existente, sem duplicar formato ou
checksum, que continuam exclusivos de `RenavamValidator`); `brand`/`model`
obrigatórios e não brancos nas três operações, inclusive em `update` — antes
só o DTO de criação (`@NotBlank`) garantia isso, e o `EditRequestDTO` aceitava
string vazia por não repetir a anotação; `color` obrigatório e não branco
somente em `create`, já que a edição parcial (BACK-326) pode legitimamente
limpá-lo para `null`; `fuelType`, `towing` e `createdAt` obrigatórios
(`NullPointerException` via `Objects.requireNonNull`) nas três operações;
`status` obrigatório em `reconstitute`; ano de fabricação e ano do modelo não
anteriores a 1900; capacidade de assentos de pelo menos 1; cilindrada não
negativa; e `updatedAt`, quando presente, nunca anterior a `createdAt`
(`IllegalStateException`), validado tanto em `reconstitute` quanto no `now`
recebido por `update`.

O contrato HTTP atual é inconsistente quanto a limites superiores: apenas
`EditRequestDTO` declara `@Max` para ano de fabricação/modelo (2100) e
capacidade de assentos (50); `IncludeRequestDTO` (criação, usada por membro e
admin) não declara limite superior para nenhum dos dois. O domínio protege
somente o limite inferior comum às duas entradas (1900 e 1, respectivamente)
para não inventar, silenciosamente, uma regra de negócio mais restritiva para
`create` do que o contrato atual permite. Unificar esses limites é uma
evolução de contrato HTTP fora do escopo desta consolidação.

Campos sem regra observável no contrato atual (`nickname`, `photo`, e
comprimento máximo de string) permanecem sem validação de domínio, também
para não inventar regra nova; essas garantias continuam vindo do Bean
Validation dos DTOs.

`VehicleEditResolver` continua resolvendo presença/ausência/null explícito
antes de chamar `Vehicle.update`; as invariantes acima são a segunda camada
de proteção do agregado, não uma duplicata da semântica de edição parcial.

## Integração e persistência

`UserPort` é uma porta consumer-owned de Vehicles. O `VehicleIdentityAdapter`
a implementa por meio de `identity.api.module.UserQuery.existsById` e
`UserQuery.isAdministrativelyActive`; hoje ela é consumida somente na criação
administrativa. Não existe contrato Java público em `vehicles.api.module` nem
acesso direto a repository, entity ou service interno de Identity.

`VehicleEntity`, `VehicleHistoryEntity`, repositories JPA, mappers e o adapter
de persistência ficam em `infra.persistence`. A entidade operacional mantém
placa e RENAVAM únicos com constraints nomeadas (`uk_vehicle_plate`,
`uk_vehicle_renavam`, preservadas sem alteração desde a BACK-328). O schema
atual continua representado pelas entities/Hibernate, sem migration
versionada introduzida por este módulo; não há backfill de dados legados
eventualmente não canônicos, consistente com a política atual do projeto de
recriar o banco de desenvolvimento quando necessário em vez de introduzir
migrations.

### Mapeamento JPA endurecido pela BACK-331

`VehicleEntity` e `VehicleHistoryEntity` (o histórico reflete o mesmo
snapshot final do hard delete, então segue a mesma lógica de nullability e
length da tabela operacional) declaram explicitamente:

- `length` das colunas textuais alinhado ao maior limite já aceito pelo
  contrato entre `IncludeRequestDTO` (criação) e `EditRequestDTO` (edição
  parcial), para nunca truncar um valor que algum dos dois DTOs já aceitava:
  `nickname` 100, `photo` 500, `brand` 50, `model` 100, `color` 30. `plate` e
  `renavam` usam o tamanho exato da forma canônica sempre persistida pelo
  domínio (`Vehicle.normalizePlate`/`normalizeRenavam`, BACK-328/BACK-329): 7
  e 11, respectivamente;
- `nullable = false` explícito nas colunas que a BACK-329 tornou
  obrigatórias no domínio para as três operações — `plate`, `renavam`,
  `brand`, `model`, `manufacturingYear`, `modelYear`, `seatingCapacity`,
  `fuelType`, `engineDisplacement`, `status`, `towing`, `ownerId`,
  `createdAt` — incluindo `seatingCapacity`/`engineDisplacement` (já eram
  `int`/`double` obrigatórios no contrato, mas a coluna não declarava a
  constraint) e `towing` (tornou-se obrigatório com a BACK-326/BACK-329 mas a
  coluna ainda aceitava `NULL`);
- `nickname`, `photo`, `color` e `updated_at` permanecem `nullable` nas duas
  tabelas, refletindo opcionalidade real: os três primeiros podem ser
  limpos por edição parcial (BACK-326) e `updated_at` só existe após a
  primeira atualização.

Índice novo: `idx_vehicle_owner_id_status` (`owner_id`, `status`) na tabela
operacional, sustentado por `VehicleRepository.findAllByOwnerIdAndStatus`
(listagem de membro, paginada). Nenhum índice dedicado foi criado para
`findAllByStatus` (listagem admin): `status` tem baixa cardinalidade — hoje
praticamente todo registro operacional é `ACTIVE` —, então um índice
só nessa coluna não reduziria I/O de forma relevante; `findByIdAndOwnerId` já
resolve pela PK (`id`) e não se beneficia de índice adicional. Os índices do
histórico (`idx_vehicle_history_owner_id`, `idx_vehicle_history_plate`,
`idx_vehicle_history_renavam`, `idx_vehicle_history_deleted_at`) não foram
alterados.

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
binding do `PUT` via `JsonNode` e o OpenAPI. `VehicleTest` cobre, além da
canonicalização de placa/RENAVAM, as invariantes descritas em "Invariantes do
agregado": criação, reconstituição e atualização válidas e inválidas para
cada campo obrigatório, limite numérico e a consistência de
`createdAt`/`updatedAt`. `VehicleRepositoryAdapterTest.operationalColumnMetadataReflectsHardenedNullabilityAndLength`
e `.historyColumnMetadataReflectsHardenedSnapshotNullabilityAndLength`
consultam `INFORMATION_SCHEMA.COLUMNS` do H2 de teste para caracterizar o
`length`/`nullable` reais gerados pelo mapeamento endurecido na BACK-331; e
`.savingEntityWithNullTowingViolatesNotNullConstraint` prova que a coluna
`towing`, agora `NOT NULL`, rejeita a persistência mesmo quando o domínio é
contornado e a entity é salva diretamente com o campo nulo.

### Matriz final (BACK-332)

A BACK-332 substitui formalmente o backlog antigo (BACK-293/294..300,
encerrado como *superseded*: aquelas subtasks partiam de premissas já
inválidas, como soft delete operacional e metas fixas de cobertura/PIT) e
fecha as lacunas concretas restantes após a suíte já ampliada por
BACK-326/327/328/329/330/331, sem duplicar nenhuma delas:

- **Rollback transacional do snapshot histórico** — lacuna explicitamente
  identificada e antes sem nenhum teste: `VehicleRepositoryAdapterTest
  .deleteRollsBackOperationalRemovalWhenHistorySnapshotFails` prova, com H2
  real (sem mock de service), que uma falha na gravação do histórico
  (constraint `uk_vehicle_history_vehicle_id` violada) não deixa o veículo
  operacional removido nem qualquer estado parcial persistido;
- **Delete repetido/concorrente** — `VehicleAlreadyDeletedException` não
  tinha nenhum teste; `.repeatedDeleteLosingTheLockRaceIsTranslatedToAlreadyDeletedConflict`
  prova, de forma determinística (sem sleep/probabilístico), que uma segunda
  exclusão sobre um veículo já removido é tratada como conflito de negócio,
  não erro genérico;
- **Segurança administrativa real** — antes desta Story, `@PreAuthorize`/
  `@RequiredPermission` das rotas `/vehicles/*/admin/**` só era caracterizado
  estaticamente no OpenAPI (`VehiclesOpenApiIntegrationTest`), nunca aplicado
  de fato em runtime. `VehicleAdminSecurityIntegrationTest` carrega a cadeia
  de segurança real (não `addFilters = false`, não contexto fatiado) e prova,
  para cada uma das quatro authorities (`VEHICLES_VEHICLE_CREATE/READ/
  UPDATE/DELETE`): 401 sem autenticação, 403 `ACCESS_DENIED` sem a authority
  específica (inclusive uma authority *diferente* das quatro não é
  suficiente — não existe "authority de admin genérica"), e sucesso com a
  authority correta. Segue o mesmo padrão já usado em
  `DependentSecurityIntegrationTest`/`PaymentReceiptSecurityIntegrationTest`
  (mock de `JwtTokenParser`/`UserAuthoritiesProvider`), sem inventar
  mecanismo de teste novo;
- **Fluxo completo real** — nenhum teste existente exercitava o módulo
  inteiro (controller → `VehicleService` real → `VehicleRepositoryAdapter`
  real → H2) de ponta a ponta; os testes MVC existentes mockam o service.
  `VehicleLifecycleIntegrationTest` cobre criar → listar → consultar →
  editar → consultar novamente → excluir → consultar (404) pela pilha real,
  uma única vez, sem duplicar a matriz campo a campo já coberta por
  `VehicleEditContractCharacterizationTest`.

Áreas revisadas e já adequadamente cobertas por Stories anteriores, portanto
não duplicadas: edição parcial campo a campo (BACK-326,
`VehicleEditResolverTest`/`VehicleEditContractCharacterizationTest`);
ownership/desabilitação administrativa do membro (BACK-327,
`VehicleOwnerAuthenticationIntegrationTest`, services de membro/admin);
canonicalização e duplicidade concorrente de placa/RENAVAM sem 500 genérico
(BACK-328, `VehicleRepositoryAdapterTest.concurrentDuplicate*`); hard delete
feliz, reuso de identificadores e compatibilidade de leitura legada
(BACK-330); invariantes de domínio e metadata de schema (BACK-329/331,
`VehicleTest`, `VehicleRepositoryAdapterTest.*ColumnMetadata*`). Ampliações
funcionais além dessas lacunas permanecem na BACK-332 original apenas onde
ainda fizerem sentido, e devem seguir os
[critérios globais](../../../../../../../../docs/architecture/feature-development-rules.md#testes-m%C3%ADnimos).
