# BACK-409 — execução após auditoria

Início local: 04/11/2026, a partir de `develop@09dc0180`. A auditoria do PR #82
é baseline histórica. A descrição e os IDs das subtasks foram fornecidos pelo
solicitante; este documento não consulta nem altera o status administrativo no Jira.

## Situação das entregas

| Subtask | Evidência local nesta entrega | Estado técnico |
| --- | --- | --- |
| BACK-477 / PD-01 | [Registro de decisões e contratos](decisions.md) | Preparação iniciada; aprovações e atualização repo-wide pendentes |
| BACK-478 / PD-02 | Operação descrita como VPS/Docker/MySQL; provider KMS não confirmado | Pendente |
| BACK-479 / PD-03 | Nenhum AEAD/blind index compartilhado incorporado | Pendente |
| BACK-480 / PD-04 | Health e snapshots ainda sem cifra | Pendente |
| BACK-481 / PD-05 | Bloqueadores de substring/sort explicitados em D01 | Pendente |
| BACK-482 / PD-06 | Sem aplicação de cifra em Dependents/Vehicles | Pendente |
| BACK-483 / PD-07 | Sem aplicação de cifra em Events/Publications | Pendente |
| BACK-484 / PD-08 | Minimização de logs operacionais, caminhos e falhas; testes de não exposição | Incremento implementado; escopo completo ainda pendente |
| BACK-485 / PD-09 | Sem proteção criptográfica dos bytes | Pendente |
| BACK-486 / PD-10 | Sem nova mudança de configuração de produção/SMTP nesta entrega | Pendente |
| BACK-487 / PD-11 | Prazos por finalidade aguardam D02 | Pendente |
| BACK-488 / PD-12 | Sem acesso a base/infra ou execução de cutover | Pendente |

## Primeiro incremento de BACK-484

O contrato operacional detalhado e os limites estão no
[guia proprietário de logging](../../../src/main/java/com/jeepclub/backend/platform/logging/docs/README.md).
Remove nome do MDC/console, substitui a URI bruta não resolvida por marcador,
remove a duplicação de path de auditoria no log operacional e elimina mensagens,
causas e stack traces crus nos handlers/worker/compensação de comprovantes
identificados pela auditoria. Correlação HTTP e auditoria persistida continuam
disponíveis.

Os testes exercitam autenticação atual/legada, cleanup de MDC, caminhos com
identificadores/tokens e falhas sintéticas contendo CPF/e-mail/storageKey nas
mensagens e causas. Também verificam que o alvo obrigatório continua no evento
de auditoria e que a resposta inesperada continua genérica.

Não é o fechamento da BACK-484: alvos persistidos ainda precisam de proteção,
claims JWT e retenção precisam de decisão, e proxy/APM/SMTP/overrides de logging
exigem evidência por ambiente. Headers de correlação são entradas externas;
clientes devem usar IDs opacos e nunca dados pessoais em `X-Request-Id`.

## Gate da Story

Não fechar BACK-409 ou BACK-425 com este incremento. O fechamento exige os
aceites de BACK-477, KMS implementado e validado, proteção por módulo e bytes,
retenção, massa sintética sem plaintext residual, migrations/índices e os ensaios
operacionais de cutover/backfill/rollback/restore.

A política de schema atual já é Flyway com migrations imutáveis e Hibernate
validate. Evoluções devem seguir `docs/database/04-flyway-strategy.md`; os trechos
da auditoria que mencionavam Hibernate update descrevem a baseline antiga.

## Validação local — 04/11/2026

- Testes focados em logging, privacidade e lifecycle de comprovantes: passaram.
- `mvnw.cmd test -q`: exit 0; 2.585 testes sem falhas/erros. Após essa execução
  foi acrescentado o caso de URI privada na integração HTTP.
- `mvnw.cmd verify -q` no estado final: exit 0; 2.586 testes em 258 suites,
  nenhuma falha, erro ou teste pulado; JAR empacotado.
- `git diff --check`: sem erros de whitespace.

Ambiente local: Windows, JDK 26.0.2 disponível via NetBeans, compilação para
Java 17 conforme o POM. A execução no JDK 17 do CI continua sendo o gate do
ambiente alvo. Os logs completos ficam localmente em
`target/back-409-test.log` e `target/back-409-verify.log`; relatórios em
`target/surefire-reports`. Esses artefatos são gerados e não são entregues no Git.

Não foram executados os perfis opcionais MySQL/Postgres nem operações de
infraestrutura. Não houve alteração de schema neste incremento; os testes locais
não comprovam backfill, ausência de plaintext na base real ou conformidade da
Story inteira.
