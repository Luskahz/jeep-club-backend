# BACK-547 — Rodada 1: auditoria de aceites

Base inicial `46f26e6f7796ed840e4650b65237ca7f86cbbae6`. A avaliação considera o código até `ee32ad5f` e a cobertura HTTP adicional desta alteração. Fontes: descrições e comentários das cinco subtasks no Jira, BACK-547 D01–D15 e comentário 10453. Comentários posteriores de delegação prevalecem sobre bloqueios históricos de aprovação de detalhes técnicos. `ATENDIDO` significa evidência restrita ao critério citado; não constitui aceite da Story nem liberação de produção.

Fontes Jira: [BACK-547](https://jeep-club-developers.atlassian.net/browse/BACK-547), [BACK-548](https://jeep-club-developers.atlassian.net/browse/BACK-548), [BACK-591](https://jeep-club-developers.atlassian.net/browse/BACK-591), [BACK-551](https://jeep-club-developers.atlassian.net/browse/BACK-551), [BACK-553](https://jeep-club-developers.atlassian.net/browse/BACK-553) e [BACK-599](https://jeep-club-developers.atlassian.net/browse/BACK-599). O [PR #139](https://github.com/Luskahz/jeep-club-backend/pull/139) está em rascunho. No SHA `ee32ad5f270f1d100824319f495979f28742aa2b`, o [workflow manual](https://github.com/Luskahz/jeep-club-backend/actions/runs/43355250303) passou com 2.690 testes Maven (1 skip), 192 PostgreSQL e 597 MySQL em cada uma das duas passagens; sem falhas/erros. Deploy ignorado. Essa execução não prova commits posteriores.

No SHA `45c4bbdbb445fe91d47e6f20ad4ef908d7b6962b`, `mvnw.cmd test` executou 2.685 testes, sem falhas/erros, com 3 ignorados: benchmark opt-in, symlink indisponível e teste POSIX em Windows. O benchmark foi executado separadamente conforme [relatório](back-551-round1-benchmark.md). Esta suíte não substitui o `verify` de CI nem os perfis MySQL/PostgreSQL. Logs locais: `target/back-547-round1-global-test.log` e relatórios Surefire.

## BACK-548 — decisões e ADR

| Critério Jira | Estado | Evidência e limite |
|---|---|---|
| Usuário participou e exemplos não foram confundidos com requisitos | ATENDIDO | Decisões registradas nos comentários BACK-547 10330–10453; comentário 10453 diferencia contrato de negócio e escolhas técnicas. |
| D01–D15 respondidas ou adiadas sem bloquear recorte | ATENDIDO | Consolidação BACK-548 #10348 e delegação BACK-547 #10453; descarte físico e validação jurídica permanecem fora deste recorte. |
| Story/subtasks revisadas, MVP e não escopo explícitos | PARCIAL | Jira consultado e recorte documentado em [ADR 003](adr/003-storage-image-contract-round1.md); descrições antigas no Jira não foram atualizadas nem reconciliadas integralmente. |
| Sem código/PR/deploy sem nova autorização | ATENDIDO | A autorização posterior #10453 e o pedido desta rodada autorizam backend não destrutivo; não houve merge, deploy ou alteração de status. |

## BACK-591 — inventário

| Critério Jira | Estado | Evidência e limite |
|---|---|---|
| Matriz por tela/endpoint com frontend e backend sem omissões | PARCIAL | [Inventário](back-591-round1-discovery.md) tem contratos backend e riscos legados; frontend não localizado, impossibilitando provar cobertura de telas. |
| Comentário/avatar, navbar, perfil admin e carteirinha diferenciados | PARCIAL | Diferenças de finalidade/segurança e crop independente documentadas; dimensões, navbar e exportação reais não foram observadas no cliente. |
| Lacunas de payload, dono e consulta em lote | ATENDIDO | `PublicationSocialController.CommentResponse` não contém nome/foto; matriz atribui projeção mínima e consulta em lote a Publications/Identity. |
| Recomendações e compatibilidade submetidas a BACK-548 | PARCIAL | ADR 003 usa decisões de produto e delegação técnica; adaptação e homologação das superfícies ainda dependem da integração. |
| BACK-551 e BACK-559 referenciam matriz na implementação/homologação | PARCIAL | ADR referencia a matriz e o pipeline incorpora crop independente; homologação BACK-559 nos consumidores não foi realizada. |

## BACK-551 — pipeline

| Critério Jira | Estado | Evidência e limite |
|---|---|---|
| D06–D08/D13 respondidas e dimensionamento aprovado | PARCIAL | Produto decidiu finalidades, limites e delegou detalhes (#10453); dimensões reais das superfícies e qualidade de exportação ainda não foram verificadas. |
| Variantes no mesmo asset e sujeitas a ACL/moderação | ATENDIDO | `MediaUploadService` grava variantes/bindings no mesmo asset; `MediaResolutionService` valida referência e moderação em toda variante; testes HTTP/JPA de resolução e moderação. A integração dos demais consumidores é posterior. |
| Falha parcial, retry e fallback preservam integridade/privacidade | PARCIAL | Testes `failedUploadRollsBackAssetVariantsReferencesAndAuditButRetainsBlob`, concorrência e fallback JPEG/PNG; crash de processo/máquina, múltiplas instâncias e qualidade em ambiente alvo não comprovados. |
| Ganhos nas telas e testes HTTP/integração/corrupção | PARCIAL | Testes HTTP, WebP/EXIF e corrupções passaram; [benchmark](back-551-round1-benchmark.md) mediu p95 local 263 ms em cenário delimitado. Ganhos visuais e de transferência nas telas não foram medidos. |

## BACK-553 — administração

| Critério Jira | Estado | Evidência e limite |
|---|---|---|
| D05/D09/D14 aprovadas | ATENDIDO | Decisões e delegação #10453; backend apenas, sem integração de consumidores/frontend. |
| Busca combinada, vínculos atuais/históricos e preview permissionado | PARCIAL | `MediaAdministrationAdapter.search/detail`, testes em H2/MySQL/PostgreSQL; preview requer permission própria, propósito, policy e auditoria. Referência não expõe identificador arbitrário do consumidor por minimização; navegação até o agregado ainda depende de contrato seguro futuro. |
| Sem path/key/PII desnecessária | PARCIAL | Revisados DTOs de listagem, detalhe, referências, auditoria, retenção e reconciliação, além de `StorageMediaErrors`; `StorageHttpFlowTest` verifica JSON e erro de preview com caminho e nota sintéticos. Exceções externas ao tratamento específico de storage e combinações não ensaiadas ainda exigem avaliação. |
| HTTP, segurança, paginação e performance | PARCIAL | 401/403/404, preview, falha de auditoria e OpenAPI testados; 250 registros em três páginas fizeram três consultas no H2 e MySQL. Não há benchmark de carga administrativa representativa. |

## BACK-599 — deduplicação

| Critério Jira | Estado | Evidência e limite |
|---|---|---|
| Dedup global byte-identical sem expor existência privada | PARCIAL | `BlobCatalogAdapter`, V17 e testes de compartilhamento; cursor administrativo não expõe hash. Os [diagnósticos locais](back-599-round1-findings.md) observaram diferença temporal entre conteúdo novo e existente na operação interna, em MockMvc e em TCP loopback com dois usuários; exploração por rede remota e resistência sob carga não foram avaliadas. |
| Dois assets/ACL independentes para o mesmo blob | ATENDIDO | `identicalUploadsHaveIndependentIdentityAndSharePhysicalRepresentations` e `orderedReferencesDoNotShareAuthorizationThroughPhysicalDeduplication`, inclusive MySQL e PostgreSQL. |
| Moderação/retirada/descarte lógico não quebra outro asset | ATENDIDO | `identicalUploadsHaveIndependentIdentityAndSharePhysicalRepresentations` verifica moderação; `logicalDiscardOfOneAssetDoesNotRemoveSharedBlobOrAffectOtherAsset` verifica descarte lógico e leitura do outro asset no H2 e MySQL. Purge físico está fora do escopo. |
| Integridade/concorrência, falhas e reprocessamento com migrations | PARCIAL | [Relatório BACK-599](back-599-round1-findings.md), MySQL 60 testes e PostgreSQL 57; falha real de máquina e múltiplos processos/instâncias não foram ensaiados. |
| F131-01 corrigido sem reescrever V13 | ATENDIDO | V16 mantém CHECK uppercase; `StorageBlobMySqlIT.uppercaseConstraintIsEnforcedAfterFlywayEvolution` passou com V1–V24; V13 preservada. |
| ADR/diagrama/retenção e PR com CI, sem consumer/deploy | ATENDIDO | ADR 002/003 descrevem fronteiras, diagrama e retenção; PR #139 está em rascunho e o workflow manual do SHA `ee32ad5f` passou. Não houve deploy nem integração de consumidores; commits posteriores requerem sua própria evidência de CI. |

## Separação de pendências

**Rodada 2:** contratos de audiência dos consumidores, DTO de autor em lote, integração visual e carteirinha, frontend/proxy e cutover do GET legado. **Rodada 1:** concluir verificações de qualidade e limites do pipeline, robustez de codec e administração em carga, além das combinações de erros administrativos ainda não ensaiadas e do CI de commits posteriores. Crash real, processos independentes e canal temporal permanecem limites explícitos da evidência, não são declarados resolvidos. Expurgo físico e autorização de produção não fazem parte desta rodada.
