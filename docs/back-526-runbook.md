# BACK-526 — runbook após release #112

Referência de código auditada em 25/10/2026: master/develop
`9ad04a71e50917e0a53c1c4855ea6770730bd8b9`. A release já inclui V1–V9, inclusive
V4 Registry, V5 lifecycle (BACK-512) e V6 onboarding (BACK-523). Reconfirmar o SHA
efetivamente instalado e a history real na janela autorizada. Não tratar baseline
V3 e aplicação inicial de V4 como estado corrente presumido da VPS.

Este runbook prepara operações futuras. Não houve deploy, leitura da VPS,
importação real nem ativação de flags nesta missão. As três fases têm aprovação
e evidências independentes. Merge para master dispara pipeline de produção e
exige autorização específica; este PR para develop não a concede.

## Preparação e registro de evidências

O responsável registra ticket/janela, operador e aprovador, SHA/imagem instalada
e candidata, timestamps, configuração efetiva sanitizada, versões/checksums Flyway,
resultado de backup/restore, gates e decisão GO/NO-GO. Não anexar dados de
associados, env completo, credenciais, cookies ou tokens a GitHub/Jira.

Antes de mutação autorizada: coordenar suspensão de escritores; obter backup
consistente de MySQL, uploads e configuração protegida, com manifesto de hashes,
local externo seguro, retenção, responsável e procedimento de recuperação.
Dimensionar espaço, duração e RTO/RPO com a operação; esta entrega não inventa
limites aceitáveis. A fonte histórica e os backups ficam fora do repositório.

Ensaio obrigatório em ambiente isolado autorizado:

1. Restaurar o backup em MySQL 8.0.46 com volumes, portas, rede e credenciais de
   laboratório, sem ligação com produção, SMTP real ou rotinas externas.
2. Conferir hashes do material restaurado e comparar schema, history/checksums,
   contagens por tabela/categoria, fingerprints privados e invariantes relacionais
   com o ponto do backup. Totais agregados não comprovam igualdade de registros.
3. Subir a imagem do SHA candidato com Hibernate validate, sem emissão/import apply.
   Executar Flyway normal somente para migrations realmente pendentes. Nesta revisão
   não há migration nova; não recriar V4 nem editar V1–V9.
4. Ensaiar interrupção e retomada no laboratório, preservar diagnóstico e medir
   recuperação. Conferir que backup + uploads + configuração restauram juntos.
5. Registrar resultados sanitizados, duração, divergências, limitações e aceite
   operacional. Destruir cópias conforme política autorizada de retenção.

Os Testcontainers desta entrega usam apenas fixtures sintéticas. Não constituem
restauração comprovada do backup ativo. Essa evidência permanece pendente.

## Fase 1 — estrutura e compatibilidade

Pré-condições: revisão/CI do SHA exato, configuração e catálogo de permissions
conferidos, backup/restauração comprovados e autorização de publicação separada.

Conferir history sem versões failed/pending inesperadas e checksums coerentes,
schema das quatro tabelas Registry, UNIQUE de identity/number/claim, @Version,
FKs restritivas e índice de audit. Validar via Hibernate `ddl-auto=validate`.
Não usar update, clean, repair, alteração de baseline nem recriação de banco.
V10 ou posterior só se uma mudança real de schema for demonstrada em outro PR.

Manter ambas flags efetivas false:

```text
MEMBERSHIPS_REGISTRY_ISSUANCE_ENABLED=false
MEMBERSHIPS_REGISTRY_ISSUANCE_APPROVED=false
MEMBERSHIPS_REGISTRY_IMPORTS_APPLY_ENABLED=false
```

Comparar Identity IDs, campos e referências de Authentication, Authorization,
MembershipApplication e Billing antes/depois. Nenhum backfill, User novo ou
renumeração deve ocorrer por startup. Members já existentes devem permanecer;
não exigir tabelas vazias num ambiente que já tenha uso autorizado do Registry.

Smoke autorizado com contas/dados sintéticos de laboratório:

| Verificação | Resultado esperado |
| --- | --- |
| Login e /authentication/me de legado sem Member | Acesso normal por userId; nenhuma matrícula artificial |
| Perfil/detalhe/lista/export com e sem Member | Número vigente ou null/ausente/vazio conforme contrato |
| Retificação sintética seguida de GET/lista/CSV/PDF | Número novo, IDs/relações financeiros intactos |
| Sem autenticação / sem permission | 401 / 403 |
| Entrada inválida / inexistente / colisão-versão-reserva | 400 / 404 / 409 |
| CUSTOM sem grants de Registry | Nenhuma autoridade implícita |
| Primeiro acesso nos dois métodos com emissão false | COMPLETED, sem consumo do contador nem Member automático |
| Rejeição pendente e Member retido | BACK-523 reverte somente provisioning elegível; BACK-512 bloqueia DELETE de Member |
| Apply com IMPORT mas flag false | 409 MEMBER_REGISTRY_IMPORT_DISABLED, sem escrita |

GO: integridade/validate/health/smokes aprovados e nenhuma diferença indevida.
NO-GO: drift/checksum inesperado, migration falha, integridade divergente,
falha de login/Billing, flag inesperada ou ausência de backup restaurável.
Não executar mutações sintéticas no banco real sem autorização específica.

Script de smoke técnico reutilizando a suíte existente, para um checkout local
isolado com Java 17 e Docker (somente bancos Testcontainers sintéticos):

```bash
#!/usr/bin/env bash
set -euo pipefail
./mvnw --batch-mode --no-transfer-progress \
  -Dtest=MemberRegistryIntegrationTest,MemberRegistryApiIntegrationTest,MemberRegistryImportDisabledIntegrationTest,MemberRegistryPolicyTest,MembershipActivationFlowIntegrationTest,PaidMembershipFlowIntegrationTest,PrincipalLifecycleIntegrationTest test
./mvnw --batch-mode --no-transfer-progress -Pmysql-migrations -DskipUnitTests=true \
  -Dit.test=MemberRegistryMySqlIT,MembershipRegistryOnboardingMySqlIT,PrincipalLifecycleMySqlIT verify
```

Esse roteiro seleciona cenários já incluídos nos gates completos desta revisão;
não é um cliente de produção e não requer credenciais reais. Para smoke operacional,
usar a tabela anterior na cópia isolada autorizada e registrar resultado próprio.

## Fase 2 — matrículas históricas

Pré-condições: fase 1 aceita; fonte oficial versionada e conferida por responsável
autorizado; grants mínimos; janela e importação real especificamente autorizadas;
backup atualizado e restauração testada. Manter emissão automática false.

Inventariar em leitura autorizada: total de Identity, ROOT/técnicos, pendentes de
primeiro acesso, vínculos APPROVED/COMPLETED e sem aplicação, com/sem Member.
Categorias podem sobrepor: não somar contagens como total sem definir interseções.
Não inferir quantidade de usuários do antigo agregado de 712 linhas/61 tabelas.

Conciliar explicitamente cada identityUserId existente contra fonte oficial.
CPF isolado, ordem de linhas, User.id e created_user_id não revelam matrícula.
Não recriar User nem criar aplicação fictícia. Divergências e múltiplas
correspondências voltam à conferência; sem confirmação ficam UNVERIFIED.

Contrato HTTP (exemplo **sintético**, IDs não são números de produção):

```http
POST /admin/members/imports/dry-run
Content-Type: application/json
Authorization: Bearer <sessao-administrativa>

{"rows":[{"identityUserId":108,"membershipNumber":"027","administrativelyConfirmed":true,"sourceReference":"cadastro-v1/linha-12"}],"reason":"Lote conferido; referencia opaca"}
```

Revisar índices/outcomes do relatório: READY, ALREADY_LINKED, UNVERIFIED,
DUPLICATE_SOURCE, IDENTITY_NOT_FOUND, IDENTITY_INACTIVE, IDENTITY_ALREADY_LINKED
e NUMBER_UNAVAILABLE. Este último cobre reserva, claim antiga e colisão numérica;
não é autorização para liberar claim. Divergência cadastral é conferência humana,
não matching automático. Dry-run não modifica Member, claims, audit nem contador.

Guardar o relatório sanitizado e a aprovação do lote fora dos logs públicos.
`administrativelyConfirmed` declara conferência humana; não prova que o backend
consultou a fonte oficial. `sourceReference` é opaca e sem dados pessoais.

Somente na janela autorizada habilitar apply; repetir o payload **exato** em
`POST /admin/members/imports/apply`, acrescentando `dryRunToken` do preview.
Proof expira em 15 minutos e depende do processo/ator/payload; restart, outra
instância, fonte/motivo/linhas alteradas exigem novo dry-run. Não registrar proof.
Cada unidade confirma ou reverte isoladamente e revalida locks/claims; a ausência
de conflito no preview não garante aplicação futura. HTTP 200 contém resultados
parciais, incluindo FAILED/CONFLICT/NOT_FOUND; examinar todas as linhas.

Reprocessar o lote é idempotente; subconjunto de pendências requer novo preview.
Falha intermediária não desfaz unidades já confirmadas. Nunca corrigir por SQL
direto, trocar identidade ou apagar audit. Retificação autorizada usa PATCH
`/admin/members/by-identity/{userId}/membership-number` com membershipNumber,
expectedVersion e reason. Um número retirado permanece indisponível.

Verificar vínculos/projeções, contagens READY/aplicadas/já vinculadas/pendentes,
auditoria e integridade de Identity/credenciais/Billing. Desabilitar apply ao
encerrar a janela, conferir configuração efetiva e registrar aceite.

GO: fonte/conferência/autorização válidas, conflitos entendidos, dry-run revisado,
integridade e auditoria corretas. NO-GO: dúvida na identidade/fonte, alteração
concorrente não resolvida, reserva inesperada, falha sem diagnóstico, backup
inválido ou autorização ausente. Não forçar linhas conflituosas.

## Fase 3 — novas matrículas

Pré-condições: homologação registrada de formato, zeros/case, reservas/lacunas,
faixa inicial e marco formal de associação; reconciliação histórica revisada;
responsáveis/grants e janela de ativação aprovados. Fase 2 e fase 3 são decisões
separadas. Não inferir início de MAX, PK ou último cadastro observado.

Configurar explicitamente INITIAL_NUMBER, WIDTH e PREFIX (pode ser vazio),
APPROVED=true e ENABLED=true apenas no ambiente autorizado. Sem homologação,
manter defaults; exemplos de testes não são configuração oficial. A assinatura
persistida fixa início/formato após uso. Divergência exige revisão e conflito,
nunca reset do contador. Claims antigas/reservas são puladas; falha desfaz avanço.

Homologar access link e senha temporária: aprovação sem emissão; conclusão
formal de primeiro acesso emite uma vez; retries/reenvio/expiração/rejeição não
consomem números. ROOT/técnicos sem admissão não recebem matrícula por login ou
criação de User. Contas COMPLETED enquanto flag false não têm catch-up automático:
tratamento posterior é explícito e autorizado.

GO: decisões assinadas, configuração exata válida, smokes dos dois métodos e
concorrência aprovados, observabilidade e suporte preparados. NO-GO: qualquer
decisão pendente, colisão inexplicada, configuração incompleta, duplicidade,
emissão prematura ou regressão nos contratos existentes.

## Interrupção, recuperação e pós-publicação

Se houver erro, suspender a operação e escritores afetados, manter flags false e
preservar volume, history, logs privados, SHA, backup e relatório de unidades.
Não liberar claims nem renumerar para esconder lacunas. Em falha de lote, levantar
unidades já confirmadas antes de repetir; correção de dado exige decisão rastreável.

DDL MySQL pode confirmar parcialmente. Ensaiar correção para frente ou restauração
coordenada de SQL/uploads/configuração em instância isolada, comparar integridade
e decidir com a operação. Não executar imagem antiga sobre schema incompatível
nem prometer rollback ACID de DDL. Uma restauração pode perder fatos posteriores:
RPO/RTO e reconciliação precisam ser aprovados antes do retorno.

Após publicação autorizada: confirmar SHA/imagem, Flyway/validate, health, catálogo,
flags efetivas, login/refresh/perfil/exports/Billing e métricas sanitizadas de 409,
FAILED e latência/locks. Registrar duração, resultado e aceite de cada fase.
CI verde é evidência de laboratório; não encerra BACK-526 sem decisões externas.
