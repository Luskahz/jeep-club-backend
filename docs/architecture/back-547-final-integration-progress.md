# BACK-547 — incremento dos contratos centrais, 2026-11-12

**Implementação integrada completa: NÃO. Pronto para merge da entrega final: NÃO. Pronto para ativação produtiva: NÃO.**

Este relatório descreve um incremento da Etapa 1. Não substitui os critérios das Etapas 2–6 e não declara consumidores integrados. As decisões de produto foram consultadas ao vivo em BACK-547, BACK-548–559, BACK-591 e BACK-599, incluindo o comentário 10453, que prevalece sobre gates de planejamento antigos. Não é necessária nova aprovação de detalhes técnicos para prosseguir.

## Base e commits

- Checkout inicial: `jeep-club-backend`, branch `feature/back-585-574-463-573-admin-hardening`, HEAD `a317f7757b9cd5dc0b19d89b415e07fbe0d631fb`. Arquivos não rastreados preservados.
- Worktree de execução: `C:\Users\lucas.l\Documents\Devlopments\jeep-club-back-547-wave2`.
- Branch: `feature/back-547-storage-implementation-wave2`.
- HEAD inicial confirmado: `a39a5033260f28cb68b4ef11aa6b4948df3c8e54`.
- `89b6dd6e`: resolução explícita por provider, ACL anterior ao IO, integridade e ausência de fallback.
- `eb1858ab`: upload sem referência fictícia, proveniência por policy, permissões distintas, transação obrigatória, idempotência e substituição histórica.
- Commit posterior de testes/documentação contém este relatório e cobertura de filesystem temporário.

Nenhuma migration foi adicionada ou alterada. V13–V23 permanecem intactas; não houve necessidade de DDL neste incremento.

## Decisões implementadas

Upload cria MediaAsset, variantes e custódia, mas nenhuma MediaReference. `Uploaded.referenceId` passa de `long` para `Long`, retornando `null`. O owner e o sujeito vêm de `StorageConsumerPolicy.uploadIdentity`; o default conserva UNKNOWN. A policy Profile declara explicitamente o próprio usuário como owner e sujeito para upload próprio autorizado. O ator de upload continua sendo registrado separadamente.

`mayAssociate` e `mayDissociate` são capacidades separadas de `mayUpload`; defaults negam. Uma implementação de contexto pode autorizar compartilhamento sem alterar ownership do asset. A regra central não exige mais actor == owner indiscriminadamente. Isso ainda precisa ser demonstrado por fluxos reais dos consumidores.

MediaReferenceService exige uma transação preexistente (`MANDATORY`). O serviço consumidor deverá bloquear seu agregado, mudar a referência cadastral e executar a alteração do catálogo na mesma transação. A exigência de transação é uma barreira técnica; **não prova que o consumidor já foi implementado**.

Repetir associação atual do mesmo asset/contexto/consumer/role/posição reutiliza a referência, sob o lock pessimista já existente do asset. `replace` bloqueia os assets em ordem crescente, preserva o vínculo antigo com `REPLACED`, timestamp e ator e registra o novo. Falha do consumidor reverte ambos. A integração deve manter ordem global agregado → assets e seu próprio controle de revisão; isto não substitui expectedRevision de Vehicles.

Uploads sem vínculo usam o caminho já existente de retenção desde createdAt; teste SQL confirma zero referências e decisão WAITING. Custódia não é removida por rollback. Descarte lógico continua impedindo associação/resolução.

## Contratos HTTP

| Contrato | Resultado neste incremento |
|---|---|
| `POST /storage/media/profile` | 201 com mediaId e referenceId nulo; upload avulso sem alteração do cadastro Identity |
| `POST /storage/media/{id}/references` | 404 sanitizado; rota genérica não pode inventar associação sem agregado |
| `DELETE /storage/media/{id}/references/{referenceId}` | 404 sanitizado; desassociação deve ocorrer pelo consumidor |
| GET de variantes | Mesma URL/DTO binário; ACL/moderação antes da seleção do provider e IO |
| GET legado `/media/images?key=...` | Não alterado; continua pendência de segurança e gate de cutover |

As duas rotas genéricas de mutação ficam indisponíveis mesmo com o flag experimental ativo. Não existe substituto consumidor funcional entregue nesta rodada. Clientes experimentais precisam considerar essa mudança. Os contratos legados de Profile não foram alterados. OpenAPI completo e contratos novos dos consumidores continuam pendentes.

## Providers e preservação física

`storage-blobs` exige binding SQL, provider LOCAL, key, tamanho e SHA-256 consistentes com a variante, além da verificação física do BlobCatalog. Falta de binding nunca aciona leitura legada.

`images` e `private-images` seguem pelo LegacyMediaSource após autorização contextual. Namespace e prefixo precisam concordar, e tamanho/digest devem coincidir com a variante catalogada. Provider/namespace desconhecido falha fechado. O mero conhecimento de uma key não é entrada desse resolvedor. Não há novo DTO comum com key, path ou digest.

Isso **não integra Dependents**: nenhuma policy de dependente foi criada. Seus serviços de autorização, sessões, ACTIVE, permissões administrativas, snapshots, auditoria e no-store precisam ser preservados na integração real. Não habilitar entrega privada por uma policy simplificada. Erros físicos continuam dependendo dos handlers existentes; uma revisão HTTP específica para ausência/corrupção por provider permanece necessária.

Evidência de preservação: o código produtivo alterado só seleciona e lê o provider legado; não contém store/move/rename/delete de arquivos antigos. O teste de filesystem cria root temporário e verifica bytes e mtime inalterados após resolução; corrupção/ausência são simuladas exclusivamente na fixture. Não foi acessada a VPS, nem inventariado seu conteúdo. Nenhum backfill produtivo foi executado.

## Validação executada

Java local: `C:\Users\lucas.l\.jdks\ms-17.0.18`, somente por JAVA_HOME do processo.

| Comando | Resultado | Evidência local |
|---|---|---|
| `mvnw.cmd -Dtest=MediaResolutionServiceTest,StorageUploadFlowTest,StorageHttpFlowTest test` | 27 passaram na primeira alteração do resolvedor | `target/final-core-provider-tests.log` |
| `mvnw.cmd -Dtest=MediaResolutionServiceTest,StorageUploadFlowTest,StorageHttpFlowTest,MediaCatalogPersistenceTest,RetentionPolicyTest test` | 49 passaram, zero falhas/erros/skips, árvore final do código produtivo em eb1858ab | `target/final-core-complete-tests.log` |
| `mvnw.cmd -Dtest=StorageBlobMySqlIT,MediaCatalogPostgresIT test` | 41 passaram: MySQL 25 e PostgreSQL 16; zero falhas/erros/skips | `target/final-core-databases-tests.log` |
| `mvnw.cmd -Dtest=MediaResolutionServiceTest test` | 6 passaram, incluindo leitura in-place, bytes/mtime preservados, corrupção e ausência em root temporário | `target/final-core-filesystem-tests.log` |

Uma execução intermediária de 29 testes também passou, mas não é usada para validar mudanças que ainda estavam sendo feitas. Não somar reruns como cenários únicos. A cobertura adicional de filesystem é registrada no commit de teste posterior.

MySQL 8.0.46 descartável aplicou Flyway V1–V23 e validou Hibernate; PostgreSQL 17.6 descartável validou o catálogo com Hibernate DDL, **não a cadeia Flyway MySQL**. Ambos usaram Testcontainers; nenhuma base operacional foi usada.

Antes dos bancos, C: tinha aproximadamente 6,83 GiB livres; Docker reportou 923,2 GiB disponíveis no filesystem virtual. Ambas as imagens já existiam. Nenhum prune, remoção de volume ou limpeza de dados foi executado. O MySQL global anteriormente interrompido não foi reclassificado como aprovado.

Não foram executadas suítes globais, perfis globais completos, Linux/POSIX, benchmark p95 representativo, prova de deadlock entre consumidores ou crash multi-instância. Não há resultado de CI novo nesta execução.

## Matriz das 14 subtasks

Estado abaixo é de evidência desta execução e do código inspecionado, não atualização de status Jira.

| Subtask | Atendido/verificado neste incremento | Restante para aceite final |
|---|---|---|
| BACK-548 | Decisões e delegação 10453 consultadas; escolhas centrais registradas | ADR/matriz de audiências e evidências finais de todos os consumidores |
| BACK-549 | Catálogo existente reutilizado; roundtrip H2/PostgreSQL e schema MySQL | Evolução cadastral das integrações e migrations V24+ quando necessárias |
| BACK-550 | Upload separado, vínculo idempotente, replace histórico e rollback transacional testados | Correlação efetiva e concorrência de cada agregado consumidor |
| BACK-551 | Processador existente inspecionado | Preservação segura de qualidade MASTER, crop/focal tipado exposto, WebP decoder opcional, limites/timeout e p95 |
| BACK-552 | ACL antes do IO, dual-provider explícito, checksum, ausência de fallback; mutações genéricas bloqueadas | Policies reais por estado/audiência, compartilhamento autorizado e proteção do GET legado |
| BACK-553 | Administração existente inspecionada | Detalhe sanitizado, filtros completos, consultas sem N+1 e preview granular auditado |
| BACK-554 | Regressão de moderação/restauro/descarte do núcleo passou | Aplicação em todos consumidores, GET legado, caches e motivo público autorizado |
| BACK-555 | Auditoria transacional existente preservada; rollback de referência testado | Leitura sensível por contexto, ações/propósitos e histórico de todos consumidores |
| BACK-556 | Regressões existentes de inventário/quotas exercitadas nos fluxos Storage | Inventário cruzado com agregados atuais/históricos, missing/unknown e visão administrativa completa |
| BACK-557 | COMMON/SENSITIVE, hold e upload avulso testados; nenhum purge | Política operacional/jurídica e reconciliação completa; purge continua fora do escopo autorizado |
| BACK-558 | Backfill existente reexecutado nos testes; keys preservadas; leitura legada sem cópia | Descoberta por módulo, reconstrução de referências com proveniência, relatório e rollback lógico completos |
| BACK-559 | Testes centrais H2/HTTP/MySQL/PostgreSQL passaram | Todas integrações, suítes globais, HEAD/304/Range completos, cache, p95 e rollout |
| BACK-591 | Inventário Jira consultado; perfil e contratos atuais inspecionados | Apresentação mínima de autor/avatar em lote nos comentários e inventário atualizado completo |
| BACK-599 | Regressões de dedup, identidades/ACL independentes, custódia/rollback e MySQL passaram | Canais temporais, crash/recuperação multi-instância, performance e retenção cruzada completas |

## Consumidores

Nenhum consumidor novo foi integrado efetivamente neste incremento. Profile recebeu apenas ajuste da policy central. Tools, Vehicles, Publications/Notices/Events/Comments, Dependents e Membership permanecem pendentes de integração real. Medical ainda exige inspeção para confirmar se existe consumidor; não foi inventado um. Billing/comprovantes permanece fora do escopo.

## Gates para as próximas etapas e produção

1. Concluir os contratos/garantias centrais restantes, em especial auditoria contextual privada e testes reais de compartilhamento/concorrência entre agregados.
2. Concluir BACK-551 antes de integrar consumidores dependentes do pipeline.
3. Integrar cada módulo com cadastro persistido, referências históricas, ACL/audiência, moderação e testes próprios.
4. Completar administração, backfill lógico descoberto por módulo e proteção do GET legado; nenhuma execução produtiva autorizada.
5. Executar validações globais e perfis completos após as integrações, medir p95 e testar crash, recovery, deadlocks e filesystem Linux.
6. Revisar HTTP/OpenAPI, matriz de permissões, backup/restore e plano de rollout/rollback com flags desligados. `STORAGE_MEDIA_ENABLED` e `STORAGE_LEGACY_BACKFILL_ENABLED` continuam false por padrão.

**Backend final concluído**, **aprovação de merge** e **aprovação de ativação em produção** são decisões distintas. Nenhuma das três está concedida por este incremento. Nenhum ticket foi fechado, não houve push, merge, deploy, alteração de frontend, VPS ou arquivo legado operacional.
