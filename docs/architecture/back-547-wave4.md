# BACK-547 — rodada 4: findings e hardening

Base: `774e9437d7c1a4e226f674f658bb3c1684dceae5`. Branch: `feature/back-547-storage-implementation-wave2`. Foram preservados histórico, V13–V21, consumidores e presets. F-W3-01 permanece descartado; não foi criado outro mecanismo de lock/associação para esse finding. Não houve merge, deploy, alteração de VPS, expurgo ou backfill produtivo.

## Correções no fluxo

| Finding | Correção efetiva e evidência |
|---|---|
| F-W3-03 | MediaGovernanceAdapter resolve a última decisão contextual pelo uso `(mediaId, context, consumerId, role)` através do ledger e das referências históricas. R2 do mesmo uso herda o bloqueio de R1. Restore administrativo auditado restaura o uso; outros usos e assets permanecem independentes. `contextualBlockSurvivesReassociationUntilAuthorizedRestoration` passou em H2/MySQL. |
| F-W3-04 | MediaControlEntity.discarded é independente do estado de moderação. Descarte só altera lifecycle; moderação/restauro rejeitam descartados; associação e resolução continuam negadas. V22 migra estados antigos e também reconhece LOGICAL_DISCARD no histórico, preservando tombstones anteriormente sobrescritos pelo bug. `moderationCannotResurrectDiscardedMedia` e upgrade real V21→V23 com dados/auditoria antigos passaram. |
| F-W3-02 | Backfill revalida origem e checksum, bloqueia o checkpoint, confirma a identidade de origem e reativa apenas custódia LEGACY_IMPORT sem owner/uploader nem referências, repondo hold e auditando LEGACY_REAPPLIED. Reutiliza o asset, sem alterar moderação. Arquivo modificado é rejeitado. Reaplicação concorrente e compatibilidade com checkpoint/cópia wave3 passaram. |
| F-W3-05 | LocalPhysicalBlobStore usa atributos POSIX explícitos 0700 no namespace privado e 0600 no staging/arquivo; o rename conserva os atributos. A restrição é aplicada somente a storage-blobs; o provider legado não mudou. Compatibilidade Windows passou. Teste de modos POSIX foi ignorado neste host Windows e deve ser executado em Linux antes da ativação. ACL do root Windows continua sendo responsabilidade da configuração do host e não foi validada em produção. |
| F-W3-06 | legalHold exige justificativa não vazia, até 2000 caracteres, tanto na ativação como na liberação. Ator, data, código e justificativa são persistidos; justificativa fica em internal_note e não aparece no DTO de auditoria. Hold liberado não muda classe histórica, vínculos, descarte ou autorização de expurgo. Eventos antigos não foram reescritos para inventar motivos. |
| F-W3-07 | BlobInspectionAdapter agrega contagens por digest em lote e verifica reservas da página física com uma consulta IN. O teste com estatísticas Hibernate confirma que aumentar a página não aumenta o número de consultas SQL. Inventário físico usa DirectoryStream por cursor, sem reiniciar o scan a cada página, até 100 nomes por página e oito scans abertos. TTL de dois minutos, expiração na próxima interação e fechamento no shutdown limitam os recursos. O resultado é observacional, não um snapshot nem autoridade de descarte. |
| F-W3-08 | Novas importações mantêm provider, namespace, key e bytes originais, sem blobs.store ou cópia. MIME/dimensões são identificados pelo conteúdo quando o reader confirma cabeçalho/formato e limites; conteúdo não identificado conserva application/octet-stream e custódia fail-closed. Cópias/checkpoints wave3 já existentes são verificados e reutilizados, sem apagamento ou nova cópia. Testes de codec, catálogo sem cópia e checkpoint publicado passaram. |
| F-W3-09 | Não foi encontrado rate limiter reutilizável em src/main/java ou configuração aplicável de gateway em docs/.github. Foi criado controle específico de orçamento de custódia no SQL, com locks entre instâncias, sem framework paralelo de rate limiting. Admissão acontece antes da leitura do payload e do processamento. Quotas, frequência e concorrência foram testadas em H2/MySQL; HTTP passou com o controle integrado. |

## Contratos e configuração

O endpoint de legal hold agora requer JSON, sob a mesma permissão granular existente:

`POST /admin/storage/media/{id}/legal-hold`

```json
{"held": true, "justification": "Motivo interno auditável"}
```

`held` e `justification` são obrigatórios. A justificativa não é retornada nos DTOs públicos. Não existe operação HTTP nova para desfazer descarte; somente o fluxo interno de reaplicação do checkpoint pode reativar custódia legada após validar suas invariantes.

Reconciliação mantém paginação SQL por afterDigest/limit. O scan físico é solicitado explicitamente com includeFilesystem=true e continuado com inventoryCursor. A resposta inclui nextBlobCursor e nextInventoryCursor; cursor físico expirado é rejeitado e requer novo inventário explícito. Ausência/repetição de arquivos sob escrita concorrente não autoriza exclusão. Não são retornados nomes físicos ao usuário comum.

Orçamentos padrão, configuráveis sem conceder permissões de negócio:

| Propriedade / ENV | Padrão |
|---|---|
| storage.upload.max-user-bytes / STORAGE_UPLOAD_MAX_USER_BYTES | 512 MiB |
| storage.upload.max-global-bytes / STORAGE_UPLOAD_MAX_GLOBAL_BYTES | 10 GiB |
| storage.upload.max-hourly-attempts / STORAGE_UPLOAD_MAX_HOURLY_ATTEMPTS | 30 tentativas por usuário/hora |
| storage.upload.max-concurrent-per-user / STORAGE_UPLOAD_MAX_CONCURRENT_PER_USER | 2 por usuário, coordenadas no SQL |
| storage.upload.max-retained-uploads-per-user / STORAGE_UPLOAD_MAX_RETAINED_UPLOADS_PER_USER | 1000 uploads contabilizados por usuário |

A reserva prévia é de 20 MiB, o teto agregado já existente do pipeline. Após processar, contabiliza-se a soma lógica das representações antes de iniciar a gravação física, independentemente de reuso por deduplicação. Isso evita uma diferença de quota que revele existência de conteúdo compartilhado. Falha antes dessa etapa libera bytes reservados; falha posterior conserva o custo de custódia. O contador inicial considera objetos/variantes já catalogados. O semáforo existente do processador continua limitando CPU/memória por instância; não foi criado outro processador.

Reservas de processo morto ou término com falha SQL ficam conservadoramente retidas para revisão operacional. Não há expiração que libere orçamento de bytes cujo destino seja desconhecido. Isso pode bloquear novos uploads após crashes e exige procedimento administrativo/reconciliação; não é autorização para excluir imagens ou alterar contadores às cegas. Limites também precisam ser calibrados com capacidade real e complementados pelos limites do gateway. Não se presume nenhuma configuração da VPS.

## Migrations

- V22__storage_independent_discard.sql: flag independente, preservação de descarte por estado/histórico, índices de uso e moderação.
- V23__storage_upload_custody_budget.sql: orçamentos e permissões de admissão persistentes; nenhuma FK cruza bounded contexts.

V13–V21 não foram alteradas. A migração de dados antigos foi exercitada em MySQL descartável: Flyway até V21, inserção de descarte e de descarte posteriormente sobrescrito por moderação, aplicação de V22/V23, conferência do tombstone/hold/versão/auditoria. Implantação futura deve manter os flags desligados e impedir escritores da versão antiga durante a evolução.

## Validação localizada

Foi usado Java 17. Todos os comandos Maven tiveram filtro explícito. Não houve suíte global, verify completo, perfil global de banco ou cobertura global.

- `mvnw.cmd -Dtest=StorageUploadFlowTest,StorageBlobMySqlIT,LocalPhysicalBlobStoreTest,LegacyMediaSourceAdapterTest,StorageHttpFlowTest test`: fluxo H2 (18 casos), MySQL (19), HTTP e filesystem passaram; um teste POSIX foi ignorado em Windows. O teste novo de MIME falhou inicialmente por key de fixture inválida e foi corrigido/reexecutado isoladamente, sem mudar a regra de validação.
- `mvnw.cmd -Dtest=LegacyMediaSourceAdapterTest,StorageDiscardUpgradeMySqlIT test`: 2 passaram.
- `mvnw.cmd -Dtest=StorageDiscardUpgradeMySqlIT test`: upgrade final, incluindo tombstone sobrescrito historicamente, passou.
- `mvnw.cmd -Dtest=StorageUploadFlowTest#publishedWave3CopiedCheckpointIsReappliedWithoutCreatingAnotherAssetOrCopy test`: passou.
- `mvnw.cmd -Dtest=StorageUploadFlowTest#realLegacySourcePersistsVerifiedMimeAndDimensionsWithoutCopy test`: passou.
- `mvnw.cmd -Dtest=StorageUploadFlowTest#concurrentUploadAdmissionNeverExceedsTheSharedUserLimit,StorageBlobMySqlIT#concurrentUploadAdmissionNeverExceedsTheSharedUserLimit test`: 2 passaram; quatro tentativas simultâneas concederam exatamente dois slots.

Logs locais: target/wave4-final.log, wave4-upgrade.log, wave4-upgrade-final.log, wave4-copy-compatibility.log, wave4-mime-integration.log e wave4-quota-concurrency.log. Não somar reruns como cenários distintos. A compilação de produção e dos testes foi executada pelos comandos acima e passou. Os dois casos adicionais de compatibilidade/MIME integrado rodaram somente em H2; sua repetição em MySQL é verificação complementar pendente.

## Pendências e riscos

- MÉDIO / validação: executar o teste POSIX em filesystem Linux real: `./mvnw -Dtest=LocalPhysicalBlobStoreTest#privateObjectsUseRestrictivePosixModesWhenSupported test`. Validar ACL/owner do root em cada host. Não foi inspecionada a VPS.
- MÉDIO / operação: calibrar orçamentos e revisar reservas desconhecidas após crash. A contagem lógica é conservadora e permanece cobrada mesmo se blobs forem compartilhados ou um upload falhar depois de iniciar custódia.
- MÉDIO / operação: cursores de diretório pertencem à instância, expiram e não são snapshots consistentes; clientes devem tratar expiração explicitamente.
- Validação posterior: suíte geral `mvnw.cmd test`, perfis globais e auditoria ficam para a rodada autorizada de validação. A falha histórica de recuperação de senha não foi tratada nesta correção de Storage.
- Os gates anteriores continuam: consumidores restantes, GET legado, canal temporal da deduplicação, p95 e rollout. Nenhum desses itens foi apresentado como corrigido por esta rodada.

STORAGE_MEDIA_ENABLED e STORAGE_LEGACY_BACKFILL_ENABLED permanecem false por padrão. Nenhuma subtask Jira foi encerrada. A aprovação jurídica de expurgo continua pendente e nenhum mecanismo de exclusão física foi habilitado.
