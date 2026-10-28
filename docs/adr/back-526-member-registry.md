# ADR BACK-526 — matrícula numérica vigente

Decisão de negócio homologada pela solicitação de 26/10/2026. Substitui a política
conservadora de #104/#113. Aprovação da regra não autoriza implantação nem flags.

## Identificadores e representação

Memberships é proprietário de Member e membershipNumber. Identity conserva User
e sua PK. Member.id, User.id e identityUserId não mudam por retificação; JWT,
credenciais, roles e referências financeiras continuam usando os contratos atuais.
Uma Identity pode não ter Member; não há gate novo de login ou fallback para ID.

membershipNumber continua String no Java/JSON e VARCHAR(64) no MySQL. Representa
decimal ASCII positivo canônico, de 1 a 10^64−1: `027` é normalizado para `27`.
Zero, sinais, prefixos, espaços, frações, expoentes, dígitos Unicode e entradas
maiores que 64 caracteres são 400. A capacidade vem da coluna existente, sem
limite baseado no número de associados e sem perda de precisão em JavaScript.

## Alocação e concorrência

Sob Identity → singleton do Registry, a consulta nativa ordena
`CAST(membership_number AS DECIMAL(65,0)) DESC LIMIT 1 FOR UPDATE`. Ela lê o maior
número **vigente**; BigInteger calcula +1, ou 1 sem Members. Não lê audit, IDs,
claims históricas, next_value ou format_signature. Não usa comparação lexicográfica.
Reservas explícitas a partir do candidato são puladas sem liberar reservas.

FOR UPDATE é leitura corrente no REPEATABLE READ do MySQL, inclusive após esperar
o mutex com snapshot anterior. Todas as escritas existentes continuam usando esse
coordenador; UNIQUE de número/Identity/claim e @Version complementam o lock.
Nenhuma outra matrícula é renumerada. H2 não substitui a prova MySQL de snapshot.

Exemplo: 200 e 9999 vigentes → próximo 10000. Corrigir 9999 para 150 antes da
emissão → próximo 201. Se 10000 já pertence a outro Member, permanece intacto e
o próximo é 10001. Não há contador crescente independente do estado atual.

## Ocupação, reserva e histórico

Claim com memberId é ocupação atual; claim sem memberId é reserva explícita.
Retificação exige Identity/Member, permission REGISTRATION_MANAGE, justificativa
e versão. Reivindica destino livre, atualiza Member, remove somente a claim
anterior do mesmo Member e acrescenta CORRECT, em uma transação. Falha reverte
tudo. Exemplo: 20→150 libera 20 para outro associado; o histórico do primeiro
continua com ASSIGN 20 e CORRECT 20→150. Não existe edição/exclusão de auditoria.

| Situação | Resultado |
| --- | --- |
| Atribuição repetida da mesma matrícula canônica | Idempotente, sem novo audit |
| Outra matrícula em PUT para identidade já vinculada | 409; usar PATCH |
| Destino vigente ou reservado | 409 |
| Destino usado apenas no passado e livre agora | Permitido |
| Versão antiga | 409, inclusive na correção repetida |
| Identity/Member ausente | 404 |
| Nova atribuição a Identity inativa/arquivada | 409 |
| Intervalo automático esgotado | 409, sem escrita |
| Entrada inválida / ausência de autenticação / permission ausente | 400 / 401 / 403 |

Permissions existentes permanecem separadas: READ, REGISTRATION_MANAGE,
AUDIT_READ, NUMBER_RESERVE, NUMBER_ISSUE e IMPORT, sob MEMBERSHIP_MEMBER_.
CUSTOM não recebe grants implícitos. Não há privilégio por nome de pessoa.

## Onboarding, legado e lifecycle

Aprovação cria Identity/Authentication pendente, sem Member automático. Primeiro
acesso por link ou senha temporária conclui a aplicação e emite na mesma transação
somente quando habilitado. Reenvio/expiração/retry não duplicam Member. Com flag
false, primeiro acesso e login continuam funcionando sem matrícula.

BACK-523 permanece: PENDING pode ser rejeitado; APPROVED somente pode ser revertido
antes do primeiro acesso e se todos os guards permitirem. COMPLETED não é reversível.
BACK-512 continua responsável por inativação/arquivamento/exclusão segura. Member,
audit e referências como ator impedem exclusão incompatível; números vigentes de
identidades inativas/arquivadas continuam atribuídos e participam do máximo.

O fluxo histórico homologado é cadastro → primeiro acesso → número automático →
consulta humana da matrícula histórica → PATCH administrativo. Importação em lote,
planilha e backfill não são pré-requisitos de aceite dessa funcionalidade manual.
O importador existente continua opcional, com dry-run/proof e apply false por padrão.

## Upgrade e configuração

V10 preserva V1–V9 e as PK/FKs. Preflight recusa Members/reservas inválidos,
colisões canônicas e Member sem claim atual. Copia toda metadata das claims para
LEGACY_CLAIM_SNAPSHOT no audit existente, libera claims retiradas, canonicaliza
vigentes/reservas e invalida versões de Members cujo literal mudou. Auditorias
anteriores e literais históricos opacos são preservados verbatim.

Singleton permanece como mutex; next_value/format_signature legados são limpos,
mas suas colunas ficam para compatibilidade de schema. enabled/approved continuam
false. initial-number/width/prefix foram aposentados: configuração explícita
antiga falha no startup, mesmo disabled (prefixo vazio é neutro). Não há faixa
inicial para configurar. Ver runbook para preflight/backup/recuperação.

Pendências externas são operacionais: release autorizada, backup/restauração
representativa, preflight da base real e ativação de flags. Formato, início,
reutilização e marco formal foram homologados; não permanecem bloqueios de negócio.
