# BACK-526 — runbook da política numérica (26/10/2026)

Base auditada: develop 236c6715, com #104/#108/#113 integrados. V10 é a reconciliação
incremental desta política; V1–V9 permanecem imutáveis. A solicitação homologa as
regras, não autoriza merge, deploy, acesso à VPS, dados reais ou flags.

## Fase 1 — preflight, backup e compatibilidade

Responsável operacional registra janela, aprovador, SHA/imagem atual/candidata,
configuração sanitizada, history/checksums Flyway e plano de recuperação. Parar
todos os escritores antigos: as políticas antiga/nova não podem operar juntas.
Obter backup consistente de SQL, uploads e configuração protegida, manifesto de
hashes, cópia externa e retenção. Definir RTO/RPO com a operação, sem valores inventados.

Restaurar backup autorizado em MySQL 8.0.46 isolado, com rede/portas/volumes próprios
e sem SMTP/rotinas reais. Conferir fingerprints privados, contagens por categoria,
history/checksums e integridade antes/depois. Testcontainers sintético não comprova
restaurabilidade de backup produtivo; essa evidência continua operacional pendente.

Antes de executar Flyway, obter **somente contagens sanitizadas** de:

- Members e reservas com literal fora de dígitos ASCII positivos (zero também inválido).
- Colisões após remover zeros iniciais, entre Members e reservas explícitas.
- Members sem claim correspondente ao mesmo memberId e número atual.
- Claims retiradas e auditorias existentes, para conferência de preservação.

Usar os mesmos predicados da V10 em consultas de leitura autorizadas. Não expor
linhas/CPF/e-mail em Jira/logs. Qualquer incompatibilidade é NO-GO; não remover
cadastros, inventar número ou converter texto alfanumérico por aproximação.
Dados vigentes/reservas incompatíveis exigem plano de saneamento autorizado antes
da migration; não se permite repair/clean/reset de baseline como recuperação.

Remover configurações aposentadas INITIAL_NUMBER/WIDTH/PREFIX. O aplicativo
recusa valores antigos explícitos, mesmo com emissão false; prefixo vazio é neutro.
Manter as três flags efetivas false durante implantação:

```text
MEMBERSHIPS_REGISTRY_ISSUANCE_ENABLED=false
MEMBERSHIPS_REGISTRY_ISSUANCE_APPROVED=false
MEMBERSHIPS_REGISTRY_IMPORTS_APPLY_ENABLED=false
```

Conferir que o usuário de migration possui CREATE ROUTINE, EXECUTE e ALTER ROUTINE
para o procedimento auxiliar da V10, além das permissões DML/DDL usuais; não
confundir com grants RBAC administrativos da aplicação.

Ensaiar Flyway normal da versão realmente instalada até V10, depois Hibernate
ddl-auto=validate. A V10 não altera tipos, PK/FKs ou sequências técnicas. Sob lock,
preflight precede DML; copia metadata de todas as claims para LEGACY_CLAIM_SNAPSHOT,
remove apenas claims retiradas, canonicaliza Members/reservas e limpa contador
legado. Se literal de Member mudar (027→27), version avança; clientes devem reler.
Audits antigos, inclusive literais opacos históricos, permanecem intactos.

Conferir Identity/Authentication/Authorization/Billing byte a byte nos campos
anteriores; Members mantêm id/identityUserId, reservas seguem bloqueantes e audit
contém todos os snapshots com ator/data/motivo originais. Não há criação de Member
por startup. Reaplicar Flyway normal não duplica a migration.

GO técnico: CI do SHA correto, preflight compatível, backup/restore comprovados,
validate/health e integridade aprovados. NO-GO: inconsistência, checksum/drift,
backup não restaurável, versão/configuração errada, falha de login/Billing ou autorização ausente.

## Fase 2 — matrículas históricas por correção manual

Este é o fluxo homologado; não exige planilha nem importação em lote:

1. Cadastrar usuário e concluir primeiro acesso normalmente.
2. Quando emissão estiver habilitada, receber matrícula automática.
3. Administrador com MEMBERSHIP_MEMBER_REGISTRATION_MANAGE consulta matrícula histórica.
4. GET /admin/members/by-identity/{userId}; obter a versão atual.
5. PATCH /admin/members/by-identity/{userId}/membership-number com número, expectedVersion e reason.
6. Conferir novo número nas próximas consultas/listas/exports e histórico do Member.

Exemplo sintético de payload: `{"membershipNumber":"150","expectedVersion":0,"reason":"Histórico conferido"}`.
Anterior fica livre para outro associado. Não apagar audit nem usar User.id/CPF
isolado/ordem para inferir matrícula. 409 por destino ocupado/reservado/versão pede
releitura e revisão; não sobrescrever titular. Contas antigas sem Member seguem
autenticando e podem receber associação explícita pelo PUT existente.

Importador existente é opcional, não gate da funcionalidade manual. Se futuramente
autorizado, exige fonte conferida, permission IMPORT, administrativelyConfirmed,
sourceReference opaca, dry-run exato/proof válido e apply em janela própria.
027/27 colidem; destino vigente/reservado não pode ser tomado. Falha de uma unidade
não desfaz unidades já confirmadas. Repetir lote é idempotente; subconjunto exige
novo preview. Proof dura 15 minutos, é por processo/ator/payload; não registrar tokens.
Manter apply false ao fim da janela. Nenhuma importação real é autorizada aqui.

## Fase 3 — ativação operacional de novas matrículas

Formato, início 1, reutilização e primeiro acesso já estão homologados. Não
exigir aprovação de faixa inicial fixa: ela não existe na nova política.
Ativação produtiva continua dependendo de release/janela autorizada e preflight.
Somente nessa decisão habilitar ENABLED/APPROVED; não habilitar importação junto.

Gerador lê maior matrícula vigente + 1 sob mutex/current read numérico, ou 1 sem
Members, pulando reservas explícitas. Correção de 9999→150 com máximo restante
200 faz próximo 201; se 10000 já foi emitida, permanece e o próximo é 10001.
next_value/format_signature nunca determinam o candidato; não reinicializar PKs.

Homologar os dois primeiros acessos, aprovação/reenvio/expiração sem emissão,
retry sem duplicação, rollback tardio e rejeição concorrente. Não permitir
reversão de COMPLETED. ROOT/técnicos não recebem número por login ou mera criação.
Contas COMPLETED com flag false não têm emissão retroativa automática.

## Smoke técnico e recuperação

Em checkout isolado, Java 17/Docker, somente fixtures sintéticas:

```bash
./mvnw --batch-mode --no-transfer-progress verify
./mvnw --batch-mode --no-transfer-progress -Pmysql-migrations -DskipUnitTests=true verify
bash deploy/test-future-migration.sh
```

API: 400 inválido; 401 sem autenticação; 403 sem permission; 404 ausente; 409
ocupado/reservado/versão. Conferir reuso 20→150→20 em outro Member com histórico
preservado; perfis/listas/CSV/PDF atuais, null sem Member e ausência de N+1.

Falha exige suspender escritores/flags, preservar volume/history/logs privados e
snapshot, identificar unidades confirmadas e ensaiar recuperação isolada. DML da
V10 reverte em erro, mas DDL de procedure e history MySQL não são ACID. Preflight
em Flyway pode deixar registro failed/procedure auxiliar; evitar isso com leitura
prévia e resolver por restauração/plano versionado aprovado, não repair/clean.
Não prometer downgrade automático: código antigo pode recriar bloqueios permanentes
ou numeração antiga. Restauração SQL/uploads/configuração deve ser coordenada e
avaliar fatos posteriores conforme RPO/RTO aprovados.

Depois de publicação autorizada, confirmar SHA/imagem, history/validate/health,
flags efetivas, login/refresh, Registry/Billing e métricas sanitizadas de conflitos,
FAILED, latência/locks. Registrar aceite operacional. Não executar nada disso na VPS
nesta rodada; BACK-536 mantém pendências que exigem evidência operacional real.
