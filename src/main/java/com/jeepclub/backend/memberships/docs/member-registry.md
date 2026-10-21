# Registro de associados — BACK-526

## Decisão técnica (BACK-527)

Member pertence a Memberships. User.id e Member.id são chaves técnicas imutáveis.
identityUserId é escalar, único e obrigatório. Matrícula é o identificador cadastral principal e visual do associado em todo o clube; é texto vigente, opcional
na leitura de uma Identity. A ausência nunca é substituída por um ID técnico.
Não há relacionamento JPA com entidades de outro contexto.

O registro de números retém permanentemente números atribuídos e reservas.
Retificação muda somente a matrícula vigente e acrescenta um evento de auditoria
na mesma transação. Números anteriores não podem ser transferidos nem reutilizados,
inclusive pelo mesmo associado. Uma correção de correção usa um número livre.
Não há operação de exclusão ou transferência nesta entrega (BACK-512 pendente).

Valores são preservados literalmente (1–64 caracteres, sem whitespace externo
ou controles). Não fazemos trim, case folding, padding ou conversão para número
na matrícula armazenada. Colunas de matrícula usam collation binária no MySQL.
Como a equivalência de 027 e 27 não foi homologada, o guard conservador recusa
coexistência de variantes compostas apenas de dígitos ASCII com o mesmo valor.
Isso protege uma ambiguidade sem afirmar que sejam a mesma matrícula de negócio.
O guard é uma chave SHA-256 de texto literal, ou de dígitos sem zeros iniciais.
Alterar essa política exige análise das claims persistidas e nova revisão.

Todas as escritas bloqueiam Identity pelo contrato público UserIdentityLock e
depois a linha singleton do registry. Esse mutex coordena criação, retificação,
reserva e emissão, inclusive quando Member ainda não existe. UNIQUEs e @Version
são proteções adicionais. A ordem é Identity → registry; reservas só usam registry.
O audit é append-only pela aplicação, não existe endpoint de edição/exclusão.
Eventos confirmados têm resultado SUCCESS; erros não confirmam alteração alguma.

## Associação e emissão (BACK-532/BACK-533)

A aprovação não cria Member. A emissão automática, quando explicitamente
habilitada, ocorre na conclusão do primeiro acesso da solicitação aprovada,
na mesma transação. Reenvio, expiração, rejeição e cadastro técnico não emitem
matrícula. Com emissão desabilitada, o onboarding termina normalmente e a
solicitação COMPLETED sem Member representa pendência para conciliação explícita.
Não há runner para retroativamente emitir matrículas nem backfill no Flyway.
BACK-523 pode cancelar uma aprovação pendente sem haver número consumido.

O contador persistido é independente de AUTO_INCREMENT. A configuração exige
habilitação, confirmação explícita de homologação, número inicial, largura e
prefixo (este pode ser vazio). Nunca infere MAX nem um último número histórico.
A primeira emissão inicializa o contador sob lock; o formato homologado fica
fixado junto ao contador e mudanças posteriores são recusadas. Números já
reivindicados são pulados, e rollback reverte o avanço e a criação do associado.

## Pendências de negócio

O clube deve homologar formato, equivalência de zeros/case, reservas, lacunas,
faixa inicial e momento formal de associação antes de ativar emissão. O ponto
técnico implementado é conclusão do primeiro acesso, sujeito a essa homologação.
Não há valor de produção predefinido. Não foi consultada nem alterada a VPS.

## Matriz de conflitos

| Situação | Resultado |
| --- | --- |
| Mesma identidade, mesma matrícula vigente | Repetição idempotente, sem novo audit |
| Mesma identidade, matrícula diferente em atribuição | 409, usar retificação |
| Número vigente, antigo, reservado ou variante numérica ambígua | 409 |
| Versão divergente na retificação | 409 |
| Identity inexistente | 404 |
| Nova atribuição para Identity desativada | 409 |
| Entrada inválida ou justificativa ausente | 400 |
| Emissão desabilitada | Nenhuma criação automática |

As queries públicas não têm cache: leitura seguinte ao commit resolve a matrícula
vigente. Listagens resolvem um conjunto de IDs por consulta, sem lookup por linha.


## Modelo físico e contratos

A V4 cria `membership_members` (PK técnica, identidade única, matrícula vigente
única, timestamps e version), `membership_number_claims` (chave de proteção única,
número literal único, Member opcional, ator, instante e motivo),
`membership_member_audits` (Member, de/para, ator, instante, motivo, operação e
resultado) e `membership_registry_sequence` (singleton, próximo valor e assinatura
de formato). A linha coordenadora começa sem faixa inicial e sem número emitido.
As FKs físicas impedem apagar Member com histórico/claims e Identity vinculada.
São colunas escalares no JPA, sem import de entities de outros contextos.
A V4 também amplia o ENUM de permission, preservando a ordem dos códigos antigos.

`MemberRegistrationQuery` publica leitura por Identity, lote de até 500 IDs e
pesquisa reversa literal da matrícula vigente. Seu resultado é uma projeção
imutável, sem entity/JPA. A implementação depende somente do repository do registry.
`MemberIdentityPort` usa UserIdentityLock e UserQuery por adapter de integração.
A SPI de apresentação de Identity é implementada em Platform por composição da
query do registry. Isso evita que um serviço de Identity dependa de um service
Membership que por sua vez dependa do mesmo serviço de Identity.

## Adaptação do frontend

O ID técnico continua disponível para operações do sistema, mas a matrícula
vigente é o identificador cadastral exibido ao usuário. Exemplo ilustrativo:

```json
{"id":108,"membershipNumber":"027","name":"Associado sintético"}
```

Mostre `Matrícula 027`, mantendo `id=108` para os paths/requests já existentes.
Se não houver Member, mostre ausência de matrícula. Nunca use o ID como fallback.
Após retificação, releia o recurso: perfil, detalhe, listagem e exportação de
Identity compõem a matrícula atual. A API não altera JWT ou IDs para fazer isso.
`CurrentUserResponseDTO` e `UserProfileResponseDTO` retornam null; o DTO admin
omite valores null por sua política NON_NULL preexistente. A coluna Matrícula
fica vazia no CSV/PDF quando ausente.

A listagem de Identity mantém filtros e paginação (20 por padrão, máximo HTTP 50).
Sem `fields`, todos os campos, incluindo matrícula, são considerados.
`fields=MEMBERSHIP_NUMBER` ou `fields=NAME,MEMBERSHIP_NUMBER` recupera somente as
colunas solicitadas e o ID auxiliar interno, sem expor `id` no JSON. `fields=ID,NAME`
omite matrícula intencionalmente. O lookup de matrículas é único por lote; não
busca entidades completas nem consulta por linha. Ausências continuam omitidas.

`membershipNumber=027` resolve o vínculo vigente exato pela query API pública de
Membership, aplica o Identity ID como predicado adicional e combina com todos os
filtros cadastrais antes de contar/paginar. Número ausente produz página vazia.
`q` preserva a busca textual existente e inclui OR com o vínculo de matrícula
vigente exata. Números antigos não encontram o associado pelo registry.
`sort=membershipNumber` em Identity retorna 400: não se ordena uma página em memória.
A listagem própria de associados permite essa ordenação global no banco.

CSV/PDF padrão usam `layout=MEMBER`: Matrícula primeiro, Nome e cadastro em seguida,
ID técnico na última coluna para operações administrativas. `layout=TECHNICAL`
preserva exatamente os cabeçalhos e a ordem anteriores (`ID;Matrícula;Nome;...`);
nesse contrato explícito de integração, ID significa chave técnica. Consumidores
que dependem do CSV anterior devem solicitar TECHNICAL. Ambos resolvem matrícula
vigente dinamicamente; ausência é coluna vazia. Não são reemitidos documentos antigos.
Os exports usam lotes de 200. Registros financeiros e documentos históricos não
são reescritos. Requests e IDs de Billing/Authentication/Authorization permanecem
intactos. Nenhum DTO anterior anunciava um campo de matrícula próprio nesses
contextos; o escopo de apresentação é o cadastro/perfil e sua exportação.

Os contratos HTTP completos são gerados no OpenAPI sob a tag Membership Registry.
As novas rotas estão em `/admin/members`, com permissions por operação. Todas as respostas desse controller
possuem Cache-Control: no-store. Listagens usam PageResponse, tamanho padrão 20 e máximo 50,
ordenação permitida por id, identityUserId, membershipNumber, createdAt e updatedAt.
O histórico é apresentado pelo ID do evento em ordem decrescente. Pesquisa por
matrícula é literal e vigente, sem procurar automaticamente no histórico.

Atribuição individual (PUT por identityUserId):

```json
{"membershipNumber":"027","reason":"Conferência com fonte oficial validada"}
```

Retificação (PATCH do mesmo vínculo):

```json
{"membershipNumber":"037","expectedVersion":0,"reason":"Correção cadastral conferida"}
```

A mesma atribuição vigente repetida retorna o registro sem novo evento. Uma
retificação exige a versão recebida no último GET e justificativa. Em conflito,
releia e revise a decisão. Números retirados não retornam ao conjunto disponível.
Sucessos persistem outcome SUCCESS. Falhas recusadas não produzem mudança parcial
nem evento de alteração fictício; os outcomes do request seguem o contrato HTTP
sanitizado. A API não oferece edição ou exclusão do histórico.

## Conciliação e importação — BACK-534

Uma fonte oficial deve ser conferida por pessoa autorizada. O contrato recebe
vínculos explícitos, sem matching aproximado e sem deduzir matrícula de CPF ou ID.
`administrativelyConfirmed=true` é a declaração explícita de conferência humana,
feita pelo ator autenticado com acesso à fonte oficial. O backend valida somente
integridade técnica, autorização, existência/vínculo e conflitos; não consulta nem
comprova uma fonte oficial externa. O antigo campo `verified` não é o contrato vigente.
`sourceReference` identifica versão/lote/linha da evidência, sem dados pessoais;
aceita referência opaca ASCII `[A-Za-z0-9][A-Za-z0-9._:/-]{0,99}`.
Divergências devem permanecer não confirmadas; ausência da declaração é 400; false produz UNVERIFIED,
sem aplicação. Referência alterada, declaração, motivo, linhas ou ator diferentes
invalidam o comprovante da simulação.

```json
{
  "rows":[{"identityUserId":108,"membershipNumber":"027","administrativelyConfirmed":true,
           "sourceReference":"fonte-homologada-linha-12"}],
  "reason":"Conciliação aprovada"
}
```

Envie primeiro ao dry-run. A simulação não escreve no banco. O relatório contém
índice zero-based da linha e outcome, sem CPF/e-mail ou eco de linha pessoal.
Possíveis outcomes: READY, ALREADY_LINKED, UNVERIFIED, DUPLICATE_SOURCE,
IDENTITY_NOT_FOUND, IDENTITY_INACTIVE, IDENTITY_ALREADY_LINKED e NUMBER_UNAVAILABLE.
Para aplicar, reenvie a fonte e motivo exatos com o `dryRunToken` recebido.
O comprovante HMAC é vinculado ao ator, expira após 15 minutos e é local ao
processo. Restart ou outra instância exige novo dry-run (em múltiplas instâncias,
encaminhar a operação para a mesma instância). Não é credencial nem substitui RBAC.

A aplicação revalida o estado: cada atribuição usa o mesmo serviço de gestão, JPA,
lock, claim e audit em uma transação. O motivo persistido inclui a referência da
fonte. Uma linha em conflito não força sobrescrita. Outras linhas válidas podem
confirmar. Outcomes de aplicação incluem APPLIED, ALREADY_LINKED, CONFLICT,
NOT_FOUND e FAILED; este último exige investigação sanitizada antes de repetir.
Reenvio do mesmo lote é idempotente. Para repetir só pendências, monte um subconjunto
conferido e faça novo dry-run. Nenhuma fonte real foi importada nesta implementação.

## Autorizações separadas e habilitação da importação

| Permission | Operações |
| --- | --- |
| MEMBERSHIP_MEMBER_READ | Listagem, pesquisa vigente e consulta de vínculo |
| MEMBERSHIP_MEMBER_REGISTRATION_MANAGE | Atribuição manual e retificação |
| MEMBERSHIP_MEMBER_AUDIT_READ | Histórico de alterações |
| MEMBERSHIP_MEMBER_NUMBER_RESERVE | Reserva permanente |
| MEMBERSHIP_MEMBER_NUMBER_ISSUE | Emissão administrativa |
| MEMBERSHIP_MEMBER_IMPORT | Dry-run e apply de reconciliação |

Não existe MANAGE global no controller. Cada método usa PreAuthorize e a mesma
RequiredPermission no OpenAPI. ROOT recebe o catálogo pelo bootstrap estrutural;
CUSTOM exige concessão explícita. MANAGE não inclui IMPORT, READ não permite
escrita, IMPORT não permite reserva/emissão. O número em DTOs Identity continua
protegido pelas permissions existentes de Identity, sem exigir novo READ de Membership.

`memberships.registry.imports.apply-enabled=false` é o default. No Compose,
`MEMBERSHIPS_REGISTRY_IMPORTS_APPLY_ENABLED=false`. IMPORT sozinha não libera
carregamento: apply também exige habilitação operacional explícita, conferência
humana, fonte e comprovante exatos. Desabilitada retorna 409
MEMBER_REGISTRY_IMPORT_DISABLED antes de qualquer escrita. Dry-run continua disponível.
Habilitar exige autorização separada, fonte homologada, backup/restore e revisão
sanitizada da simulação; ao concluir a janela, desabilitar novamente. Nenhum hook
habilita importação nem executa dados no startup.

## Compatibilidade com lifecycle de Identity

Disable/enable e mudanças de nome/e-mail/CPF preservam Member, matrícula, claims e
auditoria. Reativação não emite outro número. A FK não possui ON DELETE CASCADE;
MySQL recusa exclusão física de Identity vinculada e mantém registros intactos.
No código atual não há endpoint de exclusão física de Identity, portanto não há
rota pública nova convertendo uma falha SQL em 500. BACK-512 deverá definir
arquivamento/exclusão e tradução controlada para 409 antes de expor essa operação.
Não há exclusão de Member, transferência de número ou nova exceção de autorização
por ser associado. Claims antigos/reservas não são liberados por lifecycle.

## Configuração da emissão

Propriedades Spring sob `memberships.registry.issuance`:

| Propriedade | Default / requisito |
| --- | --- |
| enabled | false |
| approved | false, exige homologação para habilitar |
| initial-number | ausente; número positivo explicitamente aprovado |
| width | ausente; 1–64, limitada com o prefixo ao comprimento máximo |
| prefix | ausente; explicitamente configurado, pode ser vazio |

Os nomes de ambiente do Compose usam `MEMBERSHIPS_REGISTRY_ISSUANCE_` seguido de
ENABLED, APPROVED, INITIAL_NUMBER, WIDTH e PREFIX. `.env.example` não fornece faixa
arbitrária. Configuração habilitada incompleta/ilegítima impede startup com erro
explícito, sem emitir número. Desabilitada permite startup e onboarding normais.
A assinatura de configuração fixa início/formato após o primeiro uso; mudar o
formato de contador já iniciado é conflito, exigindo revisão operacional.

O lock permite pular claims históricas e reservas. Cada alocação faz no máximo
10.000 tentativas, falhando sem escrita se o intervalo estiver excessivamente
ocupado ou houver overflow de long. Não há reutilização nem inferência MAX+1.

## Rollout e dependências — BACK-536

1. Revisão humana e CI do PR para develop, sem merge nesta tarefa. Schema e código
   podem ser integrados em develop sem deploy. O merge futuro em master é outra
   autorização e executará o pipeline produtivo.
2. O agente DevOps deve conferir SHA/release exatos, baseline V3, V1–V3 imutáveis,
   ausência de V4 concorrente, backup SQL/uploads e restauração isolada comprovada.
   Deve ensaiar a V4 sobre cópia autorizada equivalente à base ativa, conferir
   dados de Identity e MembershipApplication antes/depois e manter emissão false e imports.apply-enabled false.
3. Após futura instalação: Flyway history sem falhas, Hibernate validate, health,
   dados antigos preservados, zero backfill e APIs de autenticação/Billing normais.
   Verificar o ENUM de permissions e a concessão por role, sem exceções pessoais.
4. Conciliação histórica é fase separada, com fonte oficial e autorização própria.
   Inventariar usuários por categoria de forma sanitizada. O snapshot agregado de
   712 linhas em 61 tabelas não é uma contagem de usuários.
5. Só depois da homologação, aprovar a faixa e reservas, conferir simulações,
   preparar configuração e habilitar emissão. Contas COMPLETED sem matrícula são
   conciliadas explicitamente; não há catch-up automático de startup.

DDL MySQL pode confirmar parcialmente. Em falha, preservar volume, logs privados,
history, release e backup, diagnosticar o DDL aplicado e ensaiar recuperação em
instância separada. Não usar clean, repair, baseline automática, Hibernate update
ou prometer rollback ACID do schema. Não reativar imagem antiga sobre schema
incompatível automaticamente. A restauração real da VPS e autorização de release
pertencem ao futuro agente DevOps, não foram executadas aqui.

BACK-512 deverá definir lifecycle/arquivamento e políticas de retenção antes de
liberar remoção de identidades vinculadas. BACK-523 poderá evoluir rejeição após
aprovação enquanto primeiro acesso estiver pendente; como este momento ainda não
emite matrícula, a correção não libera nem consome número indevidamente. O
registry não introduz novo gate de membritude, autenticação ou Billing.
