# Health

## Responsabilidade

`health` é o bounded context proprietário do perfil médico operacional. Ele
mantém um único `MedicalProfile` para cada par `(ownerType, ownerId)`, normaliza
seus dados e preserva um snapshot de histórico quando o perfil é excluído.

O perfil contém somente os dados implementados hoje: tipo sanguíneo, alergias,
condições crônicas, medicamentos de uso contínuo, dados de convênio, contato de
emergência e observações. `bloodType` ausente é normalizado para `UNKNOWN`;
textos opcionais são aparados e vazios tornam-se nulos; o telefone de emergência
é persistido somente com 10 ou 11 dígitos.

Health não possui o cadastro ou lifecycle de `User` (Identity), nem o cadastro
ou lifecycle de `Dependent` (Dependents). Também não possui credenciais,
sessões, roles ou permissions. Consulte primeiro a
[governança global](../../../../../../../../docs/architecture/README.md), as
[fronteiras de módulos](../../../../../../../../docs/architecture/module-organization.md)
e as [regras de desenvolvimento](../../../../../../../../docs/architecture/feature-development-rules.md).

## Owners, acesso e integrações

`MedicalProfileOwnerType` possui os valores `USER` e `DEPENDENT`. As integrações
em `infra.integration` consomem somente contratos públicos: `UserQuery` de
Identity e `DependentsQuery` de Dependents. Elas classificam o owner como ativo,
inativo ou inexistente e, para um dependente, validam que ele é ativo e pertence
ao `userId` do `UserPrincipal` autenticado.

- O usuário autenticado opera apenas sobre o seu próprio perfil e o perfil de
  seus dependentes ativos.
- A superfície administrativa é separada e requer as authorities
  `HEALTH_MEDICAL_PROFILE_READ`, `HEALTH_MEDICAL_PROFILE_UPDATE` ou
  `HEALTH_MEDICAL_PROFILE_DELETE`, conforme a operação.
- `AdminMedicalProfileService` consulta owners ativos em lotes para a listagem;
  perfis de owners inativos ou removidos não aparecem nela. A limpeza
  administrativa por ID continua possível para perfis retidos.

Leia [medical-profile-owner-lifecycle.md](medical-profile-owner-lifecycle.md)
para as regras completas de owner, retenção e erros de ownership.

## Lifecycle e persistência

As operações de escrita são upserts: para um owner ativo e válido, um perfil
inexistente é criado; um perfil já existente é bloqueado para atualização e tem
`updatedAt` avançado com o `Clock` injetado. `createdAt` não muda. A unicidade
física de `(owner_type, owner_id)` complementa esse bloqueio contra criação
concorrente.

Uma exclusão não é soft delete: ela grava um snapshot em
`medical_profiles_history`, com `deletedByUserId` e `deletedAt`, e remove o
registro operacional na mesma transação. O histórico não possui endpoint de
consulta; criar outro perfil posteriormente é um novo ciclo operacional.

Leia [persistence-error-policy.md](persistence-error-policy.md) para o
mapeamento Health-específico de conflitos, indisponibilidade e falhas de
persistência.

## Contrato público e HTTP

`health.api.module.medicalprofile.MedicalProfileQuery` é a API Java pública
read-only do módulo: `existsByOwner(MedicalProfileOwner, ownerId)` informa se
existe perfil operacional para um owner `USER` ou `DEPENDENT`. Não há consumo
externo desse contrato no código atual.

O contrato HTTP detalhado — rotas do usuário, rotas administrativas, schemas,
paginação, permissions e respostas RFC 9457 — é o OpenAPI publicado em
`/v3/api-docs`. A listagem administrativa usa o envelope transversal
`PageResponse<MedicalProfileSummaryResponse>`: página zero-based, `size` padrão
20 e limite global 50, com ordenação padrão por `id`.

## Testes relevantes

Os testes do módulo cobrem normalização, owner ativo/inativo, ownership de
dependente, exclusão com histórico, concorrência e tradução de falhas de
persistência. Testes de contrato OpenAPI caracterizam a superfície HTTP sem
duplicar o catálogo de rotas nesta documentação.
