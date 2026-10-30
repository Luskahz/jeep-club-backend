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
existe perfil operacional para um owner `USER` ou `DEPENDENT`.
`EmergencyMedicalProfileQuery` fornece leitura individual mínima para Event,
validando atividade/existência do owner pelos mesmos ports do módulo. Ausência
ou owner inacessível resulta em Optional vazio. Publications impõe permission,
participação confirmada e Event IN_PROGRESS, e audita a consulta com
`SystemLogService.recordRequired`, persistido sincronamente antes do retorno.
Nenhum perfil ou snapshot clínico é persistido em Publications.

O contrato HTTP detalhado — rotas do usuário, rotas administrativas, schemas,
paginação, permissions e respostas RFC 9457 — é o OpenAPI publicado em
`/v3/api-docs`. A listagem administrativa usa o envelope transversal
`PageResponse<MedicalProfileSummaryResponse>`: página zero-based, `size` padrão
20 e limite global 50, com ordenação padrão por `id`.

### Apresentação administrativa do proprietário

`AdminMedicalProfileService` compõe uma projeção interna somente após validar
o owner (ou montar a página final de owners elegíveis). A porta
`MedicalProfileOwnerPresentationQuery`, implementada em `infra.integration`,
consome `DependentsQuery.findDetailsByIds` e `UserQuery.findByIds`. IDs dos
usuários titulares e responsáveis são combinados e deduplicados. São feitas
no máximo uma consulta de detalhes de Dependents e uma de Identity por página
HTTP de até 50 itens; página vazia não consulta nomes. Ambos os contratos são
limitados a 500 IDs por lote, inclusive para consumidores internos.

Os DTOs administrativos acrescentam `ownerName` e `responsibleName` aos campos
anteriores, sem modificar o envelope `PageResponse`. Para USER, `ownerId` é
`Identity.User.id`, o nome vem de Identity e `responsibleName` é null. Para
DEPENDENT, `ownerId` é `Dependent.id`; o nome e o `userId` responsável vêm do
cadastro atual de Dependents e o nome do responsável vem de Identity.
CPF, e-mail, telefone e outros campos cadastrais consultados internamente não
integram a projeção HTTP. Matrícula não participa da resolução.

`MedicalProfileResponse` e `MedicalProfileSummaryResponse` exigem a projeção
completa no mapper `fromPresentation`; não há mapper administrativo que receba
somente o domínio. As rotas pessoais usam `PersonalMedicalProfileResponse`,
com os mesmos campos JSON e tipos anteriores; o nome do schema OpenAPI pessoal
passa a refletir essa separação. Mutações, exportações e acesso emergencial
mantêm seus contratos próprios. As quatro leituras administrativas retornam
`Cache-Control: no-store` e continuam exigindo `HEALTH_MEDICAL_PROFILE_READ`.

A projeção é recalculada a cada consulta, sem cache, colunas novas ou nomes
persistidos em Health. As leituras participam da transação read-only da
aplicação e do isolamento configurado no banco, sem locks pessimistas. Em
MySQL REPEATABLE READ refletem o snapshot de leitura dessa transação; em READ
COMMITTED mudanças entre statements podem ser observadas. Nome e vínculo do
dependente são obtidos juntos; o responsável é resolvido pelo `userId` desse
mesmo detalhe, nunca por nome. Alterações posteriores ao snapshot aparecem
na próxima consulta; não há garantia de linearização com alterações cadastrais
concorrentes. Se a resolução observar owner removido/inativo após a seleção,
falha com os erros existentes 404/409 em vez de devolver dados clínicos com
identificação parcial. Um responsável ausente gera
`MEDICAL_PROFILE_OWNER_NOT_FOUND` para o dependente; seu status não acrescenta
uma nova regra de elegibilidade ao dependente ativo. Totais e itens não são
recalculados silenciosamente durante o enriquecimento.

A necessidade funcional veio do PR #115, encerrado sem merge; esta solução foi
refeita sobre a `develop`, sem reaproveitar sua implementação ou criar rotas
de seleção de usuários/owner.

## Testes relevantes

A [auditoria da suíte](test-audit.md) registra a matriz entre produção,
cobertura preexistente e incrementos, além das evidências de JaCoCo/PIT.

Os testes do módulo cobrem normalização, owner ativo/inativo, ownership de
dependente, exclusão com histórico, concorrência e tradução de falhas de
persistência. Testes de contrato OpenAPI caracterizam a superfície HTTP sem
duplicar o catálogo de rotas nesta documentação.


## Exportações CSV/PDF — BACK-410

Export administrativo exige HEALTH_MEDICAL_PROFILE_EXPORT. Perfil individual por profileId/userId/dependentId, todos, household=true com userId e histórico separado. Household inclui titulares/dependentes sem ficha e só relaciona dependentes do titular consultado. A visão administrativa comum preserva owners ativos; a ficha household inclui todos os vínculos atuais, ACTIVE ou DISABLED. MedicalProfileCoverageQuery fornece exclusivamente IDs com ficha, em lotes de até 500, sem campos clínicos. Publications continua usando EmergencyMedicalProfileQuery individual. Auditoria obrigatória registra ator/ação sem conteúdo clínico.

Contratos HTTP, limites, segurança e evidências estão em `docs/exports/` na raiz do repositório. As exportações são administrativas, sem paginação HTTP, com auditoria síncrona e `Cache-Control: no-store`.
