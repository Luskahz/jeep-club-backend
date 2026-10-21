# BACK-537 — auditoria final de encerramento

Story relida integralmente no Jira em 20/10/2026 e confrontada com domínio,
application, controllers, persistência, V9 e testes. Rodada exclusivamente na
feat/back-537-event-guest-lifecycle / PR111, sobre o HEAD anterior 3bf9e851.
PR105/bdea5a19, a base BACK-525 e as demais branches não são alterados.
Resultados e HEAD final efetivamente executado constam na descrição do PR111;
contagens antigas não constituem evidência do novo commit.

## Findings corrigidos

1. EventOperations.requestGuest chamava guestSpace antes de criar PENDING.
   Removida somente essa verificação; ativo/propriedade/inscrição são validados
   explicitamente. reviewGuest, assign e seleção mantêm capacidade atômica.
   EventOperationsIntegrationTest: fullVehicleAcceptsMultiplePendingRequestsWithoutReservingSeats,
   pendingRequestsExceedAvailableSeatsButApprovalAndCancellationControlCapacity,
   pendingRequestStillRequiresEligibleOwnedActiveVehicleAndValidGuest;
   herdados por EventOperationsMySqlIT. A corrida twoGuestApprovalsCannotTakeSameLastSeat
   agora exige conflito de capacidade no perdedor e preservação do pedido PENDING,
   além de uma única aprovação. HTTP: pendingRequestsDoNotReserveAndApprovalReturnsCapacityConflict.
2. approvedVisits era count de IDs atualmente APPROVED, nunca evidência de visitas
   ou soma histórica de decisões. Únicos consumidores: AdminEventService,
   AdminEventController.guests e testes. EventReportPeople não consome o indicador;
   monta linhas por guest ID, preservando CPFs repetidos e transporte separado.
   Renomeados os métodos de repository/adapter/service e acrescentado o campo HTTP
   currentApprovedGuestCount. approvedHistoricalVisits é mantido, depreciado e
   descrito como alias do mesmo valor atual, sem mudança silenciosa da query.
   Não se cria métrica de comparecimento nem agregado histórico incompleto.
   Testes: currentApprovedCountsAreRecordsNotAttendanceOrHistoricalDecisions,
   guestCountHasExplicitCurrentStateMeaningAndDeprecatedAlias e OpenAPI.

## Contrato do indicador

GET /admin/events/{id}/guest-requests, permission PUBLICATIONS_EVENT_GUEST_ADMIN_READ:

```json
{
  "request": {"id": 123, "origin": "ADMIN", "status": "APPROVED", "transportStatus": "STANDBY"},
  "approvedHistoricalVisits": 2,
  "currentApprovedGuestCount": 2
}
```

Exemplo de um item da página, DTO request abreviado. Os dois números são iguais.
Conta registros distintos atualmente APPROVED para o CPF em todos os Events,
incluindo o próprio ID, ADMIN standby e dados operacionais retidos de Events
excluídos. Cancelamento retira o ID; reativação inclui o mesmo ID uma vez.
Solicitações distintas do mesmo CPF, no mesmo Event ou em Events diferentes,
contam separadamente. Alocar/desassociar não altera a contagem de participação.
Não conta ações de auditoria nem presenças, visitas, Events únicos ou check-ins.
Histórico de aprovação/recusa/cancelamento permanece no endpoint autorizado de
transições por guest ID. CPF não é exposto em path/query/log para esta contagem.
Frontend deve migrar do alias depreciado, rotular como "registros atualmente
aprovados" e tratar a informação como apoio à decisão administrativa.

## Matriz dos critérios oficiais

Atendido abaixo indica comportamento implementado e evidência de teste indicada;
os gates de execução final são registrados por HEAD no PR, separadamente da leitura
do código. Não se infere aprovação funcional somente da quantidade de testes.

| Critério de aceite da Story | Avaliação | Código e evidência |
| --- | --- | --- |
| Vários aprovados por carro e decisão individual | Atendido | guestSpace considera ocupantes efetivos; twoPersonalGuestsWithSameCpfCanBothBeApprovedInOneVehicle; personalDecisionsRemainIndependentAndRejectedHistoryIsNotCancellation |
| CPF repetido sem deduplicação, histórico por ID | Atendido | Domain não usa CPF como chave; V9 remove uk_event_guest_cpf; testes de CPF/contagem, EventReportServiceTest preserva linhas distintas |
| MEMBER origem elegível imutável; cancelar inscrição encerra todos | Atendido | originRegistrationId/requestedVehicleId imutáveis no ORM; cancelRegistration; registrationCancellationEndsAllPersonalPendingAndApprovedGuests |
| Titular/admin cancela somente pessoal, preservando finanças | Atendido | cancelGuest verifica proprietário; personalCancellationIsTerminalOwnedAndIndependentOfRegistration; cancellingOnePersonalGuestFreesOnlyItsSeatAndKeepsOtherGuest |
| ADMIN standby; oferta/direto, realocação, retirada e desistência do motorista | Atendido | standby/assign/unassign; directRelocationAndUnassignmentPreserveSelectedOfferHistory; driverWithdrawalReturnsAdministrativeGuestToStandbyAndAllowsNewDriver |
| ADMIN cancelado sem veículo, reativação somente standby | Atendido | domain cancel/reactivate; administrativeCancellationAndReactivationAlwaysReturnToStandby; reactivationRequiresNewOfferAndNeverResurrectsOldAcceptedOrSelectedOffer |
| 2+ ADMIN por carro, sem oferta obrigatória | Atendido | assign/destination/guestSpace; twoAdministrativeGuestsShareVehicleAndThirdAllocationCannotExceedPhysicalCapacity |
| Locks, última vaga, dupla alocação, atomicidade e rollback | Atendido | Event primeiro, pais antes dos filhos; testes simultâneos, snapshots RR MySQL, auditFailureRollsBackGuestOfferAndSeatTogether e registrationRollbackRestoresAllPersonalGuestsAndAdministrativeTransport |
| Auditoria por ação, ownership, permissions e erros seguros | Atendido | record + MANDATORY/@Immutable; ações com ator/motivo/antes-depois; controllers + RequiredPermission/PreAuthorize + EventExceptionHandler; testes HTTP/auth e guestTransitionHistorySurvivesEventDeletionAndContainsOriginalReview |
| Flyway legado sem perda, V1–V8 intactas, MySQL/Hibernate validate | Atendido | V9 preflight/backfill/constraints/snapshot; MySqlMigrationIT positivo, preflight negativo, hashes históricos e JPA validate; V9 não modificada nesta rodada |
| Domínio, HTTP, auth, concorrência, rollback, migrations e três gates | Atendido no código de testes | Novos cenários herdados por MySQL, suites anteriores preservadas; resultados finais vinculados ao HEAD no PR111 |
| Frontend/OpenAPI fluxos completos member/admin | Atendido | EventController/AdminEventController/GuestReview @Schema; EventHttpIntegrationTest verifica schema, alias depreciado, capacity 409 e permissions; documento principal contém endpoints/payloads |

## Verificações complementares de segurança e persistência

- MEMBER CANCELLED/REJECTED não reativa; retorno exige novo ID. cancel preserva
  reviewer/reviewedAt/rejectionReason. Nenhuma conversão MEMBER → ADMIN.
- ADMIN standby é participação ativa sem veículo; CANCELLED não recebe oferta
  selecionada. WITHDRAWN retém respondedAt/selectedAt, acrescenta withdrawnAt e
  limpa seleção atual; novas ofertas possuem IDs próprios. Nada restaura vínculo.
- V9 remove somente unicidades indevidas de CPF/veículo/resposta, mantendo uma
  seleção atual por guest. Índices de capacidade/resposta/histórico substituem os
  caminhos removidos. FKs compostas vinculam origem/destino a Event e membro;
  checks validam estado/transporte/projeção antiga. Sem cascade destrutivo.
- Preflight recusa origem/veículo ausente, associação aprovada ambígua e seleção
  incoerente. IDs, datas, decisões e ofertas legadas são preservados; ADMIN pending
  vira standby em reconciliação explícita, nunca REJECTED → CANCELLED.
  LEGACY_MIGRATION registra antes/depois com ator técnico nulo e sem revisão humana
  inventada. Novo ENUM de permissions não concede nada a CUSTOM. ROOT estrutural.
- Auditoria independente sobrevive à exclusão de Event. Identity deletion guards
  consideram referências retidas. Platform registra acesso HTTP administrativo
  com ator/rota/status/requestId, sem body logging global; o histórico de domínio
  registra mutações atomicamente. Leitura de histórico exige sua permission própria.
  Não se promete auditoria síncrona durável de cada leitura: o logging HTTP
  transversal não participa atomicamente da transação de domínio.
- Query do indicador é batch, agrupa CPF e conta IDs de registros; não faz join em
  transições e não multiplica o mesmo ID por cancelamentos/reativações. Um mapa por
  CPF não deduplica linhas do resultado administrativo. Não controla elegibilidade.

## Concorrência e limites conhecidos

Event é sempre o primeiro lock. Referências e destinos são descobertos antes de
filhos; Vehicles em ordem crescente, depois Dependents. Guest requests,
Registrations/coleções e Ride offers são lidos por ID atual e ordenado. A ordem
relativa destes filhos varia por comando (request lê Registration primeiro;
review lê Guest primeiro; select lê Offer primeiro). Isso é seguro entre comandos
do mesmo Event porque nenhum deles adquire filhos antes do lock exclusivo comum.
Não se afirma uma ordem global fictícia Guest → Registration → Offer.
Guards de lifecycle dos pais encontram o Event esperando no pai antes de deter
filhos, evitando a inversão pai/filho. Não houve nova REQUIRES_NEW nesta rodada.

Comandos normais READ_COMMITTED usam uma conexão. Chamadores externos RR já
existentes conservam sua transação e usam o fallback de IDs commitados em conexão
auxiliar; flush/união de IDs próprios/detach antes de locks evitam snapshot antigo
ou sobrescrita de @Version. Os quatro testes MySQL com snapshot antigo comprovam
espera real no performance_schema; o teste de quatro alocações com pool=2 comprova
que comandos normais não usam o fallback. Dimensionamento do pool em fluxos RR e
carga real permanecem risco de homologação, sem refatoração arbitrária nesta rodada.

## Integração, bloqueios e homologação

Não foi identificado novo bloqueio funcional adicional após os dois refinamentos.
O parecer por HEAD e resultado final dos gates é publicado no PR111. Resultados de
execuções anteriores não substituem a validação do commit final.

Preservados contratos públicos entre contexts, Billing, mídia, lifecycle e Registry;
suítes completas da ancestralidade verificam BACK-460/510/512/523/524/525/526.
A corrida intermitente de import do Registry observada anteriormente no PR105 é
preexistente; não foi corrigida nem descartada como resolvida por uma execução verde.

bdea5a19 continua somente no #105 e precisa ser propagado na integração ordenada
antes de release; nenhuma antecipação nesta branch. CI verde não autoriza merge.

Homologação futura: cópia real autorizada não disponibilizada; migrar backup local
representativo, restaurabilidade, metadata locks/duração de DDL, índices/carga,
pool para RR, grants deliberados e frontend. MySQL DDL faz commits implícitos:
preflight reduz falhas conhecidas mas V9 não é transação atômica. Uma falha após DDL
pode deixar schema/dados parciais; preservar erro/histórico/checksums, parar writers,
restaurar cópia/backup e investigar. Não raw replay, repair/clean ou downgrade
binário para mascarar inconsistência. A procedure de preflight pode permanecer
num schema que abortou. Nenhum ensaio dessa rodada toca ambiente compartilhado.

Não há variável, volume, storage ou flag nova; emissão/import apply do Registry
continuam desabilitados. Nenhum merge, deploy, VPS, push protegido ou Jira write.
