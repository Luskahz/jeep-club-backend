# Relatório 2 — Histórico de persistência e deploy

Datas de merge abaixo em America/Sao_Paulo; execuções usam UTC explicitamente. Fontes: Git first-parent de master, PRs #85–#95 e [jobs públicos](https://github.com/Luskahz/jeep-club-backend/actions). Metadados de todas as nove execuções push/master e jobs estão em [master-workflows.json](master-workflows.json). Logs consultados para os deploys #87/#89/#93/#95; excertos mínimos em [deploy-confirmations.txt](deploy-confirmations.txt).

**Não foi possível identificar o commit do primeiro deploy manual aproximadamente em 02/10.** Nesse dia master transitou de `ef81689` (merge #83 em 01/10 21:19) para `e4aa00e` (20:55 de 02/10). Não há prova de qual foi implantado manualmente, nem de checkout de feature branch. Os dois têm o mesmo schema JPA; `e4aa00e` é uma referência reproduzível, não uma alegação de deploy inicial. #86 descreve stack VPS preexistente; não informa SHA inicial verificável.

| Data local | Commit/PR | Persistência e schema esperado | Evidência de deploy / classificação |
|---|---|---|---|
| 01/10 21:19 / 02/10 | `ef81689` #83 | Estado V1, 58 tabelas | CI apenas; primeiro deploy manual não verificável |
| 02/10 20:55 | `e4aa00e` #85 CORS | Mesmo V1 | Run 37079857968 só Maven verify; deploy não verificável |
| 07/10 10:15 | `c60a397` #86 Compose/VPS | Mesmo V1; volume mysql-data, `ddl-auto=update` | Run 37627035648 só verify; merge confirmado, deploy manual descrito sem SHA |
| 07/10 11:17 | `e9a2ad4` #87 autodeploy | Mesmo V1 | **Confirmado**: run 37635409005, job 112841549537; `Deployed commit e9a2ad4869e7a9c6f2b66216ee6be3b31e9d45f9` às 14:22:40Z, backend/MySQL saudáveis |
| 07/10 12:34 | `49024ed` #89 frontend gate | Mesmo V1 | **Confirmado: falhou**, run 37645002751; log retorna a e9a2ad4. Não considerar SHA implantado com sucesso |
| 07/10 12:54 | `724ecab` #90 mail health | Mesmo V1 | Run 37649279836 cancelado, nenhum job; não comprova deploy próprio |
| 07/10 13:05 | `ec7690d` #88 MySQL local | Mesmo V1 | **Confirmado**: job Deploy success run 37649374328 às 16:11:42Z; contém correção #90 |
| 07/10 21:20 | `bde973f` #93 develop | V2: adiciona identity_user_profiles; Billing/Health sem novo mapping | **Confirmado**: run 37707229805 job 113085384881, log `Deployed commit bde973f9a079cb5e6e6d60f2a2413f12cddef998` em 08/10 00:27:14Z |
| 13/10 13:44 | `6865bc4` #95 BACK-511 | V3: adiciona authentication_account_activation_invitations | **Confirmado**: run 38433026697 job 115421196435, log `Deployed commit 6865bc4f600462c5fcc6cf1212c9660591eff4db` às 16:49:44Z |
| 15/10 candidato | `9d3fba3` / #100 | Mesmo V3; #96/#97/#98/#99 | Não implantado por esta auditoria; PR #100 aberto |

Os jobs confirmam execução e saúde naquele instante. Com Hibernate update e backend saudável após #93/#95, é **provável** que ambas as tabelas já tenham sido criadas. Não comprovam tipos, índices, defaults, objetos residuais, conteúdo nem estado atual. Banco pode ter sido restaurado ou alterado depois. O log de 13/10 mostra MySQL `mysql:8.0` com seis dias de uptime; não revela patch/digest nem data original do volume.

Infraestrutura auditada: `.github/workflows/backend-ci.yml` verifica antes de SSH `deploy SHA`; Dockerfile Java 17; Compose mantém MySQL/arquivos em volumes; hook `/usr/local/sbin/jeep-club-deploy` faz backup e antigamente tentava rollback apenas da imagem. A chave GitHub tem forced command restrito a status/deploy, portanto não serve para consultas SQL arbitrárias. Cópias reais do hook, backup script e arquivos privados na VPS precisam ser comparadas aos versionados antes da adoção.
