# Relatório 1 — Auditoria JPA do PR #100

Referências imutáveis: master `6865bc4f600462c5fcc6cf1212c9660591eff4db`; develop `9d3fba331fba5a9284c85a0e5ab718efb58f93b5`; referência de 02/10 `e4aa00e4a492900b26cec78839dcfae480712b0b`.

**O PR #100 não muda o schema físico esperado.** Comparados integralmente os 52 `@Entity`, três `@Embeddable`, todos os enums Java (incluindo os importados pelas entidades), configurações, SQL e diff completo. Os 55 fontes mapeados têm o mesmo blob Git. Não existem AttributeConverter/@Converter nem orm.xml/persistence.xml introduzidos no intervalo. `pom.xml` mantém Boot 4.0.4/Hibernate 7.2.7.Final e drivers; suas mudanças são cobertura/PIT. Naming strategies e opções JPA não mudam. Evidência detalhada: [mapping-audit.json](mapping-audit.json), incluindo hashes e os 116 inputs indiretos examinados. Somente os dois bundles de mensagens diferem nesses inputs entre master e develop.

Isso prova igualdade dos contratos de DDL entre os dois commits, **não** prova igualdade com a VPS. A comparação também abrange PK/IDENTITY, FKs, @Version, auditoria, nullable, precisão/escala, enums nativos, índices/unique e tabelas de collections/herança. Os SQL V1–V3 materializam os tipos e constraints exatos dos mappings; a suíte MySQL compara todas as colunas, índices e relações do resultado com o DDL Hibernate de referência.

| Entidade/tabela | master (blob) | develop (blob) | Mudança | Migration | Risco |
|---|---|---|---|---|---|
| ChargeCycleEntity / `billing_charge_cycles` | `23b1ed6a40a2` | `23b1ed6a40a2` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| ChargeDefinitionEntity / `billing_charge_definitions` | `ac4303e3900c` | `ac4303e3900c` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| EventChargeContextEntity / `billing_event_charge_contexts` | `91fddc2289d0` | `91fddc2289d0` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| MemberChargeEntity / `billing_member_charges` | `92a03e5c229e` | `92a03e5c229e` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| MemberPaymentEntity / `billing_member_payments` | `c8487235bd81` | `c8487235bd81` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| MemberRefundEntity / `billing_member_refunds` | `5dc517a1d660` | `5dc517a1d660` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| AllMembersChargeAssignmentEntity / `billing_all_members_charge_assignments` | `31b7d6eba7a2` | `31b7d6eba7a2` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| ChargeAssignmentEntity / `billing_charge_assignments` | `47fbbdfaa071` | `47fbbdfaa071` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| EventParticipantsChargeAssignmentEntity / `billing_event_participants_charge_assignments` | `7c46c88145b3` | `7c46c88145b3` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| RoleChargeAssignmentEntity / `billing_role_charge_assignments` | `2c49cf0bca8b` | `2c49cf0bca8b` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| UserChargeAssignmentEntity / `billing_user_charge_assignments` | `1eab7e398f0f` | `1eab7e398f0f` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| DependentEntity / `dependents_dependent` | `e8463572b45c` | `e8463572b45c` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| DependentHistoryEntity / `dependents_dependent_history` | `1b17939ee642` | `1b17939ee642` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| MedicalProfileEntity / `medical_profiles` | `19fcb1815e40` | `19fcb1815e40` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| MedicalProfileHistoryEntity / `medical_profiles_history` | `04024e37f00e` | `04024e37f00e` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| AccountActivationInvitationEntity / `authentication_account_activation_invitations` | `336550da277d` | `336550da277d` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| AuthenticationAccountEntity / `authentication_accounts` | `a60559d52513` | `a60559d52513` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| PasswordChangeChallengeEntity / `authentication_password_change_challenges` | `e7e96f4758b1` | `e7e96f4758b1` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| PasswordRecoveryRequestEntity / `authentication_password_recovery_requests` | `f6f2ae792341` | `f6f2ae792341` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| RefreshTokenEntity / `authentication_refresh_tokens` | `8905792e05be` | `8905792e05be` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| SessionEntity / `authentication_sessions` | `356571fcce1a` | `356571fcce1a` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| PermissionEntity / `authorization_permissions` | `d63d0a12d525` | `d63d0a12d525` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| RoleEntity / `authorization_roles` | `fc4919be80e5` | `fc4919be80e5` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| RolePermissionEntity / `authorization_role_permissions` | `81a5c43d4a7c` | `81a5c43d4a7c` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| UserRoleEntity / `authorization_user_roles` | `748f6199c972` | `748f6199c972` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| UserEntity / `identity_users` | `06fae27cf631` | `06fae27cf631` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| UserProfileEntity / `identity_user_profiles` | `57f450f803c5` | `57f450f803c5` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| MemberActivationTokenEntity / `membership_activation_tokens` | `59f4714beeee` | `59f4714beeee` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| MembershipApplicantBlockEntity / `membership_applicant_blocks` | `3df25fdd47a5` | `3df25fdd47a5` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| MembershipApplicationEntity / `membership_applications` | `8bb408ef6ea1` | `8bb408ef6ea1` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| MembershipBillingConfigurationEntity / `membership_billing_configurations` | `ccca0e7156b3` | `ccca0e7156b3` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| SystemLogEntity / `platform_logs` | `f14810f9309f` | `f14810f9309f` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| EventChargeRuleEntity / `event_charge_rules` | `bb3b54790e57` | `bb3b54790e57` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| EventEntity / `publication_events` | `cbab0188839c` | `cbab0188839c` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| EventGuestRequestEntity / `event_guest_requests` | `4f26f4a39512` | `4f26f4a39512` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| EventHistoryEntity / `publication_event_history` | `8b035da2e99a` | `8b035da2e99a` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| EventOccupantEmbeddable / `event_registration_occupants (embeddable)` | `78ab25da7406` | `78ab25da7406` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| EventRegistrationEntity / `event_registrations` | `b936f124d5c5` | `b936f124d5c5` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| EventRideOfferEntity / `event_ride_offers` | `835bc2caf237` | `835bc2caf237` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| NoticeEntity / `publication_notices` | `5b054fa5c7c2` | `5b054fa5c7c2` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| NoticeHistoryEntity / `publication_notice_history` | `6afe20d6a238` | `6afe20d6a238` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| PublicationCommentEntity / `publication_comments` | `e5c5ad4dc28a` | `e5c5ad4dc28a` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| PublicationCommentImageEntity / `publication_comment_images (embeddable)` | `dad4ac53dd31` | `dad4ac53dd31` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| PublicationEntity / `publications` | `e0a0f32986e0` | `e0a0f32986e0` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| PublicationHistoryEntity / `publication_history` | `44ce29755acd` | `44ce29755acd` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| PublicationImageEntity / `publication_images (embeddable)` | `74a5b360add2` | `74a5b360add2` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| PublicationLikeEntity / `publication_likes` | `2c206235550b` | `2c206235550b` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| ServicePublicationChangeRequestEntity / `service_publication_change_requests` | `bd5cf6548e8b` | `bd5cf6548e8b` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| ServicePublicationEntity / `publication_services` | `35cd795cbc93` | `35cd795cbc93` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| ServicePublicationHistoryEntity / `publication_service_history` | `b78967628afe` | `b78967628afe` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| ServicePublicationRequestEntity / `service_publication_requests` | `07ba0e5401b5` | `07ba0e5401b5` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| ToolEntity / `tools` | `430378e41ab0` | `430378e41ab0` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| ToolHistoryEntity / `tools_tool_history` | `09b83da8d42d` | `09b83da8d42d` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| VehicleEntity / `vehicles_vehicle` | `b24880f57818` | `b24880f57818` | Idêntico | Nenhuma no PR #100 | Verificar drift B |
| VehicleHistoryEntity / `vehicles_vehicle_history` | `f1f29d1b698f` | `f1f29d1b698f` | Idêntico | Nenhuma no PR #100 | Verificar drift B |

## Alterações indiretas e semântica de dados

- PR #97 / merge `1f8b520` (BACK-522): alteração administrativa permite CPF/e-mail mantendo ID estável; valida CPF apenas quando alterado. A persistência reusa colunas e unique CPF/e-mail/RG existentes. Dados antigos não são regravados no startup; um identificador antigo inválido pode exigir correção numa edição futura. Não converter CPFs em massa.
- `AccountActivationInvitation.invalidate`: novo valor textual `INVALIDATED` cabe em `varchar(24)`; não é enum MySQL, conversor nem CHECK novo. Também expira o convite. Sem backfill. Rollback funcional exige revisão do fluxo de credenciais, mesmo sem mudança de DDL.
- Novas consultas JPQL e `PESSIMISTIC_WRITE` em User/PasswordRecoveryRequest, nova projeção do applicationId em Membership: usam colunas existentes. Locks seguem identidade → conta → convite/token; concorrência precisa smoke test MySQL e monitoramento de deadlocks. Não foi adicionado índice especulativo.
- PR #96 / `b00f492`: foto de Tools e proprietário de Vehicles são DTO/projeção de atributos existentes; sem coluna nova. PRs #98/#99 (`9d3fba3`/`a439204`) adicionam testes/correções e não alteram mappings.
- Três queries nativas preexistentes (`SELECT id ... FOR UPDATE`) em Publication/ServicePublicationRequest/ChangeRequest não mudaram. Nenhum DDL manual ou script SQL de produção foi adicionado pelo PR #100 original. Auditoria cadastral passa a gerar eventos adicionais em `system_logs`, sem mudança da tabela.

## História física e coleções

Desde a referência de 02/10, somente duas novas entidades físicas: UserProfileEntity (commit `71a9abc`, integrado pelo #93) e AccountActivationInvitationEntity (`dfc583a`, integrado pelo #95). Todos os outros mappings e enums persistidos já estavam presentes. `UserActivationMethod`, novo no #95, é contrato Java/API e não atributo JPA. V1 representa 58 tabelas; V2 adiciona `identity_user_profiles` com PK/FK compartilhada e nove campos opcionais; V3 adiciona convite com PK identity_id, token_hash unique, datas, delivery_status e versão otimista. Nenhuma dessas migrations altera linhas existentes.

O alvo final possui 60 tabelas de aplicação, além de `flyway_schema_history`. Inclui tabelas de herança JOINED e collections; não se deve usar a contagem de entidades como contagem de tabelas. Inventário físico completo abaixo:

- `authentication_account_activation_invitations`
- `authentication_accounts`
- `authentication_password_change_challenges`
- `authentication_password_recovery_requests`
- `authentication_refresh_tokens`
- `authentication_sessions`
- `authorization_permissions`
- `authorization_role_permissions`
- `authorization_roles`
- `authorization_user_roles`
- `billing_all_members_charge_assignments`
- `billing_charge_assignments`
- `billing_charge_cycles`
- `billing_charge_definitions`
- `billing_event_charge_contexts`
- `billing_event_participants_charge_assignments`
- `billing_member_charges`
- `billing_member_payments`
- `billing_member_refunds`
- `billing_role_charge_assignments`
- `billing_user_charge_assignments`
- `dependents_dependent`
- `dependents_dependent_history`
- `event_charge_rules`
- `event_guest_requests`
- `event_registration_occupants`
- `event_registration_unallocated_dependents`
- `event_registration_vehicles`
- `event_registrations`
- `event_ride_offers`
- `identity_user_profiles`
- `identity_users`
- `medical_profiles`
- `medical_profiles_history`
- `membership_activation_tokens`
- `membership_applicant_blocks`
- `membership_applications`
- `membership_billing_configurations`
- `platform_logs`
- `publication_comment_images`
- `publication_comments`
- `publication_event_history`
- `publication_events`
- `publication_history`
- `publication_image_history`
- `publication_images`
- `publication_likes`
- `publication_notice_history`
- `publication_notices`
- `publication_service_history`
- `publication_services`
- `publications`
- `service_publication_change_request_images`
- `service_publication_change_requests`
- `service_publication_request_images`
- `service_publication_requests`
- `tools`
- `tools_tool_history`
- `vehicles_vehicle`
- `vehicles_vehicle_history`
