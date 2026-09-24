# Auditoria da suíte Tools (`develop@80ddc7de`, 24/09/2026)

| Produção / contrato vigente | Baseline já existente | Lacuna e incremento nesta branch |
| --- | --- | --- |
| Criação member/admin, listagem, leitura, atualização parcial (PUT), ativação/desativação idempotentes | `ToolServiceTest`, `ToolContractCharacterizationTest` | Fluxo HTTP member/admin com principal e permissão reais: `ToolsHttpIntegrationTest` |
| ACTIVE e INACTIVE continuam listáveis/editáveis; delete físico com snapshot histórico | `ToolContractCharacterizationTest`, `ToolRepositoryAdapterTest` | HTTP percorre INACTIVE, delete e leitura 404; confere snapshot persistido |
| Ownership member; admin opera ferramenta de qualquer dono com permissões | `ToolServiceTest` verifica leitura de outro dono; `ToolsOpenApiIntegrationTest` declara permissões | HTTP testa listagem isolada e 403 em leitura, atualização, status e delete; admin sem/com permissão |
| Repositório: filtro admin por nome/status e listagem isolada por userId | Adapter já testa delete, conflito de histórico e concorrência | `ToolRepositoryAdapterTest` combina filtros e comprova isolamento por userId |
| Validação do payload, RFC 9457, OpenAPI | DTOs com Bean Validation; `ToolExceptionHandler`; `ToolsOpenApiIntegrationTest` | HTTP testa entrada inválida, content type problem+json, códigos `TOOL_ACCESS_DENIED` e `TOOL_NOT_FOUND` |
| Validação do owner em criação admin (BACK-378) | Characterization comprova ausência de consulta a Identity | Ainda não integrada à `develop` auditada; executar e adaptar testes próprios quando BACK-378 entrar |

Os testes novos usam relógio injetado, CPFs distintos válidos, transação revertida no final e recursos criados pelos próprios testes. Não alteram comportamento de produção; a characterization permanece como baseline.

## JaCoCo e PIT

Executar `./mvnw verify` e consultar `target/site/jacoco/index.html`, filtrando o pacote `com.jeepclub.backend.tools`. Conferir especialmente branches de `Tool`, `ToolService`, `AdminToolService`, `ToolSpecifications` e a tradução HTTP. Cobertura percentual isolada não prova autorização: os testes HTTP verificam acesso cruzado e persistência do histórico.

Executar PIT apenas para domínio/serviços selecionados com `./mvnw -Ptools-mutation test-compile org.pitest:pitest-maven:mutationCoverage`; o perfil sobrescreve o escopo padrão de Dependents. Consultar `target/pit-reports/index.html`; revisar mutantes sobreviventes, principalmente ownership, status idempotente e null/blank no update. Essa análise quantitativa depende do relatório da execução real.

Neste workspace, `test`, `verify` e PIT param antes da compilação: o Maven Central não resolve `spring-boot-starter-parent:4.0.4`. Nenhum resultado de JaCoCo/PIT foi inferido nem alegado como aprovado. Após executar em ambiente com dependências, registrar aqui o resumo dos relatórios e eventuais sobreviventes relevantes.

Esta página registra a cobertura preexistente, os gaps e o incremento para revisão da branch, sem abrir PR.

## Reexecução recebida

O `./mvnw test` executou 589 testes: os 2 novos testes HTTP e os 4 testes JPA de Tools passaram. O único erro ocorreu em `IdentityOpenApiIntegrationTest`, por reutilizar o CPF `39053344705` em outro teste. O fixture desse teste foi isolado com CPF válido diferente (`47831962573`); aguarda nova execução da suíte inteira.
