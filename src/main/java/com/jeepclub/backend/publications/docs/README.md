# Publications

Leia primeiro a [governança global](../../../../../../../../docs/architecture/README.md), a
[organização dos módulos](../../../../../../../../docs/architecture/module-organization.md)
e as [regras de desenvolvimento](../../../../../../../../docs/architecture/feature-development-rules.md).

Este documento registra as decisões arquiteturais e de domínio estáveis do bounded context
`publications`. O OpenAPI será a fonte de verdade dos contratos HTTP à medida que as
Stories de implementação adicionarem controllers, requests, responses, permissions e erros.

## Responsabilidade e linguagem do domínio

`Publication` é a raiz conceitual de todo conteúdo que pode aparecer no feed de membros,
mas **não é uma publicação concreta** e não deve ser instanciada diretamente.

A hierarquia funcional da V1 é:

```text
Publication (abstrata)
├── Notice
├── Event
└── ServicePublication
```

Toda publicação nasce com um propósito de domínio. Não existe "publicação genérica" no
produto.

As especializações compartilham conteúdo editorial, mídia e capacidades sociais, mas
mantêm seus próprios campos, lifecycles e regras. Um enum/discriminador técnico pode
existir quando necessário para persistência ou representação, porém não pode substituir o
polimorfismo com `switch`/condicionais centrais que concentrem o comportamento do módulo.

## Ownership

Publications é proprietário de:

- identidade e conteúdo editorial da publicação;
- referência ao usuário autor/originador;
- lifecycle editorial comum;
- timestamps da publicação;
- associação entre Publication e suas imagens;
- identidade das interações sociais ligadas à publicação;
- snapshots históricos produzidos pelo hard delete do próprio bounded context.

Publications **não** é proprietário do cadastro de User/Member, Vehicle, Dependent,
MemberCharge/MemberPayment ou dados de Health.

Referências a outros bounded contexts devem armazenar apenas os identificadores necessários
e usar contracts públicos ou ports consumer-owned para validação/consulta. Nunca importar
entity, repository ou service interno de outro módulo.

## Autoria

A referência canônica do autor é o `userId` interno.

Quando os fluxos HTTP forem implementados, o autor de operações de membro deve vir do
`UserPrincipal.userId`; o cliente não pode escolher arbitrariamente o autor pelo payload.

Não copiar nome, CPF, e-mail ou outros dados de Identity para a Publication apenas por
conveniência de leitura.

## Conteúdo comum

A classe base deve concentrar somente atributos e comportamentos realmente comuns às
especializações.

O modelo final deve contemplar, no mínimo, os conceitos de:

- identificador;
- autor/originador;
- título/conteúdo conforme as invariantes definidas na implementação;
- estado editorial;
- timestamps de criação/alteração/publicação/arquivamento quando aplicáveis;
- galeria de imagens;
- identidade necessária para curtidas e comentários.

Campos exclusivos de Event, Notice ou ServicePublication não pertencem à classe base.

## Imagens

Toda Publication concreta deve possuir **entre 1 e 5 imagens**, inclusive, com:

- exatamente uma imagem principal;
- ordem determinística;
- referência por `storageKey`;
- validação da existência da mídia antes da associação.

Publications não armazena arquivo, path físico, provider, bucket ou URL persistida.

O upload é transversal e ocorre pela infraestrutura global de mídia. O consumidor recebe a
`storageKey` e Publications associa essa referência à publicação ou a uma solicitação
de Service antes da aprovação.

Conceitualmente:

```text
POST /media/images
        ↓
ImageMediaService / FileStorage
        ↓
storageKey
        ↓
PublicationImage
```

A exclusão ou substituição de uma associação de imagem não implica exclusão física do
objeto global. Uma mesma chave pode ter outras referências; limpeza de órfãos é uma
responsabilidade transversal e não deve ser presumida por Publications.

## Lifecycle editorial

O lifecycle editorial de `Publication` é independente do lifecycle específico de suas
especializações.

Transições implementadas na fundação:

```text
DRAFT -> PUBLISHED -> ARCHIVED
```

Notice e Event nascem em `DRAFT` com `createdAt = updatedAt`. `publish(now)` aceita
somente `DRAFT`, preenche `publishedAt` e atualiza `updatedAt`; `archive(now)` aceita
somente `PUBLISHED`, preenche `archivedAt` e atualiza `updatedAt`. O domínio rejeita
transição repetida, regressão temporal e edição de imagens após arquivamento. Não há
transição de volta para `DRAFT` nem reativação de `ARCHIVED` nesta fundação. Os
application services passam `Instant.now(clock)` para essas operações. ServicePublication
nasce diretamente em `PUBLISHED`, com `createdAt = updatedAt = publishedAt`, porque sua
criação materializa a aprovação da solicitação. `Publication.publish` não conhece essa
aprovação e não há uma segunda etapa administrativa.

As seguintes regras continuam estáveis:

- arquivamento não é delete;
- `DELETED` não é status operacional;
- finalizar/cancelar uma especialização não implica automaticamente arquivar ou apagar a
  Publication;
- um Event pode estar `FINISHED` e continuar publicado como registro social do evento.

Toda lógica dependente de "agora" deve usar o `Clock` global/injetado. Não esconder
`Instant.now()`/equivalente dentro do domínio.

## Curtidas

Curtir é uma capacidade comum da Publication.

O modelo deve permitir no máximo uma curtida por par:

```text
(publicationId, memberUserId)
```

A unicidade precisa ser protegida também na persistência para suportar concorrência.

Remover a curtida remove a interação operacional. Não manter contador mutável dentro de
Publication na fundação; contagens podem ser derivadas por query/projeção para evitar
divergência entre contador e linhas reais.

Os endpoints completos pertencem à Story de interações sociais.

## Comentários

Comentários pertencem a uma Publication e a um membro autor.

O modelo deve permitir comentário com conteúdo textual e mídia associada. Imagens de
comentário também usam apenas `storageKey` da infraestrutura global.

O encerramento da regra específica não encerra automaticamente a discussão social. Em
particular, Event `FINISHED` pode continuar recebendo comentários enquanto sua Publication
permanecer acessível segundo o lifecycle editorial.

Comentários devem possuir persistência própria e futuramente paginação própria; não carregar
a coleção completa dentro de toda consulta de Publication/feed.

Threading/replies, reações além de like e política avançada de moderação não fazem parte da
fundação da BACK-397.

## Notice

`Notice` é a especialização mais simples da Publication e representa comunicados como
avisos institucionais, lembrete de evento, aniversário ou informação geral.

Notice pode inicialmente não possuir atributos exclusivos além da base. Ainda assim deve
permanecer um tipo concreto próprio para evoluir independentemente.

A criação/edição/publicação/delete HTTP e suas permissions são responsabilidade da
BACK-398.

## Event

`Event` representa um evento real do Jeep Club e possui lifecycle próprio separado do
lifecycle editorial.

As capacidades funcionais já definidas para evolução do Event incluem:

- data de realização;
- inscrição de membros;
- associação de dependents;
- associação de vehicles;
- convidados aprovados;
- referências opcionais a uma ou mais cobranças de Billing;
- operação administrativa de participantes, ocupação e vagas;
- acesso emergencial e permissionado a dados de Health durante o evento.

Essas integrações pertencem à BACK-399 e suas subtasks. A BACK-397 deve apenas garantir que
a fundação não impossibilite essa evolução.

Billing continua dono de cobrança/pagamento; Vehicles continua dono do veículo e sua
capacidade; Dependents continua dono do dependent; Health continua dono dos dados de saúde.

## ServicePublication

`ServicePublication` representa um serviço oferecido por um membro à comunidade do Jeep
Club.

O membro cria uma `ServicePublicationRequest` independente. Ela não estende Publication,
não ocupa linha em `publications`, não entra no feed e não recebe likes/comments. A
request contém autor, título, conteúdo, valor informativo (`BigDecimal` com duas casas),
telefone de contato e galeria de 1 a 5 referências `storageKey` com uma principal e posições
contínuas. A mesma validação de galeria do domínio é usada nas Publications e nas requests;
o application service verifica a existência de cada chave pelo `ImageMediaService` antes de
persistir a solicitação. Rejeitar uma request não remove mídia do storage global.

```text
ServicePublicationRequest PENDING
    ├── reject(reason, reviewer, now)  -> REJECTED (sem Publication)
    └── approve(reviewer, serviceId, now) -> APPROVED
                                            ↓
                              ServicePublication PUBLISHED
```

A aprovação administrativa usa uma transação: carrega a request PENDING sob lock
pessimista, cria a ServicePublication com os dados aprovados, persiste a Publication para
obter seu ID, registra `reviewedByUserId`, `createdPublicationId` e `reviewedAt` na request,
persiste a request e faz commit. Falha em qualquer gravação causa rollback de ambas. A
unicidade de `publication_services.source_request_id` impede dois Services da mesma request
mesmo diante de erro de aplicação. Uma request aprovada permanece como trilha operacional;
não é um snapshot de hard delete.

O Service real possui `sourceRequestId` imutável, `amount` e `contactPhone`. Suas imagens
são a galeria revisada na request. `publishedAt` é o instante da aprovação, sem um estado
intermediário de Service pendente. Somente o Service já criado recebe interações sociais.

As demais regras estáveis são:

- o serviço pertence ao membro criador;
- as permissions HTTP específicas serão definidas na BACK-438;
- pode receber imagens, curtidas e comentários por ser uma Publication;
- creator e administrador autorizado poderão possuir fluxos de exclusão conforme contrato
  da Story;
- negociação e pagamento acontecem **fora do sistema**.

ServicePublication não cria `ChargeDefinition`, `MemberCharge`, `MemberPayment`,
checkout, comissão ou qualquer fluxo financeiro interno.

Os controllers, permissions, contratos OpenAPI, listagens, edição, exclusão por owner/admin
e eventual regra de reenvio pertencem à BACK-438. Billing não participa da request nem da
aprovação.

## Permissions

Publication não concede uma permission genérica que autorize todas as especializações.

As operações devem ter permissions explícitas por ação e por domínio quando aplicável. Ter
permission para criar Notice não implica permission para criar Event ou ServicePublication.

O enforcement HTTP futuro usa o mecanismo global de Authorization/`@PreAuthorize`; não
criar uma tabela local `ClubPublicationPermission` como no desenho histórico antigo.

`@RequiredPermission`, quando usado, permanece documental/OpenAPI e deve refletir a
authority efetivamente exigida.

## Persistência das especializações

O domínio não depende de JPA.

Entities, repositories JPA, mappers e adapters pertencem a
`publications.infra.persistence`.

A estratégia de persistência deve suportar:

- dados comuns em Publication;
- evolução independente de Notice, Event e ServicePublication;
- recuperação polimórfica para consultas futuras de feed;
- ausência de uma única tabela inflada por dezenas de campos específicos anuláveis.

A fundação usa `@Inheritance(JOINED)` apenas nas entities. `publications` guarda
conteúdo, estado e timestamps comuns; `publication_notices`, `publication_events`
e `publication_services` guardam a identidade concreta. `publication_events`
contém `starts_at`, a agenda mínima do Event nesta Story. O mapper reconstrói
o subtipo de domínio; a tabela comum sustenta a leitura polimórfica futura sem
colunas próprias de cada especialização anuláveis. `publication_images` guarda
somente `storage_key`, posição e principal, com unicidade de posição/chave por
Publication. A constraint `UNIQUE(publication_id, member_user_id)` protege
`publication_likes`. `publication_comments` e `publication_comment_images` são
separadas da galeria da Publication, permitindo consultas sociais independentes.

`service_publication_requests` é uma entity independente da herança JOINED, com `@Version`
e galeria em `service_publication_request_images`. Guarda estado PENDING/APPROVED/REJECTED,
autor, conteúdo, valor, telefone, dados da revisão e ID da Publication criada.
`publication_services` guarda `source_request_id` único, `amount` e `contact_phone`.
O lock da linha da request serializa revisões concorrentes; a versão fornece proteção
adicional contra gravação de estado obsoleto.

O projeto continua seguindo a política global vigente de schema derivado das entities /
Hibernate; não introduzir Flyway/Liquibase isoladamente neste módulo.

## Hard delete e histórico

A exclusão operacional segue o padrão consolidado em módulos como Tools e Vehicles:
**snapshot histórico + hard delete na mesma transação**.

Fluxo obrigatório:

```text
carregar recurso sob lock
        ↓
criar snapshot histórico
        ↓
registrar deletedByUserId + deletedAt
        ↓
persistir histórico
        ↓
hard delete operacional
        ↓
commit
```

Se o snapshot histórico falhar, a remoção operacional deve sofrer rollback.

O histórico deve preservar informação suficiente para reconstruir o estado auditável no
momento da exclusão, incluindo:

- identificador original;
- autor;
- conteúdo comum;
- estado editorial;
- timestamps;
- especialização concreta;
- campos próprios da especialização existentes naquele momento;
- referências de imagens associadas;
- usuário executor da exclusão;
- instante da exclusão.

O histórico não é um estado operacional reativável e não substitui a tabela atual.

Hard delete da Publication remove suas associações operacionais do bounded context, mas não
deve assumir ownership exclusivo do arquivo armazenado no storage global.

O histórico também usa JOINED: `publication_history` guarda o snapshot comum,
`publication_notice_history`, `publication_event_history` e
`publication_service_history` preservam o subtipo, e
`publication_image_history` preserva as chaves, posições e imagem principal.
`publication_service_history` preserva também `source_request_id`, `amount` e
`contact_phone`. A `ServicePublicationRequest` continua persistida após o hard delete do
Service, pois registra a aprovação; o history registra o estado do Service eliminado.
No delete, o adapter trava primeiro a linha de `publications` com
`SELECT ... FOR UPDATE`, carrega o subtipo, grava e faz flush do histórico,
remove comentários/curtidas operacionais e então remove a Publication e sua
galeria. Tudo ocorre na transação do application service. O lock direto na
tabela raiz evita o follow-on locking problemático do Hibernate em uma consulta
polimórfica JOINED quando outro delete acaba de remover a linha.

Os testes da fundação cobrem invariantes de domínio e mídia, transições,
reconstituição dos três subtipos, persistência JOINED, unicidade de Like,
imagens de comentário, snapshot de cada subtipo, delete repetido, rollback
quando history falha e corrida de dois deletes com lock pessimista. Os testes de request
cobrem aprovação/rejeição, validação de mídia, unicidade de origem, rollback transacional
e aprovação concorrente sem duplicar Service.

## Boundaries conhecidos

As integrações previstas devem respeitar os seguintes owners:

| Contexto | Owner | Uso futuro por Publications |
| --- | --- | --- |
| Identity/User | Identity | validar/identificar autor ou membro por contrato público |
| Billing | Billing | cobranças e pagamentos relacionados a Event |
| Vehicles | Vehicles | veículo, ownership e capacidade para Event |
| Dependents | Dependents | dependents associados à inscrição em Event |
| Health | Health | consulta emergencial read-only e auditada em Event |
| Storage/Media | Platform/shared | armazenar e carregar objetos referenciados por storageKey |

Publications não deve criar ports especulativos sem um caso de uso consumidor real. As
interfaces cross-module são adicionadas nas Stories em que a integração se torna necessária.

## Feed

O feed é uma projeção/leitura unificada das especializações concretas:

```text
Notice
Event
ServicePublication
```

Não existe item de Publication genérica.
`ServicePublicationRequest` não é item do feed; apenas Services materializados após
aprovação podem participar da consulta futura.

A consulta final, filtros, paginação, detalhe polimórfico, contagens de interação e
otimizações contra N+1 pertencem à BACK-400.

## Escopo da BACK-397

A BACK-397 é responsável por estabelecer:

- estrutura do bounded context;
- domínio base e especializações;
- invariantes comuns;
- mídia;
- fundação de Like/Comment;
- persistência e mapeamento;
- lifecycle editorial;
- hard delete/history;
- documentação e testes da fundação.

Permanecem fora desta Story:

- CRUD HTTP completo de Notice/Event/Service;
- feed final;
- endpoints completos de likes/comments;
- inscrições e operação completa de Event;
- implementação interna de Billing/Vehicles/Dependents/Health;
- pagamentos de Service;
- storage próprio.

## Stories relacionadas

- **BACK-397** — fundação de domínio, persistência e lifecycle;
- **BACK-398** — criação e gestão de Notice;
- **BACK-399** — Event, inscrições e operação;
- **BACK-400** — feed e consultas;
- **BACK-438** — ServicePublication com aprovação;
- **BACK-439** — curtidas e comentários com mídia.

As regras detalhadas de cada capacidade devem ser documentadas quando o runtime
correspondente for implementado, mantendo este README como entrada principal do bounded
context e evitando transformar documentação futura em contrato antes do código.
