# Membership Registry — V10 numérica

Política homologada em 26/10/2026; V4 criou o Registry, V5/V6 integraram lifecycle
e reversão, e V10 reconcilia a nova política sobre V1–V9 imutáveis.

Não há conversão de tipo/PK/FK: membershipNumber continua VARCHAR(64) e string HTTP,
decimal canônico positivo de 1 a 10^64−1. Dados são pré-validados antes de DML:
Members e reservas devem ser numéricos/positivos, sem colisão canônica; cada Member
precisa de claim atual correspondente. Falha recusa sem modificar dados de negócio.

V10 copia todas as claims, com metadados originais, para LEGACY_CLAIM_SNAPSHOT no
audit existente, antes de remover claims retiradas. Canonicaliza vigente/reservas
e suas chaves SHA-256 numeric:, avança version dos Members cujo literal mudou e
zera campos legados next_value/format_signature. Singleton permanece como mutex.
Auditorias anteriores e evidências históricas opacas não são alteradas ou apagadas.

Retificação futura libera a claim antiga na mesma transação de Member + audit.
O máximo é determinado somente por Members vigentes via CAST DECIMAL(65,0) e
FOR UPDATE; cálculo BigInteger. Não usar MAX lexicográfico ou histórico/contador.
Claims sem Member são reservas explícitas. Identity/Authentication/Authorization/
Billing não recebem backfill nem alterações de identificadores.

Teste deve cobrir banco vazio, upgrade V9→V10 com claims antigas e rejeição segura
de dados incompatíveis, além de Hibernate validate. Não editar migrations aplicadas,
não usar clean/repair/baseline reset, nem executar dados reais nesta missão.

[ADR](../adr/back-526-member-registry.md) · [contrato](../../src/main/java/com/jeepclub/backend/memberships/docs/member-registry.md) ·
[auditoria e evidências atuais](../back-526-numeric-policy.md) · [runbook](../back-526-runbook.md).
