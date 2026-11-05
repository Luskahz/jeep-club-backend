# BACK-549 — Fundação isolada do catálogo Storage

Data: 05/11/2026. Base inspecionada: `origin/develop` em `09dc0180d3dd433a1a15936d903e1c7814fb06cf`.

## Autoridade e escopo

[BACK-548, comentário 10357](https://jeep-club-developers.atlassian.net/browse/BACK-548?focusedCommentId=10357) e [BACK-549, comentário 10358](https://jeep-club-developers.atlassian.net/browse/BACK-549?focusedCommentId=10358) autorizam excepcionalmente esta fundação antes do fechamento da BACK-548. A task continua aberta; este documento não aprova o MVP completo. Decisões D01–D15 e adendos D03/D08/D13 da BACK-547 prevalecem sobre perguntas antigas e discovery anterior.

Entregue: domínio, persistência interna, migração aditiva, integridade, concorrência, auditoria mínima e testes. Nenhum endpoint, consumidor integrado, autorização de consumo, acesso físico, backfill, processamento de imagem, scheduler ou descarte. BACK-550 e BACK-555 fornecem requisitos de estrutura, sem entregar seus fluxos de negócio completos.

## Desenho técnico

`com.jeepclub.backend.storage` é um bounded context irmão dos outros módulos. Segue `core/domain/{model,enums}`, `core/repository`, `infra/persistence/{entity,mapper,adapter}`, como os módulos existentes. Records imutáveis e IDs Long seguem o precedente de Memberships/Member; entidades JPA ficam restritas à infraestrutura. O próprio ID gerado da tabela de asset é o `mediaId` lógico estável e nunca deriva de key/hash. Variant, reference e audit possuem IDs independentes.

A porta `MediaCatalogRepository` tem propósito restrito à persistência interna. Não é contrato público entre módulos nem prova de autorização. Métodos de gravação são transacionais e exigem contexto de auditoria. A consulta por ID não retorna bytes ou URL. Não existe consulta por storageKey, delete, API administrativa, input port de associação, serviço de consumo ou chamada a FileStorage. Camadas `application` e `api` ganharão casos de uso e contratos somente quando houver uma operação autorizada; não há serviços vazios ou controllers artificiais nesta entrega.

## Modelo e invariantes

| Registro | Responsabilidade | Invariantes |
| --- | --- | --- |
| MediaAsset | Identidade lógica, origem e contexto, sourceRecordId técnico opcional, uploader, owner, sujeito, finalidade, classificação, estado físico observado e timestamps | Uploader/owner/sujeito independentes; IDs externos escalares positivos e opcionais; sujeito UNKNOWN exige ID nulo; sujeito conhecido exige ID; updatedAt não retrocede; versão otimista |
| MediaVariant | Descrição imutável da representação, localização provider/namespace/key, MIME, bytes, dimensões, checksum opcional, preset/versão/transformação quando comprovados | Código único no asset; localização física única em provider+namespace; bytes não negativos; dimensões ambas positivas ou ambas nulas; checksum e algoritmo juntos; sem armazenamento de bytes |
| MediaReference | Intervalo de uso do asset por contexto tipado e consumidor, papel/posição e proveniência de associação | N vínculos atuais/históricos; ID consumidor opaco, sem FK externa; unlinkedAt não precede linkedAt; vínculo só termina uma vez; sem reativação ou remoção; versão otimista |
| MediaAuditEvent | Evento imutável de alteração cadastral, ator, operação, instante, alvo, versões e correlationId técnico | Sem notas livres, storageKeys, paths, nomes de arquivo ou atributos pessoais; FK do alvo inclui mediaId; append dentro da mesma transação; sem métodos de edição/exclusão |

UNKNOWN/null representa informação ausente, nunca um usuário fictício. `UNCLASSIFIED/CLASSIFIED` registra classificação cadastral, não uma audiência ou decisão de moderação. `UNKNOWN/PRESENT/MISSING` é observação física, sem verificação de filesystem neste PR; criar asset inicia UNKNOWN. Não se implementam transições de classificação/moderação ou retenção. `purpose`, `role`, presets, transformationCode, sourceRecordId e correlationId são códigos técnicos limitados, não campos de texto livre. Valores de presets não foram definidos. IDs de ator/uploader/owner/sujeito não têm FKs para Identity ou Dependents e não comprovam direitos.

MASTER é a representação canônica privada, possivelmente otimizada, sem obrigação de igualdade byte a byte com o upload. Há um único slot MASTER por asset. ORIGINAL é tratado como nome conceitual da mesma fonte; não se aceita uma segunda fonte ORIGINAL em paralelo. DERIVED requer código semântico próprio em maiúsculas. `isPrivateSource()` descreve a fonte e não concede audiência às derivadas. Toda resolução futura dependerá de ACL e bloqueio global/contextual, incluindo MASTER, derivados e respostas condicionais.

Dimensões, algoritmo/checksum, preset, versão e transformação são nulos somente se não comprovados; não são inventados para legado. Nenhum pipeline é executado. D08 aprovado para implementação futura: entrada máxima **10 MiB (10.485.760 bytes)**; MASTER armazenada até **2 MiB** em perfil/dependente ou **5 MiB** nas demais categorias aprovadas. Os valores são documentais nesta fundação: a presença de metadados não prova decodificação/otimização segura. Validação multipart, pixels, formatos, qualidade, crop/EXIF e benchmark continuam fora.

## Transações e concorrência

Cada operação do adapter grava o registro e o evento antes do commit. Pode participar de transação externa; falha posterior desfaz registros e eventos. Não há IO para compensar. `@Version` em asset e reference protege alterações concorrentes inclusive com instâncias destacadas. Atualização de asset aceita somente finalidade/timestamp; origem, identidade e proveniência não podem ser reatribuídas pela atualização. Finalização de referência mantém imutáveis consumidor, contexto, papel, posição e data/ator de associação. Variantes e eventos são append-only no adapter e `@Immutable` no Hibernate.

Não existe garantia de ACL, sincronia com agregado consumidor ou proteção contra writer SQL privilegiado nesta fundação. O futuro caso de uso deve revalidar direitos e estado dos agregados dentro da transação e definir ordem de locks antes de integrar consumidores. Inserir referência no repositório interno não autoriza associação em API. Bloqueio global e contextual são D09, mas não há máquina de moderação presumida.

## Schema e compatibilidade

A próxima migration encontrada foi **V13__storage_catalog_foundation.sql**, após V12. Cria somente:

- `storage_media_assets`;
- `storage_media_variants`;
- `storage_media_references`;
- `storage_media_audit_events`.

FKs são internas e restritivas, sem cascata destrutiva. Auditoria usa FKs compostas `(media_id,variant_id/reference_id)` para impedir vincular evento a alvo de outro asset. Unicidade de código é por asset; localização é por provider+namespace+storageKey. Referências não têm unicidade de consumidor/papel que impedisse múltiplos usos/histórico. Índices atendem uploader, owner, sujeito, classificação, consumidor/intervalo, asset/intervalo e trilha por asset/data/ID. Não há deduplicação por checksum.

A localização e o código semântico usam `utf8mb4_0900_bin` (MySQL 8), comparação sensível a caixa e NO PAD. Não normalizamos storageKey nem impomos regex de caminhos do provider atual. A key permanece interna, opaca e nunca autoriza acesso. Nenhuma migration aplicada foi editada. Nenhuma tabela legada recebe FK para Storage. O helper de fixtures históricas exclui mappings Storage antes da V13; o gate de equivalência descreve separadamente as propriedades físicas específicas de MySQL.

Flyway de produção é MySQL no repositório. `MediaCatalogMySqlIT` migra V1–V13 e inicializa Hibernate validate. `MySqlMigrationIT` cobre schema completo, baseline histórico, preservação e evolução. O suporte PostgreSQL existente usa DDL Hibernate, sem cadeia Flyway PostgreSQL: `MediaCatalogPostgresIT` mantém essa estratégia e exercita as mesmas invariantes e transações no PostgreSQL real. Não se declara portabilidade das migrations MySQL V1–V13 para PostgreSQL.

Records sobrescrevem toString para não imprimir localização, dados pessoais ou metadados. Nenhum logger foi adicionado; proteção já existente de JDBC errors/bind parameters permanece. Futuras APIs não devem serializar domínio ou entidades JPA diretamente, nem propagar exceções de banco com valores internos.

## Contratos e gates futuros

BACK-548 continua aberta. Dependem de aprovação final: famílias do MVP/Billing; matriz ACL/audiências D03/D13; reuso/temporários/deduplicação D04; presets, formatos, dimensões e qualidade D06/D08; permissões/razões de moderação D09/D10; temporalidade jurídica e descarte D10/D11. Não há enum de audiências, permissions, motivo de moderação ou prazo legal inventado.

Próximos PRs devem tratar separadamente: pipeline síncrono com benchmark p95/concorrência (BACK-551), ACL e resolvedor por mediaId+variantCode (BACK-552), sincronização de vínculos/DTOs consumidores após autorização (BACK-550/559), trilha administrativa e visibilidade (BACK-555), moderação (BACK-554), inventário/reconciliação (BACK-556), política de retenção (BACK-557), backfill preservando arquivos/keys (BACK-558). BACK-593/scheduler é independente.

D13: comuns exigem revalidação de ACL/moderação em cada request, inclusive 304/HEAD/Range; sensíveis/MASTER exigem private,no-store. Não há mudança HTTP agora. O GET legado e seu problema conhecido de acesso por key continuam como antes; a fundação não corrige nem ativa o cutover.

Sem merge, deploy, operação em VPS, backup operacional, frontend ou dados reais. Resultado de teste local/CI é evidência técnica, sem aceitação operacional.

## Registro de validação

Os comandos e resultados finais do SHA publicado ficam na descrição do PR, com link para a execução de CI. O perfil `mysql-migrations` inclui MediaCatalogMySqlIT; `publications-postgresql` inclui MediaCatalogPostgresIT por convenção de sufixo. A execução normal inclui MediaDomainTest e MediaCatalogPersistenceTest.

Durante a implementação foram corrigidos: isolamento do slice JPA (não carregar repositories de outros módulos), nomes duplicados de checks e enums VARCHAR com checks nomeados/converters explícitos. O teste H2 usa datasource com pool para não depender de sessão de DDL fechada na avaliação de checks. Não se alterou versão do H2 nem configuração operacional.

A primeira comparação física integral falhou porque a migration declarava RESTRICT nas cinco FKs Storage enquanto o DDL JPA usava NO ACTION. O mapping passou a declarar `ForeignKey.options` com `ON DELETE RESTRICT ON UPDATE RESTRICT`, preservando a restrição em ambos os dialetos. A comparação integral permanece exigida; nenhuma diferença foi ignorada ou removida do snapshot. Essa falha é distinta dos testes funcionais do catálogo, que já haviam passado em MySQL e PostgreSQL. Resultados de commits anteriores não substituem os gates do HEAD final.
