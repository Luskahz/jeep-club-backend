# Runbook operacional de Flyway e deploy

O relatório [08-operational-adoption.md](08-operational-adoption.md) registra a execução real autorizada em 16/10/2026. Não repetir baseline nem reconciliação em releases normais. A autorização daquela adoção não permite apagar dados, sobrescrever o banco ativo ou editar histórico Flyway.

## Release normal, sem SSH manual

1. Acrescentar `V4__descricao.sql`, `V5__descricao.sql` etc. Nunca editar V1–V3 nem qualquer migration aplicada. Revisar compatibilidade de dados e locks/DDL; não inserir operações de teste na cadeia produtiva.
2. PR para develop: Maven verify e MySQL migrations obrigatórios. H2 não substitui MySQL. CI testa a cadeia SQL empacotada, legado histórico, Hibernate validate, falhas de checksum/histórico, migration futura e DDL parcial.
3. Integrar develop e abrir/atualizar PR develop → master. Ambos os gates também precisam passar nesse PR. O responsável aprova a release; esta execução não integra #100.
4. Push aprovado na master dispara Actions com o SHA exato. A chave existente envia apenas `deploy <SHA>`. O wrapper exige origem fixa, master HEAD atual, progressão da release e código com suporte a Flyway.
5. Build exato → backend parado → backup completo → Compose da release → Flyway valida/aplica pendências → Hibernate validate → health. Estado de sucesso só é persistido no final. Não executar SQL, baseline ou `flyway migrate` manual em cada release.

## Componentes instalados e estado

- `/usr/local/sbin/jeep-club-deploy`: wrapper estável, root:root 0700, fonte `deploy/vps-deploy-wrapper.sh`.
- `/usr/local/sbin/jeep-club-backup`: backup diário consistente, fonte `deploy/vps-backup.sh`. O deploy usa a cópia versionada do mesmo script no SHA aprovado.
- `/opt/jeep-club-repo`: clone usado para buscar/verificar master. Não é necessariamente o diretório da aplicação ativa depois do primeiro deploy pelo wrapper.
- `/var/lib/jeep-club-deploy/release-<SHA>-<id>`: árvore extraída do commit exato, privada. Compose recebe `--project-name jeep-club`, `--project-directory` e `-f` explícitos. O `.env` vem de `/opt/jeep-club-compose/.env`.
- `current-commit`, `current-image`, `current-release`: última release concluída. `last-failure` e `logs/` indicam uma tentativa interrompida; não confundir checkout Git com release bem-sucedida.
- O lock `/var/lib/jeep-club-deploy/deploy.lock` é compartilhado por deploy, backup diário e operações extraordinárias.
- A preparação inicial cria `compose.override.yaml` no clone legado para trocar somente configuração de startup por Hibernate validate e flags Flyway seguras. A imagem antiga não contém o starter Flyway; execução automática efetiva começa com a release que inclui #101. O Compose explícito das novas releases já contém essas proteções e não depende do override.

O wrapper não se atualiza automaticamente: isso é intencional, pois define a fronteira de confiança. Atualizações comuns do runtime de deploy, do backup usado no deploy e de migrations são extraídas do commit aprovado; não exigem reinstalar o wrapper. Mudanças no contrato do wrapper exigem manutenção administrativa extraordinária, revisão e cópia preservada da versão anterior. Nunca substituir a chave restrita por acesso administrativo do Actions.

## Backup e evidências

O backup diário continua agendado para 03:00 UTC. Agora aguarda o lock e pausa o backend durante SQL/uploads. Em modo diário, reativa o mesmo container após a coleta. Em deploy, deixa o backend parado até o startup da release. O dump inclui routines/events/triggers, usa transação consistente, hex-blob, sem tablespaces ou alteração de GTID. Uploads são montados somente para leitura em container sem rede. Configuração, certificados, imagens e estado ficam em arquivo privado.

Cada backup fica em `/var/backups/jeep-club/release-<UTC>-<id>`, fora de volume efêmero, modo privado, com SHA256. Não há exclusão automática de backups neste script. Planejar retenção externa e acompanhar espaço livre. Backup local não protege contra perda física da VPS; nenhuma cópia externa foi declarada como existente.

Para testar recuperação, criar instância MySQL independente com o mesmo digest, rede interna e diretório/volume **novo**, sem portas públicas. Conferir mounts antes de restaurar. Restaurar SQL, extrair uploads em diretório privado próprio, comparar metadata com `audit-schema.sh` + `compare-schema.py`, contagens tabela a tabela, hashes dos arquivos e dump determinístico dos registros. Hash do arquivo sozinho não comprova restore.

O coletor aceita `MYSQL_CONTAINER`, `BACKEND_CONTAINER` e opcionalmente `AUDIT_DATABASE` para bases de laboratório. Sempre confirmar `DATABASE()` e a URL da aplicação. Um diretório de saída deve ser novo. Metadados também podem conter literais sensíveis: revisar antes de compartilhar. Nunca publicar env, dumps, tokens, logs brutos de requisições ou dados pessoais em GitHub.

## Adoção inicial, já distinta do fluxo normal

Foi encontrado legado equivalente a V3 exceto por collation herdada do dump pré-Docker: 32 tabelas/123 colunas. A reconciliação específica fica em `deploy/adoption/reconcile-legacy-collation.sql`; não pertence a `db/migration` e não é executada pelo pipeline. Somente equivalência estrita, restore comprovado, preservação de registros e ausência de incompatibilidades permitem registrar baseline V3. Consultar o relatório operacional para resultado efetivo e hashes.

Baseline via API oficial Flyway, `baselineOnMigrate=false`, `cleanDisabled=true`, versão explícita. Nunca INSERT/UPDATE/DELETE na history. V1–V3 não passam a ter checksums individuais numa baseline V3: a verificação inicial foi estrutural; a history recebe BASELINE 3 e futuros SQL recebem checksums normais. Nunca utilizar repair para ocultar falhas.

## Incidente de deploy ou DDL parcial

1. Falha após início do corte mantém o backend parado; Actions recebe falha, SHA, estágio e localização do log privado. Verificar `last-failure`, `logs/`, estado dos containers e history. Não reativar imagem antiga automaticamente.
2. Preservar o volume afetado, a imagem, os SQL imutáveis, os logs e o backup anterior. Coletar `SHOW CREATE`/metadata para distinguir DDL aplicado de DDL pendente. MySQL pode fazer commits implícitos; não há promessa de rollback transacional de schema.
3. Não executar clean, repair, baseline automática, Hibernate update ou repetir migration com erro sem diagnóstico. Um SQL parcialmente aplicado pode coexistir com history `success=0`; checksum/histórico não deve ser adulterado.
4. Ensaio de recuperação: restaurar o backup pré-deploy em **outra instância e armazenamento**, iniciar a imagem compatível com esse backup e validar health, dados, uploads e integridade. Este procedimento foi testado preservando o schema de falha no laboratório.
5. Trocar conexão/tráfego para a instância recuperada exige autorização operacional específica e reconciliação das escritas posteriores ao backup. Restauração sobre o ativo, remoção de volume ou operação destrutiva exigem autorização explícita. Nunca executar `docker compose down -v`.
6. Só após evidência de compatibilidade decidir entre recuperação pelo backup ou correção incremental revisada. Publicar correções por PR; não modificar migrations já aplicadas para fazer a falha desaparecer.
