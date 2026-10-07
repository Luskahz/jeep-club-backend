# Auditoria da suíte Health

## Baselines e escopo

Referência do pedido: `develop@80ddc7de896fa52a75b67dcc45322c9b3355a5c5`.
Baseline efetivamente disponível para execução: `develop@4b9e76068f51ce1df68689f26133b06a75c0d313`.
Os incrementos de teste foram preservados no commit `b6592c4f` antes da retomada.

A comparação entre a referência e a baseline local mostrou acréscimos de
exportação CSV/PDF, consulta de cobertura em lotes e consulta de emergência.
Os controllers operacionais, o domínio, o adapter e seus testes preexistentes
mantiveram os mesmos contratos. Esta auditoria inclui os acréscimos reais da
develop local e testes externos ao diretório Health que já exercitam suas
integrações. Não houve alteração de produção para acomodar testes.

## Matriz produção → cobertura existente → gap e incremento

Os nomes abaixo identificam classes em `src/test/java`; exportações,
dependentes e publicações possuem testes adicionais em seus próprios módulos.

| Produção / comportamento vigente | Cobertura preexistente | Gap comprovado e tratamento |
| --- | --- | --- |
| `MedicalProfileData`: textos, limites, telefone, UNKNOWN | `MedicalProfileDataTest`; `MedicalProfileNormalizationConsistencyTest` | Sem gap relevante de normalização; mantidos testes de limites exatos e excedidos, branco/null e telefone de 10/11 dígitos. |
| `MedicalProfile`: owner, identidade, datas e atualização atômica | `MedicalProfileDomainUnitTest` | `MedicalProfileInvariantTest`: IDs zero/negativos, owner inválido na reconstituição, datas ausentes/regressivas, igualdade temporal permitida, falha em campo tardio preserva estado e substituição limpa opcionais. |
| Operações member e owner ativo | `MedicalProfileOwnerValidationServiceTest`; `MedicalProfileDeleteServiceTest`; normalização | `MedicalProfileAccessBoundaryTest`: bloquear escrita/exclusão própria para owner inativo/inexistente; leitura e upsert bem-sucedidos de dependente usam identidade e lock corretos. |
| Operações admin e validação de identificadores | Owner validation, delete e normalização | Access boundary: IDs inválidos em leitura/upsert/delete, actor da exclusão inválido, ownerType ausente, leituras ativas por ID/owner e distinção entre ficha ausente e owner ausente/inativo. |
| Ownership de dependente via contrato público | Owner validation; `DependentSystemFlowTest` | `HealthHttpIntegrationTest`: GET/PUT/DELETE alheios negados sem alteração, dependente inativo/inexistente, fluxo autorizado com integração real de Identity/Dependents; somente autenticação é substituída no teste. |
| CRUD HTTP member: leitura e PUT integral, exclusão | OpenAPI; serviços; integração indireta de dependentes | Health HTTP: criar/substituir/ler/excluir titular e dependente, principal determina owner, query com userId alheio não muda o alvo, actor do histórico é o principal. |
| Segurança HTTP das 13 operações operacionais | OpenAPI descrevia parte das permissions; sem execução dos controllers no JaCoCo inicial | Health HTTP: 401 em todas; 403 para cada operação admin com todas as outras authorities de Health; authority correta permite execução real. |
| Listagem administrativa filtrada por owner ativo | Owner validation: totais exatos, lote de 100, múltiplas páginas, vazio, sem N+1; repository adapter | Health HTTP: envelope público, offset, limite global 50, remoção de owners inativos e ausência de dados clínicos no resumo. Não duplicada lógica de paginação já coberta em serviço. |
| Persistência JPA: unicidade `(ownerType, ownerId)` | `MedicalProfileJpaIntegrationTest`; `MedicalProfileRepositoryAdapterTest` | Incremento JPA: mesmo ID numérico em USER/DEPENDENT não colide; projeção de cobertura filtra tipo e IDs solicitados. |
| Exclusão com histórico na mesma transação | Repository adapter: histórico, exclusão, ficha recriada, dupla exclusão | `MedicalProfileDeleteRollbackTest`: falha no flush após histórico e delete reverte ambas as operações em banco real. |
| Tradução de falhas de infraestrutura | `MedicalProfileRepositoryExceptionTranslationTest`: unicidade, lock Spring na leitura, transient, erro genérico; handler | Incremento no teste existente: JPA lock/timeout/optimistic e PersistenceException em save/read, mais falhas Spring no saveAndFlush; causa interna não vira mensagem pública. |
| Concorrência de criação e atualização | `MedicalProfileConcurrencyIntegrationTest`: duas criações → uma linha/um conflito; member/admin serializados por lock | Preservada cobertura real de banco e sincronização. H2 valida o contrato exercitado; não equivale a prova de comportamento de todos os providers de banco. |
| Consulta intermodular de existência e cobertura | Integrações de dependentes/publicações; cobertura JPA indireta | `MedicalProfilePublicQueriesTest`: ambos os tipos, ausência de argumentos sem consulta, lote vazio, máximo de 500 e rejeição de 501; somente IDs retornados. |
| Consulta de emergência | `EmergencyMedicalProfileQueryServiceTest`; testes de eventos/publicações | Preservados bloqueio de owner inativo, ficha ausente e resultado público; nenhuma API HTTP de emergência inventada. |
| Validação HTTP e RFC 9457 | `MedicalProfileExceptionHandlerTest`; OpenAPI | Health HTTP: JSON inválido, enum inválido, telefone e tamanho inválidos, 400/401/403/404/409 com problem+json, status/title/detail/code/timestamp, sem eco do conteúdo clínico. `type` é opcional no envelope vigente. |
| Contratos OpenAPI | `HealthOpenApiIntegrationTest` | Ampliado para permissions restantes e schemas sem dados clínicos. Assertions de parâmetros page/size/sort agora exigem resultado não vazio; antes uma lista vazia também passaria. |
| Proteção de dados em respostas/logging | `MedicalProfileAdministrativeResponseTest`; exception translation/handler; `SystemRequestLoggingFilterTest` | Reutilizados testes de redaction. Health HTTP usa marcadores sintéticos, desliga impressão MockMvc e SQL/binds e verifica resumo/mutação/erros sem marcador clínico. Evidência contém apenas nomes, contagens e métricas. |
| Exportações atuais e consultas JPA de exportação | `ExportHttpIntegrationTest`: CSV/PDF, 401/403, filtros, household, histórico separado, isolamento e auditoria sem clínica; contratos OpenAPI de exports | Health HTTP completa rejeição de IDs não positivos, seletores incompatíveis, household sem titular e owners inexistentes. Preservada cobertura transversal. Exportações não têm paginação HTTP; não duplicada suíte de downloads no módulo Health. |

## Não aplicáveis e limites

- Criação separada por POST e edição por PATCH não existem nas rotas de perfil:
  criação/atualização são o PUT efetivamente implementado.
- Não existe leitura/edição member por userId arbitrário; `/me` usa o principal.
- Paginação member, de perfil individual e de exportações não existe. A
  paginação HTTP auditada pertence somente à listagem administrativa.
- Não existe endpoint operacional de leitura do histórico clínico; existe
  exportação administrativa de histórico, coberta pela suíte de exports.
- `DependentOwnershipValidationUnavailableException` e seu handler existem,
  mas o adapter atual delega diretamente ao contrato de Dependents e não cria
  essa exceção. Não foi inventado um cenário de produção que a emita.
- Branches defensivos de mappers para entrada null e classes de metamodelo
  geradas não justificam testes artificiais. O serviço só envia perfis válidos
  ao mapper; essas lacunas são registradas, não perseguidas por percentual.

## Execução e resultados

A suíte original executada antes dos incrementos passou em `mvnw verify`:
1.167 testes, nenhuma falha/erro, um teste ignorado por indisponibilidade de
links simbólicos no ambiente Windows. A rodada focada dos incrementos passou
com 70 testes. Esses 70 incluem testes preexistentes nas classes selecionadas,
portanto não representam 70 testes novos.

PIT usa o perfil `health-mutation` no POM, selecionando `MedicalProfile`,
`MedicalProfileData`, `MedicalProfileService` e `AdminMedicalProfileService`.
O conjunto de testes inclui domínio/serviços e os testes de redaction existentes
em `health.api.http.dto`; testes Spring/JPA/HTTP são verificados pela suíte
normal. A revisão do PIT mostrou que o teste de redaction existente cobria
request/command/response, mas não `MedicalProfileData`. Foi acrescentada a
verificação de sigilo do valor de domínio em `MedicalProfileDataTest`, sem
duplicar os testes dos DTOs.

Comandos reprodutíveis (no Windows, usar `mvnw.cmd`):

```text
./mvnw test
./mvnw verify
./mvnw -Phealth-mutation test-compile org.pitest:pitest-maven:mutationCoverage
```

Os logs/XML da auditoria ficam em `target/health-audit`, o JaCoCo em
`target/site/jacoco` e o PIT em `target/pit-reports/health`. Artefatos gerados
não são versionados. Resultados e análise de sobreviventes estão registrados
nas seções seguintes.

## PIT final e análise dos sobreviventes

| Resultado | Inicial | Final |
| --- | ---: | ---: |
| Mutações geradas | 110 | 110 |
| KILLED no XML | 88 | 108 |
| TIMED_OUT | 1 | 1 |
| SURVIVED | 14 | 1 |
| NO_COVERAGE | 7 | 0 |

O resumo do PIT conta timeout entre as detecções: 109/110 (99%) no final,
com 263/263 linhas das classes selecionadas exercitadas. Não há sobreviventes
relevantes de validação de owner, datas, acesso, identidade ou escrita.

- `MedicalProfileData.toString`, linha 119: substituir a representação
  redigida por `""` sobrevive ao teste de sigilo. Ambos os resultados ocultam
  todos os campos. O contrato não exige a mensagem literal `[REDACTED]`;
  exigir essa string apenas para aumentar o score seria um caso artificial.
- `AdminMedicalProfileService.listMedicalProfiles`, linha 102: inverter
  `hasNext()` torna infinita a varredura de páginas no teste. O minion termina
  por timeout, detectando a mutação. O cenário original é finito e permanece
  coberto pelos testes existentes de múltiplas páginas, totais e consultas em
  lote. Não foi acrescentado sleep ou retry para acomodar esse mutante.

## JaCoCo final e lacunas justificadas

Contagens de linhas exercitadas/total, obtidas dos XML inicial e final do
JaCoCo, com a suíte completa e suas integrações transversais:

| Componente | Inicial | Final |
| --- | ---: | ---: |
| MedicalProfile | 49/56 | 56/56 |
| MedicalProfileData | 28/29 | 29/29 |
| MedicalProfileService | 61/66 | 66/66 |
| AdminMedicalProfileService | 80/93 | 93/93 |
| MedicalProfileController | 0/20 | 20/20 |
| AdminMedicalProfileController | 0/19 | 19/19 |
| MedicalProfileRepositoryAdapter | 66/97 | 97/97 |
| MedicalProfileExportService | 39/42 | 41/42 |
| Health inteiro, incluindo tipos gerados/legados | 488/640 | 625/640 |

Branches agregados de Health: 192/246 inicialmente e 221/246 no final.
MedicalProfile, MedicalProfileData e AdminMedicalProfileService exercitam
todos os seus branches. As contagens não constituem uma meta percentual.

Lacunas restantes foram revistas contra o fluxo real:

- Guards null de mappers e do owner adapter, schema auxiliar legado e
  metamodelos JPA gerados permanecem sem cenário artificial. Os serviços
  validam IDs e owners antes da integração; o principal e os parâmetros de
  path fornecem IDs não nulos nas rotas operacionais. A defesa null de
  MedicalProfileService é a sua única ramificação restante.
- No repository adapter, duas ramificações tratam metadados de constraint
  ausentes/diferentes dos nomes configurados. O fallback para falha genérica
  e a unicidade real já são exercitados, além das falhas JPA/Spring no flush.
- O handler de indisponibilidade de ownership não é emitido pelo adapter
  atual. O handler de ficha já excluída tem mapeamento 409 revisado, mas sem
  teste HTTP específico dessa corrida: o desfecho do adapter é coberto por
  `deleteRejectsAnAlreadyDeletedMedicalProfile`, e exclusão HTTP sequencial
  repetida encontra ausência e retorna 404. Isso não é uma nova promessa de
  concorrência de delete; criação/atualização e rollback foram preservados e
  executados em banco real.
- Nas exportações, o incremento fecha rejeição de filtros incompatíveis e
  owners inexistentes. Restam alternativas de streaming/cursor, limite
  interno de varredura e combinações defensivas. Não foram criadas massas
  artificiais de 100 mil fichas para atingir esses branches. A execução real
  dos formatos, isolamento de household, histórico, permissions e auditoria
  segue coberta pelos testes de exportação preexistentes.

## Evidência de conclusão e resumo para revisão

Conclusão em 06/10/2026: `mvnw.cmd test verify` passou com **1.233 testes,
zero falhas, zero erros e um ignorado**. O Surefire executou a suíte na fase
test e reutilizou essa execução na fase verify; empacotamento e relatórios
JaCoCo também concluíram com BUILD SUCCESS. O teste ignorado é o cenário de
links simbólicos de storage, indisponíveis neste ambiente Windows.

Health passou de 65 cenários em 14 classes para 131 cenários em 19 classes:
**66 cenários incrementais**, sem substituição da cobertura preexistente.
Os testes das integrações em outros módulos continuam incluídos na suíte
completa. PIT final: 108 KILLED, um TIMED_OUT e um SURVIVED equivalente para o
contrato de sigilo; nenhum NO_COVERAGE nas quatro classes selecionadas.

Resumo para PR/revisão: a suíte já validava normalização, ownership em
serviços, JPA, concorrência, exclusão com histórico e erros de persistência.
O incremento cobre os controllers reais e suas permissões, validação e
ownership HTTP, invariantes temporais/IDs, rollback de delete, exceções JPA,
consultas públicas e filtros de exportação. Produção e contratos foram
preservados. A matriz acima identifica também hipóteses antigas não aplicáveis
e limitações deliberadas da auditoria.

Evidências geradas: `target/health-audit/baseline-verify.log`,
`jacoco-baseline.xml`, `pit-baseline.xml`, `pit-final.log`, `pit-final.xml`,
`final-test-verify.log` e `jacoco-final.xml`. O POM contém o perfil reproduzível
de PIT; a matriz e este resumo são a evidência versionável, sem payloads
clínicos ou credenciais.
