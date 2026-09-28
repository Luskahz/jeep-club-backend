# Contratos HTTP de exportação

Todas as rotas são GET e administrativas. `format=CSV` (padrão) ou `PDF`, também em minúsculas. A ficha emergencial só aceita PDF. Sem paginação HTTP: os filtros selecionam todo o conjunto até o limite; resposta 413 se excedido.

| Módulo | Rota | Permission | Parâmetros |
| --- | --- | --- | --- |
| billing | `/billing/admin/definitions/export` | `BILLING_EXPORT` | format, BillingExportFilter: ver tabela abaixo |
| billing | `/billing/admin/cycles/export` | `BILLING_EXPORT` | format, BillingExportFilter: ver tabela abaixo |
| billing | `/billing/admin/member-charges/export` | `BILLING_EXPORT` | format, BillingExportFilter: ver tabela abaixo |
| billing | `/billing/admin/payments/export` | `BILLING_EXPORT` | format, BillingExportFilter: ver tabela abaixo |
| billing | `/billing/admin/refunds/export` | `BILLING_EXPORT` | format, BillingExportFilter: ver tabela abaixo |
| dependents | `/admin/dependents/export` | `DEPENDENTS_DEPENDENT_EXPORT` | format, id, userId, name, status |
| dependents | `/admin/dependents/history/export` | `DEPENDENTS_DEPENDENT_EXPORT` | format |
| health | `/admin/medical-profiles/export` | `HEALTH_MEDICAL_PROFILE_EXPORT` | format, profileId, userId, dependentId, household |
| health | `/admin/medical-profiles/history/export` | `HEALTH_MEDICAL_PROFILE_EXPORT` | format |
| iam/authentication | `/authentication/admin/accounts/export` | `AUTHENTICATION_EXPORT` | format, identityId |
| iam/authentication | `/authentication/admin/password-recovery-requests/export` | `AUTHENTICATION_EXPORT` | format, id, status, userId |
| iam/authentication | `/authentication/admin/refresh-tokens/export` | `AUTHENTICATION_EXPORT` | format, id, status, userId |
| iam/authentication | `/authentication/admin/sessions/export` | `AUTHENTICATION_EXPORT` | format, id, status, userId |
| iam/authorization | `/authorization/admin/permissions/export` | `AUTHORIZATION_EXPORT` | format, id |
| iam/authorization | `/authorization/admin/roles/export` | `AUTHORIZATION_EXPORT` | format, id, name, status |
| iam/authorization | `/authorization/admin/users/export` | `AUTHORIZATION_EXPORT` | userId, format |
| iam/identity | `/identity/admin/users/export` | `IDENTITY_USER_EXPORT` | format, AdminUserFilterDTO: mesmos filtros de Identity |
| memberships | `/admin/membership-applications/blocks/export` | `MEMBERSHIP_EXPORT` | format, id, from, to |
| memberships | `/admin/membership-applications/export` | `MEMBERSHIP_EXPORT` | format, id, name, status, from, to |
| publications | `/admin/events/{eventId}/reports/manifest/export` | `PUBLICATIONS_EXPORT` | eventId, format, status, type, withVehicle, view |
| publications | `/admin/events/{eventId}/reports/transport/export` | `PUBLICATIONS_EXPORT` | eventId, format, status, type, withVehicle, view |
| publications | `/admin/events/{eventId}/reports/financial/export` | `PUBLICATIONS_EXPORT` | eventId, format, status, type, withVehicle, view |
| publications | `/admin/events/{eventId}/reports/access/export` | `PUBLICATIONS_EXPORT` | eventId, format, status, type, withVehicle, view |
| publications | `/admin/events/{eventId}/reports/health-coverage/export` | `PUBLICATIONS_EXPORT` | eventId, format, status, type, withVehicle, view |
| publications | `/admin/events/{eventId}/reports/post-event/export` | `PUBLICATIONS_EXPORT` | eventId, format, status, type, withVehicle, view |
| publications | `/admin/events/{eventId}/health/{type}/{target}/export` | `PUBLICATIONS_EVENT_HEALTH_EMERGENCY_READ` | eventId, type, target |
| publications | `/admin/notices/export` | `PUBLICATIONS_EXPORT` | format, id, status, from, to |
| publications | `/admin/notices/history/export` | `PUBLICATIONS_EXPORT` | format |
| publications | `/admin/services/export` | `PUBLICATIONS_EXPORT` | format, id, status, from, to |
| publications | `/admin/services/history/export` | `PUBLICATIONS_EXPORT` | format |
| publications | `/admin/events/export` | `PUBLICATIONS_EXPORT` | format, id, status, from, to, lifecycle |
| publications | `/admin/events/history/export` | `PUBLICATIONS_EXPORT` | format |
| publications | `/admin/service-publication-requests/export` | `PUBLICATIONS_EXPORT` | format, id, status, from, to |
| publications | `/admin/service-publication-change-requests/export` | `PUBLICATIONS_EXPORT` | format, id, status, from, to |
| tools | `/admin/tools/export` | `TOOLS_TOOL_EXPORT` | format, id, userId, name, status |
| tools | `/admin/tools/history/export` | `TOOLS_TOOL_EXPORT` | format |
| vehicles | `/vehicles/admin/export` | `VEHICLES_VEHICLE_EXPORT` | format, id, ownerId |
| vehicles | `/vehicles/admin/history/export` | `VEHICLES_VEHICLE_EXPORT` | format |

Billing: id, userId, chargeDefinitionId, chargeCycleId, eventId, recurrence, year, month (exige year), status, paymentMethod, chargeStatus, effectiveStatus, dueFrom/dueTo e from/to. Filtros incompatíveis retornam 400. Definition não aceita período de ciclo; Cycle não aceita usuário; paymentMethod só pertence a pagamentos. Em pagamentos, from/to seleciona submittedAt; em refunds, requestedAt; demais produtos usam createdAt. dueFrom/dueTo e ano/mês usam vencimento do ciclo.

Health: profileId, userId, dependentId são mutuamente exclusivos; household=true exige userId. Histórico não possui ID ou filtros.

Event: status da inscrição, type (MEMBER/DEPENDENT/GUEST), withVehicle nas visões de participantes. Financeiro aceita status; pós-evento é consolidado sem filtros. view é exclusivo de transporte: ALL, FULL, AVAILABLE, UNALLOCATED_PEOPLE, UNALLOCATED_GUESTS, PENDING_GUESTS, ACCEPTED_RIDES. Quando filtra veículos por ocupantes, a capacidade/ocupação continua sendo a real, sem descontar participantes ocultos pelo filtro.

Erros: 400 filtros/formato, 401 sem autenticação, 403 permission insuficiente, 404 referência inexistente, 413 limite, 500 falha de geração/auditoria. Event mantém seus erros operacionais globais, inclusive 409 para conflito. Arquivo só é enviado depois da geração e auditoria completas.
