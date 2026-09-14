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

O domínio atual não normaliza nem valida seus argumentos em `create`,
`reconstitute` ou `update`; as garantias de formato hoje vêm principalmente
dos DTOs HTTP e da persistência. No cadastro, a placa deve ter sete caracteres
em formato antigo ou Mercosul e já chegar em maiúsculas; o RENAVAM deve chegar
com onze dígitos e checksum válido. Na edição, o validador de RENAVAM ignora
caracteres não numéricos para calcular o checksum, mas o valor original é
encaminhado à persistência. Não existe canonicalização uniforme de placa ou
RENAVAM no core.

Antes de criar ou trocar identificadores, os services fazem consultas de
existência por igualdade exata. A tabela operacional também possui unicidade
sem nomes explícitos para placa e RENAVAM. Essa combinação não serializa duas
criações concorrentes: a checagem prévia e a constraint podem produzir
resultados diferentes, e uma violação de integridade concorrente não possui
tradução específica no módulo.

## Criação e edição atuais

Todo veículo criado começa `ACTIVE`. A rota de membro usa diretamente o
`userId` já autenticado e não consulta `UserPort`; a validação do token vigente
já exige User administrativamente ativo, mas Vehicles não repete essa consulta
no caso de uso. A criação administrativa usa `UserPort.existsById` e rejeita
somente proprietário inexistente; ela não distingue User `ACTIVE` de
`DISABLED`.

A edição HTTP é um `PUT` que chama `Vehicle.update` com todos os valores do
DTO e substitui todos os campos editáveis. Ela **não possui semântica parcial
uniforme**:

- ausência de qualquer primitive (`manufacturingYear`, `modelYear`,
  `seatingCapacity`, `engineDisplacement` ou `towing`) é rejeitada durante a
  leitura do JSON como `400 HTTP_400`, antes de chamar o service;
- campos de referência omitidos chegam como `null`; os opcionais podem ser
  limpos, enquanto omitir campos exigidos pela persistência pode terminar em
  falha não controlada.

Por isso, nenhuma feature deve tratar o payload atual como patch nem assumir
que “somente campos enviados” serão alterados. A evolução da parcialidade é
acompanhada pela BACK-326.

## Status, exclusão e histórico

`VehicleStatus` possui `ACTIVE` e `SOFT_DELETED`.

```text
create -> ACTIVE --update--> ACTIVE
                  |
                  +--delete HTTP--> snapshot histórico + remoção operacional

ACTIVE --softDelete()--> SOFT_DELETED   (método legado, sem fluxo HTTP atual)
```

Os services e listagens atuais só expõem `ACTIVE`. `softDelete(Instant)` ainda
define `SOFT_DELETED`, `updatedAt` e `deletedAt`, e o mapper mantém esse instante
na coluna `disabled_at`; porém nenhum caso de uso ou endpoint atual chama esse
método. Um registro legado reconstituído como `SOFT_DELETED` é ocultado como
`404` e não pode ser editado ou excluído pelas operações atuais.

O delete efetivamente usado é hard delete com histórico. Dentro da transação do
service, o adapter obtém lock pessimista pela ID, cria um snapshot completo em
`vehicles_vehicle_history` com `deletedByUserId` e `deletedAt`, e remove a linha
de `vehicles_vehicle`. Falha no snapshot desfaz a remoção. O histórico não tem
API de consulta e não reserva placa ou RENAVAM; após a remoção operacional, os
identificadores podem ser usados por um novo veículo. Uma segunda exclusão que
perde a corrida pelo lock recebe conflito de veículo já removido. A coexistência
desse fluxo com o estado e método legados é a ambiguidade acompanhada pela
BACK-330.

## Integração e persistência

`UserPort` é uma porta consumer-owned de Vehicles. O
`VehicleIdentityAdapter` a implementa por meio de
`identity.api.module.UserQuery.existsById`; hoje ela é consumida somente na
criação administrativa. Não existe contrato Java público em `vehicles.api.module`
nem acesso direto a repository, entity ou service interno de Identity.

`VehicleEntity`, `VehicleHistoryEntity`, repositories JPA, mappers e o adapter
de persistência ficam em `infra.persistence`. A entidade operacional mantém
placa e RENAVAM únicos, campos obrigatórios para marca, modelo, combustível,
status, owner e criação, mas não define nomes para essas constraints nem índices
explícitos. O histórico tem unicidade por `vehicleId` e índices por owner,
placa, RENAVAM e data de exclusão. O schema atual continua representado pelas
entities/Hibernate, sem migration versionada introduzida por este módulo.

## Limitações funcionais já rastreadas

- BACK-326: tornar coerente a semântica de edição parcial;
- BACK-327: validar de forma explícita a existência e atividade do owner;
- BACK-328: uniformizar placa/RENAVAM e proteger duplicidade concorrente;
- BACK-329: mover e reforçar invariantes no domínio;
- BACK-330: eliminar a ambiguidade entre `SOFT_DELETED` e hard delete histórico;
- BACK-331: endurecer constraints, tamanhos e índices JPA;
- BACK-332: ampliar a matriz funcional, MVC, segurança, rollback e concorrência.

Essas limitações descrevem o estado atual; os tickets não são regras já
implementadas.

## Testes

Os testes existentes cobrem services de membro/admin e o adapter de exclusão
histórica. Testes focais de contrato caracterizam o binding do `PUT` e o
OpenAPI. Ampliações funcionais permanecem na BACK-332 e devem seguir os
[critérios globais](../../../../../../../../docs/architecture/feature-development-rules.md#testes-m%C3%ADnimos).
