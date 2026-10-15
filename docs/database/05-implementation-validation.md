# Relatório 5 — Implementação e validação

Branch: `codex/flyway-schema-baseline`, criada de develop `9d3fba3`, sem alterar master/develop nem reescrever #98/#99. PR para develop: [#101](https://github.com/Luskahz/jeep-club-backend/pull/101); CI e SHA final registrados em validation.json. Integração não é automática.

Arquivos principais: pom.xml (starter/module/testcontainers/perfil Failsafe), application.properties (política segura), compose.yaml (validate/Flyway), SchemaSafetyConfiguration, três SQL em db/migration, SchemaMappings/SchemaMappingExportTest/SchemaSafetyConfigurationTest/MySqlMigrationIT, workflow com gate MySQL, scripts de coleta/comparação e vps-deploy.sh com interrupção segura de falha. Sem alteração de entidades de negócio, nomes de volumes ou credenciais.

Verificações:

```bash
./mvnw --batch-mode --no-transfer-progress verify
./mvnw --batch-mode --no-transfer-progress -Pmysql-migrations -DskipUnitTests=true verify
```

A segunda exige Docker e MySQL 8.0.44 isolado. Não apontar a suíte para um banco existente: cria databases efêmeros em container próprio. Histórico V1/V2/V3 é reconstruído a partir dos mappings idênticos comprovados; não é cópia do banco real. Comparação inclui engine/collation, tipos completos/enums, defaults, nullable, PK, unique, índices e FKs/regras. Testes validam todos os 52 mappings e 60 tabelas. O coletor de leitura também é exercitado contra o MySQL isolado, conferindo que não muda seu schema e não exporta credenciais.

Metadados de auto_increment corrente e ordem de colunas não alteram equivalência funcional.

Dois slices JPA antigos não selecionavam o profile test; agora o declaram explicitamente para usar a configuração H2 prevista.

Exportação local inicial: 14 verificações de configuração/mapping aprovadas. Docker Desktop falhou ao inicializar seu Inference manager; testes MySQL serão executados no runner Linux do CI, sem mascarar falha local como sucesso. Validação local completa: Maven verify, 1.867 testes, zero falhas/erros, um skip Windows (symlink), BUILD SUCCESS; o empacotamento `-DskipTests package` usado no Dockerfile também passou. Resultados MySQL/CI serão registrados após execução.

Limite explícito: Hibernate validate verifica tabelas/colunas/tipos; não é auditoria completa de FKs/índices/defaults/collation. Flyway validate verifica histórico/checksums; também não prova ausência de drift físico manual. Por isso a comparação estrutural e o ensaio da cópia real são critérios obrigatórios antes da baseline e promoção. Logs locais completos ficam em target/audit-evidence (ignorados pelo Git); CI publica artifacts de Failsafe.

A auditoria de infraestrutura identificou risco do rollback antigo: reinstalar imagem com Hibernate update após DDL parcial. O hook versionado agora bloqueia primeira adoção sem history e, em falha de release com migrations, para backend e exige recuperação manual. **O hook instalado na VPS não se atualiza sozinho**; instalação e comparação fazem parte do plano operacional e exigem autorização.
