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

Os endpoints de interação social estão descritos na seção abaixo.

## Comentários

Comentários pertencem a uma Publication e a um membro autor.

O modelo deve permitir comentário com conteúdo textual e mídia associada. Imagens de
comentário também usam apenas `storageKey` da infraestrutura global.

O encerramento da regra específica não encerra automaticamente a discussão social. Em
particular, Event `FINISHED` pode continuar recebendo comentários enquanto sua Publication
permanecer acessível segundo o lifecycle editorial.

Comentários possuem persistência e paginação próprias; não carregar
a coleção completa dentro de toda consulta de Publication/feed.

Threading/replies, reações além de like e política avançada de moderação não fazem parte da
fundação da BACK-397.

## Notice

`Notice` é a especialização mais simples da Publication e representa comunicados como
avisos institucionais, lembrete de evento, aniversário ou informação geral.

Notice pode inicialmente não possuir atributos exclusivos além da base. Ainda assim deve
permanecer um tipo concreto próprio para evoluir independentemente.

A BACK-398 expõe uma API administrativa própria de Notice. Cada ação tem authority
específica de Notice; o controller usa `@PreAuthorize` para enforcement e
`@RequiredPermission` para OpenAPI. Como a superfície é administrativa e permissionada,
não exige `@RequiresMembership`. A consulta administrativa por ID pode ler DRAFT,
PUBLISHED e ARCHIVED; não existe leitura pública ou listagem que antecipe o feed.

Na criação, o `authorUserId` vem de `UserPrincipal.userId`, e o domínio cria o Notice em
DRAFT. O cliente envia apenas título, conteúdo e galeria; estado e timestamps pertencem
ao servidor. A resposta usa `storageKey`, posição e principal, conforme o contrato global
em que `GET /media/images?key=...` resolve a imagem. O arquivo global não é removido ao
substituir a galeria nem ao excluir o aviso.

A edição parcial preserva campos omitidos e rejeita `null` explícito para título,
conteúdo ou galeria. Enviar galeria substitui integralmente a anterior, depois de validar
as referências pelo `ImageMediaService`. DRAFT e PUBLISHED aceitam edição de conteúdo e
imagens; `updatedAt` muda sem alterar `publishedAt`. ARCHIVED é somente leitura e não
pode ser republicado. Corpo vazio é uma operação sem mudança.

Publicar e arquivar usam as transições editoriais da fundação. Excluir pode ocorrer em
qualquer estado editorial e reutiliza lock, snapshot histórico e hard delete da BACK-397;
o executor é o usuário autenticado. O OpenAPI dos controllers/DTOs é a fonte dos paths,
payloads, statuses e erros HTTP desta superfície.

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

Essas integrações são implementadas pela BACK-399 e suas subtasks, sobre a fundação
polimórfica da BACK-397. As superfícies administrativas e de membro são separadas;
somente as rotas de membro exigem também `@RequiresMembership`. As authorities
`PUBLICATIONS_EVENT_*` são específicas por ação, com enforcement e OpenAPI alinhados.

### Agenda, estados e inscrições

Event nasce OPEN e possui startsAt obrigatório/futuro na criação HTTP; endsAt,
quando presente, precisa ser posterior. `effectiveStatus(now)` resolve OPEN antes
do início, IN_PROGRESS a partir dele e FINISHED no fim configurado. Finish manual
aceita IN_PROGRESS; cancel aceita OPEN/IN_PROGRESS. FINISHED/CANCELLED são terminais.
A leitura usa Clock e não depende de scheduler; o estado temporal é derivado,
enquanto decisões terminais explícitas são persistidas. Nenhuma transição muda
PublicationStatus. Um FINISHED publicado continua sendo conteúdo publicado.

Registration possui identidade e unicidade Event + user. O principal identifica
o membro. Sem regra financeira obrigatória, confirma imediatamente; com requisitos
pendentes nasce PENDING_PAYMENT. Consultar a própria inscrição, dashboard ou
operação que exige confirmação reavalia requisitos; CONFIRMED nunca é rebaixado
pela situação financeira posterior. Cancelamento de inscrição preserva a linha,
não permite reinscrição duplicada e é limitado ao período OPEN. Cancelar o Event
preserva as inscrições como fatos e cancela seus ciclos financeiros.

Dependents ativos próprios são validados pelo contrato público de Dependents.
Podem estar alocados a um veículo ou presentes em `unallocatedDependentIds`.
Somente IDs são persistidos; a mesma pessoa não pode aparecer em dois lugares.
Veículos próprios usam `EventVehicleQuery` e a capacidade canônica de Vehicles.
Alocações relacionam veículo, membro e dependents; o membro ocupa no máximo um
veículo. Edição de alocação antes do início revalida ownership, dependents,
capacidade e convidados já aprovados. Não há escolha automática de motorista.

### Cobranças e cutoff

Cada `EventChargeRule` guarda eventId, chargeDefinitionId,
requiredForParticipation, participationCutoff e financialDueDate opcional (`LocalDate`). Não deriva obrigatoriedade do
`ChargeDefinition.required`. O catálogo público de Billing permite selecionar
ACTIVE/ONE_TIME; alternativamente o payload cria uma definição inline, com
ONE_TIME/ACTIVE/AFTER_DUE_DATE fixos e assignment próprio, na mesma transação.
Para regras required, seleção exige AFTER_DUE_DATE para permitir regularização
sem prazo após recusa tardia. Também se exige AFTER_DUE_DATE quando
financialDueDate não é configurado. Uma definição com outra política só pode
ser opcional e ter vencimento financeiro explícito.

Cutoff omitido usa startsAt e não pode ultrapassar o início. O
`participationCutoff` decide participação; `financialDueDate` configura o
vencimento financeiro e pode ocorrer depois do Event. Quando informado, é a
`dueDate` do ciclo e das MemberCharges. Quando null ou omitido na configuração
completa de charges, não há limite final de regularização definido pelo Event:
Billing usa a data UTC de startsAt como dueDate técnica, mantendo
AFTER_DUE_DATE. MemberCharge.dueDate continua obrigatória. O vencimento
financeiro não altera o cutoff de participação.
Toda cobrança, inclusive opcional, é garantida na inscrição. Billing cria um
ciclo por Event/definição e uma dívida por inscrito, usando snapshots financeiros.

Todas as required devem estar PAID ou PENDING_VALIDATION com submissão até o
cutoff. Recibo tardio, mesmo depois confirmado, não cria direito retroativo.
Depois de CONFIRMED, recusa administrativa mantém participação e dívida
regularizável. A decisão é registrada no momento da avaliação; não existe job
que confirme inscrições silenciosamente. A superfície pública não contém dívida.

PATCH preserva campos superiores omitidos e rejeita null explícito, exceto
endsAt. A lista `charges`, quando presente, substitui a configuração completa;
em cada item, financialDueDate ausente e null têm a mesma semântica de ausência
de prazo financeiro final.
Configuração financeira e agenda ficam bloqueadas após qualquer inscrição ou
dívida do contexto, inclusive inscrições canceladas. Conteúdo editorial permanece
editável conforme lifecycle de Publication. Nenhum snapshot financeiro é reescrito.

### Convidados e transporte

GuestRequest registra CPF normalizado, requester do principal, veículo opcional,
PENDING/APPROVED/REJECTED, reviewer, timestamps e motivo de rejeição. CPF é único
por Event, inclusive requests rejeitados. PENDING não ocupa vaga definitiva;
aprovação exige inscrição confirmada, capacidade atual e no máximo um guest por
veículo. Histórico de aprovações por CPF permanece consultável como aviso sem bloqueio.

Admin cria request sem transporte. Membros confirmados com vagas consultam os IDs
e respondem por veículo próprio vinculado à inscrição. RideOffer registra Event,
guest, registration, user, vehicle, ACCEPTED/DECLINED/SELECTED e timestamps.
Visualização e aceite não reservam vaga. Admin seleciona oferta aceita e a aprovação
revalida capacidade sob lock; perda da vaga produz EVENT_RIDE_CAPACITY_CHANGED.
Cada veículo responde uma vez àquela request. Não há push ou escolha automática.

### Operação, Health e persistência

Dashboard deriva inscrições, dependents, guests, pessoas confirmadas, veículos,
capacidade, lugares reservados, não pagos, pagamentos em análise e pendências
pós-cutoff. Inscrições PENDING_PAYMENT reservam sua alocação operacional, mas não
contam como pessoas confirmadas. Guests só ocupam após aprovação. A próxima leitura
reflete mutações; dados clínicos não entram no dashboard. Billing e Vehicles são
consultados em lote para essa projeção; as coleções JPA de alocação usam batch fetch.

Health exige IN_PROGRESS efetivo, participante CONFIRMED USER/DEPENDENT e permission
emergencial específica. Dependents são novamente validados no módulo proprietário.
Consulta é individual, read-only, por `EmergencyMedicalProfileQuery`. Perfil ausente
gera erro controlado. Tentativas que chegam ao caso de uso gravam ator, Event,
tipo/ID alvo, resultado e instante via `SystemLogService.recordRequired` em transação
independente. Falha de auditoria impede retorno dos dados; não grava conteúdo clínico.

Mutações bloqueiam a raiz da Publication antes de decidir inscrição ou capacidade.
Registration, GuestRequest, RideOffer e ChargeRule possuem versões JPA. Constraints
protegem Event/member, Event/CPF, Event/veículo aprovado, oferta guest/vehicle,
guest selecionado e ocupante por inscrição. Corridas são exercitadas com transações
independentes. Handlers Event são limitados aos seus controllers e retornam RFC 9457.

Hard delete exige estado terminal quando há inscrições. Snapshot preserva agenda,
estado explícito e dados editoriais; registrations, rules, guests e offers permanecem
congelados por referência escalar ao ID removido. Billing mantém seus contextos e
fatos sem FK/cascade destrutivo. A mídia física global não é removida.
Listagens administrativas de Event, guests e ofertas usam PageResponse. Coleções
operacionais são ordenadas por ID e paginadas depois da resolução de elegibilidade;
o dashboard é uma projeção completa do Event. Contratos exatos ficam no OpenAPI.

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

Na BACK-438, a API de membro para Service exige membership ativa com `@RequiresMembership`,
authority específica por ação e ownership para solicitações privadas, alteração e delete.
A API administrativa exige authorities próprias e não usa o guard de membership. O
requester e o executor do delete vêm de `UserPrincipal.userId`. O membro consulta apenas
suas requests; requests de terceiros são tratadas como não encontradas. A leitura
`GET /services/{id}` mostra somente Services `PUBLISHED`; a superfície administrativa
pode ler qualquer estado editorial. As respostas usam `storageKey` para resolver imagens
pelo endpoint global de mídia.

Alterações públicas posteriores seguem outro agregado, independente da Publication:

```text
ServicePublication PUBLISHED (última versão aprovada)
    └── PATCH do owner -> ServicePublicationChangeRequest PENDING
                            ├── reject -> REJECTED (Service inalterado)
                            └── approve -> APPROVED (mesma ServicePublication atualizada)
```

O PATCH aceita título, conteúdo, valor, telefone e galeria. Campo omitido conserva o
valor atual; `null` explícito, campo desconhecido e corpo vazio são inválidos. Galeria
enviada substitui a anterior e suas novas chaves são verificadas pelo `ImageMediaService`
antes de persistir. A change request guarda o snapshot completo proposto: dados públicos,
`servicePublicationId`, requester, status, motivo, reviewer e timestamps. Ela não recebe
likes/comments, não aparece no feed e não possui `PublicationStatus`. Enquanto PENDING,
o Service permanece publicado com a última versão aprovada.

Na aprovação, a aplicação trava a change request PENDING e depois a raiz da Publication
alvo, confere existência e owner, aplica todos os campos ao mesmo Service, salva o Service,
registra reviewer/timestamp e salva a change request numa transação. `publicationId`,
`authorUserId`, `sourceRequestId`, `createdAt` e `publishedAt` permanecem; `updatedAt`
avança. Assim curtidas, comentários e links continuam ligados ao mesmo ID. Rejeição não
altera o Service. Apenas um change request PENDING por Service é permitido: a criação
trava a raiz, consulta pendência, e `UNIQUE(pending_service_publication_id)` protege o
invariante no banco; a coluna fica nula em requests terminais. Locks e `@Version`
impedem aplicação dupla. Depois de APPROVED/REJECTED pode nascer outra request.

O proprietário pode hard deletar apenas seu Service; administrador com permission
específica pode deletar qualquer Service. A request inicial e todas as change requests
permanecem como trilha operacional. Uma change request PENDING de Service deletado
permanece PENDING, mas sua aprovação retorna erro de Service inexistente. O snapshot do
hard delete preserva a última versão aprovada, incluindo `sourceRequestId`, valor,
telefone e galeria. Objetos do storage global permanecem.

As demais regras estáveis são:

- o serviço pertence ao membro criador;
- permissions HTTP são distintas para request inicial, change request, leitura e delete de Service;
- pode receber imagens, curtidas e comentários por ser uma Publication;
- creator e administrador autorizado poderão possuir fluxos de exclusão conforme contrato
  da Story;
- negociação e pagamento acontecem **fora do sistema**.

ServicePublication não cria `ChargeDefinition`, `MemberCharge`, `MemberPayment`,
checkout, comissão ou qualquer fluxo financeiro interno.

Os contratos de HTTP, permissions, paginação e erros estão nos controllers/DTOs OpenAPI.
As listagens administrativas paginadas aceitam filtro opcional por status e retornam
`PageResponse<T>`. Billing não participa da request, aprovação ou alteração.

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
- recuperação polimórfica para consultas de feed;
- ausência de uma única tabela inflada por dezenas de campos específicos anuláveis.

A fundação usa `@Inheritance(JOINED)` apenas nas entities. `publications` guarda
conteúdo, estado e timestamps comuns; `publication_notices`, `publication_events`
e `publication_services` guardam a identidade concreta. `publication_events`
contém `starts_at`, `ends_at` e estado operacional explícito do Event. O mapper reconstrói
o subtipo de domínio; a tabela comum sustenta a leitura polimórfica sem
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

`service_publication_change_requests` também fica fora da herança JOINED e contém o
snapshot proposto, reviewer, estado e `@Version`. Sua galeria está em
`service_publication_change_request_images`, somente com `storageKey`, posição e principal.
O índice único em `pending_service_publication_id` impede duas propostas PENDING para
o mesmo Service sem impedir várias propostas aprovadas/rejeitadas ao longo do tempo.

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
`ServicePublicationRequest` e `ServicePublicationChangeRequest` não são itens do feed;
apenas Services materializados após aprovação participam da consulta, sempre
com a última versão aprovada enquanto houver proposta pendente.

`GET /publications/feed` retorna `PageResponse` de Notice, Event e Service publicados,
ordenados por `publishedAt DESC, id DESC`. Aceita `type=NOTICE|EVENT|SERVICE` e período
inclusivo `publishedFrom`/`publishedTo`. Rascunhos e arquivados não aparecem. Event
`FINISHED` continua visível se a Publication permanecer `PUBLISHED`. O item contém
`authorUserId`, campos editoriais, imagem marcada `primary`, resumo social e `details`
específico do tipo. `GET /publications/{publicationId}` retorna galeria completa na
ordem persistida e os mesmos dados públicos, sem DTO administrativo ou dados de
inscrição, cobrança, saúde ou guest. O feed usa consultas agrupadas de likes/comments,
consulta de likedByMe por conjunto de IDs e leitura em lote das imagens.

## Interações sociais

Membros autenticados com as permissões específicas podem usar `POST
/publications/{publicationId}/likes` (200 idempotente), `DELETE
/publications/{publicationId}/likes/me` (204 idempotente), `POST
/publications/{publicationId}/comments` (201) e `GET
/publications/{publicationId}/comments` (PageResponse, `createdAt DESC, id DESC`).
O autor/curtidor vem sempre do principal. Um comentário pode ter texto, imagens por
`storageKey` global ou ambos; texto vazio sem imagens é inválido. Cada chave é
validada por `ImageMediaService` antes da associação. A unicidade de like por membro
e Publication é protegida por constraint e serialização no registro principal.
O lifecycle editorial governa a acessibilidade: apenas `PUBLISHED` aceita interações;
um Event finalizado continua disponível enquanto publicado. Archive conserva os
registros, mas oculta o acesso de membro. Hard delete grava o histórico da Publication,
remove likes/comments operacionais antes do registro principal e não apaga mídia global.

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

Este README permanece a entrada principal do bounded context; os contratos públicos
acima descrevem o runtime implementado.
