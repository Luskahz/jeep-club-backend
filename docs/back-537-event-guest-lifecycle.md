# BACK-537 — participação e transporte de convidados

Autoridade: Story BACK-537 lida integralmente, BACK-460/BACK-466 e referências
BACK-443/BACK-444. As duas últimas descrevem regras históricas revogadas.
Base confirmada após concluir BACK-460: feat/back-525-vehicle-gallery, PR #110,
`aa18247f508b7edc58762a41ae38c012f1610ec3`.
Não incluir o fix temporal de #105 nesta branch por propagação antecipada.

Inspecionados: EventGuestRequest, EventRideOffer, EventRegistration,
EventOperations, EventService/AdminEventService, EventOperationsRepository,
adapter/entities/repositories JPA, controllers/DTOs member/admin, dashboard,
EventReportService, publicação/histórico, catálogo de permissions e migrations.
Investigação e matriz registradas antes de alterar o domínio e reconfirmadas no
checkout da BACK-525. V8 era a última migration; V9 foi utilizada.

## Modelo e matriz de transições

Origem MEMBER/ADMIN imutável; participação PENDING/APPROVED/REJECTED/CANCELLED;
transporte separado NONE/STANDBY/ALLOCATED, derivado de participação/origem e da
associação atual. ADMIN ativo sem transporte significa STANDBY. Aprovação não
significa que todo participante possui veículo. CPF é atributo, nunca chave.

| Origem/estado | Ação/ator | Participação resultante | Transporte resultante |
| --- | --- | --- | --- |
| MEMBER novo | Solicitação pelo titular com inscrição/veículo elegíveis | PENDING | NONE; veículo pretendido separado |
| MEMBER PENDING | Aprovação admin | APPROVED | ALLOCATED após confirmar elegibilidade/capacidade |
| MEMBER PENDING | Recusa admin com motivo | REJECTED terminal | NONE |
| MEMBER PENDING/APPROVED | Cancelamento titular/admin | CANCELLED terminal | NONE |
| MEMBER PENDING/APPROVED | Cancelamento da inscrição originadora | CANCELLED terminal | NONE |
| MEMBER CANCELLED/REJECTED | Reativar mesmo registro | 409 | Sem alteração; retorno exige nova solicitação |
| ADMIN novo | Criação admin | APPROVED, participação vigente | STANDBY |
| ADMIN APPROVED/STANDBY | Alocação direta ou seleção de oferta admin | APPROVED | ALLOCATED |
| ADMIN APPROVED/ALLOCATED | Realocação admin | APPROVED | ALLOCATED no novo destino após capacidade |
| ADMIN APPROVED/ALLOCATED | Desassociação admin ou cancelamento do motorista | APPROVED | STANDBY |
| ADMIN APPROVED, qualquer transporte | Cancelamento admin | CANCELLED | NONE |
| ADMIN CANCELLED | Reativação admin | APPROVED | STANDBY, sem restaurar vínculo/oferta |
| ADMIN CANCELLED | Seleção/alocação | 409 | NONE |

Repetição do mesmo cancelamento/desassociação/reativação já efetiva é idempotente
quando não reinterpreta um fato anterior. Decisões incompatíveis retornam 409.
REJECTED legado permanece REJECTED; nenhuma conversão em cancelamento.

## Pontos concretos encontrados

- EventOperations.guestSpace bloqueia segunda aprovação artificialmente.
- requestGuest bloqueia mesmo CPF, inclusive rejeitado; JPA e V1 possuem
  uk_event_guest_cpf e uk_event_guest_vehicle. Remover por migration nova.
- EventService.cancel bloqueia qualquer aprovado no veículo, rejeita apenas
  pessoal pendente e encerra apenas oferta ACCEPTED. Substituir por operação
  atômica sensível à origem e à inscrição originadora, preservando Billing.
- ADMIN nasce PENDING e seleção delega reviewGuest: separar participação e
  transporte; não criar segunda aprovação ao alocar.
- EventRideOffer mantém unicidade guest/vehicle e selected_guest_id; permitir
  invalidar seleção preservando selectedAt e retornar oferta coerente ao histórico.
- Dashboard subtrai todos aprovados dos assentos. Passará a subtrair apenas
  efetivamente alocados; standby continua no total de participantes.
- Rows operacionais de Event sobrevivem hard delete sem cascade. Histórico novo
  também deve sobreviver e ser consultado por rota administrativa autorizada.
- Todas as mutações precisam manter Event como primeiro lock e leitura atual
  dos filhos, inclusive em snapshot RR antigo. Reusar locks públicos de Vehicle.
- Nova permission deve entrar no catálogo e DDL ENUM, sem grants CUSTOM implícitos.

## Persistência e compatibilidade

Migration `V9__event_guest_lifecycle.sql`. V1–V8 intocadas.
Adicionar origem/inscrição originadora e associação atual, veículo pretendido,
histórico imutável de transições sem PII. Backfill preserva IDs/CPF/nomes/datas,
review/rejectionReason, vínculos e ofertas. ADMIN pendente sem veículo é
reconciliado explicitamente para participação vigente/standby, sem inventar
revisor/data de aprovação histórica; registrar reconciliação de migration.
Detectar vínculos legados ambíguos antes de DML/DDL irreversível; nunca inventar
originador, descartar oferta ou apagar registro para passar uma constraint.

API nova exige DTOs validados, motivo, ownership e permissions distintas por
capacidade, RFC 9457 e OpenAPI. Persistência cotidiana permanece JPA. Validação
inclui H2, MySQL real, migração de dados sintéticos representativos, concorrência
da última vaga e corridas cancelamento/alocação, rollback e histórico de exclusão.
Nenhum dado produtivo ou cópia real está autorizado/disponível nesta execução.

## Implementação e decisões

EventGuestRequest mantém PENDING/APPROVED/REJECTED e acrescenta CANCELLED. O DTO
expõe origin e transportStatus separadamente. ADMIN nasce APPROVED porque sua
participação já foi autorizada pelo cadastro administrativo; STANDBY é transporte,
não uma aprovação pendente. O endpoint approve/reject analisa somente MEMBER.
REJECTED administrativo legado permanece legível e terminal.

`requesterUserId` preserva o solicitante/criador original. Para ADMIN não representa
motorista. `originRegistrationId` e `requestedVehicleId` pertencem somente à origem
MEMBER e são imutáveis. `allocatedRegistrationId`, `responsibleUserId`, `vehicleId`
e `selectedOfferId` são associação atual e ficam nulos ao desassociar/cancelar.
PENDING pessoal mostra preferência em requestedVehicleId e vehicleId nulo.
Alocação/realocação direta e retorno a standby aplicam-se a ADMIN, conforme a
Parte 7 da Story; encerramento de MEMBER usa cancel, nunca uma conversão para ADMIN.

Os services member/admin existentes continuam donos das transações. EventOperations
reúne as regras internas; não acessa nenhum repository privado de Vehicles/Billing.
Somente contratos públicos de capacidade e lifecycle são reutilizados. Cancelar
convidado não escreve Charge/Payment/Refund/Registration. Cancelar inscrição altera
atomicamente seus convidados/ofertas e preserva as obrigações financeiras da BACK-460.
Registro de inscrição CANCELLED continua histórico e não ganha reinscrição automática.

EventGuestTransition possui append/read próprios, @Immutable e colunas não
atualizáveis, na mesma transação MANDATORY da alteração. Guarda criação, avaliação,
oferta, associação, realocação, retirada, cancelamento e reativação. Ator, motivo,
instante, estados e veículos/inscrições anterior/posterior são persistidos; motivo
aceita até 1000 caracteres. Histórico sobrevive à remoção física do Event e não
contém cópia de CPF/nome. Guards de Identity consideram ator/revisor/responsável.

## Locks e MySQL

Event é sempre o primeiro lock. Uma leitura curta, não bloqueante, em transação
READ_COMMITTED separada descobre referências já commitadas, mesmo se a transação
chamadora tiver snapshot RR antigo. União com destinos recebidos é bloqueada em
ordem crescente: Vehicles, depois Dependents. Esse passo não escreve nem promete
atomicidade entre transações: o lock do Event impede outra mutação de seu agregado.

Filhos têm IDs estáveis e não são excluídos operacionalmente. Leituras curtas de
IDs commitados são unidas aos IDs visíveis da própria transação, incluindo inserts
ainda não commitados. Instâncias JPA antigas são descartadas antes das leituras
bloqueantes por chave; consultar um intervalo vazio FOR UPDATE poderia bloquear
inscrições de outro Event e produzir deadlock. Coleções são lidas em projeções
atuais, por inscrição, e substituídas integralmente pelo ORM. Não existe SQL manual
de gravação cotidiana. Event/galeria também têm projeções bloqueantes atuais para
evitar a leitura polimórfica antiga; snapshots de exclusão usam os valores atuais.

Corridas MySQL preenchem previamente o cache e as coleções com snapshot antigo,
mantêm o lock do Event em outra conexão, comprovam contenção no performance_schema
e então liberam o commit concorrente. Não usam sleeps como sincronização.
Reservas disputadas recebem conflito controlado; não há retry invisível de comando.
Leituras auxiliares exigem pool com conexões disponíveis; homologar volume e
contenção. Não foi realizado teste de carga produtivo.

## HTTP, permissions e frontend

Paths existentes de solicitação, listagem, approve/reject e ofertas permanecem.
Todas as operações usam autenticação/permissions atuais ou capacidades novas
específicas, ownership e erros RFC 9457. Nenhum grant CUSTOM é automático. ROOT
continua estrutural; administradores devem conceder explicitamente as capacidades.

| Método/path | Permission | Payload/efeito |
| --- | --- | --- |
| POST /events/{id}/guest-requests | PUBLICATIONS_EVENT_GUEST_REQUEST_CREATE | cpf, guestName, vehicleId; MEMBER/PENDING |
| POST /events/{id}/guest-requests/{guestId}/cancel | PUBLICATIONS_EVENT_GUEST_REQUEST_CANCEL | reason; somente próprio MEMBER; terminal |
| POST /admin/events/{id}/guest-requests | PUBLICATIONS_EVENT_GUEST_ADMIN_CREATE | cpf, guestName; ADMIN/STANDBY, sem vehicleId |
| POST /admin/events/{id}/guest-requests/{guestId}/approve | PUBLICATIONS_EVENT_GUEST_APPROVE | Sem body; avalia MEMBER |
| POST /admin/events/{id}/guest-requests/{guestId}/reject | PUBLICATIONS_EVENT_GUEST_REJECT | reason; avalia MEMBER |
| PUT /admin/events/{id}/guest-requests/{guestId}/allocation | PUBLICATIONS_EVENT_GUEST_ALLOCATE | vehicleId, registrationId opcional, reason; aloca/realoca ADMIN |
| POST /admin/events/{id}/guest-requests/{guestId}/unassign | PUBLICATIONS_EVENT_GUEST_UNASSIGN | reason; ADMIN retorna standby |
| POST /admin/events/{id}/guest-requests/{guestId}/cancel | PUBLICATIONS_EVENT_GUEST_CANCEL | reason; encerra participação |
| POST /admin/events/{id}/guest-requests/{guestId}/reactivate | PUBLICATIONS_EVENT_GUEST_REACTIVATE | reason; somente CANCELLED ADMIN → standby |
| GET /admin/events/{id}/guest-requests/{guestId}/history | PUBLICATIONS_EVENT_GUEST_HISTORY_READ | PageResponse; inclusive após exclusão do Event |
| POST /events/{id}/ride-requests/{guestId}/response | PUBLICATIONS_EVENT_RIDE_OFFER_RESPOND | vehicleId, accept; aceite não reserva |
| POST /admin/events/{id}/ride-offers/{offerId}/select | PUBLICATIONS_EVENT_RIDE_OFFER_SELECT | Sem body; reserva atômica e rastreável |

Exemplo de solicitação pessoal (CPF sintético, ilustra formato):

```json
{"cpf":"12345678901","guestName":"Convidado","vehicleId":101}
```

Alocação administrativa direta; registrationId, se informado, deve corresponder
ao titular/veículo elegível deste Event:

```json
{"vehicleId":101,"registrationId":42,"reason":"Transporte confirmado pela administração"}
```

Cancelamento/desassociação/reativação:

```json
{"reason":"Decisão informada à administração"}
```

Frontend deve identificar itens por id, permitir CPFs repetidos e múltiplos pedidos
por veículo, mostrar participação e transporte separadamente, e usar
responsibleUserId/allocatedRegistrationId para motorista. Não exibir requesterUserId
administrativo como responsável pelo carro. ADMIN standby não usa approve; alocar
diretamente ou selecionar oferta. CANCELLED pessoal exige novo cadastro; ADMIN
permite reativar, seguido de nova escolha de transporte. Ofertas WITHDRAWN são
histórico e não podem ser selecionadas. Conflitos exigem reler estado/capacidade.

guestsApproved inclui standby; guestsAllocated determina ocupação e guestsStandby
expõe espera por transporte. Recorrência conta registros aprovados por CPF, sem
deduplicar participações/Events nem afirmar presença física. Exports mantêm linhas
distintas por convidado e não confundem standby com vaga ocupada.
Criação não é deduplicada por CPF: repetir um POST pode criar outro ID. A interface
deve evitar retry cego/double submit e consultar os registros após resposta incerta.

## Migração, legado e checklist pré-deploy futuro

V9 remove unicidades Event/CPF, Event/veículo aprovado e resposta única
guest/vehicle. Mantém a proteção de uma oferta selecionada atual por guest.
Acrescenta origem/associação, withdrawnAt, tabela de transições, índices,
FKs compostas para inscrição/Event/membro e CHECKs de estados/associação.
approved_vehicle_id permanece como projeção compatível, sem unicidade.
O ENUM físico de permissions recebe as seis capacidades novas.

MEMBER é relacionado à inscrição original por Event/requester; sua antiga referência
de veículo vira requestedVehicleId, inclusive em PENDING/REJECTED. Aprovados mantêm
transporte e têm motorista/inscrição resolvidos; oferta SELECTED é preservada.
ADMIN PENDING sem veículo vira participação APPROVED/STANDBY. Ninguém inventa
reviewerId/reviewedAt; todos os IDs, CPF, nomes, createdAt e decisões históricas são
preservados. REJECTED nunca vira CANCELLED. Um snapshot LEGACY_MIGRATION por registro
grava antes/depois e instante da reconciliação, ator nulo explicitamente técnico.

Preflight falha antes de alterar tabelas/dados quando origem/transporte é ausente
ou ambíguo ou oferta selecionada contradiz o vínculo retido. Não tenta escolher
arbitrariamente um motorista, apagar registros ou recuperar falha com repair/clean.
Após falha, preservar backup/logs e investigar; a procedure de preflight pode
permanecer no schema abortado. Não reexecutar arbitrariamente o arquivo aplicado.
Repetir migrate normal não reaplica V9 nem duplica auditoria.

Antes de futura release, homologar restauração e cópia real autorizada; conferir
V1–V9/checksums e dados legados; desligar writers antigos; ensaiar Flyway seguido
de Hibernate validate; avaliar metadata locks/DDL, duração, índices, pool e carga;
conceder permissions deliberadamente; coordenar frontend e triagem de convidados.
Não remover colunas antigas agora. Não há variável de ambiente, volume, storage,
importação ou flag operacional nova. Não usar downgrade binário após novos estados,
histórico e associações; restauração/correção para frente exige plano independente.

## Dependências e validação

Base temporária #110/BACK-525. Cadeia preservada: develop → #105 → #106 → #107 →
#108 → #109 → #110 → BACK-537. O fix temporal BACK-460
`bdea5a19fcfed119353dce8f5cc2850b5e5d4722` pertence somente ao PR #105 e deverá ser
propagado durante integração ordenada, com regressão acumulada. Esta branch ainda
herda o timestamp anterior aos locks; a lacuna é conhecida e não foi mascarada
com duplicação do fix neste diff. As restrições funcionais antigas de convidados
estão substituídas aqui. V4 Registry e políticas de matrícula permanecem intactas.

Resultados finais, evidências e CI são registrados na descrição do
[PR #111](https://github.com/Luskahz/jeep-club-backend/pull/111), vinculados aos HEADs
efetivamente validados. Tentativas de desenvolvimento com falha não são aprovação.
Os dados de teste são exclusivamente sintéticos locais;
não foi disponibilizada cópia real autorizada para ensaio. Nenhum merge, deploy,
push protegido, VPS ou banco compartilhado/produtivo é permitido por esta entrega.
