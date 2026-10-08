# Auditoria final da suíte Health — BACK-254

## Referências verificadas em 07/10/2026

- Jira: BACK-254 e BACK-255 a BACK-261 consultadas diretamente nesta revisão.
- Develop remota integrada: `bd2296ea88268423eb3fef0e403afebb36bccd71`.
- HealthTests remota inicial: `6c50895d5a90f77d0574a6ecdb5fc0b0361dd6f7`.
- Merge preservando histórico: `18e537d`.
- Commit de código/testes validado: `4057237f4535165ab0475c606bc5cda41bf5b2eb`.
- O commit documental posterior contém este relatório; não modifica a árvore Java/POM validada.

A HealthTests original tem dois commits exclusivos e acrescenta cinco classes
Health (14 → 19), sem alteração de produção. A develop atual acrescenta a suíte
Billing da BACK-437. O merge apresentou conflito somente no POM: foram mantidos
os perfis independentes health-mutation e billing-mutation, sem substituir os
testes ou a configuração recentes da develop.

O baseline foi executado novamente a partir de `git archive origin/develop`
em diretório isolado. Os números de 06/10/2026 do relatório anterior são históricos
e não constituem evidência desta revisão.

## Matriz produção → cobertura existente → gap e incremento

Os nomes abaixo identificam classes em `src/test/java`; exportações,
dependentes e publicações possuem testes adicionais em seus próprios módulos.

| Produção / comportamento vigente | Cobertura preexistente | Incremento HealthTests | Lacuna residual / limite |
| --- | --- | --- | --- |
| `MedicalProfileData`: textos, limites, telefone, UNKNOWN | `MedicalProfileDataTest`; `MedicalProfileNormalizationConsistencyTest` | Sem gap relevante de normalização; mantidos testes de limites exatos e excedidos, branco/null e telefone de 10/11 dígitos. | Sem lacuna comportamental relevante identificada nesta revisão. |
| `MedicalProfile`: owner, identidade, datas e atualização atômica | `MedicalProfileDomainUnitTest` | `MedicalProfileInvariantTest`: IDs zero/negativos, owner inválido na reconstituição, datas ausentes/regressivas, igualdade temporal permitida, falha em campo tardio preserva estado e substituição limpa opcionais. | Sem lacuna comportamental relevante identificada nesta revisão. |
| Operações member e owner ativo | `MedicalProfileOwnerValidationServiceTest`; `MedicalProfileDeleteServiceTest`; normalização | `MedicalProfileAccessBoundaryTest`: bloquear escrita/exclusão própria para owner inativo/inexistente; leitura e upsert bem-sucedidos de dependente usam identidade e lock corretos. | Sem lacuna comportamental relevante identificada nesta revisão. |
| Operações admin e validação de identificadores | Owner validation, delete e normalização | Access boundary: IDs inválidos em leitura/upsert/delete, actor da exclusão inválido, ownerType ausente, leituras ativas por ID/owner e distinção entre ficha ausente e owner ausente/inativo. | Sem lacuna comportamental relevante identificada nesta revisão. |
| Ownership de dependente via contrato público | Owner validation; `DependentSystemFlowTest` | `HealthHttpIntegrationTest`: GET/PUT/DELETE alheios negados sem alteração, dependente inativo/inexistente, fluxo autorizado com integração real de Identity/Dependents; somente autenticação é substituída no teste. | Sem lacuna comportamental relevante identificada nesta revisão. |
| CRUD HTTP member: leitura e PUT integral, exclusão | OpenAPI; serviços; integração indireta de dependentes | Health HTTP: criar/substituir/ler/excluir titular e dependente, principal determina owner, query com userId alheio não muda o alvo, actor do histórico é o principal. | Sem lacuna comportamental relevante identificada nesta revisão. |
| Segurança HTTP das 13 operações operacionais | OpenAPI descrevia parte das permissions; sem execução dos controllers no JaCoCo inicial | Health HTTP: 401 em todas; 403 para cada operação admin com todas as outras authorities de Health; authority correta permite execução real. | Sem lacuna comportamental relevante identificada nesta revisão. |
| Listagem administrativa filtrada por owner ativo | Owner validation: totais exatos, lote de 100, múltiplas páginas, vazio, sem N+1; repository adapter | Health HTTP: envelope público, offset, limite global 50, remoção de owners inativos e ausência de dados clínicos no resumo. Não duplicada lógica de paginação já coberta em serviço. | Sem lacuna comportamental relevante identificada nesta revisão. |
| Persistência JPA: unicidade `(ownerType, ownerId)` | `MedicalProfileJpaIntegrationTest`; `MedicalProfileRepositoryAdapterTest` | Incremento JPA: mesmo ID numérico em USER/DEPENDENT não colide; projeção de cobertura filtra tipo e IDs solicitados. | Sem lacuna comportamental relevante identificada nesta revisão. |
| Exclusão com histórico na mesma transação | Repository adapter: histórico, exclusão, ficha recriada, dupla exclusão | `MedicalProfileDeleteRollbackTest`: falha no flush após histórico e delete reverte ambas as operações em banco real. | Sem lacuna comportamental relevante identificada nesta revisão. |
| Tradução de falhas de infraestrutura | `MedicalProfileRepositoryExceptionTranslationTest`: unicidade, lock Spring na leitura, transient, erro genérico; handler | Incremento no teste existente: JPA lock/timeout/optimistic e PersistenceException em save/read, mais falhas Spring no saveAndFlush; causa interna não vira mensagem pública. | Sem lacuna comportamental relevante identificada nesta revisão. |
| Concorrência de criação e atualização | `MedicalProfileConcurrencyIntegrationTest`: duas criações → uma linha/um conflito; member/admin serializados por lock | Preservada cobertura real de banco e sincronização. H2 valida o contrato exercitado; não equivale a prova de comportamento de todos os providers de banco. | Revisão final acrescenta exclusão simultânea com um snapshot; H2 não prova todos os providers MySQL. |
| Consulta intermodular de existência e cobertura | Integrações de dependentes/publicações; cobertura JPA indireta | `MedicalProfilePublicQueriesTest`: ambos os tipos, ausência de argumentos sem consulta, lote vazio, máximo de 500 e rejeição de 501; somente IDs retornados. | Sem lacuna comportamental relevante identificada nesta revisão. |
| Consulta de emergência | `EmergencyMedicalProfileQueryServiceTest`; testes de eventos/publicações | Preservados bloqueio de owner inativo, ficha ausente e resultado público; nenhuma API HTTP de emergência inventada. | Sem lacuna comportamental relevante identificada nesta revisão. |
| Validação HTTP e RFC 9457 | `MedicalProfileExceptionHandlerTest`; OpenAPI | Health HTTP: JSON inválido, enum inválido, telefone e tamanho inválidos, 400/401/403/404/409 com problem+json, status/title/detail/code/timestamp, sem eco do conteúdo clínico. `type` é opcional no envelope vigente. | type omitido equivale a about:blank ([RFC 9457 §3.1.1](https://www.rfc-editor.org/rfc/rfc9457.html#section-3.1.1)); indisponibilidade de ownership não é emitida pelo adapter atual. |
| Contratos OpenAPI | `HealthOpenApiIntegrationTest` | Ampliado para permissions restantes e schemas sem dados clínicos. Assertions de parâmetros page/size/sort agora exigem resultado não vazio; antes uma lista vazia também passaria. | Sem lacuna comportamental relevante identificada nesta revisão. |
| Proteção de dados em respostas/logging | `MedicalProfileAdministrativeResponseTest`; exception translation/handler; `SystemRequestLoggingFilterTest` | Reutilizados testes de redaction. Health HTTP usa marcadores sintéticos, desliga impressão MockMvc e SQL/binds e verifica resumo/mutação/erros sem marcador clínico. Evidência contém apenas nomes, contagens e métricas. | Revisão final inclui tipo sanguíneo na asserção de sigilo; somente fixtures sintéticas. |
| Exportações atuais e consultas JPA de exportação | `ExportHttpIntegrationTest`: CSV/PDF, 401/403, filtros, household, histórico separado, isolamento e auditoria sem clínica; contratos OpenAPI de exports | Health HTTP completa rejeição de IDs não positivos, seletores incompatíveis, household sem titular e owners inexistentes. Preservada cobertura transversal. Exportações não têm paginação HTTP; não duplicada suíte de downloads no módulo Health. | Alternativas defensivas e limite interno de 100 mil registros não recebem testes artificiais. |

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

## Correções da revisão independente

- POM: integração semântica preserva os perfis PIT Health e Billing e toda a suíte BACK-437.
- `MedicalProfileConcurrencyIntegrationTest`: duas exclusões concorrentes após ambas as transações lerem a ficha; exige um sucesso, uma `MedicalProfileAlreadyDeletedException`, remoção operacional e exatamente um histórico com ator, data e conteúdo esperados. Mantém intacto o DEPENDENT com mesmo ownerId. Usa latches e futures limitados, sem sleep novo, e aguarda encerramento do executor antes da limpeza. SQL desabilitado nesse teste para não imprimir campos clínicos.
- `HealthHttpIntegrationTest`: 403 administrativo e operações sobre dependente inativo verificam ficha preservada e nenhum histórico, antes de testar o caminho autorizado. Isso elimina falsos positivos em implementações que mutassem estado antes de negar acesso.
- `MedicalProfileInvariantTest`: comparação recursiva do agregado completo após atualização inválida, inclusive falha no último campo, em vez de conferir somente alergia e updatedAt.
- `MedicalProfileExceptionHandlerTest`: exige HTTP 409 e código MEDICAL_PROFILE_ALREADY_DELETED para o outcome da exclusão concorrente reproduzida, com status do corpo coerente, título, detalhe e timestamp.
- `MedicalProfileDataTest`: sigilo inclui tipo sanguíneo conhecido, além dos marcadores de texto e telefone.

Não foi necessário alterar produção, excluir testes/classes de cobertura ou enfraquecer regras. As adições originais fecham gaps de HTTP, queries públicas, invariantes, tradução de exceções e rollback. Esta revisão acrescenta dois cenários além dos 66 originais: concorrência de exclusão e seu mapeamento de erro HTTP; as demais mudanças reforçam testes existentes. Contagens finais são extraídas dos XML abaixo.

## Conformidade com os critérios Jira

| Critério de aceite BACK-254 | Estado | Evidência |
| --- | --- | --- |
| Matriz baseada na develop atual | ATENDIDO | SHA remoto, comparação Git e matriz acima; baseline isolado recém-executado. |
| Rotas/regras antigas inexistentes não aplicáveis | ATENDIDO | Seção de não aplicáveis; nenhum POST/PATCH ou paginação inventados. |
| Nenhum teste duplicado sem justificativa | ATENDIDO | Suíte preexistente preservada; novos testes de domínio/serviço/HTTP/JPA exercitam fronteiras diferentes; revisão acrescenta só corrida de exclusão e reforça asserções. |
| Gaps relevantes de domínio, serviço, persistência, HTTP e segurança | ATENDIDO | Matriz e testes de controllers reais, normalização, ownership, histórico e rollback. |
| Ownership e acesso negado auditados | ATENDIDO | GET/PUT/DELETE de dependente alheio, owners inativos/inexistentes, authorities específicas e estado preservado. |
| Concorrência/determinismo preservados | ATENDIDO | Criação e atualização preexistentes, exclusão concorrente nova; clocks fixos, latches, futures e limpeza transacional. Limite: integração H2. |
| JaCoCo analisado e lacunas tratadas/justificadas | ATENDIDO | Baseline e final novos; análise por classe/método abaixo. |
| PIT aplicado e sobreviventes relevantes analisados | ATENDIDO | mutations.xml final inspecionado; análise individual abaixo. |
| test e verify verdes | ATENDIDO | clean test e clean verify finais com BUILD SUCCESS, zero failures/errors. |
| PR/evidência distingue cobertura preexistente de incremento | ATENDIDO | Este relatório e descrição técnica do PR. |

BACK-255: matriz/baseline atualizados. BACK-256: gaps e testes no nível correto.
BACK-260: PIT final e sobreviventes analisados. BACK-261: árvore final, Maven e evidências consolidados. BACK-257/258/259 já estão concluídas por absorção de
escopo, conforme o Jira; não representam novas obrigações independentes. Nenhum
status Jira foi alterado automaticamente.

## Limitações e pendências externas ao escopo

- Banco real de teste é H2 em modo MySQL: valida transações, locks e constraints exercitados, sem alegar homologação específica do MySQL de produção.
- O teste preexistente de serialização member/admin usa sleep de 200 ms para observar espera de lock, além de latches/futures e estado final. Foi preservado; o teste novo de exclusão não depende de sleep.
- Owners e Health não participam de transação distribuída de lifecycle, conforme decisão arquitetural existente.
- O handler de ownership indisponível não é emitido pelo adapter atual; não foi fabricado cenário de negócio para acioná-lo.
- Guards null de mappers, metamodelos gerados e schema auxiliar legado não justificam testes artificiais. Cobertura por classe/método e branches residuais será registrada abaixo.
- Proposta de issue separada para Billing/BUILD: corrigir `billing-mutation` para usar `reportsDirectory` em lugar de `outputDirectory`. O descritor `META-INF/maven/plugin.xml` do PIT 1.19.0 instalado declara reportsDirectory, mas não outputDirectory. Configuração herdada da develop foi preservada; essa pendência não altera o perfil Health.
- Evidências contêm somente fixtures sintéticas, identificadores técnicos, nomes de testes e contagens. Nenhum dado clínico real ou credencial foi incluído na documentação ou PR.
## Resultados reproduzidos nesta revisão

Árvore de código/testes: `4057237f4535165ab0475c606bc5cda41bf5b2eb`.
Maven Wrapper; Windows; compilação release 17. Baseline executado com Java 25.0.1;
validação final test/verify/PIT com Microsoft OpenJDK 17.0.20. O CI usa Temurin 17.

| Execução | Resultado | Testes | Falhas | Erros | Skipped |
| --- | --- | ---: | ---: | ---: | ---: |
| develop remota isolada: clean verify | BUILD SUCCESS | 1.389 | 0 | 0 | 1 |
| árvore final: mvnw.cmd clean test | BUILD SUCCESS | 1.457 | 0 | 0 | 1 |
| árvore final: mvnw.cmd clean verify | BUILD SUCCESS | 1.457 | 0 | 0 | 1 |

Skipped individual: `LocalFileStorageTest.shouldRejectSymbolicLinkInsteadOfFollowingItOutsideRoot`
foi abortado por `Assumptions.assumeTrue`: links simbólicos indisponíveis no
ambiente Windows. O mesmo cenário já era skipped no baseline; nenhum teste
Health foi ignorado. A execução final inclui os testes Billing recém-integrados
sem exclusões ou regressões. Health: 65 → 133 cenários, 14 → 19 classes.

A primeira execução integrada e as rodadas intermediárias ficaram em logs
separados. A evidência definitiva é a rodada acima, após o teste do handler;
os resultados anteriores de 1.455/1.456 testes não são usados como conclusão.


## JaCoCo final por classe e método

| Classe | Linhas baseline | Linhas finais | Branches finais |
| --- | ---: | ---: | ---: |
| MedicalProfileData | 28/29 | 29/29 | 16/16 |
| MedicalProfile | 49/56 | 56/56 | 22/22 |
| MedicalProfileOwnerStatusAdapter | 20/22 | 20/22 | 18/24 |
| MedicalProfileOwner | 3/3 | 3/3 | 0/0 |
| EmergencyMedicalProfileQuery$Profile | 1/1 | 1/1 | 0/0 |
| MedicalProfileExportService | 39/42 | 41/42 | 82/94 |
| AdminMedicalProfileService | 80/93 | 93/93 | 38/38 |
| MedicalProfileService | 61/66 | 66/66 | 11/12 |
| MedicalProfileExceptionHandler | 13/21 | 19/21 | 0/0 |
| MedicalProfileCoverageJpaQuery | 2/2 | 2/2 | 0/0 |
| MedicalProfileExportJpaQuery | 8/8 | 8/8 | 13/14 |
| MedicalProfileRepositoryAdapter | 66/97 | 97/97 | 8/10 |
| InvalidMedicalProfileException | 2/2 | 2/2 | 0/0 |
| MedicalProfileAlreadyDeletedException | 2/2 | 2/2 | 0/0 |
| MedicalProfileHistoryMapper | 22/23 | 22/23 | 1/2 |
| MedicalProfileMapper | 38/40 | 38/40 | 2/4 |
| MedicalProfileOwnerType | 3/3 | 3/3 | 0/0 |
| BloodType | 10/10 | 10/10 | 0/0 |
| MedicalProfileConflictException | 4/4 | 4/4 | 0/0 |
| MedicalProfileOwnerInactiveException | 2/2 | 2/2 | 0/0 |
| MedicalProfileAccessDeniedException | 2/2 | 2/2 | 0/0 |
| DependentOwnershipValidationUnavailableException | 0/2 | 0/2 | 0/0 |
| InvalidMedicalProfileDataException | 2/2 | 2/2 | 0/0 |
| MedicalProfileOwnerNotFoundException | 2/2 | 2/2 | 0/0 |
| MedicalProfilePersistenceUnavailableException | 2/2 | 2/2 | 0/0 |
| MedicalProfileNotFoundException | 0/2 | 2/2 | 0/0 |
| MedicalProfilePersistenceException | 2/2 | 2/2 | 0/0 |
| UpsertMedicalProfileCommand | 2/2 | 2/2 | 0/0 |
| MedicalProfileResponse | 2/19 | 19/19 | 0/0 |
| MedicalProfileSummaryResponse | 0/6 | 6/6 | 0/0 |
| MedicalProfileRequest | 3/3 | 3/3 | 0/0 |
| MedicalProfilePageResponseSchema | 0/1 | 0/1 | 0/0 |
| MedicalProfileMutationResponse | 0/6 | 6/6 | 0/0 |
| AdminMedicalProfileController | 0/19 | 19/19 | 0/0 |
| AdminMedicalProfileExportController | 2/2 | 2/2 | 0/0 |
| HealthMedicalProfileQueryService | 0/6 | 6/6 | 4/4 |
| MedicalProfileCoverageQueryService | 3/3 | 3/3 | 4/4 |
| EmergencyMedicalProfileQueryService | 5/5 | 5/5 | 2/2 |
| MedicalProfileEntity_ | 0/1 | 0/1 | 0/0 |
| MedicalProfileHistoryEntity_ | 0/1 | 0/1 | 0/0 |
| MedicalProfileExportQuery$Entry | 1/1 | 1/1 | 0/0 |
| DependentsOwnershipAdapter | 1/1 | 1/1 | 0/0 |
| MedicalProfileOwnerStatus | 4/4 | 4/4 | 0/0 |
| MedicalProfileController | 0/20 | 20/20 | 0/0 |

Todos os métodos Health foram inspecionados no XML; inventário completo em
`target/health-audit/health-methods-final.csv`. Métodos com linhas ou branches residuais:

| Método | Linhas cobertas/total | Branches cobertos/total |
| --- | ---: | ---: |
| MedicalProfileOwnerStatusAdapter.getStatus | 4/5 | 5/8 |
| MedicalProfileOwnerStatusAdapter.findActiveOwnerIds | 6/7 | 5/8 |
| MedicalProfileExportService.export | 13/13 | 43/44 |
| MedicalProfileExportService.household | 12/13 | 5/8 |
| MedicalProfileExportService.householdRow | 4/4 | 14/16 |
| MedicalProfileExportService.lambda$household$3 | 0/1 | 0/0 |
| MedicalProfileExportService.lambda$export$2 | 11/11 | 20/26 |
| MedicalProfileService.validateAccessibleOwner | 8/8 | 7/8 |
| MedicalProfileExceptionHandler.handleDependentOwnershipValidationUnavailable | 0/2 | 0/0 |
| MedicalProfileExportJpaQuery.read | 8/8 | 13/14 |
| MedicalProfileRepositoryAdapter.isOwnerUniqueConstraintViolation | 9/9 | 6/8 |
| MedicalProfileHistoryMapper.toHistoryEntity | 21/22 | 1/2 |
| MedicalProfileMapper.toEntity | 19/20 | 1/2 |
| MedicalProfileMapper.toDomain | 18/19 | 1/2 |
| DependentOwnershipValidationUnavailableException.&lt;init&gt; | 0/2 | 0/0 |
| MedicalProfilePageResponseSchema.&lt;init&gt; | 0/1 | 0/0 |
| MedicalProfileEntity_.&lt;init&gt; | 0/1 | 0/0 |
| MedicalProfileHistoryEntity_.&lt;init&gt; | 0/1 | 0/0 |

Health final: **627/640 linhas (97,97%)** e **221/246 branches (89,84%)**.
Baseline remoto desta revisão: **486/640 linhas (75,94%)** e **192/246 branches (78,05%)**.
O ganho é de 141 linhas exercitadas. O resultado histórico de 625/640 corresponde
à árvore anterior ao teste do handler: as duas linhas adicionais são o mapeamento
de exclusão concorrente para 409. Não houve redução de cobertura, exclusão de
classes ou alteração do denominador. A diferença entre o antigo baseline de
488 linhas e o baseline remoto recém-executado não foi usada para calcular ganho.

Justificativas dos métodos residuais:

- OwnerStatusAdapter: argumentos null/coleções vazias nas fronteiras defensivas; chamadas de serviço validam IDs e tipo antes da integração. Estados ativo/inativo/inexistente são testados.
- MedicalProfileService.validateAccessibleOwner: ramo defensivo para ownerId null, inacessível por principal/path válidos; IDs não positivos são cobertos.
- ExportService/export JPA: alternativas de cursor, combinações de seleção e ausência/presença de ficha no household. A lambda de indexação de fichas de dependentes no household permanece sem exercício direto; a consulta de dependente e a exportação individual, o isolamento de household e os formatos CSV/PDF são cobertos pela suíte transversal. Não foi criada massa de 100 mil registros ou duplicada toda a suíte de exportação para elevar percentual.
- RepositoryAdapter: metadados de constraint ausentes/diferentes; há testes de violação owner, fallback genérico, traduções Spring/JPA e unicidade física.
- Mappers: retorno null defensivo; os fluxos reais enviam entidades/agregados válidos.
- Handler/exceção de indisponibilidade de ownership: caminho legado não emitido pelo adapter vigente.
- Schema auxiliar legado e construtores dos metamodelos JPA gerados: sem regra comportamental a acrescentar.

## PIT final: XML, sobrevivente e timeout

Comando: `mvnw.cmd -Phealth-mutation test-compile org.pitest:pitest-maven:mutationCoverage`.
BUILD SUCCESS em 07/10/2026, Java 17, sobre o commit de código/testes citado.
Escopo: MedicalProfile, MedicalProfileData, MedicalProfileService e
AdminMedicalProfileService; 263/263 linhas dessas quatro classes exercitadas.

| Estado no mutations.xml | Quantidade |
| --- | ---: |
| Gerados | 110 |
| KILLED | 108 |
| TIMED_OUT | 1 |
| SURVIVED | 1 |
| NO_COVERAGE | 0 |

O resumo de console agrega timeout como detecção: 109/110 (99,09%). A contagem
acima foi extraída do XML, não inferida do console.

- **SURVIVED** — `MedicalProfileData.toString`, linha 119, substitui retorno por `""`. O contrato é não expor campos médicos: representação redigida e vazia satisfazem esse contrato. O teste confere todos os marcadores de texto, telefone e tipo sanguíneo conhecido. Não há exigência de texto literal REDACTED e não foi criada uma para matar esse mutante.
- **TIMED_OUT** — `AdminMedicalProfileService.listMedicalProfiles`, linha 102, `negated conditional` sobre `sourcePage.hasNext()`. Nos cenários preexistentes cujo primeiro lote é a última página, false vira true; páginas vazias subsequentes também mantêm o loop. `pagesFrom` retorna páginas vazias após o fim; não há condição adicional de saída. A suíte normal termina e valida conteúdo/totais/lotes. A execução mutada registrou `Minion exited abnormally due to TIMED_OUT` às 21:04:25, e mutations.xml classifica precisamente esse mutante como TIMED_OUT. O XML não atribui killingTest para timeout; não se inventa nome individual de teste. A detecção pela execução da suíte é a evidência, sem sleep/retry acrescentado para acomodá-lo.

A primeira tentativa PIT restrita falhou por AccessDeniedException no cache
Maven, antes de executar mutações. Foi repetida com acesso ao cache e obteve
BUILD SUCCESS; essa falha ambiental não foi mascarada como teste verde.

## Evidências e decisão

Artefatos locais ignorados pelo Git em `target/health-audit/`:

- `baseline-verify.log`, `jacoco-baseline.xml` e `health-methods-baseline.csv`;
- `final-jdk17-clean-test.log` e `final-jdk17-clean-verify.log`;
- `jacoco-final.xml`, `health-methods-final.csv`, `health-totals.json`, `test-totals.json`;
- `final-commit-pit.log` e `mutations-final.xml`.

Os relatórios nativos continuam em `target/site/jacoco/` e
`target/pit-reports/health/`. São reproduzíveis pelos comandos acima; este
relatório versiona resultados, limitações e justificativas sem payloads clínicos.

BACK-255, BACK-256, BACK-260 e BACK-261 têm evidência técnica para conclusão após
revisão do PR. BACK-257/258/259 já foram concluídas por absorção de escopo. Nenhuma
issue Jira foi fechada e nenhuma aprovação/merge do PR foi realizada.

**Veredito técnico local: APTO PARA MERGE**, condicionado à revisão e aos checks
remotos do PR. O estado remoto será conferido e apresentado na entrega; a
pendência de configuração Billing e a homologação MySQL permanecem separadas.
