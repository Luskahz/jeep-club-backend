# ADR 002 — Custódia física compartilhada e fluxo contextual

Data: 09/11/2026. Decisão técnica implementada em branch; não constitui aprovação de release ou fechamento da BACK-547.

## Modelo e transações

`MediaAsset` permanece a identidade lógica. `storage_blobs` usa SHA-256 dos bytes persistidos como chave e índice de conteúdo; `storage_variant_blobs` associa variantes de assets distintos ao mesmo objeto. Localizações antigas permanecem intactas. V17 substitui a unicidade antiga por índice comum e unicidade gerada somente para localizações legadas; apenas o namespace privado novo permite compartilhamento. V13–V16 não foram alteradas.

A reserva de custódia é confirmada em transação REQUIRES_NEW antes do IO. A reserva possui tamanho, digest e data, mesmo antes de READY. O lock SQL pessimista por digest serializa criação/reuso entre instâncias que compartilhem o MESMO catálogo SQL e root físico. O provider grava um arquivo de staging, força seus bytes e faz rename atômico. O caminho final é derivado do digest e não pode ser solicitado por HTTP. Todos os reusos e leituras verificam tamanho e SHA-256. Não existem operações de delete no port físico novo.

Crash antes do rename deixa reserva e possível arquivo pending; crash depois do rename e antes do commit deixa objeto presente com reserva não confirmada. Uma nova tentativa verifica o conteúdo e conclui READY. Conteúdo corrompido não é sobrescrito. Reconciliação relata reservas, pending, conteúdo ausente/corrompido e objetos sem vínculo. Durabilidade após falha de máquina/filesystem e operação entre processos distintos ainda requer ensaio operacional; o rename não substitui backup nem garantia de fsync do diretório.

O processamento e a custódia física terminam antes da transação lógica de upload. Asset, variantes, bindings, referência e eventos são gravados juntos. Falha lógica reverte esses registros e preserva blobs já catalogados. Isso é custódia conservadora, não compensação por exclusão. Bytes compartilhados não possuem owner/permissão de negócio.

## Processamento

O adapter utiliza o processador existente: entrada até 10 MiB, até 20 milhões de pixels/10.000 por lado; normaliza orientação EXIF JPEG/PNG, remove metadados na recodificação, normaliza a MASTER para até 4096 por lado e reduz dimensões adaptativamente para 2/5 MiB. Perfil/dependentes usam teto de 2 MiB. Crops internos são independentes; avatar usa corte central quadrado e prévia preserva proporção. Os limites agregados de derivados são 20 milhões de pixels/20 MiB. O semáforo continua local à instância, não é fila distribuída.

WebP é selecionado para apresentação quando há writer ImageIO disponível; na instalação atual há fallback JPEG/PNG. Não foi acrescentado codec nativo não validado. Presets completos para todos os consumidores e contrato HTTP de crop customizado continuam pendentes. Timers `storage.image.processing` e `storage.upload.duration` permitem medir p95 em carga futura; nenhum p95 foi medido.

## Autorização e entrega

`StorageConsumerPolicy` é SPI do contexto proprietário. A integração efetiva inicial pertence a Identity e permite somente ao sujeito administrativamente ativo enviar/ler suas próprias mídias de perfil. Não há dependência de Storage em repositórios internos de Identity. Outros contextos ficam sem policy e falham de modo fechado. Isso não substitui os direitos de audiência que cada consumidor deverá fornecer.

Resolução exige IDs de asset, referência e variante, correspondência entre eles, política do consumidor, disponibilidade do asset, binding de blob e moderação global/contextual. Referência histórica não é proibida automaticamente; Identity permite o histórico do próprio sujeito. MASTER e a superfície inicial usam `private, no-store`, inclusive administração e erros. HEAD e requisições condicionais passam pela autorização e moderação. Não há ETag por hash nem URL física. O GET legado continua intocado e com o risco preexistente; não ativar cutover presumindo que foi corrigido.

As rotas ficam desabilitadas por `STORAGE_MEDIA_ENABLED=false`. O flag controla HTTP, não a aplicação futura do schema Flyway. A análise de oráculo temporal na deduplicação ainda é requisito de segurança antes da ativação; os testes atuais demonstram isolamento de identidade e bytes, não ausência de canais laterais.

## Governança e retenção

Moderação global/contextual é persistida e auditada; restauração tem permissão separada. Motivo público é padronizado e genérico, sem transportar notas internas ou inferir existência de mídia privada. Notas ficam no ledger interno. Preview administrativo também precisa da policy do consumidor: permissão de catálogo não concede leitura irrestrita.

Retenção utiliza `RetentionPolicy` existente, configuração de 365/90 dias, classe sensível histórica persistida, legal hold e transações coordenadas com criação/encerramento de referência. Histórico acima de 10.000 vínculos produz INDETERMINATE, não uma falsa elegibilidade. Contextos de dependente, medicina e identificação são protegidos. Descarte é lógico, exige ausência de vínculos ativos e preserva bytes e auditoria. Expurgo permanece proibido e não há scheduler de hard-delete.

Backfill possui checkpoint idempotente por key original, dry-run, detecção de alteração de conteúdo e rollback lógico. Importações não inventam owner/uploader, não ganham audiência e entram sob hold. Aplicação e rollback exigem `STORAGE_LEGACY_BACKFILL_ENABLED=true`, false por padrão. Não há endpoint de backfill. Nenhuma importação produtiva foi executada.
