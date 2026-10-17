# Relatório 4 — Estratégia Flyway

> Registro histórico anterior à execução por SSH. Para o estado vigente, consultar [relatório operacional 8](08-operational-adoption.md) e [runbook atualizado](06-vps-runbook.md).

Estratégia preferida: preservar estrutura e dados; não recriar automaticamente. A cadeia é baseada em mudanças físicas comprovadas do Git. A versão de adoção de B permanece **condicional à equivalência**, sem baseline de produção escolhida às cegas.

| Migration | Referência | Bancos novos | Banco existente equivalente |
|---|---|---|---|
| V1 initial_deploy_schema | e4aa00e / primeiro deploy automatizado e9a2ad4 | Cria 58 tabelas, PK/unique/índices/FKs/collections/herança | Baseline 1, depois V2 e V3 |
| V2 identity_user_profiles | bde973f / #93 | Adiciona profile (1 tabela, FK para users) | Baseline 2, depois V3 |
| V3 account_activation_invitations | 6865bc4 / #95 | Adiciona convites (1 tabela, unique token_hash) | Baseline 3; nenhuma repetição de V1–V3 |

Banco vazio: `migrate` executa V1→V2→V3; sem comando baseline. Banco não vazio e sem history: startup falha, exigindo auditoria/adoção explícita. Banco inesperado (por exemplo, só convite sem profile, FKs ausentes, enum modificado): não é uma versão conhecida. Não registrar baseline por número de tabelas; produzir plano de reconciliação preservando dados, testar na cópia e obter autorização antes de alterá-lo.

As migrations não usam `IF NOT EXISTS`, drops, seeds, backfills ou SQL que adapte silenciosamente drift. Nomes automáticos Hibernate foram preservados para permitir comparação com o legado. Charset/collation de cada tabela herdam o database, como no Hibernate anterior; exigir igualdade do ambiente de referência com B, sem impor nova collation sobre dados existentes.

Versões reais, gerenciadas pelo BOM Boot **4.0.4**: Flyway **11.14.1**, Hibernate **7.2.7.Final**, Connector/J **9.6.0**, Testcontainers **2.0.4**. Dependências `spring-boot-starter-flyway` (autoconfiguração modular Boot 4) e `org.flywaydb:flyway-mysql`; driver MySQL já existente. Ver [guia oficial Boot 4](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide) e [suporte MySQL Flyway](https://documentation.red-gate.com/flyway/reference/database-driver-reference/mysql).

Configuração compartilhada dev/prod/Compose: Flyway enabled, locations `classpath:db/migration`, baseline-on-migrate=false, clean-disabled=true, validate-on-migrate=true, out-of-order=false, ignore-migration-patterns vazio (recusa histórico de versão futura); Hibernate validate, generate-ddl=false, SQL init never, defer-datasource-initialization=false. Boot ordena migração antes de EntityManagerFactory. `SchemaSafetyConfiguration` rejeita overrides perigosos antes de instanciar Flyway/JPA. Não cria profile prod artificial: o deploy atual usa dev, e a política se aplica também a ele.

Os slices H2 existentes continuam `test` + banco estritamente em memória/create-drop; não verificam DDL MySQL. Há gate **MySQL migrations** obrigatório no workflow e dependência do deploy nos dois jobs (Maven verify e MySQL). Esse gate falha quando não há Docker, sem skip. Testes cobrem banco vazio, baseline em três referências históricas independentes geradas pelos mappings (cuja igualdade histórica é provada por hashes), preservação de linha sentinela, equivalência completa de metadados, constraints, checksums, clean bloqueado, drift e ordem Boot Flyway→validate. Cópia de B continua uma validação operacional distinta.

Política futura: V4__descricao.sql e próximos números únicos, revisados com JPA/queries/enums; migrations aplicadas são imutáveis. Alterações corretivas recebem nova versão. Nunca usar repair para esconder checksum/drift, baseline automática ou Hibernate update. DDL MySQL pode ter commits implícitos: uma falha pode deixar alterações parciais, exigindo diagnóstico e recuperação ensaiada.

Baseline é apenas registro de versão; não executa os scripts abaixo dela nem verifica equivalência. Não confundir o comando baseline com migrations cumulativas B-prefixadas (não usadas aqui). [Semântica oficial do comando](https://documentation.red-gate.com/flyway/reference/commands/baseline).
