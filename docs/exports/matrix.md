# Matriz de exportações — BACK-411 / BACK-410

Base auditada: `b051da139395708aba61a2895ebdce3d51cee967` (origin/develop).
Especificações: BACK-410, BACK-411..422, BACK-476, consultadas em 28/09/2026.

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

## Campos e filtros

As listas explícitas de colunas pertencem aos modelos de exportação do módulo. OpenAPI é a fonte dos endpoints/parâmetros. Billing deriva mês/ano do vencimento e usa EventChargeContext. O PDF por titular agrupa identificação e recursos. CSV permanece plano. A seleção filtrada percorre todos os lotes correspondentes; não aceita paginação HTTP para cortar resultados.

## Volume e operação

Não há medição de volume de produção disponível. A V2 usa limites síncronos conservadores e configuráveis: CSV 20.000 linhas; PDF 500 registros; bytes 16 MiB. Leitura interna de 200 registros por lote, ordenação estável por ID. Excesso retorna 413 sem arquivo parcial. PDF também limita páginas. Nomes são códigos estáticos em português + timestamp UTC; nunca nomes pessoais, CPF ou clínica. Datas visíveis usam America/Sao_Paulo; LocalDateTime legado representa UTC. Null é célula vazia; booleano Sim/Não. CSV usa UTF-8/BOM, `;`, CRLF e aspas em todas as células.

## Segurança / BACK-409

BACK-409 continua em investigação e não contém decisão aprovada de criptografia/masking. Esta Story consome os contratos cadastrais atuais, não inventa criptografia nem duplica CPF. A seleção de CPF integral corresponde às finalidades administrativas expressamente solicitadas e exige permission EXPORT independente de READ. Dados clínicos têm permission própria, auditoria sem payload e cache proibido. Controle de acesso de Event não contém dados financeiros ou médicos. A revisão de proteção em repouso continua pertencendo à BACK-409, sem declarar conformidade jurídica.

## Decisões de não exportar

- PasswordChangeChallenge, tokens de ativação e segredos: sem finalidade administrativa e com risco de acesso indevido.
- UserRole/RolePermission isolados: relações intermediárias da visão efetiva.
- Likes/comentários: excluídos explicitamente pela BACK-421.
- Configuração singleton de membership, mídia bruta e logs Platform: sem produto de exportação definido na Story; Platform é infraestrutura.
- Storage keys, hashes, versões JPA, material criptográfico: internals sem finalidade no relatório.
- Clínica em massa via Publications: somente cobertura de existência ou emergência individual.

Esta matriz registra o escopo de implementação; não equivale a uma declaração de conclusão. Evidências e critérios devem ser verificados ao final.
