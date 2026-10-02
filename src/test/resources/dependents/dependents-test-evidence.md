# Evidências — auditoria Dependents

Data: 25/09/2026. Base: `develop@80ddc7de896fa52a75b67dcc45322c9b3355a5c5`.
Veja a [matriz produção → teste → gap](dependents-test-matrix.md).

## Baseline e incremento

A suíte não foi criada do zero: havia 58 execuções de teste em nove classes, abrangendo
unitários, MVC, segurança real, OpenAPI, JPA e fluxo de sistema. A entrega contém 110
execuções em onze classes, incremento líquido de 52. Contagens incluem invocações de
testes parametrizados, não apenas métodos Java.

Reutilizados os testes de contrato e os testes do consumidor Health. Ampliados domínio,
services, validação HTTP, permissões, persistência e fluxo de sistema. Novas classes:
`DependentsQueryServiceTest` e `DependentRepositoryExceptionTranslationTest`.
A consolidação de dois testes idênticos de CPF está justificada na matriz.

## Bug funcional encontrado e regressão

`Dependent.reconstitute` recusava `updatedAt < createdAt`, mas `update`, `disable` e
`enable` aceitavam esse instante. Isso permitia produzir um objeto inválido para a
próxima leitura persistida. Antes da correção, os três casos de
`rejectsMutationBeforeCreationWithoutChangingState` falharam por não lançar exceção.
A correção valida o instante antes da mutação. Igualdade continua permitida; `updatedAt`
continua nulo na criação; não se introduziu monotonicidade entre duas edições posteriores.

Este é o único ajuste de produção: não se alteraram endpoints, DTOs, permissões, schema,
modelo de exclusão nem integrações para facilitar testes. A regra temporal está registrada
na documentação do módulo. Log da regressão vermelha: `target/dependents-temporal-red.log`.

## Determinismo e isolamento

O fluxo de sistema usa Clock próprio, inicializado antes do bootstrap Spring e avançado
explicitamente entre operações. Nenhum sleep/retry foi introduzido. O banco H2 tem nome
exclusivo: a primeira execução completa detectou vazamento do CPF da fixture de Dependents
para `IdentityOpenApiIntegrationTest`. O isolamento corrigiu a interferência sem alterar
Identity. O teste de rollback continua sem transação envolvendo o método de teste:
a falha ocorre em uma transação de aplicação e a consulta posterior verifica o estado
persistido em outra transação.

## Reprodução

Validação final em Windows, Eclipse Temurin 17.0.20.1 e Maven Wrapper 3.9.14. O JDK 17
portátil foi colocado em `target/test-tools` (ignorado pelo Git), sem mudar a configuração
do projeto. O primeiro diagnóstico usou o JBR 25 local e mostrou aviso de instrumentação
de proxies Spring pelo PIT; o diagnóstico final foi repetido no Java 17 do projeto.

No Windows use `mvnw.cmd` em vez de `./mvnw`, com `JAVA_HOME` apontando para um JDK 17.

```sh
./mvnw test
./mvnw verify
./mvnw test-compile org.pitest:pitest-maven:mutationCoverage \
  '-DtargetClasses=com.jeepclub.backend.dependents.core.domain.model.Dependent,com.jeepclub.backend.dependents.core.application.service.dependent.*' \
  '-DtargetTests=com.jeepclub.backend.dependents.core.domain.model.DependentTest,com.jeepclub.backend.dependents.core.application.service.*Test,com.jeepclub.backend.dependents.system.DependentSystemFlowTest' \
  '-DreportsDirectory=target/pit-reports/dependents'
```

O arquivo `target/jacoco.exec` foi removido antes do verify final para não acumular cobertura
de execuções anteriores. Não foram criados thresholds ou testes para buscar porcentagem.

## Limitações e não aplicáveis

- Persistência executada em H2 em modo MySQL; não certifica comportamento de locks/isolamento
  em um servidor MySQL real. A constraint de CPF é exercitada sem pré-checagem; delete
  repetido caracteriza deterministicamente o resultado perdedor, sem stress multithread.
- A verificação cruzada de CPF com Identity não constitui constraint atômica entre tabelas.
  Essa garantia não existe na produção e não foi redesenhada nesta task.
- Health retém perfis de owners inativos/removidos conforme seu contrato. Foram auditados
  seus testes existentes e a ligação real de status/ownership/lote com Dependents; não há
  exclusão automática de perfil médico implementada por esta entrega.
- Não existem endpoints de status, escrita administrativa, paginação ou histórico de
  Dependents para testar. `enable`/`disable` foram verificados no domínio e no service.
- Mockito nos testes HTTP valida binding/erros; os testes de segurança carregam os filtros
  e method security reais; o fluxo de sistema usa serviços e persistência reais. Cada
  camada verifica sua própria responsabilidade, sem alegar um único E2E HTTP completo.

## Resultados finais

| Execução | Resultado |
| --- | --- |
| Baseline focal em develop, antes das alterações | 58 testes, 0 falhas/erros/ignorados |
| Regressão temporal antes da correção | 3 falhas esperadas, uma por operação |
| `mvnw.cmd test` em Java 17 | 638 testes, 0 falhas, 0 erros, 0 ignorados |
| `mvnw.cmd verify` em Java 17 | 638 testes, 0 falhas, 0 erros, 0 ignorados; pacote e JaCoCo gerados |
| Dependents dentro da suíte final | 110 testes em 11 classes, 0 falhas/erros/ignorados |
| PIT final em Java 17, domínio e services selecionados | 84 mutantes, 84 mortos, 0 sobreviventes, 0 sem cobertura |
| `git diff --check` | Sem erros de whitespace |

Logs locais: `target/dependents-baseline.log`, `target/dependents-full-test.log`,
`target/dependents-verify.log`, `target/dependents-pit.log`.
Relatórios: `target/site/jacoco/index.html`, `target/site/jacoco/jacoco.xml`,
`target/pit-reports/dependents/index.html` e `mutations.xml` no mesmo diretório.
São artefatos gerados, ignorados pelo Git; os resultados e comandos são registrados aqui.

## Análise JaCoCo

Agregado exclusivo dos pacotes `com.jeepclub.backend.dependents`: **382/388 linhas
(98,45%) e 91/96 branches (94,79%)**. O denominador inclui duas classes de metamodelo
JPA geradas. Não é uma meta de aprovação nem percentual do backend inteiro.

| Classe | Linhas | Branches |
| --- | --- | --- |
| `Dependent` | 97/97 | 40/40 |
| `DependentService` | 67/68 | 18/20 |
| `AdminDependentService` | 11/11 | 2/2 |
| `DependentsQueryService` | 11/11 | 14/14 |
| `DependentRepositoryAdapter` | 45/45 | 14/14 |
| `DependentIdentityAdapter` | 3/3 | Sem branches |
| `DependentExceptionHandler` | 13/13 | Sem branches |
| Controllers member/admin | 41/41 | Sem branches |
| `DependentMapper` | 26/28 | 2/4 |
| `DependentHistoryMapper` | 16/17 | 1/2 |

As lacunas restantes foram inspecionadas:

- `DependentService.normalizeCpf`, guarda de CPF nulo/vazio: caminho defensivo repetido
  pela validação obrigatória do DTO e pela validação de CPF do domínio, já exercitada
  com nulo/vazio. O fluxo HTTP não aceita esse input no service. Sem teste adicional
  apenas para eliminar essa linha da métrica.
- Retornos nulos defensivos dos dois mappers: os fluxos operacionais passam agregados
  e entities existentes; os snapshots e campos reais foram verificados com JPA.
- Construtores das duas classes `DependentEntity_`/`DependentHistoryEntity_` geradas
  pelo processador JPA: não representam regra de negócio.

## Análise PIT

Seleção explícita: `Dependent`, `DependentService`, `AdminDependentService`, mutadores
padrão do PIT 1.19.0 e plugin JUnit 5 1.2.3 já configurados no projeto. DTOs, infraestrutura,
Spring e Health não foram declarados como tendo cobertura de mutação. O boundary de query
foi avaliado funcionalmente e com JaCoCo, não adicionado artificialmente ao recorte do PIT.

A primeira execução identificou 78 mortos, quatro sobreviventes e dois sem cobertura:

| Mutante inicial | Análise e tratamento |
| --- | --- |
| Remover `validateNow` em `Dependent.create` | Gap real de criação sem instante; teste de rejeição adicionado |
| Remover `requireText` em `Dependent.reconstitute` | Gap real de nome vazio na reconstrução; teste adicionado |
| Remover `validateUserId` em `Dependent.reconstitute` | Gap real de owner inválido na reconstrução; teste adicionado |
| Remover `requireText` em `Dependent.update` | Gap real de edição com nome vazio; rejeição e preservação do estado testadas |
| Retorno vazio de `DependentService.findAllByUserId` | A seleção inicial excluía o fluxo de sistema; incluído o fluxo existente no PIT |
| Retorno nulo de `DependentService.findById` | Consulta real acrescentada ao fluxo de sistema já existente, sem criar outra suíte |

Execução final: **84/84 mortos**, nenhum sobrevivente ou mutante sem cobertura a justificar.
O relatório PIT informa 188/189 linhas instrumentadas nas classes selecionadas; seu critério
não é o mesmo filtro de linhas do JaCoCo. O aviso comercial sobre plugin Spring opcional
não impediu a execução. No Java 17 não houve o aviso de class file 69 visto no JBR 25.
