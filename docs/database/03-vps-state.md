# Relatório 3 — Estado real do banco e comparação A/B/C

> Registro histórico anterior à execução por SSH. Para o estado vigente, consultar [relatório operacional 8](08-operational-adoption.md) e [runbook atualizado](06-vps-runbook.md).

**B está pendente.** Acesso root por SSH foi autorizado pelo usuário; duas conexões à porta 22 expiraram antes da autenticação em 15/10. A senha não foi enviada à VPS, salva em arquivos nem incluída em comandos/logs. Usuário informou possível bloqueio da rede de origem. Nenhum comando remoto foi executado e nenhum dado ativo foi alterado. Não há resultado de SELECT/SHOW CREATE oriundo da VPS.

| Item | A: referência 02/10 (`e4aa00e`) | B: VPS atual | C: develop (`9d3fba3`) |
|---|---|---|---|
| Tabelas de aplicação | 58 esperadas | Pendente coleta | 60 esperadas |
| User profiles | Ausente | Provável após #93, não verificado | Presente, PK/FK user_id |
| Activation invitations | Ausente | Provável após #95, não verificado | Presente, PK identity_id/unique token |
| Flyway history | Código não habilitava Flyway | Desconhecido | Ausente na release original; esta branch habilita |
| MySQL/charset/collation | Não determinados pelo Git | Só tag mysql:8.0 nos logs; versão/charset/collation desconhecidos | MySQL 8.0; CI 8.0.44, charset herdado do database |
| DDL/profiles | update / dev no Compose posterior | Efetivo ainda não coletado | validate + Flyway na nova branch |
| Linhas, FKs, índices, objetos obsoletos | Só esperado por mappings | Desconhecidos | Referência em V1–V3 e testes |
| Volumes | Não provar por Git | mysql-data/uploads esperados, não inspecionados | Nomes preservados |

Executar quando houver conectividade, **apenas a coleta**, sem editar configuração ou reiniciar containers:

```bash
# Na VPS, a partir de uma cópia revisada desta branch; saída nova e privada.
bash deploy/audit-schema.sh /root/jeepclub-schema-audit-YYYYMMDD-HHMM
```

O coletor usa as credenciais já disponíveis dentro do container MySQL, sem mostrá-las; lê como DBA para não ocultar objetos por privilégios. Exporta versão/database/sql_mode/charset/collation, todas as tabelas/colunas/PK/FK/índices/CHECK, views/triggers/routines/events, contagens exatas em transação READ ONLY, schema dump sem dados/locks e histórico Flyway quando existente. Registra imagens/digests/mounts, SHA atual/estado do deploy e somente variáveis permitidas do backend. Não imprime ambiente completo nem linhas da aplicação. Revise definições/defaults de objetos antes de compartilhar: metadados também podem conter literais sensíveis introduzidos manualmente.

Compare `application_database` com DATABASE() de server.tsv. Se divergirem, interromper e ajustar a coleta para o banco realmente usado. Variáveis são evidência declarada: confira também overrides em arquivos montados/JVM/configuração externa, profile ativo e propriedades empacotadas no SHA implantado; não afirmar valor efetivo apenas pelo Compose. Se faltar permissão, objeto ou arquivo, coleta incompleta não libera baseline.

Criar referências independentes V1/V2/V3 em MySQL isolado da **mesma versão, sql_mode, lower_case_table_names, charset e collation de B**; coletar com MYSQL_CONTAINER apontando somente ao container de referência. Comparador:

```bash
python3 deploy/compare-schema.py /root/reference-v3 /root/vps-audit
```

Ele ignora somente a tabela Flyway e metadados de volume/tempo/contagem na equivalência estrutural; compara nomes e definições de objetos estritamente, sem remover tipos/defaults/índices/FKs. Divergências são bloqueadoras até explicadas. Nomes diferentes podem ser equivalentes funcionalmente, mas exigem análise explícita; nunca ignorar tudo para obter verde. Compare contagens separadamente e confirme ausência de órfãos/duplicados antes de qualquer constraint futura. Cópia restaurada real de B ainda precisa passar baseline/migrate/validate e smoke tests; fixtures históricas do CI não substituem isso.
