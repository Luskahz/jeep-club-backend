# Validação — BACK-410

## Escopo e fontes

Revisão em 29/09/2026 da PR #81, branch `feat/back-410-module-exports`, base `origin/develop` em `b051da139395708aba61a2895ebdce3d51cee967`. Foram relidas as descrições vigentes da BACK-410 e das 13 subtasks BACK-411..422 e BACK-476 no Jira. Os 170 critérios estão individualizados em acceptance.md; a evidência combina inspeção da implementação, integração com banco H2 e testes unitários. Nenhuma issue foi movida ou encerrada, e a PR não foi aprovada nem integrada.

O tratamento adicional de dados pessoais e masking pertence à BACK-409 e não faz parte desta implementação. CPF e demais contratos atuais foram preservados. Esta revisão não declara conformidade LGPD.

## Findings corrigidos

| Issue | Causa | Correção |
| --- | --- | --- |
| BACK-421 | Financeiro tratava CANCELED como PAID e ignorava submissão/cutoff da confirmação. | EventParticipationRequirement extraído da regra real e compartilhado com EventOperations; inscrição, cobrança, pagamento e participação permanecem separados. |
| BACK-421 | Pós-evento dependia do Event ativo e do dashboard que bloqueia/atualiza inscrições. | Fallback EventHistory somente no pós-evento e leitura dos fatos preservados sem refresh de inscrição. |
| BACK-416 | Billing validava eventId pela Publication ativa. | Validação por EventChargeContext; apresentação pública em lote com fallback EventHistory. Os cinco produtos continuam disponíveis após exclusão. |
| BACK-422 | Teste OpenAPI cobria principalmente rotas cadastrais. | Cobertura das 38 rotas: permissions, media types exatos, string/binary, Content-Disposition, Cache-Control e erros 400/401/403/404/413/500. Emergência exclusivamente PDF. |
| BACK-416 | Último pagamento escolhido por maior ID, divergindo da cronologia financeira. | Seleção em lote por createdAt e desempate por ID, como na visão financeira de Event. |
| BACK-421 | Pós-evento omitia regras ainda sem cobrança; contexto cadastral ficava apenas nos metadados PDF. | Linhas Evento/Datas/Regras em CSV/PDF com ID, título, datas, situação, contexto histórico, obrigatoriedade, cutoff e vencimento. |
| BACK-421 | Após exclusão do Event, POST_EVENT ainda dependia dos cadastros atuais de Vehicle/Dependent; o dashboard histórico somava apenas capacidade ACTIVE. | Contratos públicos de apresentação em Vehicles/Dependents com fallback batch para snapshots por ID original. POST_EVENT e dashboard histórico recuperam identificação e capacidade sem afetar consultas operacionais. |

## Novos testes e cobertura reforçada

As duas rodadas adicionaram 28 casos executáveis ao total anterior de 1.081, agora 1.109. Esta rodada acrescentou três casos; testes preexistentes também foram ampliados.

Em EventReportServiceTest:

- `participationUsesConfirmationRuleAndKeepsAllStatuses`: nove casos com inscrição CONFIRMED: PAID antes do cutoff; CANCELED; PENDING_VALIDATION antes/no instante/depois do cutoff; PAID após cutoff; PENDING, OVERDUE e EXPIRED com pagamento rejeitado. Confere separadamente as três colunas de status.
- `missingPaymentSubmissionDoesNotSatisfyParticipation`: PAID sem data de submissão continua com pendência, conforme a confirmação existente.
- `participationDistinguishesCutoffOptionalChargeAndCancellation`: cinco casos: CANCELED antes do cutoff; OVERDUE e EXPIRED após cutoff; cobrança opcional; inscrição cancelada.
- `historicalFallbackIsLimitedToPostEventAndDoesNotRefreshRegistration`: histórico disponível somente para POST_EVENT, sem chamar o dashboard que atualiza inscrições.
- `postEventPreservesRulesEvenWithoutGeneratedCharges`: obrigatoriedade, cutoff e vencimento preservados sem fatos financeiros.

Em ExportHttpIntegrationTest:

- `postEventAndAllBillingProductsSurviveRealHardDelete`: Event ativo, finalização e exclusão pelo serviço real; EventHistory e EventChargeContext preservados; pós-evento com participantes, convidado, veículo e finanças em CSV/PDF; cinco produtos Billing antes/depois nos dois formatos; outro relatório continua 404; inscrição pendente não é alterada.
- `billingEventWithoutFinancialContextIs404`: os cinco produtos rejeitam eventId sem contexto com RFC 9457.
- `latestPaymentUsesCreationTimeThenIdAsInEventFinance`: data prevalece sobre ID e ID desempata datas iguais.
- `dependentSelectionAndHistoryStaySeparateInBothFormats`: ACTIVE/DISABLED, id, titular, exclusão de outro agregado e histórico separado em CSV/PDF.
- `membershipPeriodsAndBlocksPreserveSelectionWithoutTechnicalFields`: período, seleção individual de bloqueio, conjunto vazio e ausência de tokens/campos técnicos nos dois formatos.
- `publicationCatalogsRequestsAndDeletionSnapshotsKeepTheirOwnData`: Notice, Service, request e change request possuem conteúdo e filtros próprios; exclusões reais pelo repository preservam snapshots separados e retiram os cadastros operacionais, em CSV/PDF.
- `postEventKeepsVehicleAndDependentAfterTheirRealHardDeletes`: exclusão real do Event, Vehicle e Dependent; snapshots persistidos, POST_EVENT em CSV/PDF, participante e placa associados, capacidade/ocupação/vagas preservadas; consultas operacionais ACTIVE e ownership continuam rejeitando os registros excluídos.
- `historicalPresentationPrefersCurrentAndHandlesUnknownIds`: quando atual e histórico coexistem, vence o atual; ID sem nenhum registro retorna vazio; lotes acima de 500 são recusados.
- `historicalPresentationNeverRevealsAnotherHouseholdAndKeepsMissingVehicleId`: vínculo histórico com outro titular não revela identidade; veículo sem cadastro/snapshot preserva o ID com indicação de indisponibilidade, sem ser confundido com falta de alocação.

Em ExportRendererTest:

- `characterLimitFailsBeforeWritingOversizedCell`: limite de quatro milhões de caracteres em CSV/PDF.
- `pageLimitFailsWithoutTruncatingLongPdf`: PDF que excede mil páginas falha sem truncamento.

Testes existentes reforçados: `openApiDocumentsFilesAndMatchingPermissions` cobre as 38 rotas; `medicalHouseholdIncludesOwnerWithoutProfileAndOnlyItsDependents` também verifica PDF e dependentId individual; `eventReportsAndIndividualEmergencyExecuteRealContracts` também persiste 2.001 vínculos e verifica 413 com application/problem+json, sem header de download, em CSV/PDF.

## Evidência por issue

| Issue | Implementação e evidência principal |
| --- | --- |
| BACK-410 | Contratos HTTP/OpenAPI (38 rotas), renderer e auditoria transversal; validation.md |
| BACK-411 | Matriz comparada a controllers, queries, filtros e contratos públicos |
| BACK-412 | ExportRendererTest: encoding, injection, paginação, quatro limites, auditoria |
| BACK-413 | identityFieldsAndIndividualSelectionNeverExposeStorage + inspeção de filtros |
| BACK-414 | authenticationSelectsOnlyAdministrativeMetadataAndOwner + projeções sem hashes/tokens |
| BACK-415 | membershipStatusFilterAndIndividualSelection; membershipPeriodsAndBlocksPreserveSelectionWithoutTechnicalFields |
| BACK-416 | definitionsPreserveUnassignedAndExpandAssignments; billingPaymentsUseCycleDueDateAndStructuredEventContext; regressões histórico/último pagamento |
| BACK-417 | dependentSelectionAndHistoryStaySeparateInBothFormats + inspeção de agrupamento/contratos |
| BACK-418 | vehiclesExcludeLegacyDeletedAndPreserveFunctionalFields + inspeção de histórico/seleção |
| BACK-419 | toolsExportTraversesMultipleChunksAndCombinesOwnerNameStatus + inspeção de histórico |
| BACK-420 | medicalHouseholdIncludesOwnerWithoutProfileAndOnlyItsDependents + inspeção de histórico/contratos |
| BACK-421 | EventReportServiceTest; integração Event/emergência/hard delete e cadastros/request/histórico |
| BACK-422 | ExportHttpIntegrationTest; ExportRendererTest; execuções completas em validation.md |
| BACK-476 | authorizationIncludesUnassignedUsersAndOnlyActiveRolePermissions + inspeção das relações efetivas |

As regressões preexistentes de EventOperationsIntegrationTest também verificam lifecycle, ownership e auditoria médica. O teste de renderer verifica oito formas de formula injection, UTF-8/BOM, escaping, vazio, null/booleano, timezone, filename, campos longos/paginação, limites e falha de auditoria antes da entrega. Os testes HTTP abrangem filtros, conteúdo, ausência de secrets, authorization e headers; as queries/projeções e o agrupamento de cada produto foram inspecionados.

## Inspeção arquitetural, visual e de volume

O diff contra develop foi revisado: não foi encontrado novo acesso a internals de outro bounded context. O import preexistente RequiresMembership em EventController é uma anotação de autorização preservada. A associação legada AuthenticationAccountEntity → UserEntity não foi ampliada. Não existe suíte ArchUnit no projeto nem foram criadas exceções arquiteturais. A nova leitura EventHistory pertence a Publications; Billing usa seu contexto financeiro e o contrato público de apresentação. Nenhum endpoint global foi criado.

As queries de exportação selecionam campos explícitos. Hashes/tokens de Authentication e storage keys não são renderizados; existência funcional de foto é mantida quando aplicável. Health oferece cobertura batch sem clínica e leitura emergencial individual; a auditoria não grava o conteúdo dos relatórios.

Amostra sintética gerada pelo próprio DefaultExportRenderer e renderizada em duas páginas: títulos, acentos, resumo, agrupamento, campos longos, margens e rodapé inspecionados visualmente. Testes extraem também o conteúdo de PDFs reais obtidos por HTTP.

Consultas tabulares em páginas de 200; enriquecimento em lote (até 500 IDs em Event); último pagamento e apresentação histórica em batch. Não foi identificado N+1 evidente nas queries revisadas. Event reutiliza o grafo operacional após limites prévios de 2.000 vínculos e 20.000 fatos financeiros. Billing/Health limitam a varredura com pós-filtros a 100.000 candidatos. Renderer limita linhas, bytes, caracteres e páginas; geração/auditoria terminam antes da liberação do arquivo, sem truncamento silencioso. Não foi executado benchmark nem houve acesso a volumes de produção; o consumo de memória permanece sujeito aos limites síncronos descritos na matriz.

## Resultado das suítes completas

Ambiente: Windows, Microsoft OpenJDK 17.0.20 e Maven Wrapper executado via Git Bash. Resultados finais sobre o código e os testes desta revisão, em 29/09/2026:

| Comando | Resultado | Testes | Falhas | Erros | Ignorados |
| --- | --- | ---: | ---: | ---: | ---: |
| `./mvnw test` | BUILD SUCCESS | 1.109 | 0 | 0 | 1 |
| `./mvnw verify` | BUILD SUCCESS | 1.109 | 0 | 0 | 1 |

O único ignorado é o teste preexistente `LocalFileStorageTest.shouldRejectSymbolicLinkInsteadOfFollowingItOutsideRoot`, cuja suposição exige suporte a links simbólicos indisponível neste ambiente. Nenhum teste de exportação foi ignorado. As três classes de exportação somam 224 casos (176 HTTP, 30 EventReportService, 18 renderer), todos sem falhas/erros/ignorados. Verify também concluiu empacotamento Spring Boot e geração do relatório JaCoCo.
