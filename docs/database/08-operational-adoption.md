# Adoção operacional — 16/10/2026 America/Sao_Paulo

Evidências privadas na VPS: `/root/flyway-adoption-20261016` (modo 0700). O relógio UTC da execução já marcava 17/10. Este relatório substitui o bloqueio de conectividade dos relatórios anteriores. A autorização operacional desta execução inclui backup/restauração, instalação dos hooks e reconciliação não destrutiva comprovada em cópia; não inclui merge de #100 ou restauração sobre o ativo.

## Estado encontrado

Conexão administrativa root, Ubuntu 24.04.5 LTS, SSH porta 22. Chave Actions mantém `restrict,command="/usr/local/sbin/jeep-club-deploy"`; a chave administrativa não foi transferida ao GitHub. O clone, o marcador de deploy e a imagem em execução concordavam em `6865bc4f600462c5fcc6cf1212c9660591eff4db` (#95). Backend e MySQL saudáveis; Nginx em execução. Backend `jeep-club-backend:6865bc4f600462c5fcc6cf1212c9660591eff4db`.

MySQL **8.0.46**, digest `sha256:7dcddc01f13bab2f15cde676d44d01f61fc9f99fe7785e86196dfc07d358ae2b`; database `jeepclub`, charset `utf8mb4`, default `utf8mb4_0900_ai_ci`, `lower_case_table_names=0`, SQL mode estrito. Volumes `jeep-club_mysql-data` e `jeep-club_uploads`. Compose ativo era `/opt/jeep-club-repo/compose.yaml`, com segredos em `/opt/jeep-club-compose/.env`. Profile `dev`, `ddl-auto=update`, sem Flyway. Confirmados entrypoint, variáveis permitidas, mounts, propriedades do commit e profile nos logs; nenhum arquivo externo de configuração Spring montado.

O hook instalado era anterior ao PR #101: rollback automático de imagem após qualquer falha. O backup diário usava outro diretório Compose, não compartilhava o lock do deploy e arquivava SQL/uploads sem pausar escritas. Existiam backups diários, mas nenhum comprovante de restauração.

## Comparação integral e causa do drift

Referências independentes V1, V2 e V3 criadas pelo Flyway **11.14.1**, Connector/J **9.6.0**, no mesmo digest MySQL e mesmas configurações do ativo, em rede `--internal`, sem publicar portas e sem montar volumes de produção.

- V1: faltam na referência `identity_user_profiles` e `authentication_account_activation_invitations`; há também o drift de collation descrito abaixo.
- V2: falta na referência `authentication_account_activation_invitations`; permanece o mesmo drift.
- V3: mesmas 60 tabelas, nomes, tipos, nullability, defaults, enums, PKs, uniques, índices e FKs. Nenhum objeto extra, ausente ou constraint divergente. Nenhuma view, trigger, rotina, evento ou CHECK extra.
- Diferença concreta: **32 defaults de tabela e 123 colunas textuais** em `utf8mb4_unicode_ci`; V3 gera `utf8mb4_0900_ai_ci`. Charset igual. A lista completa das tabelas está em [vps-drift-20261016.json](vps-drift-20261016.json); todas as colunas textuais dessas tabelas têm essa diferença, sem outra diferença de definição.
- Causa comprovada: todas as 32 tabelas já tinham essa collation no dump `/root/jeep-club-before-docker.sql`, SHA256 `978251dd56339024c7f32abaca54194bdb49d68919a21989c7c8036c83f84605`. Tabelas criadas posteriormente herdaram o default do novo database. O Git não registra o dump de dados e não comprovava equivalência física do legado.

Não se aceitou a simples normalização do comparador como equivalência. Foi preparada [reconciliação específica](../../deploy/adoption/reconcile-legacy-collation.sql), fora da cadeia rotineira: conversão explícita das 32 tabelas para a collation de referência, mantendo charset, dados, colunas e constraints. Na cópia, o dump ordenado de **todos os registros** é idêntico antes/depois, todas as contagens coincidem e o comparador estrito retorna igualdade sem ignorar collation. As constraints únicas foram reconstruídas sem colisões. Também passaram 71 verificações de órfãos de FKs e valores inválidos de enums.

Baseline V3 somente é válida **depois** dessa reconciliação e de nova comparação exata. Uma baseline não atribui checksums SQL a V1–V3: registra `BASELINE 3`, e o Flyway deixa essas versões abaixo/na baseline. Os arquivos imutáveis são comprovados por hashes e pela comparação estrutural, enquanto os incrementos futuros recebem checksums normais.

## Backup e laboratório

Backup inicial: `/var/backups/jeep-club/adoption-20261017T021336Z`, no disco persistente da VPS. Backend pausado durante SQL/uploads e reativado após coleta. Inclui SQL completo com routines/events/triggers, uploads, configuração privada de recuperação, scripts, estado e certificados; hashes em `SHA256SUMS`.

Restauração em `/root/flyway-adoption-20261016/lab-mysql`, rede interna própria e mesma imagem MySQL. Comparação exata de estrutura e contagens de todas as tabelas. Uploads extraídos em diretório próprio e hashes de cada arquivo comparados. Nenhum volume produtivo utilizado pelo laboratório. Dados pessoais e dumps permanecem privados.

Na cópia reconciliada executados: `info`, baseline explícita V3, `info`, `migrate`, `validate`, `info`. Aplicação candidata `163121c9` iniciou com Flyway antes de Hibernate validate e health `UP`. SMTP vazio e rede sem saída impedem e-mails reais. A suíte operacional registra os demais cenários em `scenario-results.json` privado.

## Deploy permanente

`/usr/local/sbin/jeep-club-deploy` passa a ser o [wrapper estável](../../deploy/vps-deploy-wrapper.sh), root:root 0700. Mantém a chave restrita; aceita somente `status` ou `deploy <SHA completo>`, valida URL origin fixa, fetch de master, ancestralidade, SHA igual ao HEAD atual de master e progressão a partir da release vigente. Rejeita releases antigas sem suporte a migrations. Extrai o commit exato para um diretório privado de release e executa a lógica versionada com ambiente limpo e lock herdado. Nenhum argumento permite escolher shell/script/path.

A lógica [vps-deploy.sh](../../deploy/vps-deploy.sh) usa Compose com projeto fixo `jeep-club`, arquivo explícito da release e o `.env` privado. Ordem: preflight/history → build SHA → parar backend → backup consistente → startup com Flyway → Hibernate validate → health → persistir SHA/imagem. Falha antes do corte preserva a aplicação; falha durante/depois do corte para o backend, conserva estado anterior como **última release bem-sucedida**, grava `last-failure` e nunca volta automaticamente a outra imagem. Logs completos privados, identificador de estágio e caminho no Actions. Volumes conservados.

Backup diário e deploy compartilham o mesmo `flock`. O script de backup instalado é versionado neste PR e não depende do Compose antigo. O deploy usa a cópia de backup da própria release aprovada. Atualizações de migrations e lógica comum chegam pelo Git; o wrapper só precisa de manutenção extraordinária se mudar seu contrato de confiança.

Workflow: PRs develop/master executam Maven e MySQL sem deploy. Somente push master publica após ambos os jobs. `workflow_dispatch` executa checks, sem publicar. Concorrência no Actions e na VPS; SHA de `github.sha` enviado por SSH. Regras ativas de develop/master passaram a exigir `Maven verify` e `MySQL migrations`, com branch atualizada, preservando as demais regras e bypasses existentes. Master não foi alterada.

## Estado final e decisão

Adoção concluída em 17/10/2026 UTC (16/10 no fuso do usuário). Backup final **restaurado e verificado novamente**: `/var/backups/jeep-club/release-20261017T022513Z-ypolSb`. A cópia final teve equivalência exata de metadata, contagens das 60 tabelas, hashes dos uploads e bytes do dump determinístico dos registros. Depois da reconciliação ativa, e novamente depois da baseline, o dump dos registros permaneceu idêntico.

| Evidência | SHA256 |
|---|---|
| SQL comprimido do backup final | `cfc110b565473429671e0c2b0dd4db2212d763a3df6f2a73233f8aae0bece5e5` |
| Uploads do backup final | `4d62df1e88d69d1f86ff255eb1fe846bc18a4a24229a7a3ecf3e99c70c3a7ef1` |
| Configuração privada de recuperação | `0f883cbdaca5ca559ef8d98eb962206125fe8000075597fdda3bfa0b4f43b570` |
| Todos os registros, antes/depois da reconciliação/baseline | `dc6e22e43f8e35a7bd541aec48dad6a6962862ead4e5457be77cd85b58399504` |
| Wrapper instalado, idêntico ao Git | `7895e31c4b48846905ff54dc99cbe4bdcb6d343e2b09ee06ede874f19190737b` |
| Backup instalado, idêntico ao Git | `ce98e2f8e0c05c878ac310820c2d41335b74aef1836d435ba5513ce6c281d06f` |

History ativa: `installed_rank=1`, `version=3`, `type=BASELINE`, `description=Verified_reconciled_legacy`, `checksum=NULL`, `success=1`. Nenhum INSERT/UPDATE/DELETE manual na history, clean ou repair. `migrate` confirmou nenhuma pendência; `validate` passou. Comparação final estrita com V3 passou também após o restart.

Backend permanece na imagem/commit aprovado **6865bc4**, profile dev, agora com Hibernate **validate** e flags Flyway seguras configuradas. **Essa imagem antiga não contém Flyway**: a baseline foi feita pela ferramenta administrativa; o starter passará a executar automaticamente no primeiro deploy que contiver este PR. Hibernate inicializou, health retornou `UP`, backend/MySQL saudáveis e proxy respondeu 403 para acesso sem credencial privada, comportamento esperado. Mesmos volumes de banco e uploads, sem recriação. Nenhuma mudança de código da master ou release #100 foi publicada na VPS.

Resultados reproduzíveis em [vps-scenarios-20261016.json](vps-scenarios-20261016.json):

| Cenário | Resultado executado |
|---|---|
| Sem SQL novo | Duas inicializações saudáveis, history inalterada |
| SQL experimental empacotado | V4 no laboratório aplicada uma vez; segunda inicialização saudável; Hibernate validate |
| SQL inválido | Startup recusado, history `success=0`, DDL parcial preservado, sem repair/rollback |
| Drift | Coluna renomeada somente no laboratório; Hibernate recusou startup por coluna ausente |
| Recuperação | Backup restaurado em outro database, imagem compatível saudável, banco com DDL parcial preservado |
| APIs | Cadastro 201, login 200, Identity/me e profile 200, Membership 201 e repetição 200, somente identidades sintéticas |
| Máquina de estados do deploy | Teste real do script com Docker simulado: falhas de preflight/build preservam backend; backup/health falhos param backend sem publicar estado ou rollback |
| Entrada SSH | `status` executado; comando arbitrário e SHA fora de master rejeitados; authorized_keys inalterado |

A imagem candidata foi construída na VPS a partir de `163121c9`; as alterações posteriores de implementação foram no coletor, diagnóstico do wrapper e testes/documentação, sem modificar o código da aplicação ou V1–V3. CI [38994405191](https://github.com/Luskahz/jeep-club-backend/actions/runs/38994405191) e [38994762278](https://github.com/Luskahz/jeep-club-backend/actions/runs/38994762278) passaram Maven e MySQL, com deploy corretamente skipped por serem PRs. A versão de patch do MySQL no gate foi alinhada à VPS (8.0.46). O gate também repete a suíte em um arquivo Git isolado com uma versão SQL temporária posterior à última real; asserções acompanham a última migration, enquanto os fixtures históricos V1–V3 permanecem congelados. Essa migration temporária não entra no Git, na imagem de release ou no banco ativo.

Os scripts operacionais, logs, cópias antes/depois, manifests e o ensaio completo estão no diretório privado citado. Containers do laboratório foram parados para liberar recursos; armazenamento e evidências foram preservados. O job diário e o backup futuro utilizam lock e snapshot consistente. Não há cópia externa do backup comprovada nesta execução.

**Decisão operacional: VPS preparada para a release que inclua #101.** O merge de #100 continua reservado ao responsável. Incorporar #101 em develop e aguardar os dois gates no HEAD atualizado de #100 antes de aprovar a master. Não houve execução do novo deploy em master nesta tarefa, pois isso publicaria a release ainda não aprovada; o mecanismo foi verificado por configuração real, proteção SSH, testes de estado e startup/migrations/falhas reais no laboratório equivalente. Depois da integração do código na release, o próximo push aprovado em master executará migrations pendentes sem SSH manual.

