# Relatório 6 — Plano operacional (NÃO EXECUTADO)

Este runbook prepara operações; não autoriza executá-las no banco ativo. Coleta somente de leitura foi autorizada. Baseline, migrations, instalação do hook, edição de configuração, interrupção/restart, restauração ou troca de tráfego aguardam confirmação expressa do responsável. Nunca `docker compose down -v`, reset, clean ou repair.

## Pré-requisitos e coleta

1. Restabelecer SSH por rede autorizada; confirmar host/IP, porta e chave do host. Conferir SHA em /var/lib/jeep-club-deploy/current-commit, git HEAD e imagem/digest do backend. Último deploy remoto comprovado: 6865bc4 em 13/10, não confirmação do estado atual.
2. Executar audit-schema.sh (relatório 3). Guardar resultados privados com hashes. Confirmar MySQL patch/digest, sql_mode, charset/collation, database da aplicação, profiles e DDL efetivo; conferir volumes mysql-data/uploads e mountpoints sem divulgar env privado.
3. Revisar cópias reais de /usr/local/sbin/jeep-club-deploy e jeep-club-backup; o código Git não prova que arquivos externos coincidem. Identificar scheduler de backup, retenção, espaço livre e concorrência com deploy (mesmo flock /var/lib/jeep-club-deploy/deploy.lock).

## Backup e ensaio isolado

4. Reservar janela de manutenção. Bloquear tráfego e parar workers/backend durante o backup final e corte; registrar início e plano de comunicação. Não presumir consistência entre arquivos/uploads e SQL com aplicação escrevendo.
5. Backup lógico completo do database, incluindo triggers/routines/events, com --single-transaction --no-tablespaces --set-gtid-purged=OFF e sem senha em argumentos; confirmar InnoDB e ausência de DDL concorrente. Usar MYSQL_PWD somente dentro do container e saída para diretório privado umask 077. Guardar dump estrutural, contagens, SHA256, imagem anterior, configuração .env, hook, certificados/config proxy e uploads em backup protegido; não publicar dump/segredos em CI/Git/chat.
6. Restaurar primeiro em **instância isolada**, mesma imagem MySQL/digest e configurações de B, com volume e rede próprios (nenhuma referência a mysql-data/uploads reais). Conferir exit codes, hashes, tabela por tabela/contagens, FKs/órfãos, dados críticos privadamente e leitura pelo backend. Só backup restaurado com sucesso conta como recuperável. Não confundir validação SHA256 com restauração testada.
7. Criar também bancos referência V1/V2/V3 pela cadeia Flyway: para uma referência vazia, migrate -target=1, 2 ou 3. Coletar metadados pelo script com MYSQL_CONTAINER do laboratório. Comparar B restaurado contra cada referência usando compare-schema.py. Mesma collation/versão evita diferenças artificiais. Documentar quaisquer diferenças do Hibernate e objetos extras. Se nenhuma referência corresponder, PARAR: plano corretivo incremental e nova revisão; não forçar baseline.

## Ferramenta administrativa e baseline experimental

8. Usar Flyway CLI **11.14.1**, verificado da distribuição oficial. CLI não inclui Connector/J MySQL por licença: instalar `com.mysql:mysql-connector-j:9.6.0` na pasta drivers e confirmar `flyway info` antes do ensaio. Não depender do fallback MariaDB. Configuração privada fora do repo, modo 0600, URL do laboratório e user/password provisionados por canal privado. Locations aponta à cópia imutável dos SQL desta release. Não passar segredo em flags nem ativar debug/set -x. A documentação oficial explica [driver e módulo MySQL](https://documentation.red-gate.com/flyway/reference/database-driver-reference/mysql).
9. Após equivalência estrutural integral e backup/restore comprovados, registrar a versão correspondente **na cópia**, nunca no ativo nesta fase. Exemplo abaixo usa V3 somente se B corresponder a V3. Se corresponder a V1/V2, usar exatamente 1/2 e depois aplicar incrementos.

```bash
# Config privado aponta EXCLUSIVAMENTE à cópia isolada, driver MySQL instalado.
flyway -configFiles=/root/jeepclub-lab.conf info
python3 deploy/compare-schema.py /root/reference-v3 /root/restored-b-audit
# Apenas após equivalência e revisão; esta linha escreve o histórico na cópia.
flyway -configFiles=/root/jeepclub-lab.conf -baselineVersion=3 -baselineDescription=Verified_legacy_schema -baselineOnMigrate=false -cleanDisabled=true baseline
flyway -configFiles=/root/jeepclub-lab.conf -baselineOnMigrate=false -cleanDisabled=true migrate
flyway -configFiles=/root/jeepclub-lab.conf validate
flyway -configFiles=/root/jeepclub-lab.conf info
```

Arquivo privado contém flyway.url, flyway.user, flyway.password, flyway.driver=com.mysql.cj.jdbc.Driver, flyway.locations=filesystem:/caminho/revisado/db/migration, flyway.cleanDisabled=true e baselineOnMigrate=false. Não há modelo com valores reais versionado. Referências novas não recebem baseline; migrate cria suas tabelas. Na cópia adotada exigir history BASELINE versão correta + somente incrementos posteriores, todos success=1; banco novo exige SQL V1,V2,V3. Comparar schemas finais entre novo e cópia, bem como contagens antes/depois (history cresce; tabelas novas esperadas vazias).

10. Subir **imagem candidata exata** na rede isolada, profile/config equivalente e ddl-auto=validate; SMTP simulado/desativado e storage isolado. Confirmar Flyway antes de Hibernate, readiness UP e nenhum DDL automático. Smoke tests com contas de laboratório: login/refresh/logout/recuperação; criação administrativa/convite/reenvio/invalidação; alteração CPF/e-mail e conflitos unique; inscrição/aprovação/ativação Membership; perfil; listagem Vehicle/Tool; permissão ROOT estável após restart. Conferir timestamps/timezone, enums e logs SQL/locks. Não disparar e-mails para pessoas reais.

## Corte sujeito a aprovação

11. Apresentar diagnóstico A/B/C, versão de baseline comprovada, diffs explicados, backup restaurado, CI/commit imutável, resultado do ensaio, duração medida e riscos. Obter autorização expressa antes de qualquer mudança ativa.
12. Na janela autorizada, bloquear tráfego/escritas, obter backup final recuperável e repetir coleta/comparação fresca; cancelar se B mudou. Instalar hook revisado (incluindo nova proteção de falha) e conferir permissões/forced command. Aplicar **somente a baseline verificada** no ativo e incrementos necessários pelo mesmo conjunto imutável de scripts; registrar history antes/depois. Não registrar versão 3 se faltar qualquer objeto V3.
13. Aplicar configurações validate/Flyway, executar deploy do commit aprovado pelo fluxo protegido, validar readiness/logs/contagens/metadata/history e smoke tests. Liberar tráfego progressivamente, registrar imagem/digest/SHA/migration checksum/backup utilizado. Após integração do PR Flyway em develop, #100 precisa conter esse commit e ambos os gates verdes no merge candidato; integração e promoção dependem de revisão humana.

## Monitoramento e recuperação

14. Acompanhar startup/readiness, falhas SQL/constraint, conexões e pool, deadlocks/timeouts, erros de envio e volume de 5xx; revisar imediatamente e após período acordado. Logs podem conter dados pessoais: usar redaction e acesso restrito.
15. MySQL DDL tem commits implícitos. Em falha, parar backend, manter tráfego bloqueado, coletar history/SHOW CREATE/erro e não tentar repair, editar migration aplicada ou repetir cegamente. O hook novo interrompe a aplicação em vez de reinstalar automaticamente uma imagem com update. Se a falha ocorreu antes de qualquer alteração e o schema antigo foi comprovado intacto, rollback de imagem pode ser revisado; caso contrário requer plano de recuperação aprovado.
16. Recuperação preferida ensaiada: restaurar o backup final **em novo volume/instância**, comparar estrutura/contagens, iniciar imagem/config anterior compatível e validar; trocar conexão/tráfego somente após autorização. Preservar volume original para investigação. Não sobrescrever o banco ativo automaticamente. Tempo de restore e indisponibilidade são medidos no ensaio, ainda desconhecidos; não prometer rollback transacional de DDL.
