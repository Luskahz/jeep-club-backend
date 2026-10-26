# Membership Registry após release #112 — BACK-528 / BACK-536

Estado revisado em 25/10/2026. V4 faz parte da release #112, cujo código contém
migrations V1–V9. V4 criou quatro tabelas do Registry e ampliou o catálogo de
permissions. Não recriar nem alterar V4 ou qualquer migration aplicada. A revisão
de finalização não muda schema; V10 ou posterior só se uma necessidade real surgir.

Apenas a linha de coordenação começa sem próximo número; não há backfill ou
alteração das PKs de Identity. Members existentes, se houver uso autorizado,
continuam válidos. A emissão e o apply da importação são false por padrão.

Memberships usa JPA, locks, claims permanentes e @Version. Flyway versiona DDL;
Hibernate valida o schema. FKs não fazem cascade. BACK-512 já protege DELETE de
Identity com Member/referências retidas via guard e conflito 409. BACK-523 já
serializa reversão pendente e primeiro acesso sem apagar matrícula existente.

- [Contrato do módulo](../../src/main/java/com/jeepclub/backend/memberships/docs/member-registry.md).
- [ADR e conflitos](../adr/back-526-member-registry.md).
- [Auditoria e evidências atuais](../back-526-finalization.md).
- [Runbook de três fases e recuperação](../back-526-runbook.md).
- [Evidências históricas de #104](../back-526-validation.md).

Na janela autorizada, reconfirmar deployed SHA, history/checksums e schema reais;
não assumir baseline V3 a partir de documentação antiga. Validar backup e
restauração isolada representativa antes de mutação. Testcontainers MySQL 8.0.46
com fixtures sintéticas prova comportamento técnico, não restaurabilidade da VPS.
Nenhuma operação produtiva é autorizada ou executada por esta revisão.
