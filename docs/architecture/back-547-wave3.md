# BACK-547 — rodada 3: entrega e validação

Base local inicial: `244b59e2` (preserva `aa3541df`). Branch: `feature/back-547-storage-implementation-wave2`. A implementação permanece em branch, sem PR, merge ou deploy. Esta rodada não encerra a story.

## Fluxo integrado

Upload autenticado de perfil → policy Identity → processamento → reserva e deduplicação física → asset/variantes/bindings/referência/auditoria na mesma transação lógica → leitura por referência com moderação persistida e policy → bytes verificados com no-store. Nenhum DTO público contém storageKey ou SHA-256. Legado preservado; novas fotos não substituem automaticamente o campo de perfil legado.

## Contratos novos (desabilitados por padrão)

| Método/rota | Comportamento |
|---|---|
| POST `/storage/media/profile` | multipart file, sujeito autenticado; retorna mediaId/referenceId |
| GET/HEAD `/storage/media/{mediaId}/references/{referenceId}/variants/{code}` | resolução contextual; MASTER, AVATAR_SMALL e PROFILE_PREVIEW no fluxo inicial |
| POST `/storage/media/{mediaId}/references` | associação com ownership e autorização do consumidor |
| DELETE `/storage/media/{mediaId}/references/{referenceId}` | encerramento histórico; não apaga bytes |
| GET `/admin/storage/media` | cursor afterId, limite até 100, filtros context/state; descartados omitidos por padrão |
| GET `/admin/storage/media/{id}/references` | referências atuais e históricas |
| GET `/admin/storage/media/{id}/audit` | eventos de governança sem notas internas |
| GET `/admin/storage/media/{id}/references/{ref}/preview/{code}` | preview sob policy contextual e auditoria |
| POST `/admin/storage/media/{id}/moderation` | ocultação/revisão/bloqueio global ou por referência |
| POST `/admin/storage/media/{id}/restore` | restauração permissionada |
| POST `/admin/storage/media/{id}/legal-hold` | hold auditado |
| POST `/admin/storage/media/{id}/retention-review` | avaliação não destrutiva auditada |
| POST `/admin/storage/media/{id}/discard` | descarte lógico de mídia sem uso |
| POST `/admin/storage/media/reconciliation` | resumo dry-run de inventário físico; não executa correções destrutivas |

Permissões catalogadas no Authorization existente: STORAGE_MEDIA_READ, MODERATE, RESTORE, AUDIT_READ, LEGAL_HOLD, RETENTION_REVIEW e RECONCILE. Não são concedidas a roles por esta migration. Preview não contorna o contexto proprietário. Contratos estão anotados para geração OpenAPI pelo backend, mas ainda precisam de revisão completa de documentação/exemplos e paginação de inventário para UI.

## Findings

| Finding | Evidência e pendência |
|---|---|
| F-W2-01 | Integração Identity, resolver e teste JWT reais; parcial: demais consumidores/audiências e GET legado |
| F-W2-02 | BlobCatalogAdapter, LocalPhysicalBlobStore, V17 e testes concorrentes H2/MySQL; parcial: oráculo temporal, falha de máquina e processos distintos |
| F-W2-03 | Corrigido no fluxo: histórico pode ser autorizado; teste integrado positivo |
| F-W2-04 | Código público seguro e persistência de moderação integrados; notas não são retornadas nos DTOs |
| F-W2-05 | Compartilhamento é informação legítima; inventário real distingue reservas, arquivos inconsistentes e objetos sem vínculo |
| F-W2-06 | Pipeline conectado, EXIF, redução adaptativa e timers; parcial: codec WebP efetivo, presets completos e crops HTTP |
| F-W2-07 | Assert allowed positivo e testes integrados multiusuário/JWT/HEAD/condicional; Range e demais contextos ainda pendentes |
| F-W2-08 | Retenção, sensibilidade histórica, hold e descarte lógico persistidos; parcial: obrigações de backup/jurídico, escala e operações futuras |
| F-W2-09 | Flyway até V21 + Hibernate validate + rejeição SQL de variante minúscula em MySQL; falta cenário específico de upgrade com dados antigos inválidos |
| F-W2-10 | APIs, referências, governança e backfill implementados no recorte inicial; parcial: demais consumidores e inventário frontend |

## Subtasks

| Task | Resultado |
|---|---|
| BACK-548 | PARCIAL: ADR 002 implementado; falta fechamento global |
| BACK-549 | JÁ ENTREGUE no PR131, reutilizada; ajustes aditivos de integração |
| BACK-550 | PARCIAL: associação/desassociação/histórico funcionais; agregados legados não migrados |
| BACK-551 | PARCIAL: upload síncrono completo no recorte Identity; lacunas do F06 |
| BACK-552 | PARCIAL: policy Identity e leitura integradas; demais módulos e legado pendentes |
| BACK-553 | PARCIAL: endpoints e permissões reais; filtros/detalhes/contratos administrativos completos ainda pendentes |
| BACK-554 | PARCIAL: moderação persistida aplicada à entrega; fluxos de revisão e consulta de notas precisam de acabamento |
| BACK-555 | PARCIAL: eventos novos persistidos; auditoria e contratos finais por todos os consumidores pendentes |
| BACK-556 | PARCIAL: inventário storage-blobs e dry-run; inventário completo do legado/escala pendentes |
| BACK-557 | PARCIAL: política integrada e descarte lógico seguro; expurgo deliberadamente ausente |
| BACK-558 | PARCIAL: dry-run, checkpoint, integridade e rollback internos; sem cutover/execução produtiva |
| BACK-559 | PARCIAL: testes localizados de fluxo/HTTP/concorrência/migração; validação global e rollout pendentes |
| BACK-591 | PARCIAL: matriz backend e presets iniciais; frontend não localizado no workspace nem na busca de repositórios conectados |
| BACK-599 | PARCIAL: núcleo físico funcional e compartilhado; segurança temporal e recuperação operacional ainda a validar |

## Migrations

V17: blobs, bindings, localização compartilhada e unicidade preservada para legado. V18: controle de mídia e ledger de governança. V19: extensão dos ENUMs físicos de Authorization e cadastro granular de permissões. V20: contextos protegidos de medicina/identificação. V21: checkpoints legados. V13–V16 preservadas. Os erros de SENSITIVE reservado e ENUM de permissões foram corrigidos nas migrations novas antes da publicação, usando apenas bancos descartáveis de teste.

## Testes localizados e próximos gates

Comandos executados com filtros explícitos, Java 17:

- `mvnw.cmd -Dtest=StorageHttpFlowTest,ExifOrientationTest,ContextualMediaAccessTest,ModerationDecisionTest,ReconciliationTest,RetentionPolicyTest,SynchronousImageProcessorTest test` — sucesso; JWT, cache, autorização e unidades.
- `mvnw.cmd -Dtest=StorageUploadFlowTest,StorageBlobMySqlIT,LocalPhysicalBlobStoreTest,MediaCatalogPersistenceTest test` — sucesso; 33 testes naquele snapshot, incluindo Flyway, rollback, concorrência e corrupção.
- `mvnw.cmd -Dtest=StorageUploadFlowTest#backfillIsIdempotentUnknownHeldAndRollbackPreservesBytes+legacyBackfillDetectsChangedSourceWithoutRewritingTheCheckpoint test` — recorte de checkpoint/alteração de fonte.
- `mvnw.cmd -Dtest=StorageUploadFlowTest test` — 9 testes passaram na última revisão da retenção/backfill; log `target/wave3-final-custody.log`.

Não somar reruns como testes distintos. A suíte geral não foi executada nesta rodada. A falha anterior em recuperação de senha não foi corrigida nem revalidada aqui.

Validação global futura, somente na próxima rodada autorizada: `mvnw.cmd test`, `mvnw.cmd -Pmysql-migrations verify`, `mvnw.cmd -Ppublications-postgresql verify`. Conferir os perfis no pom antes da execução; não interpretar suíte global verde como autorização de deploy. Para repetir apenas o recorte novo: `mvnw.cmd -Dtest=StorageUploadFlowTest,StorageBlobMySqlIT,StorageHttpFlowTest,ExifOrientationTest,LocalPhysicalBlobStoreTest test`.

## Rollout e riscos

Manter STORAGE_MEDIA_ENABLED=false e STORAGE_LEGACY_BACKFILL_ENABLED=false. Validar schema e backfill em cópia isolada, fechar contratos de audiência de cada consumidor, testar canal temporal e falhas entre processos, validar codec/crops/qualidade e medir p95 com carga representativa. Ativar primeiro uma superfície canário autorizada; rollback HTTP pelo flag preserva catálogo e bytes. Não remover colunas nem reverter migrations com perda de dados. Nenhum hard-delete, scheduler destrutivo, backup, VPS ou frontend foi alterado.

ALTO: canal temporal e política de todos os consumidores não comprovados; GET legado mantém risco conhecido. MÉDIO: WebP efetivo/presets/crops, carga e crash de máquina não validados; inventário é observacional e pode mudar com uploads concorrentes, não é autoridade de expurgo. BAIXO: acabamento OpenAPI e navegação de inventário administrativo. Não há bloqueio de compilação conhecido no recorte validado.
