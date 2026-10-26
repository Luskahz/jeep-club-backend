# ADR BACK-526 — matrícula independente e governança

Data: 25/10/2026. Estado: decisões técnicas implementadas; homologação de negócio
pendente. Este documento não representa aprovação do clube nem autoriza ativação.

## Identificadores e fonte de verdade

| Identificador | Proprietário | Semântica |
| --- | --- | --- |
| Identity.User.id | Identity | PK técnica imutável; autenticação, JWT, referências e contratos existentes |
| Membership.Member.id | Memberships | PK técnica imutável do cadastro de associado |
| Member.identityUserId | Memberships → contrato público Identity | Vínculo único, escalar, com identidade existente |
| Member.membershipNumber | Memberships | String de negócio vigente, exibida como matrícula |
| MembershipApplication.id | Memberships | Processo de admissão; não é matrícula |

Uma Identity pode não ter Member. Nenhum ID técnico serve como fallback visual,
fonte de numeração ou critério isolado de reconciliação. Não se muda PK, FK,
AUTO_INCREMENT de Identity, JWT, credencial, role ou cobrança para corrigir matrícula.

## Política conservadora vigente

Armazenar literalmente 1–64 caracteres, sem espaços externos nem controles.
Não aplicar trim, case folding, padding ou conversão numérica ao valor recebido.
MySQL usa collation binária. A pesquisa encontra apenas a matrícula vigente exata.

Para colisões, strings compostas exclusivamente por dígitos ASCII têm uma claim
canônica sem zeros iniciais: `027` e `27` não coexistem. O valor exibido continua
`027`. Isso é uma proteção contra ambiguidade, não uma homologação de equivalência.
Valores `A-027` e `a-027` são distintos na regra técnica atual; sua aceitação como
formato oficial permanece pendente. A chave de claim é SHA-256 do literal ou dos
dígitos canônicos. Alterar equivalência exige análise das claims existentes e,
se necessária, evolução versionada do schema; nunca editar V4 aplicada.

Claims atribuídas, retiradas e reservadas são permanentes. Reserva não cria Member
e não pode ser convertida em atribuição pela API atual. Não há transferência,
liberação ou reutilização, nem para o mesmo associado. Uma retificação errada
exige novo número livre e justificativa; não se apaga histórico para desfazê-la.

## Transições e conflitos

| Origem / comando | Resultado técnico | Número e auditoria |
| --- | --- | --- |
| Identity ativa sem Member + atribuição explícita | Cria Member com identidade existente | Claim + ASSIGN na mesma transação |
| Mesmo vínculo e número + atribuição repetida | Retorna Member atual | Sem novo audit/consumo |
| Mesmo vínculo + outro número em atribuição | 409 | Usar retificação, não sobrescrever |
| Member + retificação, versão atual e número livre | Mantém ambas PKs; troca matrícula vigente | Claim anterior mantida, claim nova + CORRECT |
| Retificação com versão antiga | 409 | Nenhuma alteração parcial |
| Número vigente/retirado/reservado/variante numérica ocupada | 409 | Nunca toma número de outra pessoa |
| Reserva de número livre | Sem Member | Claim sem memberId + RESERVE |
| Identity inexistente | 404 | Sem criação de Identity ou claim |
| Nova atribuição/emissão para Identity inativa/arquivada | 409 | Sem novo vínculo |
| Retificação de Member já existente e Identity retida | Permitida com permission/versão/motivo | Não reativa Identity; conserva histórico |
| Disable/archive de Identity com Member | Preserva Member e claims | Revogação/estado seguem BACK-512 |
| Unarchive/enable | Preserva matrícula atual | Não emite segundo número |
| DELETE de Identity vinculada ou ator com referências retidas | 409 pelo guard de lifecycle | FK restritiva como última defesa; sem cascade |
| Aprovação temporária ou access link | APPROVED, Identity/Authentication pendentes | Sem Member automático |
| Primeiro acesso de aplicação aprovada, emissão habilitada | COMPLETED + Member | GENERATE atômico com credencial/processo |
| Primeiro acesso com emissão false | COMPLETED sem Member | Pendência de conciliação explícita; login permitido |
| Reenvio/expiração/rejeição antes do primeiro acesso | Mantém pendência ou reverte provisioning conforme BACK-523 | Sem emissão/consumo |
| Primeiro acesso e rejeição concorrentes | Serialização por Identity; um desfecho válido | Se acesso vence, rejeição não apaga Member; se rejeição vence, sem emissão |
| ROOT/técnico/legado criado fora de aplicação aprovada | Sem emissão automática por existir User ou fazer login | Sem Member obrigatório para acessar contratos atuais |

Cadastro administrativo histórico é uma decisão explícita de um operador com
permission; o backend não consegue reconhecer sozinho se uma conta técnica
também representa associado antigo. O gerador administrativo permanece uma
operação explícita com NUMBER_ISSUE, reason, faixa homologada e flag; não é um
classificador automático de elegibilidade por role/CPF. Não se inventa privilégio
ou proibição por nome de pessoa. Qualquer regra adicional exige decisão do clube.

## Coordenação e fronteiras

Todas as escritas de Registry usam Identity → mutex singleton do Registry;
reserva só precisa do mutex. Leituras bloqueadas renovam snapshots JPA. Counter,
Member, claim e audit confirmam juntos; falha desfaz todos. UNIQUE e @Version
complementam os locks. O contador não consulta MAX e não deriva de PKs.
Retries do mesmo vínculo retornam o Member existente sem segunda emissão.

Memberships consome UserIdentityLock/UserQuery por MemberIdentityAdapter.
Authentication chama MembershipOnboardingCompletion, contrato público; access
link e senha temporária participam da transação de primeiro acesso. Authorization
continua proprietário de roles/permissions. Identity compõe a matrícula via
UserMembershipNumberProjection e adapter Platform da MemberRegistrationQuery,
sem importar entity/repository privado. Billing mantém referências por userId.

Queries seguintes ao commit leem a matrícula atual; lote até 500 IDs sem cache.
Perfis, detalhe administrativo, fields, pesquisa e CSV/PDF são as superfícies de
apresentação do backend. Listagens e exports usam lote; IDs técnicos permanecem
nos contratos. A homologação de telas do frontend é evidência externa a este repo.

## Autoridade administrativa

| Permission explícita | Operação |
| --- | --- |
| MEMBERSHIP_MEMBER_READ | Consulta/lista/pesquisa vigente |
| MEMBERSHIP_MEMBER_REGISTRATION_MANAGE | Atribuição histórica e retificação |
| MEMBERSHIP_MEMBER_AUDIT_READ | Histórico |
| MEMBERSHIP_MEMBER_NUMBER_RESERVE | Reserva permanente |
| MEMBERSHIP_MEMBER_NUMBER_ISSUE | Geração administrativa configurada |
| MEMBERSHIP_MEMBER_IMPORT | Dry-run/apply, condicionado também à flag de apply |

ROOT recebe o catálogo estrutural existente. Roles CUSTOM não recebem grants
implícitos. O clube deve designar responsáveis e conceder permissões explícitas;
nenhuma exceção pelo nome Matheus ou por identidade pessoal. Motivos e referências
devem ser opacos, sem CPF/e-mail/nome de associado. O sistema persiste ator, instante,
operação, valores anterior/novo e motivo; não registra senha/token no audit.

## Decisões externas bloqueantes

| Decisão / evidência | Responsável pela aprovação | Impacto enquanto pendente |
| --- | --- | --- |
| Formato oficial, zeros/case e variantes históricas | Responsável formal do clube | Manter guard conservador; não normalizar legado automaticamente |
| Faixa inicial, prefixo/largura, reservas, saltos e lacunas | Clube com operação responsável | Emissão false e approved false; nenhum início presumido |
| Marco formal de associação, inclusive ambos primeiros acessos | Clube | Ponto técnico implementado é primeiro acesso; ativação depende de homologação |
| Fonte histórica versionada, conferência e divergências resolvidas | Responsável autorizado pelo clube | Imports apply false; dry-run sintético não comprova fonte real |
| Administradores/grants e autorização de janela | Clube e operação | Permission técnica não substitui autorização operacional |
| UI, ausência de fallback e apresentação pós-retificação | Responsável pelo frontend/clube | Contrato backend disponível; homologação visual não presumida |
| Backup, restauração representativa e assinatura das fases | Operação autorizada | NO-GO operacional até comprovação |

Não há aprovação desses itens anexada à auditoria. BACK-527 e a Story permanecem
abertas. A aprovação técnica de um PR ou CI verde não substitui homologação.
