# Matriz por campo e inventário de armazenamento

Baseline: `develop@b051da139395708aba61a2895ebdce3d51cee967`, 29/09/2026.
[Auditoria](README.md) · [Arquitetura](architecture.md) · [Backlog](backlog.md).

## Legenda e interpretação

- **PD**: dado pessoal direto/indireto quando relacionado ao titular.
- **PS**: categoria sensível; saúde está identificada explicitamente. “PS possível” exige avaliar conteúdo, não significa que todo texto/foto seja juridicamente sensível.
- **NP**: configuração/dado técnico não pessoal isoladamente; vincular a titular ou inserir PII pode mudar a classificação.
- **AEAD**: criptografia autenticada em aplicação proposta; **HMAC**: blind index de igualdade com chave separada; **Claro**: decisão de manter em claro com autorização, controles de infraestrutura e retenção, não ausência de risco.
- “Sem filtro dedicado” se refere às consultas implementadas examinadas. Não é prova de que sort arbitrário via Pageable nunca use o campo; verificar e preservar contrato na implementação.
- Unicidade é a regra **atual**: PK, constraint e regra de negócio são distinguidas. Histórico não herda unicidade civil da tabela operacional.
- Coluna física inferida usa naming strategy atual para campos sem nome explícito; não é extração de um schema de produção. Relações e coleções são indicadas como tal.
- Todas as 50 entidades (458 declarações de campo) estão listadas; existem oito tabelas de ElementCollection, discriminadas adiante. Herança JOINED mantém campos base nas tabelas base, sem duplicar fisicamente o cadastro no subtipo.
- Prioridade de leitura: Health primeiro. Recomendações condicionadas a PD-01 não são implementações autorizadas nem mudanças aprovadas de API.


## health

### `medical_profiles`

Mapping: [MedicalProfileEntity.java](../../../src/main/java/com/jeepclub/backend/health/infra/persistence/entity/MedicalProfileEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | health | `medical_profiles` | PD indireto em contexto de saúde | Alto; contexto de saúde | Sim; PK/lookup | Sim; PK | Claro; owner/PK/auditoria, aceitar inferência residual |
| `ownerType` / `owner_type` | health | `medical_profiles` | PD indireto em contexto de saúde | Alto; contexto de saúde | Leitura por recurso; avaliar sort genérico | Sim; par ownerType/ownerId | Claro; owner/PK/auditoria, aceitar inferência residual |
| `ownerId` / `owner_id` | health | `medical_profiles` | PD indireto em contexto de saúde | Alto; contexto de saúde | Sim; relacionamento/lookup | Sim; par ownerType/ownerId | Claro; owner/PK/auditoria, aceitar inferência residual |
| `bloodType` / `blood_type` | health | `medical_profiles` | PS saúde | Crítico; condição clínica | Sem filtro clínico dedicado | Não | AEAD; atual e histórico, sem blind index |
| `allergies` / `allergies` | health | `medical_profiles` | PS saúde | Crítico; condição clínica | Sem filtro clínico dedicado | Não | AEAD; atual e histórico, sem blind index |
| `chronicConditions` / `chronic_conditions` | health | `medical_profiles` | PS saúde | Crítico; condição clínica | Sem filtro clínico dedicado | Não | AEAD; atual e histórico, sem blind index |
| `continuousMedications` / `continuous_medications` | health | `medical_profiles` | PS saúde | Crítico; condição clínica | Sem filtro clínico dedicado | Não | AEAD; atual e histórico, sem blind index |
| `healthInsuranceProvider` / `health_insurance_provider` | health | `medical_profiles` | PD em contexto de saúde | Alto; contexto de saúde | Sem filtro dedicado | Não | AEAD; proteção conservadora do contexto clínico |
| `healthInsurancePlan` / `health_insurance_plan` | health | `medical_profiles` | PD em contexto de saúde | Alto; contexto de saúde | Sem filtro dedicado | Não | AEAD; proteção conservadora do contexto clínico |
| `healthInsuranceNumber` / `health_insurance_number` | health | `medical_profiles` | PD em contexto de saúde | Alto; contexto de saúde | Sem filtro dedicado | Não | AEAD; proteção conservadora do contexto clínico |
| `emergencyContactName` / `emergency_contact_name` | health | `medical_profiles` | PD de terceiro (contato) | Alto; contexto de saúde | Sem filtro dedicado | Não | AEAD; minimização/finalidade e histórico |
| `emergencyContactPhone` / `emergency_contact_phone` | health | `medical_profiles` | PD de terceiro (contato) | Alto; contexto de saúde | Sem filtro dedicado | Não | AEAD; minimização/finalidade e histórico |
| `emergencyContactRelationship` / `emergency_contact_relationship` | health | `medical_profiles` | PD de terceiro (contato) | Alto; contexto de saúde | Sem filtro dedicado | Não | AEAD; minimização/finalidade e histórico |
| `observations` / `observations` | health | `medical_profiles` | PS saúde | Crítico; condição clínica | Sem filtro clínico dedicado | Não | AEAD; atual e histórico, sem blind index |
| `createdAt` / `created_at` | health | `medical_profiles` | PD indireto em contexto de saúde | Alto; contexto de saúde | Cronologia/TTL/sort conforme fluxo | Não | Claro; owner/PK/auditoria, aceitar inferência residual |
| `updatedAt` / `updated_at` | health | `medical_profiles` | PD indireto em contexto de saúde | Alto; contexto de saúde | Cronologia/TTL/sort conforme fluxo | Não | Claro; owner/PK/auditoria, aceitar inferência residual |
### `medical_profiles_history`

Mapping: [MedicalProfileHistoryEntity.java](../../../src/main/java/com/jeepclub/backend/health/infra/persistence/entity/MedicalProfileHistoryEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | health | `medical_profiles_history` | PD indireto em contexto de saúde | Alto; contexto de saúde | Sim; PK/lookup | Sim; PK | Claro; owner/PK/auditoria, aceitar inferência residual |
| `medicalProfileId` / `medical_profile_id` | health | `medical_profiles_history` | PD indireto em contexto de saúde | Alto; contexto de saúde | Sim; relacionamento/lookup | Sim; um snapshot por original | Claro; owner/PK/auditoria, aceitar inferência residual |
| `ownerType` / `owner_type` | health | `medical_profiles_history` | PD indireto em contexto de saúde | Alto; contexto de saúde | Leitura por recurso; avaliar sort genérico | Não | Claro; owner/PK/auditoria, aceitar inferência residual |
| `ownerId` / `owner_id` | health | `medical_profiles_history` | PD indireto em contexto de saúde | Alto; contexto de saúde | Sim; relacionamento/lookup | Não | Claro; owner/PK/auditoria, aceitar inferência residual |
| `bloodType` / `blood_type` | health | `medical_profiles_history` | PS saúde | Crítico; condição clínica | Sem filtro clínico dedicado | Não | AEAD; atual e histórico, sem blind index |
| `allergies` / `allergies` | health | `medical_profiles_history` | PS saúde | Crítico; condição clínica | Sem filtro clínico dedicado | Não | AEAD; atual e histórico, sem blind index |
| `chronicConditions` / `chronic_conditions` | health | `medical_profiles_history` | PS saúde | Crítico; condição clínica | Sem filtro clínico dedicado | Não | AEAD; atual e histórico, sem blind index |
| `continuousMedications` / `continuous_medications` | health | `medical_profiles_history` | PS saúde | Crítico; condição clínica | Sem filtro clínico dedicado | Não | AEAD; atual e histórico, sem blind index |
| `healthInsuranceProvider` / `health_insurance_provider` | health | `medical_profiles_history` | PD em contexto de saúde | Alto; contexto de saúde | Sem filtro dedicado | Não | AEAD; proteção conservadora do contexto clínico |
| `healthInsurancePlan` / `health_insurance_plan` | health | `medical_profiles_history` | PD em contexto de saúde | Alto; contexto de saúde | Sem filtro dedicado | Não | AEAD; proteção conservadora do contexto clínico |
| `healthInsuranceNumber` / `health_insurance_number` | health | `medical_profiles_history` | PD em contexto de saúde | Alto; contexto de saúde | Sem filtro dedicado | Não | AEAD; proteção conservadora do contexto clínico |
| `emergencyContactName` / `emergency_contact_name` | health | `medical_profiles_history` | PD de terceiro (contato) | Alto; contexto de saúde | Sem filtro dedicado | Não | AEAD; minimização/finalidade e histórico |
| `emergencyContactPhone` / `emergency_contact_phone` | health | `medical_profiles_history` | PD de terceiro (contato) | Alto; contexto de saúde | Sem filtro dedicado | Não | AEAD; minimização/finalidade e histórico |
| `emergencyContactRelationship` / `emergency_contact_relationship` | health | `medical_profiles_history` | PD de terceiro (contato) | Alto; contexto de saúde | Sem filtro dedicado | Não | AEAD; minimização/finalidade e histórico |
| `observations` / `observations` | health | `medical_profiles_history` | PS saúde | Crítico; condição clínica | Sem filtro clínico dedicado | Não | AEAD; atual e histórico, sem blind index |
| `deletedByUserId` / `deleted_by_user_id` | health | `medical_profiles_history` | PD indireto em contexto de saúde | Alto; contexto de saúde | Sim; relacionamento/lookup | Não | Claro; owner/PK/auditoria, aceitar inferência residual |
| `createdAt` / `created_at` | health | `medical_profiles_history` | PD indireto em contexto de saúde | Alto; contexto de saúde | Cronologia/TTL/sort conforme fluxo | Não | Claro; owner/PK/auditoria, aceitar inferência residual |
| `updatedAt` / `updated_at` | health | `medical_profiles_history` | PD indireto em contexto de saúde | Alto; contexto de saúde | Cronologia/TTL/sort conforme fluxo | Não | Claro; owner/PK/auditoria, aceitar inferência residual |
| `deletedAt` / `deleted_at` | health | `medical_profiles_history` | PD indireto em contexto de saúde | Alto; contexto de saúde | Cronologia/TTL/sort conforme fluxo | Não | Claro; owner/PK/auditoria, aceitar inferência residual |

## iam/identity

### `identity_users`

Mapping: [UserEntity.java](../../../src/main/java/com/jeepclub/backend/iam/identity/infra/persistence/entity/UserEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | iam/identity | `identity_users` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `name` / `name` | iam/identity | `identity_users` | PD identificação | Alto; reidentificação | LIKE/q e sort | Não | AEAD proposto; contrato de busca/sort PD-01 |
| `birthDate` / `birth_date` | iam/identity | `identity_users` | PD cadastro | Alto; fraude/idade | Igualdade e sort | Não | AEAD + HMAC igualdade; sort depende PD-01 |
| `email` / `email` | iam/identity | `identity_users` | PD identificador | Alto; fraude/contato | Igualdade, LIKE/q e sort | Sim; nullable | AEAD + HMAC igualdade; bloqueio LIKE/q/sort PD-01 |
| `cpf` / `cpf` | iam/identity | `identity_users` | PD identificador | Alto; fraude/contato | Igualdade, q parcial e sort | Sim; global na tabela | AEAD + HMAC igualdade; bloqueio LIKE/q/sort PD-01 |
| `rg` / `rg` | iam/identity | `identity_users` | PD identificador | Alto; fraude/contato | Igualdade, q parcial e sort | Sim; nullable | AEAD + HMAC igualdade; bloqueio LIKE/q/sort PD-01 |
| `phoneNumber` / `phone_number` | iam/identity | `identity_users` | PD contato | Alto; fraude/contato | LIKE/q e sort | Não | AEAD; HMAC só se igualdade aprovada, PD-01 |
| `profilePhotoStorageKey` / `profile_photo_url` | iam/identity | `identity_users` | PD referência de imagem | Alto; imagem/correlação | Leitura e sort permitido | Não | Chave clara; bytes em storage protegido, PD-09 |
| `status` / `status` | iam/identity | `identity_users` | PD indireto | Médio; vínculo/atividade | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `createdAt` / `created_at` | iam/identity | `identity_users` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `disabledAt` / `disabled_at` | iam/identity | `identity_users` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | iam/identity | `identity_users` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |

## dependents

### `dependents_dependent`

Mapping: [DependentEntity.java](../../../src/main/java/com/jeepclub/backend/dependents/infra/persistence/entity/DependentEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | dependents | `dependents_dependent` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `name` / `name` | dependents | `dependents_dependent` | PD cadastro/contato | Alto; família/possíveis menores | Sem filtro dedicado | Não | AEAD; atual e histórico |
| `cpf` / `cpf` | dependents | `dependents_dependent` | PD identificador | Alto; fraude, possíveis menores | Igualdade para unicidade | Sim; global na tabela | AEAD + HMAC igualdade |
| `birthDate` / `birth_date` | dependents | `dependents_dependent` | PD cadastro/contato | Alto; família/possíveis menores | Sem filtro dedicado | Não | AEAD; atual e histórico |
| `relationshipType` / `relationship_type` | dependents | `dependents_dependent` | PD vínculo familiar | Alto; família/possíveis menores | Sem filtro dedicado | Não | AEAD; atual e histórico |
| `phoneNumber` / `phone_number` | dependents | `dependents_dependent` | PD cadastro/contato | Alto; família/possíveis menores | Sem filtro dedicado | Não | AEAD; atual e histórico |
| `userId` / `user_id` | dependents | `dependents_dependent` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `status` / `status` | dependents | `dependents_dependent` | PD indireto | Médio; vínculo/atividade | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `createdAt` / `created_at` | dependents | `dependents_dependent` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | dependents | `dependents_dependent` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
### `dependents_dependent_history`

Mapping: [DependentHistoryEntity.java](../../../src/main/java/com/jeepclub/backend/dependents/infra/persistence/entity/DependentHistoryEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | dependents | `dependents_dependent_history` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `dependentId` / `dependent_id` | dependents | `dependents_dependent_history` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Sim; um snapshot por original | Claro; joins/ownership, risco de correlação |
| `name` / `name` | dependents | `dependents_dependent_history` | PD cadastro/contato | Alto; família/possíveis menores | Sem filtro dedicado | Não | AEAD; atual e histórico |
| `cpf` / `cpf` | dependents | `dependents_dependent_history` | PD identificador | Alto; fraude, possíveis menores | Index físico CPF; sem lookup dedicado encontrado | Não | AEAD; revisar/remover índice CPF ou HMAC se justificado |
| `birthDate` / `birth_date` | dependents | `dependents_dependent_history` | PD cadastro/contato | Alto; família/possíveis menores | Sem filtro dedicado | Não | AEAD; atual e histórico |
| `relationshipType` / `relationship_type` | dependents | `dependents_dependent_history` | PD vínculo familiar | Alto; família/possíveis menores | Sem filtro dedicado | Não | AEAD; atual e histórico |
| `phoneNumber` / `phone_number` | dependents | `dependents_dependent_history` | PD cadastro/contato | Alto; família/possíveis menores | Sem filtro dedicado | Não | AEAD; atual e histórico |
| `userId` / `user_id` | dependents | `dependents_dependent_history` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `status` / `status` | dependents | `dependents_dependent_history` | PD indireto | Médio; vínculo/atividade | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `deletedByUserId` / `deleted_by_user_id` | dependents | `dependents_dependent_history` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `createdAt` / `created_at` | dependents | `dependents_dependent_history` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | dependents | `dependents_dependent_history` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `deletedAt` / `deleted_at` | dependents | `dependents_dependent_history` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |

## memberships

### `membership_activation_tokens`

Mapping: [MemberActivationTokenEntity.java](../../../src/main/java/com/jeepclub/backend/memberships/infra/persistence/entity/MemberActivationTokenEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | memberships | `membership_activation_tokens` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `applicationId` / `application_id` | memberships | `membership_activation_tokens` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `tokenHash` / `token_hash` | memberships | `membership_activation_tokens` | PD indireto + segredo derivado | Alto; ativação | Igualdade/unique token | Sim | Hash não reversível existente; token CSPRNG 64 bytes |
| `expiresAt` / `expires_at` | memberships | `membership_activation_tokens` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `usedAt` / `used_at` | memberships | `membership_activation_tokens` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `createdAt` / `created_at` | memberships | `membership_activation_tokens` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
### `membership_applicant_blocks`

Mapping: [MembershipApplicantBlockEntity.java](../../../src/main/java/com/jeepclub/backend/memberships/infra/persistence/entity/MembershipApplicantBlockEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | memberships | `membership_applicant_blocks` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `cpf` / `cpf` | memberships | `membership_applicant_blocks` | PD identificador | Alto; fraude/processo admissão | Igualdade/status | Não | AEAD + HMAC igualdade |
| `activeCpf` / `active_cpf` | memberships | `membership_applicant_blocks` | PD identificador derivado | Alto; bloqueio/correlação | Igualdade/unique ativo | Sim; nullable, só bloqueio ativo | HMAC somente; null no desbloqueio, cpf reversível separado |
| `reason` / `reason` | memberships | `membership_applicant_blocks` | PD; PS possível no texto | Alto; conteúdo livre/decisão | Sem filtro dedicado | Não | AEAD + minimizar conteúdo não necessário |
| `blockedAt` / `blocked_at` | memberships | `membership_applicant_blocks` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `blockedByUserId` / `blocked_by_user_id` | memberships | `membership_applicant_blocks` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `unblockedAt` / `unblocked_at` | memberships | `membership_applicant_blocks` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `unblockedByUserId` / `unblocked_by_user_id` | memberships | `membership_applicant_blocks` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
### `membership_applications`

Mapping: [MembershipApplicationEntity.java](../../../src/main/java/com/jeepclub/backend/memberships/infra/persistence/entity/MembershipApplicationEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | memberships | `membership_applications` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `name` / `name` | memberships | `membership_applications` | PD cadastro/contato | Alto | Sem filtro dedicado; revisar Pageable | Não | AEAD |
| `cpf` / `cpf` | memberships | `membership_applications` | PD identificador | Alto; fraude/processo admissão | Igualdade/status | Não | AEAD + HMAC igualdade |
| `email` / `email` | memberships | `membership_applications` | PD contato | Alto; contato/deduplicação | Igualdade/existência/status | Não | AEAD + HMAC; unicidade de negócio, não constraint global |
| `phoneNumber` / `phone_number` | memberships | `membership_applications` | PD cadastro/contato | Alto | Sem filtro dedicado; revisar Pageable | Não | AEAD |
| `message` / `message` | memberships | `membership_applications` | PD; PS possível no texto | Alto; conteúdo livre/decisão | Sem filtro dedicado | Não | AEAD + minimizar conteúdo não necessário |
| `status` / `status` | memberships | `membership_applications` | PD indireto | Médio; vínculo/atividade | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `rejectionReason` / `rejection_reason` | memberships | `membership_applications` | PD; PS possível no texto | Alto; conteúdo livre/decisão | Sem filtro dedicado | Não | AEAD + minimizar conteúdo não necessário |
| `reviewedByUserId` / `reviewed_by_user_id` | memberships | `membership_applications` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `createdUserId` / `created_user_id` | memberships | `membership_applications` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `requestedAt` / `requested_at` | memberships | `membership_applications` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `reviewedAt` / `reviewed_at` | memberships | `membership_applications` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `finishedAt` / `finished_at` | memberships | `membership_applications` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | memberships | `membership_applications` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `version` / `version` | memberships | `membership_applications` | NP técnico | Baixo | Sim; controle concorrente | Não | Claro; acesso mínimo e retenção |
### `membership_billing_configurations`

Mapping: [MembershipBillingConfigurationEntity.java](../../../src/main/java/com/jeepclub/backend/memberships/infra/persistence/entity/MembershipBillingConfigurationEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | memberships | `membership_billing_configurations` | NP (configuração) | Baixo | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `singletonKey` / `singleton_key` | memberships | `membership_billing_configurations` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Sim; configuração única | Claro; acesso mínimo e retenção |
| `chargeDefinitionId` / `charge_definition_id` | memberships | `membership_billing_configurations` | NP (configuração) | Baixo | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `enforcementEnabled` / `enforcement_enabled` | memberships | `membership_billing_configurations` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `createdAt` / `created_at` | memberships | `membership_billing_configurations` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | memberships | `membership_billing_configurations` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |

## vehicles

### `vehicles_vehicle`

Mapping: [VehicleEntity.java](../../../src/main/java/com/jeepclub/backend/vehicles/infra/persistence/entity/VehicleEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | vehicles | `vehicles_vehicle` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `nickname` / `nickname` | vehicles | `vehicles_vehicle` | PD possível em apelido | Médio/alto | Sem filtro dedicado | Não | AEAD proposto; limitar texto identificável |
| `photo` / `photo` | vehicles | `vehicles_vehicle` | PD possível; referência de imagem | Médio/alto; bytes/correlação | Leitura de referência | Não | Chave clara; bytes em storage protegido, PD-09 |
| `plate` / `plate` | vehicles | `vehicles_vehicle` | PD identificador de bem | Alto; fraude/correlação | Igualdade para unicidade | Sim; global na tabela | AEAD + HMAC igualdade |
| `renavam` / `renavam` | vehicles | `vehicles_vehicle` | PD identificador de bem | Alto; fraude/correlação | Igualdade para unicidade | Sim; global na tabela | AEAD + HMAC igualdade |
| `brand` / `brand` | vehicles | `vehicles_vehicle` | PD indireto; característica do bem | Médio; perfil patrimonial | Leitura; sem filtro dedicado | Não | Claro; necessário ao domínio, acesso/retention |
| `model` / `model` | vehicles | `vehicles_vehicle` | PD indireto; característica do bem | Médio; perfil patrimonial | Leitura; sem filtro dedicado | Não | Claro; necessário ao domínio, acesso/retention |
| `manufacturingYear` / `manufacturing_year` | vehicles | `vehicles_vehicle` | PD indireto; característica do bem | Médio; perfil patrimonial | Leitura; sem filtro dedicado | Não | Claro; necessário ao domínio, acesso/retention |
| `modelYear` / `model_year` | vehicles | `vehicles_vehicle` | PD indireto; característica do bem | Médio; perfil patrimonial | Leitura; sem filtro dedicado | Não | Claro; necessário ao domínio, acesso/retention |
| `color` / `color` | vehicles | `vehicles_vehicle` | PD indireto; característica do bem | Médio; perfil patrimonial | Leitura; sem filtro dedicado | Não | Claro; necessário ao domínio, acesso/retention |
| `seatingCapacity` / `seating_capacity` | vehicles | `vehicles_vehicle` | PD indireto; característica do bem | Médio; perfil patrimonial | Leitura; sem filtro dedicado | Não | Claro; necessário ao domínio, acesso/retention |
| `fuelType` / `fuel_type` | vehicles | `vehicles_vehicle` | PD indireto; característica do bem | Médio; perfil patrimonial | Leitura; sem filtro dedicado | Não | Claro; necessário ao domínio, acesso/retention |
| `engineDisplacement` / `engine_displacement` | vehicles | `vehicles_vehicle` | PD indireto; característica do bem | Médio; perfil patrimonial | Leitura; sem filtro dedicado | Não | Claro; necessário ao domínio, acesso/retention |
| `status` / `status` | vehicles | `vehicles_vehicle` | PD indireto | Médio; vínculo/atividade | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `towing` / `towing` | vehicles | `vehicles_vehicle` | PD indireto; característica do bem | Médio; perfil patrimonial | Leitura; sem filtro dedicado | Não | Claro; necessário ao domínio, acesso/retention |
| `ownerId` / `owner_id` | vehicles | `vehicles_vehicle` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `createdAt` / `created_at` | vehicles | `vehicles_vehicle` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | vehicles | `vehicles_vehicle` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
### `vehicles_vehicle_history`

Mapping: [VehicleHistoryEntity.java](../../../src/main/java/com/jeepclub/backend/vehicles/infra/persistence/entity/VehicleHistoryEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | vehicles | `vehicles_vehicle_history` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `vehicleId` / `vehicle_id` | vehicles | `vehicles_vehicle_history` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Sim; um snapshot por original | Claro; joins/ownership, risco de correlação |
| `nickname` / `nickname` | vehicles | `vehicles_vehicle_history` | PD possível em apelido | Médio/alto | Sem filtro dedicado | Não | AEAD proposto; limitar texto identificável |
| `photo` / `photo` | vehicles | `vehicles_vehicle_history` | PD possível; referência de imagem | Médio/alto; bytes/correlação | Leitura de referência | Não | Chave clara; bytes em storage protegido, PD-09 |
| `plate` / `plate` | vehicles | `vehicles_vehicle_history` | PD identificador de bem | Alto; fraude/correlação | Index físico; sem lookup dedicado encontrado | Não | AEAD; avaliar necessidade de índice derivado histórico |
| `renavam` / `renavam` | vehicles | `vehicles_vehicle_history` | PD identificador de bem | Alto; fraude/correlação | Index físico; sem lookup dedicado encontrado | Não | AEAD; avaliar necessidade de índice derivado histórico |
| `brand` / `brand` | vehicles | `vehicles_vehicle_history` | PD indireto; característica do bem | Médio; perfil patrimonial | Leitura; sem filtro dedicado | Não | Claro; necessário ao domínio, acesso/retention |
| `model` / `model` | vehicles | `vehicles_vehicle_history` | PD indireto; característica do bem | Médio; perfil patrimonial | Leitura; sem filtro dedicado | Não | Claro; necessário ao domínio, acesso/retention |
| `manufacturingYear` / `manufacturing_year` | vehicles | `vehicles_vehicle_history` | PD indireto; característica do bem | Médio; perfil patrimonial | Leitura; sem filtro dedicado | Não | Claro; necessário ao domínio, acesso/retention |
| `modelYear` / `model_year` | vehicles | `vehicles_vehicle_history` | PD indireto; característica do bem | Médio; perfil patrimonial | Leitura; sem filtro dedicado | Não | Claro; necessário ao domínio, acesso/retention |
| `color` / `color` | vehicles | `vehicles_vehicle_history` | PD indireto; característica do bem | Médio; perfil patrimonial | Leitura; sem filtro dedicado | Não | Claro; necessário ao domínio, acesso/retention |
| `seatingCapacity` / `seating_capacity` | vehicles | `vehicles_vehicle_history` | PD indireto; característica do bem | Médio; perfil patrimonial | Leitura; sem filtro dedicado | Não | Claro; necessário ao domínio, acesso/retention |
| `fuelType` / `fuel_type` | vehicles | `vehicles_vehicle_history` | PD indireto; característica do bem | Médio; perfil patrimonial | Leitura; sem filtro dedicado | Não | Claro; necessário ao domínio, acesso/retention |
| `engineDisplacement` / `engine_displacement` | vehicles | `vehicles_vehicle_history` | PD indireto; característica do bem | Médio; perfil patrimonial | Leitura; sem filtro dedicado | Não | Claro; necessário ao domínio, acesso/retention |
| `status` / `status` | vehicles | `vehicles_vehicle_history` | PD indireto | Médio; vínculo/atividade | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `towing` / `towing` | vehicles | `vehicles_vehicle_history` | PD indireto; característica do bem | Médio; perfil patrimonial | Leitura; sem filtro dedicado | Não | Claro; necessário ao domínio, acesso/retention |
| `ownerId` / `owner_id` | vehicles | `vehicles_vehicle_history` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `deletedByUserId` / `deleted_by_user_id` | vehicles | `vehicles_vehicle_history` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `createdAt` / `created_at` | vehicles | `vehicles_vehicle_history` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | vehicles | `vehicles_vehicle_history` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `deletedAt` / `deleted_at` | vehicles | `vehicles_vehicle_history` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |

## tools

### `tools`

Mapping: [ToolEntity.java](../../../src/main/java/com/jeepclub/backend/tools/infra/persistence/entity/ToolEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | tools | `tools` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `name` / `name` | tools | `tools` | PD possível; texto de bem | Médio; conteúdo livre | Leitura; sem filtro dedicado | Não | Claro para catálogo; minimizar PII; investigar se texto privado |
| `description` / `description` | tools | `tools` | PD possível; texto de bem | Médio; conteúdo livre | Leitura; sem filtro dedicado | Não | Claro para catálogo; minimizar PII; investigar se texto privado |
| `status` / `status` | tools | `tools` | PD indireto | Médio; vínculo/atividade | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `userId` / `user_id` | tools | `tools` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `photoStorageKey` / `photo_storage_key` | tools | `tools` | PD possível; referência de imagem | Médio/alto; bytes/correlação | Leitura de referência | Não | Chave clara; bytes em storage protegido, PD-09 |
| `createdAt` / `created_at` | tools | `tools` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | tools | `tools` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
### `tools_tool_history`

Mapping: [ToolHistoryEntity.java](../../../src/main/java/com/jeepclub/backend/tools/infra/persistence/entity/ToolHistoryEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | tools | `tools_tool_history` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `toolId` / `tool_id` | tools | `tools_tool_history` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Sim; um snapshot por original | Claro; joins/ownership, risco de correlação |
| `name` / `name` | tools | `tools_tool_history` | PD possível; texto de bem | Médio; conteúdo livre | Leitura; sem filtro dedicado | Não | Claro para catálogo; minimizar PII; investigar se texto privado |
| `description` / `description` | tools | `tools_tool_history` | PD possível; texto de bem | Médio; conteúdo livre | Leitura; sem filtro dedicado | Não | Claro para catálogo; minimizar PII; investigar se texto privado |
| `status` / `status` | tools | `tools_tool_history` | PD indireto | Médio; vínculo/atividade | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `userId` / `user_id` | tools | `tools_tool_history` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `photoStorageKey` / `photo_storage_key` | tools | `tools_tool_history` | PD possível; referência de imagem | Médio/alto; bytes/correlação | Leitura de referência | Não | Chave clara; bytes em storage protegido, PD-09 |
| `deletedByUserId` / `deleted_by_user_id` | tools | `tools_tool_history` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `createdAt` / `created_at` | tools | `tools_tool_history` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | tools | `tools_tool_history` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `deletedAt` / `deleted_at` | tools | `tools_tool_history` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |

## billing

### `billing_all_members_charge_assignments`

Mapping: [AllMembersChargeAssignmentEntity.java](../../../src/main/java/com/jeepclub/backend/billing/infra/persistence/entity/assignment/AllMembersChargeAssignmentEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| PK herdada | billing | `billing_all_members_charge_assignments` | NP configuração | Médio por vínculo | Sim; join de herança | Sim; PK | Claro; valores da superclasse nas tabelas base |
### `billing_charge_assignments`

Mapping: [ChargeAssignmentEntity.java](../../../src/main/java/com/jeepclub/backend/billing/infra/persistence/entity/assignment/ChargeAssignmentEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | billing | `billing_charge_assignments` | NP (configuração) | Baixo | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `chargeDefinitionId` / `charge_definition_id` | billing | `billing_charge_assignments` | NP (configuração) | Baixo | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `active` / `active` | billing | `billing_charge_assignments` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `createdAt` / `created_at` | billing | `billing_charge_assignments` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | billing | `billing_charge_assignments` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
### `billing_charge_cycles`

Mapping: [ChargeCycleEntity.java](../../../src/main/java/com/jeepclub/backend/billing/infra/persistence/entity/ChargeCycleEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | billing | `billing_charge_cycles` | NP (configuração) | Baixo | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `chargeDefinitionId` / `charge_definition_id` | billing | `billing_charge_cycles` | NP (configuração) | Baixo | Sim; relacionamento/lookup | Sim; par definição/code | Claro; joins/ownership, risco de correlação |
| `chargeDefinitionNameSnapshot` / `charge_definition_name_snapshot` | billing | `billing_charge_cycles` | NP catálogo; PD possível se uso indevido | Médio se PII inserida | Leitura por recurso; avaliar sort genérico | Não | Claro; minimizar/proibir dado pessoal fora da finalidade |
| `chargeDefinitionDescriptionSnapshot` / `charge_definition_description_snapshot` | billing | `billing_charge_cycles` | NP catálogo; PD possível se uso indevido | Médio se PII inserida | Leitura por recurso; avaliar sort genérico | Não | Claro; minimizar/proibir dado pessoal fora da finalidade |
| `chargeDefinitionDefaultAmountSnapshot` / `charge_definition_default_amount_snapshot` | billing | `billing_charge_cycles` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `chargeDefinitionRecurrenceTypeSnapshot` / `charge_definition_recurrence_type_snapshot` | billing | `billing_charge_cycles` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `chargeDefinitionRequiredSnapshot` / `charge_definition_required_snapshot` | billing | `billing_charge_cycles` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `chargeDefinitionPaymentAcceptancePolicySnapshot` / `charge_definition_payment_acceptance_policy_snapshot` | billing | `billing_charge_cycles` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `chargeDefinitionLatePaymentGraceDaysSnapshot` / `charge_definition_late_payment_grace_days_snapshot` | billing | `billing_charge_cycles` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `code` / `code` | billing | `billing_charge_cycles` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Sim; par definição/code | Claro; acesso mínimo e retenção |
| `dueDate` / `due_date` | billing | `billing_charge_cycles` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `status` / `status` | billing | `billing_charge_cycles` | NP (configuração) | Baixo | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `generatedByUserId` / `generated_by_user_id` | billing | `billing_charge_cycles` | PD indireto; ator | Médio; audit trail | Sim; relacionamento/lookup | Não | Claro; accountability e retenção |
| `generatedAt` / `generated_at` | billing | `billing_charge_cycles` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `canceledAt` / `canceled_at` | billing | `billing_charge_cycles` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `canceledByUserId` / `canceled_by_user_id` | billing | `billing_charge_cycles` | PD indireto; ator | Médio; audit trail | Sim; relacionamento/lookup | Não | Claro; accountability e retenção |
| `finishedAt` / `finished_at` | billing | `billing_charge_cycles` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `finishedByUserId` / `finished_by_user_id` | billing | `billing_charge_cycles` | PD indireto; ator | Médio; audit trail | Sim; relacionamento/lookup | Não | Claro; accountability e retenção |
| `archivedAt` / `archived_at` | billing | `billing_charge_cycles` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `archivedByUserId` / `archived_by_user_id` | billing | `billing_charge_cycles` | PD indireto; ator | Médio; audit trail | Sim; relacionamento/lookup | Não | Claro; accountability e retenção |
| `createdAt` / `created_at` | billing | `billing_charge_cycles` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | billing | `billing_charge_cycles` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
### `billing_charge_definitions`

Mapping: [ChargeDefinitionEntity.java](../../../src/main/java/com/jeepclub/backend/billing/infra/persistence/entity/ChargeDefinitionEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | billing | `billing_charge_definitions` | NP (configuração) | Baixo | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `name` / `name` | billing | `billing_charge_definitions` | NP catálogo; PD possível se uso indevido | Médio se PII inserida | Leitura por recurso; avaliar sort genérico | Sim | Claro; minimizar/proibir dado pessoal fora da finalidade |
| `description` / `description` | billing | `billing_charge_definitions` | NP catálogo; PD possível se uso indevido | Médio se PII inserida | Leitura por recurso; avaliar sort genérico | Não | Claro; minimizar/proibir dado pessoal fora da finalidade |
| `defaultAmount` / `default_amount` | billing | `billing_charge_definitions` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `recurrenceType` / `recurrence_type` | billing | `billing_charge_definitions` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `required` / `required` | billing | `billing_charge_definitions` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `paymentAcceptancePolicy` / `payment_acceptance_policy` | billing | `billing_charge_definitions` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `latePaymentGraceDays` / `late_payment_grace_days` | billing | `billing_charge_definitions` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `status` / `status` | billing | `billing_charge_definitions` | NP (configuração) | Baixo | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `createdAt` / `created_at` | billing | `billing_charge_definitions` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | billing | `billing_charge_definitions` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `archivedAt` / `archived_at` | billing | `billing_charge_definitions` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
### `billing_event_charge_contexts`

Mapping: [EventChargeContextEntity.java](../../../src/main/java/com/jeepclub/backend/billing/infra/persistence/entity/EventChargeContextEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | billing | `billing_event_charge_contexts` | NP (configuração) | Baixo | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `eventId` / `event_id` | billing | `billing_event_charge_contexts` | NP (configuração) | Baixo | Sim; relacionamento/lookup | Sim; par event/definition | Claro; joins/ownership, risco de correlação |
| `chargeDefinitionId` / `charge_definition_id` | billing | `billing_event_charge_contexts` | NP (configuração) | Baixo | Sim; relacionamento/lookup | Sim; par event/definition | Claro; joins/ownership, risco de correlação |
| `assignmentId` / `assignment_id` | billing | `billing_event_charge_contexts` | NP (configuração) | Baixo | Sim; relacionamento/lookup | Sim | Claro; joins/ownership, risco de correlação |
| `cycleId` / `cycle_id` | billing | `billing_event_charge_contexts` | NP (configuração) | Baixo | Sim; relacionamento/lookup | Sim | Claro; joins/ownership, risco de correlação |
### `billing_event_participants_charge_assignments`

Mapping: [EventParticipantsChargeAssignmentEntity.java](../../../src/main/java/com/jeepclub/backend/billing/infra/persistence/entity/assignment/EventParticipantsChargeAssignmentEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `eventId` / `event_id` | billing | `billing_event_participants_charge_assignments` | NP (configuração) | Baixo | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
### `billing_member_charges`

Mapping: [MemberChargeEntity.java](../../../src/main/java/com/jeepclub/backend/billing/infra/persistence/entity/MemberChargeEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | billing | `billing_member_charges` | PD financeiro/vínculo | Alto; situação financeira | Sim; PK/lookup | Sim; PK | Claro; cálculos/estados/joins necessários e acesso restrito |
| `userId` / `user_id` | billing | `billing_member_charges` | PD financeiro/vínculo | Alto; situação financeira | Sim; relacionamento/lookup | Sim; par user/cycle | Claro; cálculos/estados/joins necessários e acesso restrito |
| `chargeDefinitionId` / `charge_definition_id` | billing | `billing_member_charges` | PD financeiro/vínculo | Alto; situação financeira | Sim; relacionamento/lookup | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `chargeCycleId` / `charge_cycle_id` | billing | `billing_member_charges` | PD financeiro/vínculo | Alto; situação financeira | Sim; relacionamento/lookup | Sim; par user/cycle | Claro; cálculos/estados/joins necessários e acesso restrito |
| `originalAmount` / `original_amount` | billing | `billing_member_charges` | PD financeiro/vínculo | Alto; situação financeira | Cálculo/filtro/cronologia; sem índice criptográfico | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `finalAmount` / `final_amount` | billing | `billing_member_charges` | PD financeiro/vínculo | Alto; situação financeira | Cálculo/filtro/cronologia; sem índice criptográfico | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `dueDate` / `due_date` | billing | `billing_member_charges` | PD financeiro/vínculo | Alto; situação financeira | Cálculo/filtro/cronologia; sem índice criptográfico | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `paymentAcceptancePolicy` / `payment_acceptance_policy` | billing | `billing_member_charges` | PD financeiro/vínculo | Alto; situação financeira | Leitura por recurso; avaliar sort genérico | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `latePaymentGraceDays` / `late_payment_grace_days` | billing | `billing_member_charges` | PD financeiro/vínculo | Alto; situação financeira | Leitura por recurso; avaliar sort genérico | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `paymentAllowedUntil` / `payment_allowed_until` | billing | `billing_member_charges` | PD financeiro/vínculo | Alto; situação financeira | Leitura por recurso; avaliar sort genérico | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `status` / `status` | billing | `billing_member_charges` | PD financeiro/vínculo | Alto; situação financeira | Sim; estado/decisão operacional | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `createdAt` / `created_at` | billing | `billing_member_charges` | PD financeiro/vínculo | Alto; situação financeira | Cronologia/TTL/sort conforme fluxo | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `updatedAt` / `updated_at` | billing | `billing_member_charges` | PD financeiro/vínculo | Alto; situação financeira | Cronologia/TTL/sort conforme fluxo | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `paidAt` / `paid_at` | billing | `billing_member_charges` | PD financeiro/vínculo | Alto; situação financeira | Cálculo/filtro/cronologia; sem índice criptográfico | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `canceledAt` / `canceled_at` | billing | `billing_member_charges` | PD financeiro/vínculo | Alto; situação financeira | Cronologia/TTL/sort conforme fluxo | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
### `billing_member_payments`

Mapping: [MemberPaymentEntity.java](../../../src/main/java/com/jeepclub/backend/billing/infra/persistence/entity/MemberPaymentEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | billing | `billing_member_payments` | PD financeiro/vínculo | Alto; situação financeira | Sim; PK/lookup | Sim; PK | Claro; cálculos/estados/joins necessários e acesso restrito |
| `memberChargeId` / `member_charge_id` | billing | `billing_member_payments` | PD financeiro/vínculo | Alto; situação financeira | Sim; relacionamento/lookup | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `amount` / `amount` | billing | `billing_member_payments` | PD financeiro/vínculo | Alto; situação financeira | Cálculo/filtro/cronologia; sem índice criptográfico | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `paymentMethod` / `payment_method` | billing | `billing_member_payments` | PD financeiro/vínculo | Alto; situação financeira | Cálculo/filtro/cronologia; sem índice criptográfico | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `status` / `status` | billing | `billing_member_payments` | PD financeiro/vínculo | Alto; situação financeira | Sim; estado/decisão operacional | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `paidAt` / `paid_at` | billing | `billing_member_payments` | PD financeiro/vínculo | Alto; situação financeira | Cálculo/filtro/cronologia; sem índice criptográfico | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `receiptStorageKey` / `receipt_storage_key` | billing | `billing_member_payments` | PD referência documental | Alto; documento financeiro | Leitura por paymentId | Não | Chave clara; cifrar bytes privados + ownership, PD-09 |
| `confirmedAt` / `confirmed_at` | billing | `billing_member_payments` | PD financeiro/vínculo | Alto; situação financeira | Cronologia/TTL/sort conforme fluxo | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `confirmedByUserId` / `confirmed_by_user_id` | billing | `billing_member_payments` | PD indireto; ator | Médio; audit trail | Sim; relacionamento/lookup | Não | Claro; accountability e retenção |
| `rejectedAt` / `rejected_at` | billing | `billing_member_payments` | PD financeiro/vínculo | Alto; situação financeira | Cronologia/TTL/sort conforme fluxo | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `rejectedByUserId` / `rejected_by_user_id` | billing | `billing_member_payments` | PD indireto; ator | Médio; audit trail | Sim; relacionamento/lookup | Não | Claro; accountability e retenção |
| `rejectionReason` / `rejection_reason` | billing | `billing_member_payments` | PD; PS possível no texto | Alto; situação financeira | Sem filtro dedicado | Não | AEAD + minimizar texto livre |
| `canceledAt` / `canceled_at` | billing | `billing_member_payments` | PD financeiro/vínculo | Alto; situação financeira | Cronologia/TTL/sort conforme fluxo | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `notes` / `notes` | billing | `billing_member_payments` | PD; PS possível no texto | Alto; situação financeira | Sem filtro dedicado | Não | AEAD + minimizar texto livre |
| `createdAt` / `created_at` | billing | `billing_member_payments` | PD financeiro/vínculo | Alto; situação financeira | Cronologia/TTL/sort conforme fluxo | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `updatedAt` / `updated_at` | billing | `billing_member_payments` | PD financeiro/vínculo | Alto; situação financeira | Cronologia/TTL/sort conforme fluxo | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `submittedAt` / `submitted_at` | billing | `billing_member_payments` | PD financeiro/vínculo | Alto; situação financeira | Cronologia/TTL/sort conforme fluxo | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
### `billing_member_refunds`

Mapping: [MemberRefundEntity.java](../../../src/main/java/com/jeepclub/backend/billing/infra/persistence/entity/MemberRefundEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | billing | `billing_member_refunds` | PD financeiro/vínculo | Alto; situação financeira | Sim; PK/lookup | Sim; PK | Claro; cálculos/estados/joins necessários e acesso restrito |
| `memberChargeId` / `member_charge_id` | billing | `billing_member_refunds` | PD financeiro/vínculo | Alto; situação financeira | Sim; relacionamento/lookup | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `memberPaymentId` / `member_payment_id` | billing | `billing_member_refunds` | PD financeiro/vínculo | Alto; situação financeira | Sim; relacionamento/lookup | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `chargeCycleId` / `charge_cycle_id` | billing | `billing_member_refunds` | PD financeiro/vínculo | Alto; situação financeira | Sim; relacionamento/lookup | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `userId` / `user_id` | billing | `billing_member_refunds` | PD financeiro/vínculo | Alto; situação financeira | Sim; relacionamento/lookup | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `amount` / `amount` | billing | `billing_member_refunds` | PD financeiro/vínculo | Alto; situação financeira | Cálculo/filtro/cronologia; sem índice criptográfico | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `reason` / `reason` | billing | `billing_member_refunds` | PD financeiro/vínculo | Alto; situação financeira | Leitura por recurso; avaliar sort genérico | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `status` / `status` | billing | `billing_member_refunds` | PD financeiro/vínculo | Alto; situação financeira | Sim; estado/decisão operacional | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `eligibleAt` / `eligible_at` | billing | `billing_member_refunds` | PD financeiro/vínculo | Alto; situação financeira | Cronologia/TTL/sort conforme fluxo | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `eligibleUntil` / `eligible_until` | billing | `billing_member_refunds` | PD financeiro/vínculo | Alto; situação financeira | Leitura por recurso; avaliar sort genérico | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `createdByUserId` / `created_by_user_id` | billing | `billing_member_refunds` | PD indireto; ator | Médio; audit trail | Sim; relacionamento/lookup | Não | Claro; accountability e retenção |
| `requestedAt` / `requested_at` | billing | `billing_member_refunds` | PD financeiro/vínculo | Alto; situação financeira | Cronologia/TTL/sort conforme fluxo | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `requestedByUserId` / `requested_by_user_id` | billing | `billing_member_refunds` | PD indireto; ator | Médio; audit trail | Sim; relacionamento/lookup | Não | Claro; accountability e retenção |
| `approvedAt` / `approved_at` | billing | `billing_member_refunds` | PD financeiro/vínculo | Alto; situação financeira | Cronologia/TTL/sort conforme fluxo | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `approvedByUserId` / `approved_by_user_id` | billing | `billing_member_refunds` | PD indireto; ator | Médio; audit trail | Sim; relacionamento/lookup | Não | Claro; accountability e retenção |
| `rejectedAt` / `rejected_at` | billing | `billing_member_refunds` | PD financeiro/vínculo | Alto; situação financeira | Cronologia/TTL/sort conforme fluxo | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `rejectedByUserId` / `rejected_by_user_id` | billing | `billing_member_refunds` | PD indireto; ator | Médio; audit trail | Sim; relacionamento/lookup | Não | Claro; accountability e retenção |
| `rejectionReason` / `rejection_reason` | billing | `billing_member_refunds` | PD; PS possível no texto | Alto; situação financeira | Sem filtro dedicado | Não | AEAD + minimizar texto livre |
| `refundedAt` / `refunded_at` | billing | `billing_member_refunds` | PD financeiro/vínculo | Alto; situação financeira | Cronologia/TTL/sort conforme fluxo | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `refundedByUserId` / `refunded_by_user_id` | billing | `billing_member_refunds` | PD indireto; ator | Médio; audit trail | Sim; relacionamento/lookup | Não | Claro; accountability e retenção |
| `canceledAt` / `canceled_at` | billing | `billing_member_refunds` | PD financeiro/vínculo | Alto; situação financeira | Cronologia/TTL/sort conforme fluxo | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `canceledByUserId` / `canceled_by_user_id` | billing | `billing_member_refunds` | PD indireto; ator | Médio; audit trail | Sim; relacionamento/lookup | Não | Claro; accountability e retenção |
| `createdAt` / `created_at` | billing | `billing_member_refunds` | PD financeiro/vínculo | Alto; situação financeira | Cronologia/TTL/sort conforme fluxo | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
| `updatedAt` / `updated_at` | billing | `billing_member_refunds` | PD financeiro/vínculo | Alto; situação financeira | Cronologia/TTL/sort conforme fluxo | Não | Claro; cálculos/estados/joins necessários e acesso restrito |
### `billing_role_charge_assignments`

Mapping: [RoleChargeAssignmentEntity.java](../../../src/main/java/com/jeepclub/backend/billing/infra/persistence/entity/assignment/RoleChargeAssignmentEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `roleId` / `role_id` | billing | `billing_role_charge_assignments` | NP (configuração) | Baixo | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
### `billing_user_charge_assignments`

Mapping: [UserChargeAssignmentEntity.java](../../../src/main/java/com/jeepclub/backend/billing/infra/persistence/entity/assignment/UserChargeAssignmentEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `userId` / `user_id` | billing | `billing_user_charge_assignments` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |

## publications

### `event_charge_rules`

Mapping: [EventChargeRuleEntity.java](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/EventChargeRuleEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | publications | `event_charge_rules` | NP (configuração) | Baixo | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `version` / `version` | publications | `event_charge_rules` | NP técnico | Baixo | Sim; controle concorrente | Não | Claro; acesso mínimo e retenção |
| `eventId` / `event_id` | publications | `event_charge_rules` | NP (configuração) | Baixo | Sim; relacionamento/lookup | Sim; par event/definition | Claro; joins/ownership, risco de correlação |
| `chargeDefinitionId` / `charge_definition_id` | publications | `event_charge_rules` | NP (configuração) | Baixo | Sim; relacionamento/lookup | Sim; par event/definition | Claro; joins/ownership, risco de correlação |
| `requiredForParticipation` / `required_for_participation` | publications | `event_charge_rules` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `participationCutoff` / `participation_cutoff` | publications | `event_charge_rules` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `financialDueDate` / `financial_due_date` | publications | `event_charge_rules` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
### `event_guest_requests`

Mapping: [EventGuestRequestEntity.java](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/EventGuestRequestEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | publications | `event_guest_requests` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `version` / `version` | publications | `event_guest_requests` | NP técnico | Baixo | Sim; controle concorrente | Não | Claro; acesso mínimo e retenção |
| `eventId` / `event_id` | publications | `event_guest_requests` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Sim; pares event/cpf e event/vehicle | Claro; joins/ownership, risco de correlação |
| `requesterUserId` / `requester_user_id` | publications | `event_guest_requests` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `vehicleId` / `vehicle_id` | publications | `event_guest_requests` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `approvedVehicleId` / `approved_vehicle_id` | publications | `event_guest_requests` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Sim; par event/vehicle nullable | Claro; joins/ownership, risco de correlação |
| `cpf` / `cpf` | publications | `event_guest_requests` | PD identificador convidado | Alto; fraude/participação | Igualdade, IN, GROUP BY, unique(event,cpf) | Sim; par event/cpf | AEAD + HMAC; deduplicação/queries em PD-07 |
| `status` / `status` | publications | `event_guest_requests` | PD indireto | Médio; vínculo/atividade | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `administrative` / `administrative` | publications | `event_guest_requests` | PD indireto | Médio; vínculo/atividade | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `reviewerId` / `reviewer_id` | publications | `event_guest_requests` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `createdAt` / `created_at` | publications | `event_guest_requests` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `reviewedAt` / `reviewed_at` | publications | `event_guest_requests` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `rejectionReason` / `rejection_reason` | publications | `event_guest_requests` | PD; PS possível no texto | Alto; decisão privada | Sem filtro dedicado | Não | AEAD + minimizar |
### `event_registrations`

Mapping: [EventRegistrationEntity.java](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/EventRegistrationEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | publications | `event_registrations` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `version` / `version` | publications | `event_registrations` | NP técnico | Baixo | Sim; controle concorrente | Não | Claro; acesso mínimo e retenção |
| `unallocatedDependentIds` / `coleção event_registration_unallocated_dependents` | publications | `event_registrations` | PD indireto | Médio; vínculo/atividade | Sim; leitura/relacionamento | Não | Coleção; campos físicos detalhados abaixo |
| `eventId` / `event_id` | publications | `event_registrations` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Sim; par event/user | Claro; joins/ownership, risco de correlação |
| `userId` / `user_id` | publications | `event_registrations` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Sim; par event/user | Claro; joins/ownership, risco de correlação |
| `status` / `status` | publications | `event_registrations` | PD indireto | Médio; vínculo/atividade | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `createdAt` / `created_at` | publications | `event_registrations` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `confirmedAt` / `confirmed_at` | publications | `event_registrations` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `cancelledAt` / `cancelled_at` | publications | `event_registrations` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `occupants` / `coleção event_registration_occupants` | publications | `event_registrations` | PD indireto | Médio; vínculo/atividade | Sim; leitura/relacionamento | Não | Coleção; campos físicos detalhados abaixo |
| `vehicleIds` / `coleção event_registration_vehicles` | publications | `event_registrations` | PD indireto | Médio; vínculo/atividade | Sim; leitura/relacionamento | Não | Coleção; campos físicos detalhados abaixo |
### `event_ride_offers`

Mapping: [EventRideOfferEntity.java](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/EventRideOfferEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | publications | `event_ride_offers` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `version` / `version` | publications | `event_ride_offers` | NP técnico | Baixo | Sim; controle concorrente | Não | Claro; acesso mínimo e retenção |
| `eventId` / `event_id` | publications | `event_ride_offers` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `guestRequestId` / `guest_request_id` | publications | `event_ride_offers` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Sim; par guest/vehicle | Claro; joins/ownership, risco de correlação |
| `selectedGuestId` / `selected_guest_id` | publications | `event_ride_offers` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Sim; nullable | Claro; joins/ownership, risco de correlação |
| `registrationId` / `registration_id` | publications | `event_ride_offers` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `userId` / `user_id` | publications | `event_ride_offers` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `vehicleId` / `vehicle_id` | publications | `event_ride_offers` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Sim; par guest/vehicle | Claro; joins/ownership, risco de correlação |
| `status` / `status` | publications | `event_ride_offers` | PD indireto | Médio; vínculo/atividade | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `respondedAt` / `responded_at` | publications | `event_ride_offers` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `selectedAt` / `selected_at` | publications | `event_ride_offers` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
### `publication_comments`

Mapping: [PublicationCommentEntity.java](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/PublicationCommentEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | publications | `publication_comments` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `publication` / `publication_id (FK)` | publications | `publication_comments` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `authorUserId` / `author_user_id` | publications | `publication_comments` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `content` / `content` | publications | `publication_comments` | PD possível; PS possível no texto | Alto se conteúdo pessoal | Feed por status/data; sem busca textual dedicada | Não | Claro no conteúdo destinado à audiência; minimizar/moderar, risco aceito PD-01 |
| `createdAt` / `created_at` | publications | `publication_comments` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | publications | `publication_comments` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `images` / `coleção publication_comment_images` | publications | `publication_comments` | PD indireto | Médio; vínculo/atividade | Sim; leitura/relacionamento | Não | Coleção; campos físicos detalhados abaixo |
### `publication_event_history`

Mapping: [EventHistoryEntity.java](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/EventHistoryEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `startsAt` / `starts_at` | publications | `publication_event_history` | PD indireto; agenda/participação | Médio | Data/sort/estado evento | Não | Claro; calendário e regras temporais |
| `endsAt` / `ends_at` | publications | `publication_event_history` | PD indireto; agenda/participação | Médio | Data/sort/estado evento | Não | Claro; calendário e regras temporais |
| `eventStatus` / `event_status` | publications | `publication_event_history` | PD indireto | Médio; vínculo/atividade | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
### `publication_events`

Mapping: [EventEntity.java](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/EventEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `startsAt` / `starts_at` | publications | `publication_events` | PD indireto; agenda/participação | Médio | Data/sort/estado evento | Não | Claro; calendário e regras temporais |
| `endsAt` / `ends_at` | publications | `publication_events` | PD indireto; agenda/participação | Médio | Data/sort/estado evento | Não | Claro; calendário e regras temporais |
| `eventStatus` / `event_status` | publications | `publication_events` | PD indireto | Médio; vínculo/atividade | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
### `publication_history`

Mapping: [PublicationHistoryEntity.java](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/PublicationHistoryEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | publications | `publication_history` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `publicationId` / `publication_id` | publications | `publication_history` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Sim; um snapshot por original | Claro; joins/ownership, risco de correlação |
| `authorUserId` / `author_user_id` | publications | `publication_history` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `title` / `title` | publications | `publication_history` | PD possível; PS possível no texto | Alto se conteúdo pessoal | Feed por status/data; sem busca textual dedicada | Não | Claro no conteúdo destinado à audiência; minimizar/moderar, risco aceito PD-01 |
| `content` / `content` | publications | `publication_history` | PD possível; PS possível no texto | Alto se conteúdo pessoal | Feed por status/data; sem busca textual dedicada | Não | Claro no conteúdo destinado à audiência; minimizar/moderar, risco aceito PD-01 |
| `status` / `status` | publications | `publication_history` | PD indireto | Médio; vínculo/atividade | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `createdAt` / `created_at` | publications | `publication_history` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | publications | `publication_history` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `publishedAt` / `published_at` | publications | `publication_history` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `archivedAt` / `archived_at` | publications | `publication_history` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `deletedByUserId` / `deleted_by_user_id` | publications | `publication_history` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `deletedAt` / `deleted_at` | publications | `publication_history` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `images` / `coleção publication_image_history` | publications | `publication_history` | PD indireto | Médio; vínculo/atividade | Sim; leitura/relacionamento | Não | Coleção; campos físicos detalhados abaixo |
### `publication_likes`

Mapping: [PublicationLikeEntity.java](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/PublicationLikeEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | publications | `publication_likes` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `publication` / `publication_id (FK)` | publications | `publication_likes` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Sim; par publication/member | Claro; joins/ownership, risco de correlação |
| `memberUserId` / `member_user_id` | publications | `publication_likes` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Sim; par publication/member | Claro; joins/ownership, risco de correlação |
| `createdAt` / `created_at` | publications | `publication_likes` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
### `publication_notice_history`

Mapping: [NoticeHistoryEntity.java](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/NoticeHistoryEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| PK herdada | publications | `publication_notice_history` | PD indireto | Médio por vínculo | Sim; join de herança | Sim; PK | Claro; valores da superclasse nas tabelas base |
### `publication_notices`

Mapping: [NoticeEntity.java](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/NoticeEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| PK herdada | publications | `publication_notices` | PD indireto | Médio por vínculo | Sim; join de herança | Sim; PK | Claro; valores da superclasse nas tabelas base |
### `publication_service_history`

Mapping: [ServicePublicationHistoryEntity.java](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/ServicePublicationHistoryEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `sourceRequestId` / `source_request_id` | publications | `publication_service_history` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `amount` / `amount` | publications | `publication_service_history` | PD indireto; oferta de serviço | Médio; preço divulgado | Leitura/cálculo | Não | Claro; preço necessário à oferta |
| `contactPhone` / `contact_phone` | publications | `publication_service_history` | PD contato | Alto; contato/reidentificação | Sem filtro dedicado | Não | AEAD; API autorizada revela ao público previsto |
### `publication_services`

Mapping: [ServicePublicationEntity.java](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/ServicePublicationEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `sourceRequestId` / `source_request_id` | publications | `publication_services` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Sim | Claro; joins/ownership, risco de correlação |
| `amount` / `amount` | publications | `publication_services` | PD indireto; oferta de serviço | Médio; preço divulgado | Leitura/cálculo | Não | Claro; preço necessário à oferta |
| `contactPhone` / `contact_phone` | publications | `publication_services` | PD contato | Alto; contato/reidentificação | Sem filtro dedicado | Não | AEAD; API autorizada revela ao público previsto |
### `publications`

Mapping: [PublicationEntity.java](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/PublicationEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | publications | `publications` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `authorUserId` / `author_user_id` | publications | `publications` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `title` / `title` | publications | `publications` | PD possível; PS possível no texto | Alto se conteúdo pessoal | Feed por status/data; sem busca textual dedicada | Não | Claro no conteúdo destinado à audiência; minimizar/moderar, risco aceito PD-01 |
| `content` / `content` | publications | `publications` | PD possível; PS possível no texto | Alto se conteúdo pessoal | Feed por status/data; sem busca textual dedicada | Não | Claro no conteúdo destinado à audiência; minimizar/moderar, risco aceito PD-01 |
| `status` / `status` | publications | `publications` | PD indireto | Médio; vínculo/atividade | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `createdAt` / `created_at` | publications | `publications` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | publications | `publications` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `publishedAt` / `published_at` | publications | `publications` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `archivedAt` / `archived_at` | publications | `publications` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `images` / `coleção publication_images` | publications | `publications` | PD indireto | Médio; vínculo/atividade | Sim; leitura/relacionamento | Não | Coleção; campos físicos detalhados abaixo |
### `service_publication_change_requests`

Mapping: [ServicePublicationChangeRequestEntity.java](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/ServicePublicationChangeRequestEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | publications | `service_publication_change_requests` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `servicePublicationId` / `service_publication_id` | publications | `service_publication_change_requests` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `pendingServicePublicationId` / `pending_service_publication_id` | publications | `service_publication_change_requests` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Sim; nullable, pendência única | Claro; joins/ownership, risco de correlação |
| `requestedByUserId` / `requested_by_user_id` | publications | `service_publication_change_requests` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `proposedTitle` / `proposed_title` | publications | `service_publication_change_requests` | PD possível; PS possível no texto | Alto se conteúdo pessoal | Feed por status/data; sem busca textual dedicada | Não | AEAD; conteúdo pré-publicação privado |
| `proposedContent` / `proposed_content` | publications | `service_publication_change_requests` | PD possível; PS possível no texto | Alto se conteúdo pessoal | Feed por status/data; sem busca textual dedicada | Não | AEAD; conteúdo pré-publicação privado |
| `proposedAmount` / `proposed_amount` | publications | `service_publication_change_requests` | PD indireto | Médio; vínculo/atividade | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `proposedContactPhone` / `proposed_contact_phone` | publications | `service_publication_change_requests` | PD contato | Alto; contato/reidentificação | Sem filtro dedicado | Não | AEAD; API autorizada revela ao público previsto |
| `status` / `status` | publications | `service_publication_change_requests` | PD indireto | Médio; vínculo/atividade | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `rejectionReason` / `rejection_reason` | publications | `service_publication_change_requests` | PD; PS possível no texto | Alto; decisão privada | Sem filtro dedicado | Não | AEAD + minimizar |
| `reviewedByUserId` / `reviewed_by_user_id` | publications | `service_publication_change_requests` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `requestedAt` / `requested_at` | publications | `service_publication_change_requests` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `reviewedAt` / `reviewed_at` | publications | `service_publication_change_requests` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | publications | `service_publication_change_requests` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `version` / `version` | publications | `service_publication_change_requests` | NP técnico | Baixo | Sim; controle concorrente | Não | Claro; acesso mínimo e retenção |
| `proposedImages` / `coleção service_publication_change_request_images` | publications | `service_publication_change_requests` | PD indireto | Médio; vínculo/atividade | Sim; leitura/relacionamento | Não | Coleção; campos físicos detalhados abaixo |
### `service_publication_requests`

Mapping: [ServicePublicationRequestEntity.java](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/ServicePublicationRequestEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | publications | `service_publication_requests` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `requestedByUserId` / `requested_by_user_id` | publications | `service_publication_requests` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `title` / `title` | publications | `service_publication_requests` | PD possível; PS possível no texto | Alto se conteúdo pessoal | Feed por status/data; sem busca textual dedicada | Não | AEAD; conteúdo pré-publicação privado |
| `content` / `content` | publications | `service_publication_requests` | PD possível; PS possível no texto | Alto se conteúdo pessoal | Feed por status/data; sem busca textual dedicada | Não | AEAD; conteúdo pré-publicação privado |
| `amount` / `amount` | publications | `service_publication_requests` | PD indireto; oferta de serviço | Médio; preço divulgado | Leitura/cálculo | Não | Claro; preço necessário à oferta |
| `contactPhone` / `contact_phone` | publications | `service_publication_requests` | PD contato | Alto; contato/reidentificação | Sem filtro dedicado | Não | AEAD; API autorizada revela ao público previsto |
| `status` / `status` | publications | `service_publication_requests` | PD indireto | Médio; vínculo/atividade | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `rejectionReason` / `rejection_reason` | publications | `service_publication_requests` | PD; PS possível no texto | Alto; decisão privada | Sem filtro dedicado | Não | AEAD + minimizar |
| `reviewedByUserId` / `reviewed_by_user_id` | publications | `service_publication_requests` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `createdPublicationId` / `created_publication_id` | publications | `service_publication_requests` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `requestedAt` / `requested_at` | publications | `service_publication_requests` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `reviewedAt` / `reviewed_at` | publications | `service_publication_requests` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | publications | `service_publication_requests` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `version` / `version` | publications | `service_publication_requests` | NP técnico | Baixo | Sim; controle concorrente | Não | Claro; acesso mínimo e retenção |
| `images` / `coleção service_publication_request_images` | publications | `service_publication_requests` | PD indireto | Médio; vínculo/atividade | Sim; leitura/relacionamento | Não | Coleção; campos físicos detalhados abaixo |

## iam/authentication

### `authentication_accounts`

Mapping: [AuthenticationAccountEntity.java](../../../src/main/java/com/jeepclub/backend/iam/authentication/infra/persistence/entity/AuthenticationAccountEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `identityId` / `identity_id` | iam/authentication | `authentication_accounts` | PD indireto | Alto; acesso/atividade | Sim; relacionamento/lookup | Sim; PK | Claro; estados/TTL/joins necessários, acesso restrito |
| `user` / `identity_id (MapsId)` | iam/authentication | `authentication_accounts` | PD indireto | Alto; acesso/atividade | Sim; relacionamento/lookup | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `passwordHash` / `password_hash` | iam/authentication | `authentication_accounts` | PD + segredo de autenticação | Crítico; credencial | Verificação de senha, não lookup | Não | Hash BCrypt existente; avaliar custo, nunca cifra reversível |
| `accessStatus` / `access_status` | iam/authentication | `authentication_accounts` | PD indireto | Alto; acesso/atividade | Sim; estado/decisão operacional | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `authenticationStatus` / `authentication_status` | iam/authentication | `authentication_accounts` | PD indireto | Alto; acesso/atividade | Sim; estado/decisão operacional | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `credentialStatus` / `credential_status` | iam/authentication | `authentication_accounts` | PD indireto | Alto; acesso/atividade | Sim; estado/decisão operacional | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `lastLoginAt` / `last_login_at` | iam/authentication | `authentication_accounts` | PD indireto | Alto; acesso/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `createdAt` / `created_at` | iam/authentication | `authentication_accounts` | PD indireto | Alto; acesso/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `accessDisabledAt` / `access_disabled_at` | iam/authentication | `authentication_accounts` | PD indireto | Alto; acesso/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `updatedAt` / `updated_at` | iam/authentication | `authentication_accounts` | PD indireto | Alto; acesso/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `passwordChangedAt` / `password_changed_at` | iam/authentication | `authentication_accounts` | PD indireto | Alto; acesso/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `failedLoginAttempts` / `failed_login_attempts` | iam/authentication | `authentication_accounts` | PD indireto | Alto; acesso/atividade | Leitura por recurso; avaliar sort genérico | Não | Claro; estados/TTL/joins necessários, acesso restrito |
### `authentication_password_change_challenges`

Mapping: [PasswordChangeChallengeEntity.java](../../../src/main/java/com/jeepclub/backend/iam/authentication/infra/persistence/entity/PasswordChangeChallengeEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | iam/authentication | `authentication_password_change_challenges` | PD indireto | Alto; acesso/atividade | Sim; PK/lookup | Sim; PK | Claro; estados/TTL/joins necessários, acesso restrito |
| `userId` / `user_id` | iam/authentication | `authentication_password_change_challenges` | PD indireto | Alto; acesso/atividade | Sim; relacionamento/lookup | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `tokenHash` / `token_hash` | iam/authentication | `authentication_password_change_challenges` | PD indireto + segredo derivado | Alto; acesso | Igualdade/unique token | Sim | Hash não reversível existente; tokens CSPRNG 64 bytes |
| `createdAt` / `created_at` | iam/authentication | `authentication_password_change_challenges` | PD indireto | Alto; acesso/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `expiresAt` / `expires_at` | iam/authentication | `authentication_password_change_challenges` | PD indireto | Alto; acesso/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `usedAt` / `used_at` | iam/authentication | `authentication_password_change_challenges` | PD indireto | Alto; acesso/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `used` / `used` | iam/authentication | `authentication_password_change_challenges` | PD indireto | Alto; acesso/atividade | Leitura por recurso; avaliar sort genérico | Não | Claro; estados/TTL/joins necessários, acesso restrito |
### `authentication_password_recovery_requests`

Mapping: [PasswordRecoveryRequestEntity.java](../../../src/main/java/com/jeepclub/backend/iam/authentication/infra/persistence/entity/PasswordRecoveryRequestEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | iam/authentication | `authentication_password_recovery_requests` | PD indireto | Alto; acesso/atividade | Sim; PK/lookup | Sim; PK | Claro; estados/TTL/joins necessários, acesso restrito |
| `userId` / `user_id` | iam/authentication | `authentication_password_recovery_requests` | PD indireto | Alto; acesso/atividade | Sim; relacionamento/lookup | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `tokenHash` / `token_hash` | iam/authentication | `authentication_password_recovery_requests` | PD indireto + segredo derivado | Alto; acesso | Igualdade/unique token | Sim | Hash não reversível existente; tokens CSPRNG 64 bytes |
| `createdAt` / `created_at` | iam/authentication | `authentication_password_recovery_requests` | PD indireto | Alto; acesso/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `expiresAt` / `expires_at` | iam/authentication | `authentication_password_recovery_requests` | PD indireto | Alto; acesso/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `resolvedAt` / `resolved_at` | iam/authentication | `authentication_password_recovery_requests` | PD indireto | Alto; acesso/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `cancelledAt` / `cancelled_at` | iam/authentication | `authentication_password_recovery_requests` | PD indireto | Alto; acesso/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `status` / `status` | iam/authentication | `authentication_password_recovery_requests` | PD indireto | Alto; acesso/atividade | Sim; estado/decisão operacional | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `method` / `method` | iam/authentication | `authentication_password_recovery_requests` | PD indireto | Alto; acesso/atividade | Leitura por recurso; avaliar sort genérico | Não | Claro; estados/TTL/joins necessários, acesso restrito |
### `authentication_refresh_tokens`

Mapping: [RefreshTokenEntity.java](../../../src/main/java/com/jeepclub/backend/iam/authentication/infra/persistence/entity/RefreshTokenEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | iam/authentication | `authentication_refresh_tokens` | PD indireto | Alto; acesso/atividade | Sim; PK/lookup | Sim; PK | Claro; estados/TTL/joins necessários, acesso restrito |
| `sessionId` / `session_id` | iam/authentication | `authentication_refresh_tokens` | PD indireto | Alto; acesso/atividade | Sim; relacionamento/lookup | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `tokenHash` / `token_hash` | iam/authentication | `authentication_refresh_tokens` | PD indireto + segredo derivado | Alto; acesso | Igualdade/unique token | Sim | Hash não reversível existente; tokens CSPRNG 64 bytes |
| `createdAt` / `created_at` | iam/authentication | `authentication_refresh_tokens` | PD indireto | Alto; acesso/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `expiresAt` / `expires_at` | iam/authentication | `authentication_refresh_tokens` | PD indireto | Alto; acesso/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `status` / `status` | iam/authentication | `authentication_refresh_tokens` | PD indireto | Alto; acesso/atividade | Sim; estado/decisão operacional | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `replacedByTokenId` / `replaced_by_token_id` | iam/authentication | `authentication_refresh_tokens` | PD indireto | Alto; acesso/atividade | Sim; relacionamento/lookup | Não | Claro; estados/TTL/joins necessários, acesso restrito |
### `authentication_sessions`

Mapping: [SessionEntity.java](../../../src/main/java/com/jeepclub/backend/iam/authentication/infra/persistence/entity/SessionEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | iam/authentication | `authentication_sessions` | PD indireto | Alto; acesso/atividade | Sim; PK/lookup | Sim; PK | Claro; estados/TTL/joins necessários, acesso restrito |
| `userId` / `user_id` | iam/authentication | `authentication_sessions` | PD indireto | Alto; acesso/atividade | Sim; relacionamento/lookup | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `createdAt` / `created_at` | iam/authentication | `authentication_sessions` | PD indireto | Alto; acesso/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `expiresAt` / `expires_at` | iam/authentication | `authentication_sessions` | PD indireto | Alto; acesso/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `loggedOutAt` / `logged_out_at` | iam/authentication | `authentication_sessions` | PD indireto | Alto; acesso/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; estados/TTL/joins necessários, acesso restrito |
| `status` / `session_status` | iam/authentication | `authentication_sessions` | PD indireto | Alto; acesso/atividade | Sim; estado/decisão operacional | Não | Claro; estados/TTL/joins necessários, acesso restrito |

## iam/authorization

### `authorization_permissions`

Mapping: [PermissionEntity.java](../../../src/main/java/com/jeepclub/backend/iam/authorization/infra/persistence/entity/PermissionEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | iam/authorization | `authorization_permissions` | NP (configuração) | Baixo | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `code` / `code` | iam/authorization | `authorization_permissions` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Sim | Claro; acesso mínimo e retenção |
| `description` / `description` | iam/authorization | `authorization_permissions` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `module` / `module` | iam/authorization | `authorization_permissions` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `createdAt` / `created_at` | iam/authorization | `authorization_permissions` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | iam/authorization | `authorization_permissions` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
### `authorization_role_permissions`

Mapping: [RolePermissionEntity.java](../../../src/main/java/com/jeepclub/backend/iam/authorization/infra/persistence/entity/RolePermissionEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | iam/authorization | `authorization_role_permissions` | NP (configuração) | Baixo | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `role` / `role_id (FK)` | iam/authorization | `authorization_role_permissions` | NP (configuração) | Baixo | Sim; relacionamento/lookup | Sim; par role/permission | Claro; joins/ownership, risco de correlação |
| `permission` / `permission_id (FK)` | iam/authorization | `authorization_role_permissions` | NP (configuração) | Baixo | Sim; relacionamento/lookup | Sim; par role/permission | Claro; joins/ownership, risco de correlação |
| `createdAt` / `created_at` | iam/authorization | `authorization_role_permissions` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
### `authorization_roles`

Mapping: [RoleEntity.java](../../../src/main/java/com/jeepclub/backend/iam/authorization/infra/persistence/entity/RoleEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | iam/authorization | `authorization_roles` | NP (configuração) | Baixo | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `name` / `name` | iam/authorization | `authorization_roles` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Sim | Claro; acesso mínimo e retenção |
| `description` / `description` | iam/authorization | `authorization_roles` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `kind` / `kind` | iam/authorization | `authorization_roles` | NP (configuração) | Baixo | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `status` / `status` | iam/authorization | `authorization_roles` | NP (configuração) | Baixo | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `createdAt` / `created_at` | iam/authorization | `authorization_roles` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `updatedAt` / `updated_at` | iam/authorization | `authorization_roles` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
| `deletedAt` / `deleted_at` | iam/authorization | `authorization_roles` | NP (configuração) | Baixo | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |
### `authorization_user_roles`

Mapping: [UserRoleEntity.java](../../../src/main/java/com/jeepclub/backend/iam/authorization/infra/persistence/entity/UserRoleEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | iam/authorization | `authorization_user_roles` | PD indireto | Médio; vínculo/atividade | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `userId` / `user_id` | iam/authorization | `authorization_user_roles` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Sim; par user/role | Claro; joins/ownership, risco de correlação |
| `role` / `role_id (FK)` | iam/authorization | `authorization_user_roles` | PD indireto | Médio; vínculo/atividade | Sim; relacionamento/lookup | Sim; par user/role | Claro; joins/ownership, risco de correlação |
| `createdAt` / `created_at` | iam/authorization | `authorization_user_roles` | PD indireto | Médio; vínculo/atividade | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |

## platform

### `platform_logs`

Mapping: [SystemLogEntity.java](../../../src/main/java/com/jeepclub/backend/platform/logging/SystemLogEntity.java).

| Campo / coluna ou relação | Módulo | Tabela | Categoria LGPD | Impacto de vazamento | Busca/filtro necessário | Unicidade atual | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| `id` / `id` | platform | `platform_logs` | PD indireto; observabilidade | Médio; comportamento | Sim; PK/lookup | Sim; PK | Claro; pseudônimo relacional, não anonimizado |
| `actorId` / `actor_id` | platform | `platform_logs` | PD indireto; observabilidade | Médio; comportamento | Sim; relacionamento/lookup | Não | Claro; joins/ownership, risco de correlação |
| `action` / `action` | platform | `platform_logs` | PD indireto; observabilidade | Médio/alto; evento clínico | Leitura por recurso; avaliar sort genérico | Não | Claro; código controlado sem payload, retenção |
| `method` / `method` | platform | `platform_logs` | PD indireto; observabilidade | Médio; comportamento | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `path` / `path` | platform | `platform_logs` | PD alvo; contexto saúde quando aplicável | Alto; target clínico/URI | Leitura; sem filtro path dedicado | Não | AEAD para alvo privado + redaction operacional; PD-08 |
| `status` / `status` | platform | `platform_logs` | PD indireto; observabilidade | Médio; comportamento | Sim; estado/decisão operacional | Não | Claro; acesso mínimo e retenção |
| `outcome` / `outcome` | platform | `platform_logs` | PD indireto; observabilidade | Médio; comportamento | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `durationMillis` / `duration_millis` | platform | `platform_logs` | PD indireto; observabilidade | Médio; comportamento | Leitura por recurso; avaliar sort genérico | Não | Claro; acesso mínimo e retenção |
| `requestId` / `request_id` | platform | `platform_logs` | PD indireto; observabilidade | Médio; comportamento | Correlação; sem lookup dedicado no repository | Não | Claro; correlação; gerar internamente ou restringir entrada |
| `occurredAt` / `occurred_at` | platform | `platform_logs` | PD indireto; observabilidade | Médio; comportamento | Cronologia/TTL/sort conforme fluxo | Não | Claro; retenção por finalidade |

## Herança JOINED e campos físicos herdados

As linhas acima listam declarações próprias. Os campos `id/authorUserId/title/content/status/timestamps/images` de Publication e seu histórico são armazenados em `publications`/`publication_history` e suas coleções; Notice/Event/Service não duplicam esses valores. Assignment usa a mesma regra para `id/chargeDefinitionId/active/timestamps`.

| Campo | Módulo | Tabela/armazenamento | Categoria | Impacto | Busca/filtro | Unicidade | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| publication_id (PK/FK) | Publications | publication_notices | PD indireto | Médio; vínculo de conteúdo | Join da base | PK | Claro |
| publication_id (PK/FK) | Publications | publication_events | PD indireto | Médio; vínculo/agenda | Join da base | PK | Claro |
| publication_id (PK/FK) | Publications | publication_services | PD indireto | Médio; vínculo/oferta | Join da base | PK | Claro |
| history_id (PK/FK) | Publications | publication_notice_history | PD indireto | Médio; histórico | Join da base | PK | Claro; retenção |
| history_id (PK/FK) | Publications | publication_event_history | PD indireto | Médio; histórico/agenda | Join da base | PK | Claro; retenção |
| history_id (PK/FK) | Publications | publication_service_history | PD indireto | Médio; histórico/oferta | Join da base | PK | Claro; retenção |
| charge_assignment_id (PK/FK) | Billing | billing_user_charge_assignments | PD indireto | Médio; atribuição | Join da base | PK | Claro |
| charge_assignment_id (PK/FK) | Billing | billing_role_charge_assignments | NP configuração; PD ao relacionar membros | Médio por correlação | Join da base | PK | Claro |
| charge_assignment_id (PK/FK) | Billing | billing_event_participants_charge_assignments | PD indireto | Médio; participação | Join da base | PK | Claro |
| charge_assignment_id (PK/FK) | Billing | billing_all_members_charge_assignments | NP configuração | Baixo | Join da base | PK | Claro |

## Coleções e embeddables

Mapping: [PublicationImageEntity](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/PublicationImageEntity.java),
[PublicationCommentImageEntity](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/PublicationCommentImageEntity.java),
[EventOccupantEmbeddable](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/entity/EventOccupantEmbeddable.java).
As colunas de associação geradas também são dados relacionáveis.

| Campo | Módulo | Tabela | Categoria | Impacto | Busca/filtro | Unicidade | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| publication_id | Publications | publication_images | PD indireto | Médio; vínculo | Join/recurso | Com position e storage_key | Claro; ACL da audiência |
| storage_key | Publications | publication_images | PD possível; referência de arquivo | Alto se imagem privada | Resolução por chave | Com parent | Chave clara; bytes protegidos e ACL PD-09 |
| position | Publications | publication_images | NP isolado; contexto PD | Baixo | Ordenação | Com parent | Claro |
| is_primary | Publications | publication_images | NP isolado; contexto PD | Baixo | Seleção imagem | Não | Claro |
| history_id | Publications | publication_image_history | PD indireto | Médio; vínculo | Join/recurso | Com position | Claro; ACL da audiência |
| storage_key | Publications | publication_image_history | PD possível; referência de arquivo | Alto se imagem privada | Resolução por chave | Não | Chave clara; bytes protegidos e ACL PD-09 |
| position | Publications | publication_image_history | NP isolado; contexto PD | Baixo | Ordenação | Com parent | Claro |
| is_primary | Publications | publication_image_history | NP isolado; contexto PD | Baixo | Seleção imagem | Não | Claro |
| comment_id | Publications | publication_comment_images | PD indireto | Médio; vínculo | Join/recurso | Com position | Claro; ACL da audiência |
| storage_key | Publications | publication_comment_images | PD possível; referência de arquivo | Alto se imagem privada | Resolução por chave | Não | Chave clara; bytes protegidos e ACL PD-09 |
| position | Publications | publication_comment_images | NP isolado; contexto PD | Baixo | Ordenação | Com parent | Claro |
| request_id | Publications | service_publication_request_images | PD indireto | Médio; vínculo | Join/recurso | Com position e storage_key | Claro; ACL da audiência |
| storage_key | Publications | service_publication_request_images | PD possível; referência de arquivo | Alto se imagem privada | Resolução por chave | Com parent | Chave clara; bytes protegidos e ACL PD-09 |
| position | Publications | service_publication_request_images | NP isolado; contexto PD | Baixo | Ordenação | Com parent | Claro |
| is_primary | Publications | service_publication_request_images | NP isolado; contexto PD | Baixo | Seleção imagem | Não | Claro |
| change_request_id | Publications | service_publication_change_request_images | PD indireto | Médio; vínculo | Join/recurso | Com position e storage_key | Claro; ACL da audiência |
| storage_key | Publications | service_publication_change_request_images | PD possível; referência de arquivo | Alto se imagem privada | Resolução por chave | Com parent | Chave clara; bytes protegidos e ACL PD-09 |
| position | Publications | service_publication_change_request_images | NP isolado; contexto PD | Baixo | Ordenação | Com parent | Claro |
| is_primary | Publications | service_publication_change_request_images | NP isolado; contexto PD | Baixo | Seleção imagem | Não | Claro |
| registration_id | Publications | event_registration_unallocated_dependents | PD indireto | Alto; participação/família | Join | Com dependent_id | Claro; ownership/retention |
| dependent_id | Publications | event_registration_unallocated_dependents | PD indireto, possível menor | Alto; participação/família | Leitura/participação clínica | Com registration_id | Claro; evitar cópia de cadastro |
| registration_id | Publications | event_registration_occupants | PD indireto | Alto; deslocamento/família | Join | Com occupant_key | Claro |
| vehicle_id | Publications | event_registration_occupants | PD indireto | Médio; deslocamento | Join/leitura | Não isolado | Claro |
| occupant_key | Publications | event_registration_occupants | PD indireto; chave de ocupante | Alto; associação | Igualdade/unique | Com registration_id | Claro; não usar nome/CPF como chave |
| dependentId (dependent_id inferida) | Publications | event_registration_occupants | PD indireto, possível menor | Alto; família/participação | Leitura/participação clínica | Não isolado | Claro; acesso restrito |
| registration_id | Publications | event_registration_vehicles | PD indireto | Médio; deslocamento | Join | Com vehicle_id | Claro |
| vehicle_id | Publications | event_registration_vehicles | PD indireto | Médio; deslocamento | Join/leitura | Com registration_id | Claro |

## Arquivos, logs, configurações, clientes e demais cópias

| Campo/dado | Módulo | Armazenamento/superfície | Categoria LGPD | Impacto | Busca/filtro | Unicidade | Recomendação |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Bytes da foto de perfil | Identity/Platform | LOCAL, namespace images | PD; PS se conteúdo/biometria aplicável | Alto; identificação | Resolução pela key | Key UUID | Storage externo/volume protegido; ACL e minimização, PD-09 |
| Bytes de imagem de veículo | Vehicles/Platform | LOCAL, images | PD possível; placa/rosto/localização | Alto | Resolução pela key | Key UUID | Storage protegido; revisar metadados EXIF/ACL |
| Bytes de imagem de ferramenta | Tools/Platform | LOCAL, images | PD possível | Médio/alto | Resolução pela key | Key UUID | Storage protegido e minimização |
| Bytes de imagens de posts/comments/requests | Publications/Platform | LOCAL, images | PD; PS possível no conteúdo | Alto; audiência e cópias | Resolução pela key | Key UUID | Classes pública à audiência/privada; ACL e storage protegido |
| Bytes de comprovante PDF/imagem | Billing/Platform | LOCAL, billing/payment-receipts | PD financeiro/documental; PS possível no anexo | Alto; pode conter nome/CPF/conta | PaymentId + ownership | Key UUID | Envelope AEAD dos bytes privados; proteção de volume/backup |
| Metadados EXIF/texto em PDF/imagens | Platform/consumidores | Mesmo arquivo | PD/PS possível; geolocalização/documentos | Alto conforme conteúdo | Nenhuma necessidade funcional demonstrada | Não | Minimizar/remover metadados dispensáveis; conteúdo exige política de finalidade |
| filename original | Platform/consumidores | Request/memória; não localizado como coluna/filename persistido pelo provider | PD possível | Médio | Extensão/validação somente | Não | Não logar/persistir nome original; key gerada sem nome pessoal |
| storageKey/namespace/data/extensão | Platform | Path do objeto LOCAL | PD indireto ao relacionar recurso | Médio; correlação | Load/delete | UUID | Claro técnico; ACL, não colocar CPF/nome no path |
| userName | Logging | MDC e pattern console | PD direto | Alto; toda requisição autenticada | Diagnóstico humano, dispensável | Não | Remover/minimizar no padrão; PD-08 |
| userId/actorId | Logging | Console, MDC e platform_logs | PD indireto | Médio; atividade | Correlação/auditoria | Não | Claro/pseudônimo com acesso e retenção; não alegar anonimização |
| path operacional | Logging | Console; pattern MVC ou fallback URI sem query | PD/segredo possível no fallback | Alto se URI indevida | Diagnóstico | Não | Template/allowlist + redaction; não registrar URI arbitrária |
| path de auditoria clínica | Logging/Publications | platform_logs | PD alvo em contexto de saúde | Alto; alvo/Event | Leitura de auditoria | Não | AEAD proposto para target/path; manter ação/ator e auditoria obrigatória |
| requestId externo | Logging | MDC, console, System Logs quando fornecido | PD possível se identificador indevido | Médio | Correlação | Não garantida | Preferir ID gerado; restrição de caracteres não impede cliente enviar CPF |
| device | Logging | Console/MDC | PD indireto quando correlacionado | Baixo/médio | Diagnóstico agregado | Não | Claro; categoria WEB/ANDROID/IOS/UNKNOWN, evitar User-Agent bruto |
| Throwable/cause/SQL em falhas | Platform/Billing | Logs operacionais | PD/PS/segredo possível | Alto | Diagnóstico | Não | Redaction estruturada; evitar mensagem de bind/constraint/payload |
| storageKey na falha de cleanup | Billing | PaymentReceiptLifecycle log | PD indireto/documental | Médio/alto | Diagnóstico técnico | Não | Mascarar/usar ID operacional; não expor key completa sem necessidade |
| Nome/ID/sid do access token | Authentication | JWT no cliente; name/IDs assinados, não cifrados | PD e identificadores de sessão | Alto se token roubado | Autenticação; nome não autoritativo | Token/sessão | Minimizar claim name após revisão; não adicionar CPF/saúde; TLS/expiração |
| Access/refresh token, temporaryPassword | Authentication/Memberships | Response/cliente; não valor bruto persistido no DB | Segredo + PD indireto | Crítico; acesso | Fluxo de autenticação | Token | Não logar/cachear; segredos de uso/TTL próprios; não criar cifra reversível no DB |
| E-mail destinatário | SMTP | Provider SMTP/mailbox externa | PD contato | Alto | Entrega de mensagem | Não | Minimizar, TLS/acesso/retention do operador; proteção DB não cobre mailbox |
| Nome destinatário | SMTP/Memberships | Corpo de ativação/rejeição | PD cadastro | Médio/alto | Apresentação somente | Não | Minimizar; finalidade e retenção externas |
| Motivo de rejeição | SMTP/Memberships | Corpo de rejeição | PD; PS possível | Alto | Notificação | Não | Minimizar conteúdo e retenção; evitar saúde/documentos em e-mail |
| Token no link de reset/ativação | SMTP/clientes | Query param do link/mailbox/URL | Segredo + PD indireto | Crítico | Validação por hash; expiração | Token aleatório | Não logar URL; controlar proxy/referrer/APM e expiração |
| Conteúdo dos DTOs autorizados | Todos | Response/request, memória de aplicação/cliente | PD/PS conforme campo da matriz | Alto no perfil clínico | Uso funcional | Não aplicável | Manter ownership; não registrar payload; minimizar respostas e retenção de cliente |
| Bytes de comprovante em cache | Billing/cliente | Cache-Control private,max-age=300 | PD financeiro | Alto | Reuso de download | Não | Rever para no-store conforme classe de privacidade; PD-10 |
| Metadados de eventos da fila | Logging | ArrayBlockingQueue em memória, até 5.000 | PD indireto/contexto clínico | Médio/alto | Batch persist | Não | Capacidade/TTL/acesso restritos; nenhum payload clínico |
| Dados em cache distribuído | Todos | Não identificado por código/configs auditados | Não determinado | A investigar na infra | Não identificado | Não identificado | Não criar cache de perfil descriptografado; obter evidência de ambiente |
| bootstrap.name | Identity/startup | Config dev -> identity_users | PD cadastro se real | Alto | Registro | Não | Dados sintéticos; segredo/config fora do Git, perfil prod explícito |
| bootstrap.birthDate | Identity/startup | Config dev -> identity_users | PD cadastro se real | Alto | Registro | Não | Minimizar/sintético; não importar fixture real |
| bootstrap.email | Identity/startup | Config dev -> identity_users | PD contato se real | Alto | Registro/unique | Sim no DB | Sintético/local; proteção igual ao cadastro após implementação |
| bootstrap.cpf | Identity/startup | Config dev -> identity_users | PD identificador se real | Alto | Lookup bootstrap/unique | Sim no DB | Sintético/local; não usar documento real de desenvolvedor |
| bootstrap.rg | Identity/startup | Config dev -> identity_users | PD identificador se real | Alto | Registro/unique | Sim no DB | Sintético/local; proteção igual ao cadastro |
| bootstrap.phoneNumber | Identity/startup | Config dev -> identity_users | PD contato se real | Alto | Registro | Não | Sintético/local; proteção igual ao cadastro |
| bootstrap.password | Authentication/startup | Config -> hash BCrypt no DB | Segredo de credencial | Crítico | Provisionamento | Não | Secret injection, não salvar valor bruto; bloquear fluxo dev em prod |
| bootstrap.roleName | Authorization/startup | Config -> role atribuída | PD indireto por vínculo; não filiação sensível presumida | Médio; privilégio | Lookup role | Nome role único | Claro; mínimos privilégios e perfil controlado |
| SMTP username/from, DB username | Infra | Config externa/ENV; exemplos versionados | PD possível + configuração | Médio/alto | Conexão/entrega | Não | Conta técnica, secret manager quando necessário; não usar identidade pessoal |
| JWT/SMTP/DB passwords e futuras chaves de cifra/HMAC | Infra/Platform | Segredos externos, não inventariados em ambiente | Segredo; pode dar acesso a PD/PS | Crítico | Operação técnica | Não | Secret manager/KMS; separar funções/ambientes; nunca banco/Git/dump |
| Fixtures/testes | Todos | src/test, application-test.properties | Massa sintética esperada, não evidência de anonimização de produção | Alto se cópia real | Teste somente | Conforme constraints | Proibir base real; verificar pipeline de seed/import; produção usa writers protegidos |
| Dados de dumps/backups/snapshots infra | Infra | Política/provider não definidos no repo | PD/PS conforme conjunto copiado | Crítico | Restore | Não aplicável | Criptografia de backup + ACL/chaves externas; retenção, restore e descarte aprovados |
| Endereço estruturado futuro BACK-408 | Identity | Ausente nesta baseline; texto livre pode conter endereço | PD de alto impacto | Alto; localização/fraude | A definir com Story | Normalmente não; confirmar | AEAD proposto; minimização/consultas antes de adicionar campos |
| Perfil profissional futuro BACK-408 | Identity | Ausente como estrutura; serviço publicado já pode descrever profissão | PD; PS se filiação/opinião exposta | Médio/alto | A definir com Story | A definir | Minimizar; separar público previsto de dados privados, avaliar AEAD por campo |

## Evidência de comportamento e cópias

- [Identity queries](../../../src/main/java/com/jeepclub/backend/iam/identity/infra/persistence/query/AdminUserJpaQueryRepository.java) e [sort](../../../src/main/java/com/jeepclub/backend/iam/identity/infra/persistence/sort/UserSortMapper.java): filtros, projections e ordenação.
- [Dependents repository](../../../src/main/java/com/jeepclub/backend/dependents/infra/persistence/jpa/DependentJpaRepository.java), [Vehicles repository](../../../src/main/java/com/jeepclub/backend/vehicles/infra/persistence/jpa/VehicleJpaRepository.java), [Memberships repository](../../../src/main/java/com/jeepclub/backend/memberships/infra/persistence/jpa/MembershipApplicationJpaRepository.java): equality/ownership/deduplicação.
- [Guest queries](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/jpa/EventGuestRequestJpaRepository.java): CPF em igualdade, lote e agregação.
- [Health lifecycle](../../../src/main/java/com/jeepclub/backend/health/docs/README.md) e [snapshot mapper](../../../src/main/java/com/jeepclub/backend/health/infra/persistence/mapper/MedicalProfileHistoryMapper.java): exclusão guarda cópia clínica.
- [Publications delete](../../../src/main/java/com/jeepclub/backend/publications/infra/persistence/adapter/PublicationRepositoryAdapter.java): snapshot da publicação; comentários/likes são removidos nesse fluxo, não mantidos em uma tabela de histórico equivalente.
- [Emergency audit](../../../src/main/java/com/jeepclub/backend/publications/core/application/service/AdminEventService.java): registra alvo/ator antes de devolver o resultado; não persiste prontuário em Publications.
- [LOCAL bytes](../../../src/main/java/com/jeepclub/backend/platform/storage/local/LocalFileStorage.java), [global images](../../../src/main/java/com/jeepclub/backend/platform/storage/image/ImageMediaController.java), [receipt ownership](../../../src/main/java/com/jeepclub/backend/billing/core/application/service/paymentreceipt/PaymentReceiptService.java) e [cache de comprovantes](../../../src/main/java/com/jeepclub/backend/billing/api/http/controller/PaymentReceiptController.java).
- [Logging filter](../../../src/main/java/com/jeepclub/backend/platform/logging/SystemRequestLoggingFilter.java), [persistência/retention](../../../src/main/java/com/jeepclub/backend/platform/logging/SystemLogPersistenceWorker.java), [unexpected errors](../../../src/main/java/com/jeepclub/backend/platform/web/exception/GlobalExceptionHandler.java), [receipt cleanup](../../../src/main/java/com/jeepclub/backend/billing/core/application/service/paymentreceipt/PaymentReceiptLifecycle.java), [properties](../../../src/main/resources/application.properties).
- [JWT](../../../src/main/java/com/jeepclub/backend/iam/authentication/infra/security/jwt/JwtServiceImpl.java), [hash de tokens](../../../src/main/java/com/jeepclub/backend/iam/authentication/infra/security/token/Sha256TokenHashService.java), [BCrypt](../../../src/main/java/com/jeepclub/backend/iam/authentication/infra/config/security/PasswordConfig.java): proteção atual de credenciais distinta da cifra proposta de cadastro.
- [SMTP recovery](../../../src/main/java/com/jeepclub/backend/iam/authentication/infra/adapter/SmtpEmailNotificationAdapter.java), [SMTP Memberships](../../../src/main/java/com/jeepclub/backend/memberships/infra/mail/SmtpMemberActivationMailSender.java), [bootstrap dev](../../../src/main/java/com/jeepclub/backend/platform/startup/security/DevelopmentSecurityBootstrapRunner.java), [exemplo local](../../../src/main/resources/application-dev.properties.example).
