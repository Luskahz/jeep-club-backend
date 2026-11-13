# BACK-547 — Rodada 6.1

Escopo: corrigir F-R6-01, F-R6-02 e F-R6-03 nos contratos centrais. Esta entrega não conclui BACK-547 nem integra consumidores. Fonte: BACK-547/550/552/556/558/559/599, decisões D01–D15 e delegação técnica do comentário 10453, consultados no Jira. As descrições antigas de planejamento não revogam a autorização atual.

Branch: `feature/back-547-storage-implementation-wave2`. SHA inicial local e remoto confirmado: `aeba5b9949573997d000c217b48e23beab2bc1ea`. As quatro alterações locais da tentativa anterior foram preservadas e completadas no primeiro commit.

## Correções e commits

| Finding | Jira | Correção | Commit |
|---|---|---|---|
| F-R6-01 | BACK-558/552/556/559 | Confirmação física específica, versionada e auditada, após prova de conteúdo, tamanho e SHA-256; legado sem cópia | `f7c451de9730db3c50b34d7cce0b346ba7dd8bd6` e `08b6089a2f7944b1efb596fd1dc25bedb580b06d` |
| F-R6-02 | BACK-559/552 | Ausência física em 404, falha operacional/integridade em 503, resposta sanitizada e no-store | `28a3c2dd9bc6d03649baa50abbccdeea3df743b5` |
| F-R6-03 | BACK-550/591/559 | Sobrecargas com posição opcional, histórico imutável, idempotência e concorrência | `c5de0e33a5d592683dcc45522019fc8b63912001` e `649fe9599812a9c400d68b2bef8262e6bab74138` |

## F-R6-01 — prova física

`MediaCatalogRepository.confirmPhysicalPresence(mediaId, expectedVersion, variantId, verifiedBytes, audit)` é um comando específico. Bloqueia o asset, exige a versão esperada, valida a identidade da variante, o tamanho e o SHA-256 dos bytes contra o catálogo. Altera somente physicalState, updatedAt e versão. `updateAssetMetadata()` continua exclusivo para purpose.

O backfill lê a origem real, verifica conteúdo decodificável dentro dos limites existentes e comprova correspondência com o catálogo antes de confirmar PRESENT. Conteúdo não identificado ou corrompido permanece UNKNOWN. Reexecução exige checksum e tamanho do checkpoint; divergência não é tratada como sucesso. A resolução continua exigindo PRESENT e revalidando o conteúdo em cada leitura autorizada: classificação passada não dispensa IO e checksum atuais.

A mesma transação persiste a alteração, o evento de catálogo `ASSET_METADATA_UPDATED` com variantId e versões e o evento operacional `PHYSICAL_PRESENCE_CONFIRMED`, estado PRESENT e razão CHECKSUM_VERIFIED. Nenhum digest/key é incluído nessa auditoria. Falha da auditoria reverte estado, versão e eventos. A operação idempotente não adiciona eventos repetidos. UNKNOWN/MISSING podem tornar-se PRESENT somente com prova; isso não muda moderação, descarte, hold, owner, uploader ou sujeito.

Checkpoints antigos podem ter MIME `application/octet-stream` e dimensões desconhecidas. Esses valores não contradizem uma imagem efetivamente decodificada e cujo tamanho/hash coincidem; o complemento permite confirmar presença sem inventar ou alterar os metadados antigos. MIME/dimensões conhecidos que contradizem o conteúdo não autorizam confirmação.

`apply → rollback → apply` reutiliza identidades e checkpoint pelo fluxo explícito de rollback de custódia existente. Bloqueio de moderação e hold são preservados. Um descarte lógico independente, sem checkpoint marcado como rollback, continua descartado após apply e continua impedindo associação.

## F-R6-02 — HTTP

| Situação | HTTP |
|---|---:|
| Asset/referência indisponível ou ACL/moderação negada | 404 |
| Arquivo comprovadamente ausente, inclusive blob novo | 404 |
| Erro operacional de provider/filesystem | 503 |
| Conteúdo/tamanho/checksum divergente | 503 |
| Entrada inválida | 400 |
| Orçamento de upload excedido | 429 |

`MediaIntegrityException` distingue divergência física de negativa de autorização. `StorageMediaErrors` trata explicitamente ausência e falhas Storage, conservando detalhes públicos fixos e `Cache-Control: private, no-store`. O provider novo usa atributos do arquivo: NoSuchFile vira ausência; outras falhas de IO permanecem operacionais. Nenhuma resposta de falha contém bytes parciais, key, namespace, path, digest, mensagem técnica ou dados pessoais. A autenticação existente permanece preservada.

ACL/moderação são avaliadas antes de consultar o provider. Os testes HTTP verificam negativa com arquivo existente e ausente e comprovam que `LegacyMediaSource.read` não foi chamado. Os arquivos são fixtures temporárias; nenhuma operação produtiva foi executada.

## F-R6-03 — posição do vínculo

Assinaturas anteriores permanecem e delegam com position=null. Novas assinaturas:

```java
associate(long actor, long mediaId, MediaContext context, String resource, String role, Integer position)
replace(long actor, long oldReferenceId, long newMediaId, MediaContext context, String resource, String role, Integer position)
```

Posição negativa é rejeitada; null e valores não negativos são preservados no vínculo. A idempotência existente compara mídia, contexto, agregado, role e posição, excluindo intervalos encerrados. O lock pessimista do asset serializa retries concorrentes. Nenhuma unicidade global de posição foi adicionada. Um mesmo asset autorizado pode aparecer em posições diferentes sem confundir referências.

O MySQL real revelou um finding adicional comprovado: sob REPEATABLE_READ, a consulta comum de candidatos reutilizava o snapshot aberto antes do lock do asset; quatro retries retornaram quatro IDs diferentes. A consulta agora também usa PESSIMISTIC_WRITE para obter a leitura atual após o lock do asset. O teste isolado MySQL passou a retornar uma referência e um evento. Isso não é alegação de ausência de todos os deadlocks possíveis entre agregados; a ordem de locks e a transação do consumidor continuam sendo parte do contrato.

Reordenação lógica encerra o vínculo anterior e cria outro intervalo; position histórica não é reescrita. Replace preserva motivo REPLACED, ator, posição anterior e timestamp. A transação MANDATORY com o consumidor e a ordem crescente de locks dos assets permanecem. A validação da galeria completa, expectedRevision, número máximo de imagens e posição principal continua pertencendo aos consumidores, que não foram alterados nesta rodada.

## Matriz de regressão

| Requisito | Evidência |
|---|---|
| Backfill real → classificação → referência → GET | `StorageUploadFlowTest.realBackfillFilesystemAndAuthorizedHttpGetPreserveLegacyFile`, com LegacyBackfillAdapter, JPA e filesystem reais e MockMvc |
| UNKNOWN para conteúdo não comprovado; ausência/divergência | `unknownOrCorruptLegacyContentCannotBecomePresent`, `legacyBackfillDetectsChangedSourceWithoutRewritingTheCheckpoint`, `backfillConfirmsPresenceAndResolvesOnlyAuthorizedReference` |
| ACL antes do IO | `backfillConfirmsPresenceAndResolvesOnlyAuthorizedReference` e `StorageHttpFlowTest.legacyHttpResponsesAreSanitizedAndDeniedRequestsNeverReadProvider` |
| Versão, concorrência e rollback de auditoria | `physicalConfirmationChecksDigestVersionAndAuditAtomicity`, `concurrentPhysicalConfirmationsHaveOneVersionedWinner`, `MediaCatalogPersistenceTest.physicalProofUpdatesOnlyStateAndVersionWithAuditableRepresentation` |
| Reaplicação/hold/moderação/descarte | `verifiedBackfillReapplicationPreservesModerationHoldAndIdentity`, `reapplyingCheckpointDoesNotUndoIndependentLogicalDiscard`, `publishedWave3CopiedCheckpointIsReappliedWithoutCreatingAnotherAssetOrCopy` e regressões anteriores de descarte |
| HTTP existente/ausente/corrompido/IO/negativas sem IO | `StorageHttpFlowTest.legacyHttpResponsesAreSanitizedAndDeniedRequestsNeverReadProvider`; `MediaResolutionServiceTest`; `LocalPhysicalBlobStoreTest.missingBlobIsDistinguishedFromCorruptContent` |
| Galeria 0/1/2, mesma mídia em posições diferentes, retry e replace | `orderedGallerySupportsReplacementAndIndependentPositions` e `MediaCatalogPersistenceTest.orderedReferencesKeepDistinctIdentityAndHistoricalPositions` |
| Reordenação e posição histórica | `logicalReorderingKeepsOldPositionAndCreatesNewInterval` |
| Posição negativa e transação obrigatória | `negativePositionAndMissingConsumerTransactionAreRejected` |
| Retry concorrente e auditoria única | `concurrentIdenticalPositionAssociationReturnsOneReferenceAndAudit` |
| Rollback do agregado | `positionedReplacementAndAssociationRollBackTogether` |
| Dedup física com ACL independente | `orderedReferencesDoNotShareAuthorizationThroughPhysicalDeduplication` e testes anteriores de dedup |

## Validação

Java 17.0.18, configurado por JAVA_HOME do processo. Resultados finais e comandos dos perfis constam na seção de evidências abaixo. Não somar reruns como cenários únicos. Os testes de fluxo Storage são herdados pelo StorageBlobMySqlIT; os testes de catálogo são herdados por MediaCatalogMySqlIT e MediaCatalogPostgresIT.

Código final validado: `649fe9599812a9c400d68b2bef8262e6bab74138`, incluindo o complemento `08b6089a`. O commit de documentação posterior não altera o código validado.

| Comando final | Resultado | Log local |
|---|---|---|
| `mvnw.cmd -Dtest=MediaResolutionServiceTest,StorageUploadFlowTest,StorageHttpFlowTest,MediaCatalogPersistenceTest,RetentionPolicyTest,LegacyMediaSourceAdapterTest,LocalPhysicalBlobStoreTest test` | 71 executados: 70 passaram, 1 POSIX ignorado no Windows, zero falhas/erros | `target/r61-affected-local.log` |
| `mvnw.cmd -Pmysql-migrations -DskipUnitTests=true -Dit.test=MediaCatalogMySqlIT,StorageBlobMySqlIT verify` | 57 passaram: catálogo 19, fluxo Storage 38; zero falhas/erros/skips | `target/r61-affected-mysql.log` |
| `mvnw.cmd -Ppublications-postgresql -DskipUnitTests=true -Dit.test=MediaCatalogPostgresIT verify` | 18 passaram; zero falhas/erros/skips | `target/r61-affected-postgres.log` |

MySQL 8.0.46 e PostgreSQL 17.6 foram instanciados por Testcontainers. MySQL aplicou e validou Flyway V1–V23; PostgreSQL validou Hibernate DDL, não a cadeia MySQL de migrations. Os filtros restringiram a execução às classes indicadas. Os oito grupos exigidos na rodada foram executados, além das regressões de provider.

Após cada finding: F-R6-01 teve 31 testes locais aprovados; F-R6-02 teve 42 executados, com um POSIX ignorado; F-R6-03 teve 54 aprovados. A primeira rodada MySQL encontrou quatro referências no retry concorrente; após a correção, `mvnw.cmd -Pmysql-migrations -DskipUnitTests=true -Dit.test=StorageBlobMySqlIT#concurrentIdenticalPositionAssociationReturnsOneReferenceAndAudit verify` passou isoladamente e o perfil restrito completo foi repetido com os 57 testes aprovados acima. Log isolado: `target/r61-mysql-concurrency.log`.

Antes dos bancos, C: tinha aproximadamente 8 GiB livres e o filesystem virtual do Docker reportou 923,2 GiB disponíveis. As imagens já existiam. Nenhum volume/dado existente foi removido para viabilizar os testes. Não há alegação de cobertura global, de aprovação da suíte global MySQL ou de execução POSIX neste host.

As rodadas intermediárias detectaram falhas que foram corrigidas antes da evidência final, incluindo timestamp de reaplicação, checagem de ausência inicialmente aplicada ao caminho de escrita e expectativas antigas de corrupção como AccessDeniedException. Esses resultados intermediários não são usados como aprovação.

## Arquivos alterados

Além deste relatório:

```text
src/main/java/com/jeepclub/backend/storage/api/http/StorageMediaErrors.java
src/main/java/com/jeepclub/backend/storage/core/application/MediaIntegrityException.java
src/main/java/com/jeepclub/backend/storage/core/application/MediaReferenceService.java
src/main/java/com/jeepclub/backend/storage/core/application/MediaResolutionService.java
src/main/java/com/jeepclub/backend/storage/core/repository/MediaCatalogRepository.java
src/main/java/com/jeepclub/backend/storage/infra/blob/LegacyMediaSourceAdapter.java
src/main/java/com/jeepclub/backend/storage/infra/blob/LocalPhysicalBlobStore.java
src/main/java/com/jeepclub/backend/storage/infra/persistence/adapter/LegacyBackfillAdapter.java
src/main/java/com/jeepclub/backend/storage/infra/persistence/adapter/MediaCatalogRepositoryAdapter.java
src/test/java/com/jeepclub/backend/storage/api/http/StorageHttpFlowTest.java
src/test/java/com/jeepclub/backend/storage/core/application/MediaResolutionServiceTest.java
src/test/java/com/jeepclub/backend/storage/infra/blob/LocalPhysicalBlobStoreTest.java
src/test/java/com/jeepclub/backend/storage/infra/persistence/MediaCatalogPersistenceTest.java
src/test/java/com/jeepclub/backend/storage/infra/persistence/StorageUploadFlowTest.java
```

## Compatibilidade e pendências de BACK-547

Nenhuma migration nova; V13–V23 intactas. Eventos usam tabelas e campos já existentes. PostgreSQL é validado com Hibernate DDL; a cadeia Flyway existente é MySQL. Flags `STORAGE_MEDIA_ENABLED` e `STORAGE_LEGACY_BACKFILL_ENABLED` permanecem false por padrão. Os DTOs/URLs e controllers consumidores não mudaram; apenas contratos internos de referência ganharam sobrecargas e o tratamento físico ganhou respostas previsíveis.

Não houve integração de consumidores, alteração de pipeline/codecs/presets, frontend, merge, deploy, VPS, backfill produtivo, cópia/migração/exclusão de arquivos operacionais ou fechamento de tickets. As mutações de arquivos dos testes usam somente fixtures temporárias. O teste GET confirma bytes e mtime preservados após leitura do legado.

A suíte global MySQL anteriormente interrompida permanece pendente. Também permanecem os gates da integração posterior, proteção do GET legado, homologação Linux/POSIX, p95 e ativação produtiva. Os três findings desta rodada não equivalem à conclusão da BACK-547, aprovação de merge ou autorização de ativação.
