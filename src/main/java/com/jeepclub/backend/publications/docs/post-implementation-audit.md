# Auditoria pós-implementação de Publications — BACK-71

Baseline: `develop@f9dc5631adb6cd6c28a82c22e939b365673d14a4` (fetch de 2026-09-27).
Branch: `docs/publications-post-implementation-audit`. Escopo: diagnóstico de código,
correção de documentação e OpenAPI descritivo. Nenhuma regra funcional foi alterada.

## Síntese e método

Foram percorridos controllers, application services, domínio, ports, adapters, entities,
constraints e handlers de Publications; os contratos públicos e as operações relevantes de
Billing, Vehicles, Dependents, Health e logging; as Stories BACK-397, BACK-398, BACK-438,
BACK-399, BACK-439, BACK-400, as subtasks BACK-440 a BACK-447; e os documentos de
`publications/docs`, `billing/docs`, `vehicles/docs`, `health/docs`,
`platform/logging/docs` e `docs/architecture`.

A estrutura abstrata `Publication`/`Notice`/`Event`/`ServicePublication` está preservada.
Request inicial e change request de Service ficam fora da hierarquia e do feed; a
aprovação inicial cria um Service publicado, e a aprovação de change request atualiza o
mesmo ID. Os controllers exigem permissions específicas; rotas de membro usam
`@RequiresMembership`. Feed e detalhe retornam apenas `PUBLISHED`, com resumos sociais
agrupados e sem DTO financeiro, CPF de guest ou dado clínico. Notice usa semântica de
PATCH explícita e snapshot antes do hard delete. Essas verificações não eliminam os riscos
abaixo. A integração mais sensível é Event + Billing.
Foram confirmados 12 findings: 1 BLOCKER, 2 HIGH, 7 MEDIUM e 2 LOW.

## Findings de código e produto

### PUB-A01 — BLOCKER — Event/Billing: ciclo finalizado sobrevive ao cancelamento

- **Arquivos:** `billing/core/application/service/event/EventBillingService.java:81-86`;
  `billing/api/http/controller/admin/AdminChargeCycleController.java:136-149`;
  `publications/core/application/service/AdminEventService.java:95-97`.
- **Comportamento atual:** o comando administrativo pode finalizar qualquer ciclo gerado.
  Ao cancelar o Event, Billing visita seus contextos e chama `cancel` somente se o ciclo
  ainda for `GENERATED`; `FINISHED` é ignorado.
- **Problema/cenário:** gerar dívida de Event, finalizar o ciclo pelo endpoint de Billing e
  depois cancelar o Event. O ciclo e as cobranças continuam ativos; o fluxo de
  elegibilidade de refund do cancelamento não roda para ele.
- **Impacto:** cobrança persistente e possível ausência de reembolso após Event cancelado.
  **Causa provável:** contrato genérico de finalização de ciclo não considera o vínculo
  `EventChargeContext`; cancelamento filtra por estado.
- **Correção recomendada:** definir a política para ciclo de Event finalizado e impedir a
  sequência inconsistente ou compensá-la financeiramente no cancelamento.
- **Testes recomendados:** integração Event criado → inscrito/pago → ciclo `FINISHED` →
  Event `CANCELLED`, incluindo cobrança e refund; H2 e PostgreSQL.
- **Jira relacionado:** BACK-399, BACK-441, BACK-447.

### PUB-A02 — HIGH — Billing: refund de confirmação tardia pode nascer expirado

- **Arquivos:** `billing/core/application/service/memberrefund/AdminMemberRefundService.java:29,76-84`;
  `billing/core/domain/model/MemberRefund.java:245-258,266-279`;
  `billing/core/application/service/memberpayment/AdminMemberPaymentService.java:44-63`.
- **Comportamento atual:** confirmar comprovante `PENDING_VALIDATION` após o cancelamento
  cria `ELIGIBLE` até `canceledAt + 30 dias`. `request` e `approve` rejeitam elegibilidade
  vencida.
- **Problema/cenário:** administração confirma o comprovante 31 dias após cancelar o
  Event; existe pagamento confirmado e registro `ELIGIBLE`, porém ninguém consegue
  solicitar ou aprovar a devolução pela janela normal.
- **Impacto:** regularização financeira tardia sem caminho operacional de refund.
  **Causa provável:** janela de cancelamento aplicada também a pagamentos cuja confirmação
  ocorreu após seu término.
- **Correção recomendada:** decidir regra de prazo para confirmação tardia e fornecer
  caminho explícito, preservando idempotência e auditoria.
- **Testes recomendados:** confirmação no dia 29, 30 e 31, request/approve e duplicidade.
- **Jira relacionado:** BACK-399, BACK-441, BACK-447. BACK-447 pede preservar a janela
  vigente salvo decisão explícita; essa decisão de produto é necessária.

### PUB-A03 — HIGH — Vehicles: capacidade pode mudar após alocação

- **Arquivos:** `publications/core/application/service/internal/EventOperations.java:58-66,85-96`;
  `publications/core/application/service/AdminEventService.java:125-143`;
  `vehicles/core/application/service/vehicle/VehicleService.java:88-125`.
- **Comportamento atual:** alocação consulta a capacidade atual, mas edição/hard delete de
  Vehicle não consulta Event. Dashboard soma apenas veículos ainda ativos e limita vagas
  disponíveis com `Math.max(0, ...)`.
- **Problema/cenário:** alocar quatro pessoas em veículo de quatro lugares e depois
  reduzir a capacidade para dois ou excluir o veículo. A inscrição permanece; o
  dashboard mostra zero vagas, sem explicitar déficit, e operações que exigem
  `findActive` deixam de funcionar.
- **Impacto:** ocupação inválida e operação de guests/transporte bloqueada.
  **Causa provável:** falta de política de atualização entre contextos para alocação viva.
- **Correção recomendada:** decidir bloqueio, revalidação ou snapshot de capacidade em
  Event; representar déficit explicitamente no painel.
- **Testes recomendados:** edição/exclusão de veículo alocado, guest aprovado e corrida
  entre mudança de capacidade e seleção da última vaga em PostgreSQL.
- **Jira relacionado:** BACK-399, BACK-442, BACK-444, BACK-445.

### PUB-A04 — MEDIUM — Histórico: snapshot não reconstrói a operação do Event

- **Arquivos:** `publications/infra/persistence/mapper/PublicationHistoryMapper.java:9-40`;
  `publications/infra/persistence/adapter/PublicationRepositoryAdapter.java:79-91`;
  `publications/core/application/service/AdminEventService.java:99-103`.
- **Comportamento atual:** histórico copia editoria, galeria, agenda e estado Event
  persistido. Registrations, charge rules, guests e ride offers ficam em tabelas
  operacionais referenciadas por ID escalar. Likes/comments são removidos e não entram
  no snapshot. O status efetivo temporal pode ser `FINISHED` enquanto o status
  persistido copiado é `OPEN`.
- **Problema/cenário:** hard delete de Event encerrado por `endsAt` sem `finish` manual:
  o histórico pode registrar `OPEN`; a composição de participantes não é recuperável
  por um agregado de histórico e as rotas Event deixam de alcançar as linhas retidas.
- **Impacto:** trilha de auditoria fragmentada e potencial divergência entre estado
  visto antes do delete e snapshot. **Causa provável:** snapshot restrito à entity raiz.
- **Correção recomendada:** definir escopo auditável e consulta/retention de fatos
  associados, com estado efetivo e persistido distintos quando necessário.
- **Testes recomendados:** delete após fim temporal, reconstrução histórica de
  inscrições/guests/charges e política de retenção de CPF.
- **Jira relacionado:** BACK-397, BACK-399, BACK-440, BACK-443.

### PUB-A05 — MEDIUM — Cancelamento de inscrição deixa pendências financeiras e guests

- **Arquivos:** `publications/core/application/service/EventService.java:34-40`;
  `publications/core/application/service/internal/EventOperations.java:97-123`;
  `billing/core/application/service/event/EventBillingService.java:61-79`.
- **Comportamento atual:** `cancel` muda apenas a Registration. MemberCharge já criada
  permanece no ciclo do Event; guest request `PENDING` e offers permanecem. A aprovação
  posterior do guest depende de inscrição `CONFIRMED`, portanto passa a falhar.
- **Problema/cenário:** inscrito cancela antes do Event após receber dívida e pedir guest.
  A dívida continua cobrável; request e offers ficam pendentes sem dono operacional
  elegível. O Jira define o cancelamento do Event, mas não fecha a política financeira
  do cancelamento individual.
- **Impacto:** cobrança possivelmente indevida e filas administrativas sem resolução.
  **Causa provável:** operação restrita ao agregado Registration, sem comando compensatório.
- **Correção recomendada:** decidir política de dívida/refund por cancelamento individual
  e estados terminais das solicitações ligadas; implementar em task própria.
- **Testes recomendados:** cancelamento com charge aberta, pagamento pendente/confirmado,
  guest pendente e offer aceita.
- **Jira relacionado:** BACK-399, BACK-440, BACK-441, BACK-443, BACK-444.

### PUB-A06 — MEDIUM — Consultas operacionais fazem trabalho repetido em memória

- **Arquivos:** `billing/core/application/service/event/EventBillingService.java:89-106`;
  `publications/core/application/service/internal/EventOperations.java:28-31,85-96`;
  `publications/core/application/service/AdminEventService.java:125-143`;
  `publications/api/http/dto/EventPages.java:7-12`;
  `publications/infra/persistence/adapter/PublicationRepositoryAdapter.java:85-91`.
- **Comportamento atual:** `findByEvent` filtra todas as cobranças e pagamentos para
  cada contexto/charge por streams aninhadas; dashboard chama a consulta financeira
  via `refreshed` e novamente para contagem. Operações de vaga recarregam listas
  completas; listagens paginam somente após materializar todos os registros.
  Hard delete carrega todos os comments/likes antes de removê-los.
- **Problema/cenário:** Event grande com muitas inscrições, cobranças e guests aumenta
  tempo e memória por item; o dashboard também pode gravar confirmações ao ler.
- **Impacto:** latência, locks mais longos e possível degradação sob carga.
  **Causa provável:** projeções por coleção completa, apesar de batch fetch JPA.
- **Correção recomendada:** agrupar por IDs uma vez, consultar registros específicos e
  paginar no banco quando elegibilidade permitir; medir SQL e heap antes de otimizar.
- **Testes recomendados:** benchmark/contagem SQL com cardinalidade crescente e
  medição de GET dashboard com inscrições pendentes.
- **Jira relacionado:** BACK-399, BACK-441, BACK-444, BACK-445.

### PUB-A07 — MEDIUM — Interações sociais disputam lock da raiz

- **Arquivos:** `publications/core/application/service/PublicationLikeService.java:21-38`;
  `publications/core/application/service/PublicationCommentService.java:29-57`;
  `publications/infra/persistence/jpa/PublicationJpaRepository.java:12-15`.
- **Comportamento atual:** like, unlike e criação de comentário travam a linha
  `publications` com `FOR UPDATE`; delete e edição usam a mesma raiz.
- **Problema/cenário:** muitas interações simultâneas em Event popular ficam
  serializadas mesmo entre usuários independentes.
- **Impacto:** throughput baixo e timeout/contenda; a constraint única de like
  já protege duplicidade persistente. **Causa provável:** lock da raiz usado como
  guarda uniforme do estado editorial e da exclusão.
- **Correção recomendada:** projetar coordenação de estado/delete com locks de menor
  escopo, mantendo unicidade e rejeição de interação após archive/delete.
- **Testes recomendados:** corrida like/comment/archive/delete e benchmark PostgreSQL.
- **Jira relacionado:** BACK-397, BACK-439.

### PUB-A08 — MEDIUM — Comentários sem limites de payload próprios

- **Arquivos:** `publications/api/http/controller/member/PublicationSocialController.java:46-51,77-90`;
  `publications/core/domain/model/PublicationComment.java:21-65`;
  `publications/infra/persistence/entity/PublicationCommentEntity.java:19-30`.
- **Comportamento atual:** comentário exige texto ou imagem e posição/chave única,
  mas não há teto de comprimento do texto ou quantidade de imagens; cada chave é
  validada individualmente no storage. A coluna de conteúdo é `TEXT`.
- **Problema/cenário:** payload com milhares de imagens ou texto muito grande pode
  consumir tempo, memória e consultas/IO de storage por request autenticado.
- **Impacto:** abuso de recursos. **Causa provável:** a decisão de produto de limite
  de comentários ficou fora da BACK-439 (que explicitamente não herda 1..5 da galeria).
- **Correção recomendada:** definir limites técnicos proporcionais e documentá-los,
  sem impor arbitrariamente o limite editorial da Publication.
- **Testes recomendados:** limites aceitos/rejeitados, tamanho HTTP e custo de validação.
- **Jira relacionado:** BACK-439.

### PUB-A09 — MEDIUM — Handler Event mascara falhas internas como erro de cliente

- **Arquivos:** `publications/api/http/exception/EventExceptionHandler.java:28-46`;
  `publications/api/http/exception/PublicationSocialExceptionHandler.java:21-33`.
- **Comportamento atual:** qualquer `NullPointerException` de Event vira 400
  `EVENT_INVALID_REQUEST`; qualquer `DataAccessException` vira 409. Em social,
  toda violação de integridade vira `PUBLICATION_LIKE_CONFLICT`, inclusive comentário.
- **Problema/cenário:** NPE inesperado de aplicação, indisponibilidade SQL ou FK de
  comentário são apresentados como entrada inválida/conflito conhecido.
- **Impacto:** diagnóstico incorreto, alertas 5xx perdidos e retry indevido pelo
  cliente. **Causa provável:** handlers genéricos demais para exceções não caracterizadas.
- **Correção recomendada:** mapear apenas exceções esperadas e constraints
  identificadas; deixar falhas inesperadas no handler global de servidor.
- **Testes recomendados:** falha SQL não relacionada a concorrência, NPE interno e
  violação de integridade de comentário.
- **Jira relacionado:** BACK-398, BACK-399, BACK-439.

### PUB-A10 — MEDIUM — Concorrência e schema sem validação PostgreSQL

- **Arquivos:** `src/test/java/com/jeepclub/backend/publications/EventOperationsIntegrationTest.java`;
  `src/test/java/com/jeepclub/backend/publications/infra/persistence/adapter/PublicationPersistenceTest.java`;
  `publications/infra/persistence/entity/EventGuestRequestEntity.java:6-9`;
  `billing/infra/persistence/entity/EventChargeContextEntity.java`.
- **Comportamento atual:** testes de persistência usam H2; há locks/constraints de
  unicidade em colunas anuláveis e JPQL/nativo `FOR UPDATE`. Não há suíte
  PostgreSQL/Testcontainers para a Epic.
- **Problema/cenário:** ordenação de locks, timeout/deadlock, geração de DDL,
  `NULL` em unique e consulta polimórfica podem divergir no banco alvo.
- **Impacto:** garantia de concorrência ainda não demonstrada em produção.
  **Causa provável:** infraestrutura de teste concentrada em H2.
- **Correção recomendada:** validação focal em PostgreSQL com schema gerado e
  transações independentes para operações críticas.
- **Testes recomendados:** like, guest, ride, service approval, Event cycle,
  MemberCharge, hard delete, constraints anuláveis e snapshots.
- **Jira relacionado:** BACK-397, BACK-399, BACK-439, BACK-447.

### PUB-A11 — LOW — Recusa de oferta exige vaga disponível

- **Arquivos:** `publications/core/application/service/EventService.java:63-81`;
  `publications/core/application/service/internal/EventOperations.java:93-96`.
- **Comportamento atual:** `respond` chama `rideRequests` e `guestSpace` tanto para
  `accept=true` quanto para `accept=false`.
- **Problema/cenário:** membro perde a última vaga antes de responder e tenta
  recusar; recebe conflito em vez de registrar `DECLINED`.
- **Impacto:** registro de resposta e ergonomia operacional incompletos.
  **Causa provável:** validação de capacidade compartilhada entre os dois comandos.
- **Correção recomendada:** definir elegibilidade de recusa independente de vaga
  atual, preservando ownership da inscrição/veículo.
- **Testes recomendados:** recusa após vaga ocupada ou veículo inativado.
- **Jira relacionado:** BACK-444.

### PUB-A12 — LOW — Resultado da auditoria Health não distingue erros

- **Arquivos:** `publications/core/application/service/AdminEventService.java:146-165`;
  `publications/api/http/exception/EventExceptionHandler.java:28-32`.
- **Comportamento atual:** qualquer exceção de `readHealth` registra status 403 e
  `CLIENT_ERROR` no System Log, inclusive perfil ausente (HTTP 404) e falha técnica;
  sucesso registra 200. O log não contém conteúdo clínico.
- **Problema/cenário:** tentativa de participante sem perfil fica auditada como
  acesso proibido, embora a resposta seja 404.
- **Impacto:** trilha de resultado imprecisa. **Causa provável:** flag booleana de
  sucesso usada para todas as falhas.
- **Correção recomendada:** registrar classe de resultado efetiva sem logar dados
  clínicos; manter `recordRequired` antes de devolver o perfil.
- **Testes recomendados:** perfil ausente, participante inválido, falha de Health
  e falha da persistência obrigatória do log.
- **Jira relacionado:** BACK-446.

## Matriz de riscos e verificação

- **Concorrência:** locks na raiz serializam as mutações de Event e social; versões
  JPA e unique constraints complementam Service, guest e ride. H2 não confirma a
  ordem de locks no PostgreSQL. A mudança de Vehicle ocorre fora do lock de Event.
- **Persistência/PostgreSQL:** `JOINED`, coluna `TEXT`, IDs `IDENTITY`, índices,
  unicidade de CPF/guest/offer e colunas anuláveis precisam de teste no banco alvo.
  `publication_history` não agrega o histórico operacional de Event.
- **Performance/N+1:** feed agrupa contagens e batch de imagens, porém Event e
  Billing materializam coleções completas e fazem scans repetidos. Comment images
  têm batch size 100; isso reduz, mas não elimina custo proporcional à página.
- **Segurança/IDOR:** controllers usam principal para autor/owner e permissions
  distintas; requests privadas de Service ocultam terceiros como 404. Não foi
  confirmado IDOR direto nos endpoints examinados. Rotas admin não têm guard de
  membership por desenho; dependem das authorities específicas.
- **Privacidade:** DTO público de feed/detalhe não expõe dívida, comprovante,
  Health, CPF ou dados de inscrição. CPF existe em guest requests admin/próprias
  e em linhas operacionais retidas após delete; definir retenção e acesso histórico.
  Audit log emergencial registra IDs/tipo/resultado, sem conteúdo clínico.
- **Boundary modular:** integrações Java de Publications usam `api.module` de
  Billing, Vehicles, Dependents e Health; Billing consome `EventQuery` via port.
  Não foi encontrada importação de entity/repository/service interna cruzada.
  O acoplamento operacional temporal com Vehicles permanece como A03.
- **Transações:** Service approval/change request e hard delete usam transação
  única para seus writes JPA. Event registration aciona Billing na mesma transação;
  cancelamento de Event também. Consulta de dashboard pode promover inscrições
  e gravar durante GET. Validação de mídia ocorre dentro das transações de escrita.
- **Testes:** 879 testes na baseline (0 falhas, 1 ignorado), mas faltam cenários
  A01-A03, A05, limites de payload, H2×PostgreSQL, virada exata de cutoff e
  carga/contagem de SQL. Passar em H2 não valida esses casos.

## Código × Jira e código × documentação

- **Código × Jira:** A01 contraria o cancelamento financeiro do Event em
  BACK-399/BACK-441/BACK-447; A02 pede decisão explícita sobre janela de refund
  da BACK-447; A03 ameaça
  capacidade/lotação de BACK-442/BACK-444. A05 expõe política de cancelamento
  individual que o Jira não fecha completamente.
- **Código × docs corrigidos:** README de Publications antecipava implementação,
  dizia que todo ciclo financeiro seria cancelado e explicava o histórico sem deixar clara a ausência
  de snapshot operacional e a diferença entre status efetivo e persistido.
  `billing/docs/flows.md` passou a registrar os limites atuais do cancelamento
  e do refund tardio; `billing/docs/business-rules.md` explicita cutoff, due date
  e janela de pagamento; `vehicles/docs/README.md` registra a falta de
  revalidação após edição/delete. Descrições HTTP genéricas de `EventController`
  e a descrição de `PublicationFeedDTO.Details` (que inclui o valor comercial
  anunciado) foram corrigidas sem mudar schema, permission, path ou resposta runtime.

## Próximas tasks recomendadas

1. Resolver A01 antes de considerar o cancelamento Event + Billing fechado.
2. Decidir a política de confirmação tardia de refund (A02) e cancelamento
   financeiro individual (A05) com produto/financeiro.
3. Definir invariantes de capacidade entre Vehicles e Event (A03), depois
   reconstrução/retention de histórico (A04).
4. Medir A06/A07 em PostgreSQL, estabelecer limites técnicos A08 e tornar
   handlers precisos A09; incluir testes PostgreSQL A10.

## Documentação e validação

Arquivos documentais ajustados: `publications/docs/README.md`,
`billing/docs/README.md`, `billing/docs/business-rules.md`,
`billing/docs/flows.md`, `vehicles/docs/README.md` e as descrições OpenAPI
de `publications/api/http/controller/member/EventController.java` e
`publications/api/http/dto/PublicationFeedDTO.java`. Este relatório
é o único documento novo. Documentos de Health, logging e arquitetura foram lidos
e não exigiram alterações factuais relacionadas a esta auditoria.

Na branch documental final, `mvnw.cmd test` e `mvnw.cmd verify` concluíram
cada um com 879 testes, 0 falhas, 0 erros, 1 ignorado e `BUILD SUCCESS`.
