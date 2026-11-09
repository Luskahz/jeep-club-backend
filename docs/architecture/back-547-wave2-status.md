> Registro histórico da rodada 2. Para o estado posterior, consultar [rodada 3](back-547-wave3.md).

# BACK-547 — estado da implementação, rodada 2

Base: origin/develop d33af0a0. Documento de progresso, não aprovação de arquitetura, release ou rollout. BACK-549 foi entregue no PR #131; V13 não foi alterada. A decisão posterior do PO para deduplicação global e a autorização da rodada prevalecem sobre o texto histórico de planejamento.

## Inventário preliminar backend (BACK-591)

| Superfície | Evidência atual | Destino necessário | ACL/cache a validar |
|---|---|---|---|
| Upload/leitura comum | `platform/storage/image/ImageMediaController`, POST/GET `/media/images` por `storageKey` | catálogo e referência contextual, com compatibilidade durante rollout | GET legado requer autenticação mas não verifica vínculo; risco existente |
| Perfil e avatar | `iam/identity/core/domain/model/User`, `UserEntity.profilePhotoStorageKey`, `AdminUserService` | avatar pequeno, perfil e eventual foto de identificação separados | Identity decide audiência; foto de identificação é protegida |
| Publicações/comentários | `PublicationImageRequestDTO`, `PublicationFeedDTO`, `PublicationCommentImage` | feed, capa, galeria e avatar de comentário; eventual resolução em lote | Publications valida publicação, audiência e estado; evitar N+1 |
| Eventos/participantes | `EventResponseDTO`, `EventParticipantResponseDTO` | evento/capa/galeria, avatar de participante | Publications/Events determina acesso ao evento e participante |
| Veículos | `VehicleImage`, `VehicleGalleryAssembler`, `VehicleService` | miniatura e galeria, crops independentes | Vehicles determina acesso ao recurso |
| Ferramentas | `Tool`, `ToolResponseDTO`, `ToolHistoryEntity` | miniatura/visualização e histórico | Tools determina acesso ao recurso |
| Dependentes | `Dependent`, `DependentHistoryEntity`, `DependentResponseDTO`, `PrivateImageMediaQuery` | foto de dependente e histórico | dependente e MASTER privados, no-store; preservar rota protegida |
| Carteirinha/Membership | uso de foto a confirmar no contrato de exportação | fotografia de identificação independente de avatar | protegido; verificar dono do recurso e exportação |
| Billing | fora da integração funcional inicial | não integrar nesta rodada | contratos atuais preservados |

A matriz é preliminar: frontend, endpoints e dimensões reais de carteirinha, PDFs, comentários e demais superfícies não foram verificados integralmente; não representa conclusão da BACK-591.

## Decisões e estado técnico

O catálogo existente diferencia asset, variantes, intervalos de referência e auditoria. As consultas internas adicionadas nesta rodada paginam referências e eventos por ID, não constituem API autorizada. V16 acrescenta CHECK de código maiúsculo, sem corrigir dados existentes; examinar dados antes de aplicar. A V13 ainda impede compartilhamento de localização física entre variantes. Não existe StoredBlob, nem deduplicação global ou garantia arquivo/banco. Não ativar upload novo presumindo sua existência.

`SynchronousImageProcessor` é uma peça isolada: limita entrada a 10 MiB, dimensão a 10.000 por lado e 20 milhões de pixels, usa no máximo dois processamentos por instância, decodifica JPEG/PNG, cria MASTER e derivados com crops independentes, remove metadados ao recodificar e calcula SHA-256 recebido e persistido. Aplica tetos de 2/5 MiB à MASTER e rejeita resultado superior; não tem otimização adaptativa, WebP, correção de orientação EXIF, presets por consumidor, instrumentação, configuração de concorrência global ou integração com persistência. Não usar em produção até finalizar e validar esses itens; o p95 < 10 s não foi medido.

`ContextualMediaAccess` é apenas uma decisão interna, não um adapter do Authorization nem endpoint. Depende de decisões explícitas de moderação e do contexto proprietário; nenhum novo acesso a bytes foi aberto. A implementação não protege o GET legado. `ModerationDecision` modela escopo e separa notas internas, mas não persiste decisões nem aplica moderação ao tráfego.

`RetentionPolicy` avalia prazo em memória para histórico fornecido completo, legal hold conhecido e contexto identificado; a elegibilidade é somente revisão. O mapeamento ainda não cobre foto de identificação/medicina e classificação histórica completa. `Reconciliation` compara snapshots já obtidos; não inventaria filesystem nem garante snapshot consistente. Nenhuma dessas peças está agendada ou conectada a descarte. Hard-delete continua proibido; não há aprovação jurídica presumida.

## Próximos passos obrigatórios

1. Concluir catálogo de blob indexado por SHA-256 dos bytes persistidos, vínculo N:1, coordenação entre instâncias, crash/rollback e reconciliação sem apagar objetos. Isolar identidade, autorização, moderação, retenção e respostas de usuários distintos. Testar vazamento observável e concorrência real.
2. Conectar transacionalmente referências e auditoria aos comandos consumidores, sem repositórios internos cruzados; fechar consultas administrativas, filtros, permissões granulares, eventos e histórico de moderação.
3. Finalizar pipeline, tratamento de EXIF/orientação, codec, limites e configuração; instrumentar duração total e por fase, bytes, rejeições e concorrência sem identificar conteúdo, e medir p95 com carga representativa em ambiente isolado.
4. Implementar adapters de Authorization por contexto e proteção de MASTER/dependentes; validar GET/HEAD/304 após revalidação de ACL/moderação, no-store e ausência de vazamentos. Preservar contratos legados até cutover coordenado.
5. Inventário físico seguro, snapshot consistente, classificação completa, legal hold persistido, descarte lógico auditado e relatório dry-run. Nenhum job ou expurgo físico nesta rodada.
6. Preparar backfill idempotente com proveniência desconhecida, comparação de integridade, simulação, checkpoints e rollback sem apagar dados; não executar em produção. Definir flags desativadas e procedimentos de canary/rollback na integração futura.
7. Executar na próxima rodada testes de domínio, concorrência, MySQL/Flyway, PostgreSQL, segurança/HTTP, rollback e regressões. A compilação isolada não valida esses comportamentos. Não abrir PR até coordenar CI; merge/deploy exigem autorização e gates próprios.

Sem novos contratos HTTP, flags, jobs, execução de migração ou migração de dados nesta entrega parcial. A migration V16 é código de Flyway a ser testado; não foi executada nesta rodada.
