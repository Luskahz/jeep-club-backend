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
`storageKey` e Publications associa essa referência à publicação.

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

Exemplo conceitual:

```text
DRAFT -> PUBLISHED -> ARCHIVED
```

Os nomes/transições finais devem refletir o código implementado, mas as seguintes regras
são estáveis:

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

As regras estáveis já definidas são:

- o serviço pertence ao membro criador;
- criação exige permission específica;
- possui valor e telefone de contato;
- exige aprovação administrativa antes da exposição no feed;
- pode receber imagens, curtidas e comentários por ser uma Publication;
- creator e administrador autorizado poderão possuir fluxos de exclusão conforme contrato
  da Story;
- negociação e pagamento acontecem **fora do sistema**.

ServicePublication não cria `ChargeDefinition`, `MemberCharge`, `MemberPayment`,
checkout, comissão ou qualquer fluxo financeiro interno.

A implementação funcional pertence à BACK-438.

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

A estratégia JPA final deve ser escolhida e documentada junto da implementação da BACK-397.
`JOINED` é uma alternativa natural para o modelo, mas a escolha deve ser confirmada contra
a implementação real antes de ser tratada como contrato definitivo.

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
