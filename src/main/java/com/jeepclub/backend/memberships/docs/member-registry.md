# Registro de associados — BACK-526

## Decisão técnica (BACK-527)

Member pertence a Memberships. User.id e Member.id são chaves técnicas imutáveis.
identityUserId é escalar, único e obrigatório. Matrícula é texto vigente, opcional
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
