# Auditoria da suíte Tools (`develop@80ddc7de`, 24/09/2026)

| Produção / contrato vigente | Baseline já existente | Lacuna e incremento nesta branch |
| --- | --- | --- |
| Criação member/admin, listagem, leitura, atualização parcial (PUT), ativação/desativação idempotentes | `ToolServiceTest`, `ToolContractCharacterizationTest` | Fluxo HTTP member/admin com principal e permissão reais: `ToolsHttpIntegrationTest` |
| ACTIVE e INACTIVE continuam listáveis/editáveis; delete físico com snapshot histórico | `ToolContractCharacterizationTest`, `ToolRepositoryAdapterTest` | HTTP percorre INACTIVE, delete e leitura 404; confere snapshot persistido |
| Ownership member; admin opera ferramenta de qualquer dono com permissões | `ToolServiceTest` verifica leitura de outro dono; `ToolsOpenApiIntegrationTest` declara permissões | HTTP testa listagem isolada e 403 em leitura, atualização, status e delete; admin sem/com permissão |
| Repositório: filtro admin por nome/status e listagem isolada por userId | Adapter já testa delete, conflito de histórico e concorrência | `ToolRepositoryAdapterTest` combina filtros e comprova isolamento por userId |
| Validação do payload, RFC 9457, OpenAPI | DTOs com Bean Validation; `ToolExceptionHandler`; `ToolsOpenApiIntegrationTest` | HTTP testa entrada inválida, content type problem+json, códigos `TOOL_ACCESS_DENIED` e `TOOL_NOT_FOUND` |
| Validação do owner em criação admin (BACK-378) | Ausente no `develop` auditado | Na branch integrada, `ToolOwnerContractTest` e a characterization cobrem owner ativo, inexistente e desabilitado |

## Integração das três branches

Esta branch reúne validação do proprietário, mídia global e testes de Tools. Os conflitos de construtor foram resolvidos para injetar `ToolOwnerQuery` e `ImageMediaService` juntos; o teste antigo que admitia qualquer ID foi substituído pelo contrato de validação. As métricas abaixo foram obtidas antes desta integração. A suíte integrada deve ser reexecutada em ambiente com acesso ao Maven Central.

Os testes novos usam relógio injetado, CPFs distintos válidos, transação revertida no final e recursos criados pelos próprios testes. Não alteram comportamento de produção; a characterization permanece como baseline.

## JaCoCo e PIT

Executar `./mvnw verify` e consultar `target/site/jacoco/index.html`, filtrando o pacote `com.jeepclub.backend.tools`. Conferir especialmente branches de `Tool`, `ToolService`, `AdminToolService`, `ToolSpecifications` e a tradução HTTP. Cobertura percentual isolada não prova autorização: os testes HTTP verificam acesso cruzado e persistência do histórico.

Executar PIT apenas para domínio/serviços selecionados com `./mvnw -Ptools-mutation test-compile org.pitest:pitest-maven:mutationCoverage`; o perfil sobrescreve o escopo padrão de Dependents. Consultar `target/pit-reports/index.html`; revisar mutantes sobreviventes, principalmente ownership, status idempotente e null/blank no update. Essa análise quantitativa depende do relatório da execução real.

Neste workspace, `test`, `verify` e PIT param antes da compilação: o Maven Central não resolve `spring-boot-starter-parent:4.0.4`. Nenhum resultado de JaCoCo/PIT foi inferido nem alegado como aprovado. Após executar em ambiente com dependências, registrar aqui o resumo dos relatórios e eventuais sobreviventes relevantes.

Esta página registra a cobertura preexistente, os gaps e o incremento para revisão da branch, sem abrir PR.

## Reexecução recebida

O primeiro `./mvnw test` executou 589 testes: os 2 novos testes HTTP e os 4 testes JPA de Tools passaram. O único erro ocorreu em `IdentityOpenApiIntegrationTest`, por reutilizar o CPF `39053344705` em outro teste. O fixture desse teste foi isolado com CPF válido diferente (`47831962573`); a suíte foi reexecutada com sucesso antes da análise PIT abaixo.

## Resultado informado após a correção

`test` e `verify`: BUILD SUCCESS (589 testes, 0 falhas, 0 erros, 1 ignorado). O resumo do PIT selecionado informa 62/78 linhas (79%), 37 mutações, 24 eliminadas (65%), 11 sem cobertura e 2 sobreviventes do mutador `BooleanFalseReturnVals` (força dos testes: 92%). O resumo não contém os locais individuais de `NO_COVERAGE`, portanto não é possível atribuí-los a métodos específicos sem `target/pit-reports/mutations.xml`. No escopo selecionado, os únicos métodos booleanos explícitos são `Tool.activate` e `Tool.deactivate`. A characterization passa a verificar diretamente os retornos verdadeiro/falso de transição real e idempotente, além da preservação de `updatedAt`. Reexecutar PIT para confirmar a eliminação dos dois sobreviventes. O log não inclui métricas JaCoCo por pacote; consultar `target/site/jacoco/index.html` para análise quantitativa de Tools.

## Reexecução final informada

`test` e `verify` reportaram BUILD SUCCESS. O PIT selecionado passou de 2 sobreviventes para **0 sobreviventes**: 37 mutações geradas, 26 eliminadas, 11 sem cobertura; força dos testes de 100%. A cobertura de linhas medida pelo PIT nas classes selecionadas foi 63/78 (81%). As 11 mutações sem cobertura não são sobreviventes e o log resumido não traz classe/linha para priorização; o relatório XML completo permite essa triagem. O índice JaCoCo por pacote também não foi anexado, então não há porcentagem JaCoCo de Tools a registrar.

## Triagem dos relatórios XML recebidos

O `mutations.xml` confirmou exatamente 26 `KILLED`, 11 `NO_COVERAGE` e nenhum `SURVIVED`. Dez `NO_COVERAGE` pertencem a `AdminToolService`: listagem (1), leitura ausente e presente (2), atualização (2), ativação (2), desativação (2) e exclusão (1). O outro está no caminho de ausência de `ToolService.getToolDetails` (1). Todos representam comportamentos relevantes; foram acrescentados `AdminToolServiceTest` e um teste de ausência em `ToolServiceTest`. A análise dos novos resultados depende de nova execução de `test`, `verify` e PIT.

O `jacoco.xml` mostra, nas classes selecionadas, `Tool` com 34/34 linhas e 12/12 branches, `ToolService` com 19/19 linhas e 4/4 branches, `AdminToolService` com 11/16 linhas e 1/4 branches. No restante de Tools: o adapter tem 18/18 linhas; `ToolSpecifications` tem 8/8 linhas e 3/6 branches; `ToolExceptionHandler` tem 3/6 linhas. Os branches ainda descobertos de `ToolSpecifications` pertencem às combinações de filtros já parcialmente cobertas pelo repositório, e `InvalidToolStatusException` permanece sem uso no lifecycle vigente. A prioridade foi a lacuna administrativa confirmada por PIT e JaCoCo; endpoints e autorização são cobertos por `ToolsHttpIntegrationTest`.

## Mutante da reexecução após cobertura administrativa

O novo log apresentou 595 testes verdes, PIT com 37 mutações, 36 eliminadas, nenhuma sem cobertura e uma sobrevivente; cobertura das classes selecionadas: 76/78 linhas (97%). O XML da mesma execução identifica `AdminToolService.deactivateTool`, linha 56 (`NegateConditionalsMutator`). O teste anterior verificava apenas o total final de duas gravações após desativar duas vezes e reativar. A mutação trocava *qual* desativação persistia e mantinha o mesmo total. O teste agora verifica a gravação imediatamente após a primeira desativação e sua ausência na segunda. Reexecutar a suíte e o PIT para confirmar que a mutação foi eliminada.

## Validação final após o ajuste de idempotência

O último log confirma `verify` com BUILD SUCCESS: 595 testes, 0 falhas, 0 erros e 1 ignorado. O PIT selecionado também terminou com BUILD SUCCESS: **37/37 mutações eliminadas (100%)**, nenhuma sobrevivente, nenhuma sem cobertura e força dos testes de 100%. A cobertura de linhas nas três classes selecionadas pelo PIT foi 76/78 (97%). A auditoria acima registra os gaps pré-existentes, os testes adicionados e a resolução do último sobrevivente. Nenhuma regra de produção foi alterada.
