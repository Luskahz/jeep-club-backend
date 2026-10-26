# Membership Registry — BACK-526

Política homologada em 26/10/2026. [ADR](../../../../../../../../docs/adr/back-526-member-registry.md),
[análise/matriz](../../../../../../../../docs/back-526-numeric-policy.md) e
[runbook](../../../../../../../../docs/back-526-runbook.md) são os documentos atuais.

## Regras funcionais

Member pertence a Memberships. Member.id e Identity.User.id são PKs imutáveis;
identityUserId mantém o vínculo único. A matrícula é um identificador externo
numérico e mutável. Permanece string JSON/VARCHAR(64) para preservar contratos e
precisão: `027` entra como `27`; válidos de 1 até 10^64−1. Sem prefixos, sinais,
zero, espaços ou formatos arbitrários. Não há fallback visual para IDs técnicos.

Geração usa o maior número atualmente atribuído + 1, ou 1 sem Members; reservas
explícitas encontradas nesse avanço são puladas. Correções podem reduzir o máximo.
200 + 9999 → próximo 10000; corrigir 9999→150 antes de emitir → próximo 201.
Se outro Member já recebeu 10000, ele permanece com 10000 e o próximo é 10001.

Todas as escritas usam Identity → mutex do Registry; reserva só usa o mutex.
Máximo é leitura corrente FOR UPDATE com CAST DECIMAL(65,0), seguido de BigInteger.
Não deriva de PK, auditoria ou contador legado. UNIQUE e @Version complementam
os locks. Falhas revertem Member, ocupações e audit conjuntamente.

Claims representam apenas ocupação vigente ou reserva explícita (memberId null).
Retificação libera a ocupação anterior. O número pode pertencer depois a outra
identidade, mas o histórico original permanece no Member original. Não há API
de exclusão do audit ou de liberação de reserva. Inativação/arquivamento conserva
o vínculo atual; lifecycle não libera matrícula automaticamente.

## HTTP, permissões e erros

| Método / rota sob /admin/members | Permission MEMBERSHIP_MEMBER_ | Sucesso |
| --- | --- | --- |
| GET /; /by-number?membershipNumber=27; /by-identity/{userId} | READ | 200 |
| PUT /by-identity/{userId} | REGISTRATION_MANAGE | 200 |
| PATCH /by-identity/{userId}/membership-number | REGISTRATION_MANAGE | 200 |
| GET /by-identity/{userId}/history | AUDIT_READ | 200 |
| POST /by-identity/{userId}/generate | NUMBER_ISSUE | 200 |
| POST /reservations | NUMBER_RESERVE | 201 |
| POST /imports/dry-run; /imports/apply | IMPORT | 200 por linha |

Todos continuam com Cache-Control no-store e OpenAPI. PUT idempotente da mesma
matrícula canônica não cria audit adicional. PATCH exige versão atual e motivo.

```json
{"membershipNumber":"150","expectedVersion":0,"reason":"Matrícula histórica conferida"}
```

PUT usa membershipNumber/reason; generate usa reason. Destino ocupado/reservado,
versão antiga, vínculo incompatível ou emissão indisponível retornam 409. Identity
ou Member inexistente retorna 404; entrada inválida 400, sem autenticação 401 e
permission ausente 403. Erros não expõem SQL ou dados pessoais. Roles CUSTOM
exigem grants explícitos; ROOT segue catálogo estrutural existente.

GET by-number normaliza zeros e encontra o titular **vigente**, inclusive após
reutilização. Histórico é por identidade/Member, não uma pesquisa de posse atual.
Eventos guardam anterior/novo, ator, instante, motivo, operação e resultado.
LEGACY_CLAIM_SNAPSHOT conserva metadata de claims da política anterior na V10.

## Identity e frontend

Campos e IDs existentes não mudam. Resposta exemplo: `{"id":108,"membershipNumber":"27"}`.
Exibir Matrícula 27; manter id nas chamadas técnicas. Sem Member, campo null/ausente,
CSV/PDF com célula vazia. Nunca usar id como matrícula. A próxima leitura após commit
usa matrícula atual; não reescreve documentos históricos já materializados.

Queries por Identity/lote/reversa são contratos públicos Memberships; nenhum
repository/entity privado é acessado cross-module. Perfis, detalhe/lista/pesquisa
de Identity e CSV/PDF compõem a matrícula vigente dinamicamente. Lotes até 500 IDs
e exports em chunks de 200 evitam N+1. `fields=NAME,MEMBERSHIP_NUMBER` não expõe o ID
auxiliar; `fields=ID,NAME` omite a matrícula. membershipNumber aceita número com
zeros normalizáveis; q continua busca textual e inclui matrícula numérica atual.
Sort por matrícula em Identity continua 400; não se ordena página em memória.
Export padrão MEMBER prioriza matrícula; TECHNICAL põe ID/Matrícula nas primeiras
colunas e não promete cópia byte a byte do CSV anterior ao Registry.

## Onboarding e reconciliação manual

Aprovação não emite. Primeiro acesso efetivo por link ou senha temporária emite
uma vez somente com enabled=true e approved=true. Reenvio, expiração e aprovação
não criam Member. disabled permite concluir/login sem Member, sem catch-up no boot.
BACK-523 conserva rejeição PENDING e reversão APPROVED somente antes do primeiro
acesso; COMPLETED é recusado. BACK-512 mantém os guards de lifecycle e retenção.
Authentication, Authorization e Billing não ganham dependência obrigatória de Member.

Legado: cadastrar normalmente, concluir primeiro acesso, consultar o registro
histórico e corrigir administrativamente com versão/motivo. A matrícula automática
anterior fica livre. Não há necessidade de planilha, importação operacional ou backfill.

O importador existente permanece opcional e compatível: identityUserId explícito,
administrativelyConfirmed/sourceReference opaca, dry-run sem escrita e proof por
ator/payload de 15 minutos e local ao processo. Apply revalida cada unidade em sua
transação; conflitos não sobrescrevem titulares. 027/27 são duplicidade canônica.
Reprocessamento é idempotente; payload alterado exige novo dry-run. Reports por
índice/outcome não ecoam linhas pessoais. Apply continua false por padrão.

## Configuração e banco

enabled e approved continuam false. Initial-number, width e prefix não fazem parte
da nova política. Valores antigos explícitos impedem startup, mesmo disabled;
prefixo vazio é neutro. O Compose encaminha variáveis legadas para que não sejam
ignoradas silenciosamente. Removê-las antes da release, sem habilitar flags.

V10 faz reconciliação transacional das claims atuais/reservas e snapshots no audit;
preserva todas as auditorias e PK/FKs. Members/reservas não numéricos, zero,
colisões canônicas ou claim atual ausente bloqueiam antes de DML de negócio.
Hibernate continua validate. Não editar V1–V9, não usar repair/clean/baseline reset.
Nenhuma operação produtiva, concessão de grants ou ativação foi executada nesta rodada.
