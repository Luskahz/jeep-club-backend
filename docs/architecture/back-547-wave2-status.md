> Registro histórico da rodada 2. Para o estado posterior, consultar [rodada 3](back-547-wave3.md).

# BACK-547 â€” estado da implementaÃ§Ã£o, rodada 2

Base: origin/develop d33af0a0. Documento de progresso, nÃ£o aprovaÃ§Ã£o de arquitetura, release ou rollout. BACK-549 foi entregue no PR #131; V13 nÃ£o foi alterada. A decisÃ£o posterior do PO para deduplicaÃ§Ã£o global e a autorizaÃ§Ã£o da rodada prevalecem sobre o texto histÃ³rico de planejamento.

## InventÃ¡rio preliminar backend (BACK-591)

| SuperfÃ­cie | EvidÃªncia atual | Destino necessÃ¡rio | ACL/cache a validar |
|---|---|---|---|
| Upload/leitura comum | `platform/storage/image/ImageMediaController`, POST/GET `/media/images` por `storageKey` | catÃ¡logo e referÃªncia contextual, com compatibilidade durante rollout | GET legado requer autenticaÃ§Ã£o mas nÃ£o verifica vÃ­nculo; risco existente |
| Perfil e avatar | `iam/identity/core/domain/model/User`, `UserEntity.profilePhotoStorageKey`, `AdminUserService` | avatar pequeno, perfil e eventual foto de identificaÃ§Ã£o separados | Identity decide audiÃªncia; foto de identificaÃ§Ã£o Ã© protegida |
| PublicaÃ§Ãµes/comentÃ¡rios | `PublicationImageRequestDTO`, `PublicationFeedDTO`, `PublicationCommentImage` | feed, capa, galeria e avatar de comentÃ¡rio; eventual resoluÃ§Ã£o em lote | Publications valida publicaÃ§Ã£o, audiÃªncia e estado; evitar N+1 |
| Eventos/participantes | `EventResponseDTO`, `EventParticipantResponseDTO` | evento/capa/galeria, avatar de participante | Publications/Events determina acesso ao evento e participante |
| VeÃ­culos | `VehicleImage`, `VehicleGalleryAssembler`, `VehicleService` | miniatura e galeria, crops independentes | Vehicles determina acesso ao recurso |
| Ferramentas | `Tool`, `ToolResponseDTO`, `ToolHistoryEntity` | miniatura/visualizaÃ§Ã£o e histÃ³rico | Tools determina acesso ao recurso |
| Dependentes | `Dependent`, `DependentHistoryEntity`, `DependentResponseDTO`, `PrivateImageMediaQuery` | foto de dependente e histÃ³rico | dependente e MASTER privados, no-store; preservar rota protegida |
| Carteirinha/Membership | uso de foto a confirmar no contrato de exportaÃ§Ã£o | fotografia de identificaÃ§Ã£o independente de avatar | protegido; verificar dono do recurso e exportaÃ§Ã£o |
| Billing | fora da integraÃ§Ã£o funcional inicial | nÃ£o integrar nesta rodada | contratos atuais preservados |

A matriz Ã© preliminar: frontend, endpoints e dimensÃµes reais de carteirinha, PDFs, comentÃ¡rios e demais superfÃ­cies nÃ£o foram verificados integralmente; nÃ£o representa conclusÃ£o da BACK-591.

## DecisÃµes e estado tÃ©cnico

O catÃ¡logo existente diferencia asset, variantes, intervalos de referÃªncia e auditoria. As consultas internas adicionadas nesta rodada paginam referÃªncias e eventos por ID, nÃ£o constituem API autorizada. V16 acrescenta CHECK de cÃ³digo maiÃºsculo, sem corrigir dados existentes; examinar dados antes de aplicar. A V13 ainda impede compartilhamento de localizaÃ§Ã£o fÃ­sica entre variantes. NÃ£o existe StoredBlob, nem deduplicaÃ§Ã£o global ou garantia arquivo/banco. NÃ£o ativar upload novo presumindo sua existÃªncia.

`SynchronousImageProcessor` Ã© uma peÃ§a isolada: limita entrada a 10 MiB, dimensÃ£o a 10.000 por lado e 20 milhÃµes de pixels, usa no mÃ¡ximo dois processamentos por instÃ¢ncia, decodifica JPEG/PNG, cria MASTER e derivados com crops independentes, remove metadados ao recodificar e calcula SHA-256 recebido e persistido. Aplica tetos de 2/5 MiB Ã  MASTER e rejeita resultado superior; nÃ£o tem otimizaÃ§Ã£o adaptativa, WebP, correÃ§Ã£o de orientaÃ§Ã£o EXIF, presets por consumidor, instrumentaÃ§Ã£o, configuraÃ§Ã£o de concorrÃªncia global ou integraÃ§Ã£o com persistÃªncia. NÃ£o usar em produÃ§Ã£o atÃ© finalizar e validar esses itens; o p95 < 10 s nÃ£o foi medido.

`ContextualMediaAccess` Ã© apenas uma decisÃ£o interna, nÃ£o um adapter do Authorization nem endpoint. Depende de decisÃµes explÃ­citas de moderaÃ§Ã£o e do contexto proprietÃ¡rio; nenhum novo acesso a bytes foi aberto. A implementaÃ§Ã£o nÃ£o protege o GET legado. `ModerationDecision` modela escopo e separa notas internas, mas nÃ£o persiste decisÃµes nem aplica moderaÃ§Ã£o ao trÃ¡fego.

`RetentionPolicy` avalia prazo em memÃ³ria para histÃ³rico fornecido completo, legal hold conhecido e contexto identificado; a elegibilidade Ã© somente revisÃ£o. O mapeamento ainda nÃ£o cobre foto de identificaÃ§Ã£o/medicina e classificaÃ§Ã£o histÃ³rica completa. `Reconciliation` compara snapshots jÃ¡ obtidos; nÃ£o inventaria filesystem nem garante snapshot consistente. Nenhuma dessas peÃ§as estÃ¡ agendada ou conectada a descarte. Hard-delete continua proibido; nÃ£o hÃ¡ aprovaÃ§Ã£o jurÃ­dica presumida.

## PrÃ³ximos passos obrigatÃ³rios

1. Concluir catÃ¡logo de blob indexado por SHA-256 dos bytes persistidos, vÃ­nculo N:1, coordenaÃ§Ã£o entre instÃ¢ncias, crash/rollback e reconciliaÃ§Ã£o sem apagar objetos. Isolar identidade, autorizaÃ§Ã£o, moderaÃ§Ã£o, retenÃ§Ã£o e respostas de usuÃ¡rios distintos. Testar vazamento observÃ¡vel e concorrÃªncia real.
2. Conectar transacionalmente referÃªncias e auditoria aos comandos consumidores, sem repositÃ³rios internos cruzados; fechar consultas administrativas, filtros, permissÃµes granulares, eventos e histÃ³rico de moderaÃ§Ã£o.
3. Finalizar pipeline, tratamento de EXIF/orientaÃ§Ã£o, codec, limites e configuraÃ§Ã£o; instrumentar duraÃ§Ã£o total e por fase, bytes, rejeiÃ§Ãµes e concorrÃªncia sem identificar conteÃºdo, e medir p95 com carga representativa em ambiente isolado.
4. Implementar adapters de Authorization por contexto e proteÃ§Ã£o de MASTER/dependentes; validar GET/HEAD/304 apÃ³s revalidaÃ§Ã£o de ACL/moderaÃ§Ã£o, no-store e ausÃªncia de vazamentos. Preservar contratos legados atÃ© cutover coordenado.
5. InventÃ¡rio fÃ­sico seguro, snapshot consistente, classificaÃ§Ã£o completa, legal hold persistido, descarte lÃ³gico auditado e relatÃ³rio dry-run. Nenhum job ou expurgo fÃ­sico nesta rodada.
6. Preparar backfill idempotente com proveniÃªncia desconhecida, comparaÃ§Ã£o de integridade, simulaÃ§Ã£o, checkpoints e rollback sem apagar dados; nÃ£o executar em produÃ§Ã£o. Definir flags desativadas e procedimentos de canary/rollback na integraÃ§Ã£o futura.
7. Executar na prÃ³xima rodada testes de domÃ­nio, concorrÃªncia, MySQL/Flyway, PostgreSQL, seguranÃ§a/HTTP, rollback e regressÃµes. A compilaÃ§Ã£o isolada nÃ£o valida esses comportamentos. NÃ£o abrir PR atÃ© coordenar CI; merge/deploy exigem autorizaÃ§Ã£o e gates prÃ³prios.

Sem novos contratos HTTP, flags, jobs, execuÃ§Ã£o de migraÃ§Ã£o ou migraÃ§Ã£o de dados nesta entrega parcial. A migration V16 Ã© cÃ³digo de Flyway a ser testado; nÃ£o foi executada nesta rodada.
