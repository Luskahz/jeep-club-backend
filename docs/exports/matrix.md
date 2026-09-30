# Matriz de exportações — BACK-411 / BACK-410

Base auditada: `b051da139395708aba61a2895ebdce3d51cee967` (origin/develop).
Especificações: BACK-410, BACK-411..422, BACK-476, relidas integralmente em 29/09/2026.

O bounded context define colunas e consultas; shared.export define somente o contrato neutro; platform.export renderiza. Nenhuma consulta transversal acessa tabelas externas.

| Issue / módulo | Produto / seleção | Campos e restrições | Formatos | Permission |
| --- | --- | --- | --- | --- |
| BACK-413 Identity | Usuários: todos, ID, filtros cadastrais atuais | Cadastro, status, datas, existência de foto; sem storage | CSV/PDF | IDENTITY_USER_EXPORT |
| BACK-414 Authentication | Contas por identityId; sessões, refresh e recuperação: todos/ID/filtros existentes | Estados, referências e datas; nunca senha/hash/token | CSV/PDF | AUTHENTICATION_EXPORT |
| BACK-476 Authorization | Roles e permissions todos/ID; usuários todos/ID com vínculos e permissions efetivas | Nome/CPF por Identity; somente roles ativas concedem acesso | CSV/PDF | AUTHORIZATION_EXPORT |
| BACK-415 Memberships | Solicitações todos/ID/status/período; bloqueios todos/ID/filtros | Cadastro, decisão, responsáveis, datas; sem activation/version/activeCpf | CSV/PDF | MEMBERSHIP_EXPORT |
| BACK-417 Dependents | Operacional todos/ID/userId; histórico completo separado | Cadastro, vínculo, status/datas, titular público; histórico com executor/data | CSV/PDF | DEPENDENTS_DEPENDENT_EXPORT |
| BACK-418 Vehicles | ACTIVE todos/ID/ownerId; histórico separado | Dados funcionais completos, possui foto; exclui SOFT_DELETED operacional | CSV/PDF | VEHICLES_VEHICLE_EXPORT |
| BACK-419 Tools | Todos/ID/userId/nome/status; histórico separado | Nome, descrição, estado, datas, possui foto; sem storage | CSV/PDF | TOOLS_TOOL_EXPORT |
| BACK-420 Health | Todos/profileId/userId/dependentId; agregado titular/dependentes; histórico separado | Ficha clínica, owners válidos, identificação pública; sem conteúdo clínico em logs | CSV/PDF | HEALTH_MEDICAL_PROFILE_EXPORT |
| BACK-416 Billing | Definitions × Assignments; Cycles; Charges; Payments; Refunds; todos/ID/filtros | Snapshots, valores/status/datas, usuário e Event públicos; sem receiptStorageKey | CSV/PDF | BILLING_EXPORT |
| BACK-421 Publications | Notices, Services, Events todos/ID/filtros/histórico; requests por ID/status | Conteúdo, revisão, datas, sem storage | CSV/PDF | PUBLICATIONS_EXPORT |
| BACK-421 Event operação | Manifesto, transporte, financeiro, acesso, cobertura de saúde, pós-evento | Alocação real de cada pessoa; financeiro separado de inscrição; cobertura sem clínica | CSV/PDF | PUBLICATIONS_EXPORT |
| BACK-421 Event emergência | Participante individual confirmado e ownership válido | Perfil clínico individual, auditoria síncrona; lifecycle não bloqueia | PDF | PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ |

## Matriz por recurso

A necessidade operacional de cada linha é afirmativa. Colunas de exportação são definidas explicitamente no serviço proprietário; rotas, filtros e formatos são documentados no OpenAPI. Sem volume de produção disponível, não se inventa cardinalidade esperada; usa-se o limite síncrono abaixo e mede-se a carga real antes de ampliá-lo. Os riscos de CPF/contato/saúde/finanças seguem BACK-409 e a permission indicada. O filename recebe somente o código estático indicado e timestamp UTC.

| Módulo | Recurso | Necessidade | CSV | PDF | Filtros | Campos incluídos | Excluídos / minimizados | Permission | Volume esperado / limite | Risco LGPD | Filename base |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| Identity | Usuários | Sim | Sim | Sim | todos, id, filtros cadastrais | cadastro, datas, status, possui foto | storage da foto | `IDENTITY_USER_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Alto: CPF/contato | `usuarios` |
| Authentication | Contas | Sim | Sim | Sim | todos, identityId | estados, datas, tentativas | passwordHash | `AUTHENTICATION_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Moderado: operação | `contas-autenticacao` |
| Authentication | Sessões | Sim | Sim | Sim | todos, id, userId, status | usuário, datas, estado | JWT e segredos | `AUTHENTICATION_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Moderado: operação | `sessoes` |
| Authentication | Refresh tokens | Sim | Sim | Sim | todos, id, userId, status | sessão, datas, estado, substituição | tokenHash | `AUTHENTICATION_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Moderado: operação | `registros-renovacao` |
| Authentication | Recuperação | Sim | Sim | Sim | todos, id, userId, status | usuário, datas, método, estado | tokenHash | `AUTHENTICATION_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Moderado: operação | `recuperacoes-senha` |
| Authorization | Papéis | Sim | Sim | Sim | todos, id, nome, status | papel, tipo, estado, datas | relações isoladas | `AUTHORIZATION_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Moderado: operação | `papeis` |
| Authorization | Permissões | Sim | Sim | Sim | todos, id | código, módulo, descrição, datas | credenciais | `AUTHORIZATION_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Moderado: operação | `permissoes` |
| Authorization | Acesso do usuário | Sim | Sim | Sim | todos, userId | nome, CPF, roles, permissões efetivas | CPF persistido localmente | `AUTHORIZATION_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Alto: CPF/contato | `acessos-usuarios` |
| Memberships | Solicitações | Sim | Sim | Sim | todos, id, nome, status, período | cadastro, revisão, datas | activation token e version | `MEMBERSHIP_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Alto: CPF/contato | `solicitacoes-adesao` |
| Memberships | Bloqueios | Sim | Sim | Sim | todos, id, período | CPF, motivo, ator, datas | activeCpf | `MEMBERSHIP_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Alto: CPF/contato | `bloqueios` |
| Dependents | Dependentes | Sim | Sim | Sim | todos, id, userId | cadastro, vínculo, titular | storage | `DEPENDENTS_DEPENDENT_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Alto: CPF/contato | `dependentes` |
| Dependents | Histórico | Sim | Sim | Sim | todos | snapshot, ator/data exclusão | storage | `DEPENDENTS_DEPENDENT_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Moderado: operação | `historico-dependentes` |
| Vehicles | Veículos ACTIVE | Sim | Sim | Sim | todos, id, ownerId | dados funcionais, titular, possui foto | foto/storage | `VEHICLES_VEHICLE_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Moderado: operação | `veiculos` |
| Vehicles | Histórico | Sim | Sim | Sim | todos | snapshot, ator/data exclusão | foto/storage | `VEHICLES_VEHICLE_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Moderado: operação | `historico-veiculos` |
| Tools | Ferramentas | Sim | Sim | Sim | todos, id, userId, nome, status | descrição, status, titular, possui foto | photoStorageKey | `TOOLS_TOOL_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Moderado: operação | `ferramentas` |
| Tools | Histórico | Sim | Sim | Sim | todos | snapshot, ator/data exclusão | photoStorageKey | `TOOLS_TOOL_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Moderado: operação | `historico-ferramentas` |
| Health | Perfis | Sim | Sim | Sim | todos, profileId, userId, dependentId, agregado | perfil clínico, titular, dependentes | fichas de outros titulares no agregado | `HEALTH_MEDICAL_PROFILE_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Muito alto: clínica | `perfis-medicos` |
| Health | Histórico | Sim | Sim | Sim | todos | snapshot clínico, ator/data exclusão | outros módulos internos | `HEALTH_MEDICAL_PROFILE_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Muito alto: clínica | `historico-perfis-medicos` |
| Billing | Definições × atribuições | Sim | Sim | Sim | todos, id, público, recorrência, status | configuração e alvo legível | dados técnicos | `BILLING_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Alto: finanças | `definicoes-atribuicoes` |
| Billing | Ciclos | Sim | Sim | Sim | todos, id, definição, recorrência, período, status | snapshots, vencimento, operação | internals não funcionais | `BILLING_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Alto: finanças | `ciclos` |
| Billing | Cobranças | Sim | Sim | Sim | todos, id, usuário, definição, ciclo, evento, período, status | fato, referência, último pagamento, situação efetiva | receiptStorageKey | `BILLING_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Alto: finanças | `cobrancas` |
| Billing | Pagamentos | Sim | Sim | Sim | todos, id, usuário, definição, ciclo, evento, período, método, status | fato, cobrança, contexto | receiptStorageKey | `BILLING_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Alto: finanças | `pagamentos` |
| Billing | Reembolsos | Sim | Sim | Sim | todos, id, usuário, definição, ciclo, evento, período, status | fato, pagamento original, contexto | receiptStorageKey | `BILLING_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Alto: finanças | `reembolsos` |
| Publications | Avisos + histórico | Sim | Sim | Sim | todos, id, status, período; histórico todos | conteúdo, editorial, datas | mídia/storage | `PUBLICATIONS_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Moderado: operação | `publicacoes-notices / publicacoes-notice-history` |
| Publications | Serviços + histórico | Sim | Sim | Sim | todos, id, status, período; histórico todos | conteúdo, preço, editorial, datas | mídia/storage | `PUBLICATIONS_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Moderado: operação | `publicacoes-services / publicacoes-service-history` |
| Publications | Solicitações de serviço/alteração | Sim | Sim | Sim | todos, id, status, período | proposta, revisão, datas | mídia/storage | `PUBLICATIONS_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Moderado: operação | `publicacoes-service-requests / publicacoes-change-requests` |
| Publications | Eventos + histórico | Sim | Sim | Sim | todos, id, status, lifecycle, período; histórico todos | cadastro, regras financeiras resumidas | mídia/storage | `PUBLICATIONS_EXPORT` | Não medido; CSV 20.000 / PDF 500 | Alto: CPF/contato | `publicacoes-events / publicacoes-event-history` |
| Publications | Manifesto | Sim | Sim | Sim | eventId, status, tipo, alocação | pessoas, veículo, financeiro resumido | clínica | `PUBLICATIONS_EXPORT` | Não medido; evento 2.000 vínculos + 20.000 fatos; PDF 500 | Alto: CPF/contato | `evento-manifest` |
| Publications | Transporte | Sim | Sim | Sim | eventId, view e participantes | veículos, ocupação, pendências | clínica e chaves | `PUBLICATIONS_EXPORT` | Não medido; evento 2.000 vínculos + 20.000 fatos; PDF 500 | Moderado: operação | `evento-transport` |
| Publications | Financeiro | Sim | Sim | Sim | eventId, status inscrição | membro × regra/cobrança/pagamento | clínica | `PUBLICATIONS_EXPORT` | Não medido; evento 2.000 vínculos + 20.000 fatos; PDF 500 | Alto: finanças | `evento-financial` |
| Publications | Acesso | Sim | Sim | Sim | eventId, status, tipo, alocação | nome, CPF, veículo, confirmação | clínica e financeiro | `PUBLICATIONS_EXPORT` | Não medido; evento 2.000 vínculos + 20.000 fatos; PDF 500 | Alto: CPF/contato | `evento-access` |
| Publications | Cobertura de saúde | Sim | Sim | Sim | eventId, status, tipo, alocação | participante e existência de ficha | conteúdo clínico | `PUBLICATIONS_EXPORT` | Não medido; evento 2.000 vínculos + 20.000 fatos; PDF 500 | Moderado: operação | `evento-health-coverage` |
| Publications | Pós-evento | Sim | Sim | Sim | eventId | pessoas, ocupação, cobranças, pagamentos | clínica | `PUBLICATIONS_EXPORT` | Não medido; evento 2.000 vínculos + 20.000 fatos; PDF 500 | Alto: CPF/contato | `evento-post-event` |
| Publications | Ficha emergencial | Sim | Não | Somente PDF | eventId, tipo, participante confirmado | perfil clínico individual | massa clínica | `PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ` | 1 participante / PDF 1 | Muito alto: clínica | `ficha-emergencial` |

## Campos e filtros

As listas explícitas de colunas pertencem aos modelos de exportação do módulo. OpenAPI é a fonte dos endpoints/parâmetros. Billing deriva mês/ano do vencimento e usa EventChargeContext. O PDF por titular agrupa identificação e recursos. CSV permanece plano. A seleção filtrada percorre todos os lotes correspondentes; não aceita paginação HTTP para cortar resultados.

## Volume e operação

Não há medição de volume de produção disponível. A V2 usa limites síncronos conservadores e configuráveis: CSV 20.000 linhas; PDF 500 registros; bytes 16 MiB. Leitura interna de 200 registros por lote, ordenação estável por ID. Excesso retorna 413 sem arquivo parcial. PDF também limita páginas. Nomes são códigos estáticos + timestamp UTC; nunca nomes pessoais, CPF ou clínica. Datas visíveis usam America/Sao_Paulo; LocalDateTime legado representa UTC. Null é célula vazia; booleano Sim/Não. CSV usa UTF-8/BOM, `;`, CRLF e aspas em todas as células.

## Segurança / BACK-409

O tratamento adicional de dados pessoais e masking pertence à BACK-409 e não faz parte desta implementação. Esta rodada preserva CPF e os demais contratos cadastrais atuais. A seleção de CPF integral corresponde às finalidades administrativas expressamente solicitadas e exige permission EXPORT independente de READ. Dados clínicos têm permission própria, auditoria sem payload e cache proibido. Controle de acesso de Event não contém dados financeiros ou médicos. Não se declara conformidade jurídica.

## Decisões de não exportar

- PasswordChangeChallenge, tokens de ativação e segredos: sem finalidade administrativa e com risco de acesso indevido.
- UserRole/RolePermission isolados: relações intermediárias da visão efetiva.
- Likes/comentários: excluídos explicitamente pela BACK-421.
- Configuração singleton de membership, mídia bruta e logs Platform: sem produto de exportação definido na Story; Platform é infraestrutura.
- Storage keys, hashes, versões JPA, material criptográfico: internals sem finalidade no relatório.
- Clínica em massa via Publications: somente cobertura de existência ou emergência individual.

Esta matriz registra o escopo de implementação; não equivale a uma declaração de conclusão. Evidências e critérios devem ser verificados ao final.

## Contratos e evidência de entrega

- Inventário completo de 38 rotas: [endpoints.md](endpoints.md).
- Critérios de aceite, um por linha: [acceptance.md](acceptance.md).
- Evidências de validação e limites: [validation.md](validation.md).

Além de linhas/bytes, o renderer limita o total de conteúdo a 4 milhões de caracteres e PDFs a 1.000 páginas. Billing e Health abortam após 100.000 candidatos quando a aplicação precisa pós-filtrar o conjunto. Event admite no máximo 2.000 registros/vínculos operacionais e 20.000 cobranças + pagamentos antes de carregar o dashboard existente. Leituras de enriquecimento público usam lotes de até 500 IDs. O banco usa as transações e timeouts configurados pelo ambiente; nenhuma fila foi introduzida. Os limites síncronos devem ser ajustados com medições reais de produção, que não estão disponíveis nesta tarefa.

O audit log exige persistência síncrona antes do download. Identifica ator e tipo de exportação com rota estática; não armazena filtros, nomes, CPF, conteúdo clínico ou arquivo. As tentativas de emergência também usam a auditoria obrigatória já existente de Event.

## Histórico e semântica financeira

- Somente POST_EVENT resolve o cadastro pelo Event ativo ou, na sua ausência, por EventHistory (ID original, título, início, término, estado cadastral e data de exclusão). As inscrições, alocações, convidados e fatos financeiros preservados compõem o relatório. A leitura histórica não confirma nem altera inscrições; os demais relatórios continuam exigindo Event ativo.
- O pós-evento inclui contexto cadastral e regras financeiras também no CSV, inclusive regras ainda sem cobrança gerada.
- O filtro Billing por eventId valida EventChargeContext. A apresentação consulta Event ativo e busca os IDs ausentes em EventHistory, em lote, pelo contrato público EventPresentationQuery. Ausência de metadados não invalida contexto financeiro existente. Sem contexto financeiro, o filtro retorna 404.
- A situação para participação usa EventParticipationRequirement, compartilhada com a confirmação real. PAID ou PENDING_VALIDATION satisfazem a obrigação somente com submissão até o cutoff, inclusive no instante exato. CANCELED isoladamente não satisfaz; status da inscrição, cobrança e pagamento permanecem separados. Inscrição CONFIRMED pode apresentar pendência. Cobrança opcional é identificada como não obrigatória para participação.
- O último pagamento de MemberCharge é selecionado por createdAt e, no empate, ID; a mesma ordenação é usada na visão financeira de Event. Não há consulta por linha.
