# Matriz de auditoria — Dependents

Auditoria em 25/09/2026 sobre `develop@80ddc7de896fa52a75b67dcc45322c9b3355a5c5`.
A referência remota foi conferida nesta execução. A baseline focal passou com 58 testes.
As classificações abaixo distinguem a cobertura da baseline do resultado desta entrega.

## Inventário

- Domínio: `Dependent`, `DependentStatus`, `RelationshipType` e exceção de exclusão repetida.
- Aplicação: `DependentService`, `AdminDependentService`, `DependentsQueryService`,
  `DependentResult`, `DependentUserPort`, `DependentRepository` e exceções de aplicação.
- HTTP: controllers member/admin, DTOs create/update/response e `DependentExceptionHandler`.
- Persistência: adapter, dois repositories JPA, entities operacional/histórico e mappers.
- Integração: `DependentIdentityAdapter` consome `UserQuery`; `DependentsQuery` é consumida
  por `MedicalProfileOwnerStatusAdapter` e `DependentsOwnershipAdapter` em Health.
- Baseline de testes: `DependentTest`, `DependentServiceTest`, `AdminDependentServiceTest`,
  `DependentControllerTest`, `AdminDependentControllerTest`, `DependentSecurityIntegrationTest`,
  `DependentsOpenApiIntegrationTest`, `DependentRepositoryAdapterTest`, `DependentSystemFlowTest`.
- Baseline externa relevante: `MedicalProfileOwnerStatusAdapterTest` e
  `MedicalProfileOwnerValidationServiceTest`, no módulo Health.

## Produção → teste existente → gap → resultado

Nomes de testes sem pacote referem-se ao inventário acima. “Coberto” expressa o comportamento
observável avaliado, não a promessa de cobertura de todas as combinações de entrada.

| Produção / comportamento | Evidência preexistente | Baseline | Gap e incremento | Final |
| --- | --- | --- | --- | --- |
| `Dependent.create`: normalização, campos obrigatórios, CPF/telefone, owner positivo, ACTIVE e updatedAt nulo | `DependentTest` | Parcial | Limite válido de telefone com 10 dígitos e ausência de instante explícito | Coberto |
| `Dependent.update`: campos alterados, normalização e bloqueio quando DISABLED | `DependentTest` | Parcial | Nome vazio; verificar telefone, parentesco, nascimento e preservação de owner/createdAt | Coberto |
| Reconstituição: identidade, owner, nome e ordem temporal | Exercitada indiretamente por fixtures/JPA | Parcial | Casos negativos de id, owner e nome; updatedAt nulo/igual/anterior a createdAt | Coberto |
| `update/disable/enable`: createdAt <= updatedAt quando updatedAt existir | Sem teste de inversão em mutações | Ausente | Regressão falhou nos três métodos; corrigida validação de domínio; igualdade e instante nulo testados | Coberto |
| Status ACTIVE ↔ DISABLED e repetição idempotente | `DependentTest`, `DependentServiceTest` | Parcial | Repetição preserva timestamp; tempo controlado no fluxo de sistema | Coberto |
| Criação: titular inexistente/inativo e CPF ocupado em Identity ou Dependents | `DependentServiceTest` | Coberto no service | Boundary real com Identity agora cobre CPF ocupado e titular DISABLED em `DependentSystemFlowTest` | Coberto |
| Edição: novo CPF ocupado por usuário/outro dependente; mesmo CPF formatado | Apenas edição com novo CPF disponível | Parcial | Dois conflitos sem save/mutação e edição com CPF inalterado sem consulta de disponibilidade | Coberto |
| Ownership de read/update/disable/enable/delete | `DependentServiceTest` | Coberto | Reutilizado; testes de autorização HTTP não repetem regras de objeto | Coberto |
| Listagem member somente ativos e isolamento de titular; consulta do próprio recurso | MVC mockado e queries parcialmente exercitadas | Parcial | Asserts sobre consultas reais no fluxo de sistema e filtros JPA com dois owners/status | Coberto |
| Administração: leitura de ativos/desabilitados, owner incompatível e inexistência | `AdminDependentServiceTest`, `AdminDependentControllerTest` | Parcial | Desabilitado e id ausente explícitos; nome de teste antigo corrigido de bad request para not found | Coberto |
| Autenticação de todas as operações HTTP existentes | 401 apenas na listagem | Parcial | Matriz 401 dos sete pares método/path, sem chamada aos services | Coberto |
| Authority administrativa específica em listagem e detalhe | Negação das duas rotas e sucesso da listagem | Parcial | Sucesso do detalhe e negação com authority de outro módulo | Coberto |
| Bean Validation create/update | Telefone inválido no update | Parcial | Nome, CPF inválido, data ausente/futura, parentesco ausente e telefone formatado em ambos; ProblemDetail e ausência de chamadas ao service | Coberto |
| Tradução HTTP 404/403/409 e RFC 9457 | Inatividade e conflito de CPF | Parcial | Dependent ausente, ownership, owner ausente e exclusão repetida; envelope de erros e media type | Coberto |
| OpenAPI: DTOs, listas, respostas de erro e permission | `DependentsOpenApiIntegrationTest` | Coberto | Preservado, sem criar catálogo HTTP concorrente | Coberto |
| CPF operacional único, inclusive DISABLED; tradução da constraint real | `DependentRepositoryAdapterTest` | Coberto | Preservado; concorrência de insert protegida pela constraint, testada sem pré-checagem | Coberto |
| Falhas de integridade não relacionadas a CPF | Sem caso explícito | Ausente | `DependentRepositoryExceptionTranslationTest`: causa sem Hibernate, constraint desconhecida ou nula não viram conflito de CPF | Coberto |
| Exclusão: lock, histórico e remoção operacional | `DependentRepositoryAdapterTest`, `DependentSystemFlowTest` | Parcial | Snapshot completo de DISABLED, liberação/reuso de CPF, dois históricos com mesmo CPF e exclusão repetida | Coberto |
| Rollback quando histórico falha | `DependentSystemFlowTest` | Coberto | Mantido fora de transação de teste; acrescentada garantia de um único histórico após falha | Coberto |
| `DependentsQuery`: nulos/vazios e estado/ownership/lote reais | Consumidor Health usava mocks do contrato | Parcial | `DependentsQueryServiceTest` para guardas; fluxo real cobre ACTIVE → INACTIVE → NOT_FOUND e lote/ownership | Coberto |
| Health: rejeição de owner inativo, ownership e retenção após remoção | `MedicalProfileOwnerValidationServiceTest`, `MedicalProfileOwnerStatusAdapterTest` | Coberto no consumidor | Reutilizados; novo fluxo testa a ligação real com Dependents, sem copiar regras médicas | Coberto |
| Determinismo de `DependentSystemFlowTest` | Clock real com fixture fixa | Parcial | Clock próprio do contexto, inicializado antes do bootstrap, avançado explicitamente; timestamps exatos até o histórico; banco H2 exclusivo para impedir vazamento de fixtures a Identity | Coberto |
| Endpoints enable/disable, escrita administrativa, histórico HTTP e paginação | Não existem no contrato atual | Não aplicável | Nenhum endpoint ou teste de feature planejada adicionado | Não aplicável |
| Exclusão automática de perfil médico e unicidade transacional de CPF entre módulos | Não implementadas como garantias de Dependents | Não aplicável | Sem redesenho de lifecycle Health ou constraint cross-table; limites descritos nas evidências | Não aplicável |

## Não duplicação e limites

Os antigos `createBlocksCpfUsedByActiveDependent` e `createBlocksCpfUsedByDisabledDependent`
usavam o mesmo stub booleano e executavam o mesmo helper, sem diferença de estado real.
Foram consolidados em `createBlocksCpfReservedByOperationalDependent`; a reserva em DISABLED
continua comprovada com JPA real. O fluxo de sistema existente foi ampliado, não recriado.

Testes de camadas diferentes têm responsabilidades distintas: service valida ownership,
MVC valida tradução/entrada, segurança aplica filtros/authorities, JPA prova constraints,
sistema prova transações e boundaries. Nenhum teste foi criado apenas para elevar percentual.

A corrida perdedora de exclusão é caracterizada pelo estado determinístico de registro já
removido, sem simular dois threads. O lock pessimista foi auditado no repository JPA e é
exercitado pelo delete real. A suíte H2 não comprova scheduling/isolamento do MySQL em produção.
Resultados dos diagnósticos e comandos reproduzíveis: [evidências](dependents-test-evidence.md).
