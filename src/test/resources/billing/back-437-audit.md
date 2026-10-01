# BACK-437 — auditoria e evidências

## Baseline e escopo

- Repositório `Luskahz/jeep-club-backend`.
- `git fetch origin` executado; `origin/develop` confirmado em
  `0d8542fb3d90768573dc738cea85b23c1701b44c` antes de criar a branch
  `test/back-437-billing-hardening`.
- BACK-437 lida diretamente no Jira. O SHA `80ddc7d` da análise original não foi
  utilizado. A descrição pede PR; a instrução explícita desta execução pede
  somente branch remota, sem PR, merge ou encerramento da issue.
- Leitura: `docs/architecture/{README,module-organization,feature-development-rules}.md`
  e todos os arquivos atuais de `billing/docs/`.
- Suíte baseline: `mvnw.cmd test` e `mvnw.cmd verify` passaram, ambos com
  **1.109 testes, zero failures/errors, um skipped**. Logs locais em `target/`.
- Mudança local preexistente em `jwt-secrets.properties.sample` não pertence à tarefa.

## Matriz produção → testes → gaps → ação

Os nomes abaixo identificam classes reais de teste. A classificação se refere
à cobertura comportamental anterior; não é inferida somente do JaCoCo.

| Superfície/comportamento | Evidência preexistente | Antes | Ação implementada |
| --- | --- | --- | --- |
| ChargeDefinition: normalização, valor, política, ativo/inativo/arquivado | geração em EventOperationsIntegrationTest | Parcial | BillingDomainLifecycleTest, BillingConfigurationServiceTest: unicidade, lifecycle, arquivamento de assignments e isolamento histórico |
| ChargeAssignment e quatro especializações | administrativeAudiencesAndMembershipRemainIndependentOfEventCycle | Parcial | transições/repetição, referências inválidas, targets e duplicatas por tipo; persistência de todas as especializações |
| ChargeCycle: snapshots, geração, deduplicação, cancel/finish/archive | EventOperationsIntegrationTest | Parcial | deduplicação das audiences e filtro de ativos, sem público/duplicado/inativo, efeitos separados de finish/archive, releitura após lock |
| MemberCharge: dinheiro, janela inclusiva e estados efetivos | MembershipChargeQueryServiceTest, PaidMembershipFlowIntegrationTest | Parcial | limites e desconto/restauração, estados terminais, datas persistidas incoerentes; administração e ownership |
| MemberPayment: submissão, revisão, reenvio, substituição | MemberPaymentServiceReceiptTest, EventOperationsIntegrationTest | Parcial | conflitos antes de storage, amount, charge fechada, confirmação sem recalcular janela, timestamps, rejeição e terminais |
| MemberRefund: estados, prazo e decisões | realPaymentSubmissionRejectionResubmissionAndRefundLifecycle, concurrentLateConfirmCreatesSingleRefund | Parcial | lifecycle completo, instante exato de expiração, atores, request/approve após janela, idempotência, ownership e filtros |
| AdminChargeDefinitionService | criação indireta de Event | Ausente na superfície geral | canonicalização de nome, duplicata, update/lifecycle, arquiva somente assignments ativos |
| AdminChargeAssignmentService | geração lê assignments criados diretamente | Ausente | USER/ROLE/EVENT/ALL_MEMBERS, targets ausentes, definição inativa/arquivada, duplicata e ativação/desativação |
| AdminChargeCycleService | geração/cancelamento de Event | Parcial | audiences deduplicadas, conflito de geração, reconsulta sob lock e efeitos financeiros de cada comando |
| MemberChargeService / AdminMemberChargeService | leitura de Membership, exports | Parcial | escopo member, filtros administrativos, alteração impedida por payment pendente, cancel sob lock |
| MemberPaymentService / AdminMemberPaymentService | receipt tests e Event flows | Parcial | validação financeira anterior a I/O, lock payment→charge, charge fechada, amount mismatch e confirmação tardia |
| MemberRefundService / AdminMemberRefundService | cancelamento de Event e um lifecycle | Parcial | request direto/eligibility/reuso de ativo/refunded, guards financeiros e todos os comandos administrativos |
| EventBillingService | EventOperationsIntegrationTest: vários Events/mesma definição, criação concorrente, cutoff, datas e snapshots | Relevante já coberta | BillingEventBoundaryTest acrescenta falhas de catálogo/contexto fechado/assignment inativo, parent lock e preservação de FINISHED (BACK-464) |
| MembershipChargeQuery | MembershipChargeQueryServiceTest, MemberChargeMembershipQueryAdapterTest, PaidMembershipFlowIntegrationTest | Coberta | preservados seleção por período, isolamento Event e avaliação pública; não reescrever esses testes |
| ChargeDefinitionQuery / EventFinanceReportQuery | Membership e exports | Parcial | IDs inválidos/ausentes/inativos, limite de lote 500/501 e delegação pública |
| Identity boundary | IdentityBillingMembershipAdapterTest e PaidMembershipFlowIntegrationTest | Coberta | preservados; mocks dos services novos permanecem no port consumidor |
| Authorization/Event adapters | flows reais de Publications | Parcial | exercício por geração e contexto de Event; catálogo/targets controlados pelos ports |
| Persistência/adapters/mappers | schema/payment mapper, MemberChargeMembershipQueryAdapterTest, EventOperationsIntegrationTest | Parcial | round-trip com flush/clear de assignments, ciclo histórico e todos os estados de refund; estados financeiros impossíveis rejeitados |
| Pessimistic locks / concorrência | concurrentCycleAndMemberChargeCreationIsIdempotent, concurrentLateConfirmCreatesSingleRefund | Parcial | disputa de duas confirmações reais em transações separadas; ordem payment→charge e releitura de charge nos testes focais |
| Receipt/storage, rollback/cleanup | PaymentReceiptValidatorTest, PaymentReceiptServiceTest, PaymentReceiptLifecycleTest, PaymentReceiptLifecycleTransactionIntegrationTest, PaymentReceiptLocalStorageIntegrationTest | Coberta | preservados; sem duplicar testes do provider; fluxo HTTP exerce a integração pública |
| HTTP member/admin | PaymentReceiptSecurityIntegrationTest, BillingOpenApiIntegrationTest | Parcial | BillingHttpWorkflowIntegrationTest: services reais para geração, revisão/reenvio/payment/refund, principal, ownership, permissions e paginação |
| Bean Validation / RFC 9457 / handlers | OpenAPI, HttpParameterValidationTest, receipt security | Parcial | payload ausente/inválido, enum inválido, ID zero/missing, multipart inválido e conflitos de domínio no HTTP real |
| Dados financeiros / internals | receipt tests, exports | Parcial | owner-scoped charges/refunds; receiptUrl público, sem receiptStorageKey nos detalhes/listagens de payment; erros estruturados |
| OpenAPI | BillingOpenApiIntegrationTest | Coberta | preservado teste focal completo; não alterar contrato para adequar aos testes |
| Exportações de Billing | ExportHttpIntegrationTest: período, contexto estruturado, último pagamento, cinco produtos após hard delete; ExportSecurity/OpenAPI tests | Relevante já coberta | BillingExportPreconditionsTest: status incompatível, entidade financeira inexistente, filtro de cobrança em produto de configuração |

## Decisões semânticas e dívida

Pesquisa de todo o projeto BACK paginada antes de criar tickets; conferidos
summary e description para evitar duplicação. Não alterar status/Sprint da BACK-437.

- **BACK-489 (nova, Task):** transições de MemberRefund não têm lock/version.
  `requestByMemberPaymentId` bloqueia payment; `request(refundId)` e comandos
  administrativos carregam refund por `findById`, sem `@Version` na entity.
  Decisões simultâneas podem se sobrescrever. Requer estratégia de concorrência
  coerente com os locks existentes, e não refatoração estética.
- **BACK-490 (nova, Task):** cancel interno de payment REJECTED mantém dados da
  rejeição, mas reconstitution de CANCELED os proíbe. Reproduzido por
  `BillingReconstitutionTest.unusedRejectedPaymentCancellationCurrentlyCannotBeReconstituted`.
  Método não é usado por nenhum service/endpoint: dívida latente e decisão de
  histórico, não bug de endpoint que justifique alterar política silenciosamente.
- As duas issues foram criadas sem Sprint, movidas explicitamente ao backlog do
  **board 34**, e relidas. Permanecem `Tarefas pendentes`, sem campo Sprint,
  fora de **Sprint Final — V2 e Produção**.
- **BACK-460 / BACK-464 (reutilizadas):** Event cancela apenas ciclos GENERATED;
  FINISHED permanece. O teste focal caracteriza o comportamento documentado.
- **BACK-465 (reutilizada):** confirmação além de 30 dias produz eligibility já
  expirada, ancorada no cancelamento; nenhuma alteração da política neste trabalho.
- **BACK-466 (reutilizada):** efeitos financeiros de cancelamento individual de
  Registration pertencem à decisão futura de Event/Billing.
- **BACK-471 (reutilizada):** `EventBillingService.findByEvent` materializa listas
  e faz filtros repetidos; não abrir outro ticket para os mesmos scans.
- Reenvio de REJECTED revalida prazo; replacement de PENDING_VALIDATION permite
  complementar comprovante após prazo se a cobrança segue PENDING. É comportamento
  deliberadamente documentado; teste focal preserva a assimetria. Nenhum bug presumido.
- `MemberCharge.isDue` expressa atraso de cobrança aberta; regra efetiva continua
  centralizada em `effectiveStatusAt`. Não abrir ticket de naming cosmético.
- `AdminMemberChargeService` e `MemberPaymentService` fazem mais de uma leitura de
  Clock. Observação de desenho temporal, sem reprodução de falha financeira neste
  escopo; não inventar bug nem alterar produção por conveniência de teste.

## Não aplicáveis e limites

- Não existe endpoint/app service para cancel interno de MemberPayment nem scheduler
  de expiração de refunds. Testar somente o domínio e o comando manual existente.
- Estado OVERDUE/EXPIRED de charge não é persistido; tests não inventam transição JPA.
- Cutoff de participação pertence a Publications; tests existentes são a evidência.
- FileStorage provider/path traversal pertence a Platform e já tem testes próprios.
- Concorrência exercitada com H2, não confirmação de semântica MySQL/PostgreSQL em
  produção. BACK-475 continua responsável pela validação no banco alvo de Publications.
- DataJpaTest usa o datasource H2 configurado no perfil test, com pool. O datasource
  substituto sem pool produziu check evaluation com `database has been closed`;
  a suíte usa `replace=NONE`, sem remover constraints ou alterar schema de produção.
- Bean Validation recebe o mesmo Clock fixo via customizer no contexto HTTP de teste.
- Testes de domínio/application isolam invariantes e interações de lock/storage que
  flows maiores apenas percorriam; não repetem os cenários cross-module completos.
- Nenhuma regra ou classe de produção foi excluída do JaCoCo. Nenhuma mudança de
  produção foi feita para aumentar cobertura.

## JaCoCo

Fonte de verdade: `target/site/jacoco/jacoco.xml`, JaCoCo 0.8.15. Somam-se os
`counter` diretos de cada `package` com prefixo `com/jeepclub/backend/billing`;
nunca os contadores globais, classes de teste ou o relatório de Authentication.

| Métrica | Antes covered | Antes missed | Antes % | Depois covered | Depois missed | Depois % |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| LINE | 1556 | 1210 | 56,25% | 2537 | 229 | 91,72% |
| BRANCH | 458 | 427 | 51,75% | 731 | 154 | 82,60% |
| INSTRUCTION | 8629 | 5508 | 61,04% | 12861 | 1276 | 90,97% |

Os números depois correspondem ao `verify` completo de 30/09/2026, encerrado
às 22:31:33 -03:00: **1.256 testes, zero failures/errors, um skipped**, BUILD SUCCESS.
Fonte preservada localmente em `target/back-437-progress-verify-jacoco.xml`;
log em `target/back-437-progress-verify.log`. A meta de LINE >= 80% foi atingida.

Depois desse verify foram adicionados três testes HTTP. A classe inteira foi
reexecutada com `mvnw.cmd -Dtest=BillingHttpWorkflowIntegrationTest test`:
**11 testes, zero failures/errors/skipped**, BUILD SUCCESS às 22:36:03 -03:00.
O último `test` focal de todas as classes `Billing*Test` havia passado com
147 execuções; o último verify completo também passou pela fase de testes.
Esses resultados não equivalem a executar novamente a suíte completa sobre
os três últimos testes. A contagem consolidada dos XML locais é 1.259 testes
(150 a mais que a baseline); a última execução completa validada permanece 1.256.

## PIT

Perfil `billing-mutation` focal: seis agregados, assignments e serviços financeiros
de configuração, ciclo, charge, payment, refund e Event. DTOs, controllers e
infraestrutura não são alvo de mutation. A seleção focal de tests permanece em
Billing para evitar reexecutar todo o bootstrap de Publications por mutante;
os testes cross-module permanecem obrigatórios em test/verify.

Comando: `mvnw.cmd -Pbilling-mutation org.pitest:pitest-maven:mutationCoverage`.
Relatórios locais: `target/pit-reports/billing/{index.html,mutations.xml}`.
**Não executado nesta rodada.** Não há mutation score, resultado de mutantes
ou análise de survivors/NO_COVERAGE validada. O perfil apenas prepara a execução.

## Encerramento da rodada e pendências

O usuário pediu encerrar a rodada e commitar o estado atual. Este é um checkpoint
da BACK-437, não conclusão integral do objetivo original.

Entregues nove classes novas de teste e fixtures compartilhadas, auditoria dos
testes existentes, matriz de gaps, medição real acima da meta e dois tickets
de dívida no backlog. Nenhum bug foi corrigido em código de produção; BACK-490
tem reprodução por teste de caracterização e BACK-489 tem evidência estática,
sem reprodução concorrente de perda de atualização neste trabalho.

Falta para concluir:

1. Executar o PIT focal e analisar mutantes sobreviventes e sem cobertura,
   acrescentando asserções ou justificativas técnicas por comportamento.
2. Reexecutar `mvnw.cmd test` e `mvnw.cmd verify` sobre o estado final e após
   eventuais ajustes do PIT, atualizando a medição sem misturar execuções.
3. Confirmar a checklist original com essas evidências. Os tickets de dívida
   documentam trabalho posterior e não são correções implementadas aqui.

Relatórios de build, logs, HTML e arquivos temporários permanecem em `target/`,
sem versionamento. A alteração preexistente em `jwt-secrets.properties.sample`
permanece fora do commit. Não abrir PR, fazer merge ou encerrar BACK-437.
