# Relatório 7 — Decisão do PR #100

> Registro histórico anterior à execução por SSH. Para o estado vigente, consultar [relatório operacional 8](08-operational-adoption.md) e [runbook atualizado](06-vps-runbook.md).

**BLOQUEADO para promoção neste momento.** Não há prova suficiente de equivalência do MySQL real; a coleta SSH expirou antes da autenticação. Baseline/restore/ensaio da cópia real não foram executados. Flyway está numa branch específica e precisa ser validado e integrado a develop antes de #100 incorporá-lo. Esta auditoria não aprova nem integra #100.

Condições para avançar:

- PR da infraestrutura para develop revisado, migrations/verify/MySQL verdes e integração confirmada no head do #100, sem reescrever #98/#99.
- B coletado com integridade e comparação A/B/C concluída; versão de adoção comprovada por equivalência, sem drift inexplicado.
- Backup completo restaurado em laboratório; baseline/migrate/validate e smoke tests bem-sucedidos sobre cópia de B, schemas finais equivalentes e dados preservados.
- Plano e janela aprovados expressamente; hook real atualizado com proteção contra rollback inseguro; baseline ativa executada e validada quando autorizada.
- CI e compatibilidade reavaliados no commit exato de release após integração. Monitoramento e recuperação prontos.

Classificação intermediária possível: **APROVÁVEL APÓS AÇÃO OPERACIONAL** quando código estiver integrado/validado e restarem apenas ações operacionais identificadas e ensaiadas. **APROVÁVEL** somente com banco existente comprovadamente compatível e critérios concluídos. CI verde de fixtures não substitui evidência de B. Preservar PR #100 aberto até decisão humana.
