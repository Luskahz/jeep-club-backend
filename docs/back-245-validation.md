# BACK-245 — resultados de validação

Execução em 15/10/2026: Windows, Microsoft OpenJDK 17.0.20 e Maven Wrapper 3.9.14.
Resultados da CI do HEAD publicado são registrados no PR após o envio.

## Maven

| Comando e log | Testes | Falhas | Erros | Ignorados | Resultado |
|---|---:|---:|---:|---:|---|
| .\mvnw.cmd test (final-tree-test.log) | 1747 | 0 | 0 | 1 | BUILD SUCCESS |
| .\mvnw.cmd verify (final-tree-verify.log) | 1747 | 0 | 0 | 1 | BUILD SUCCESS |

O único teste ignorado no Windows é LocalFileStorageTest.shouldRejectSymbolicLinkInsteadOfFollowingItOutsideRoot (privilégio para criar symlink). Não pertence ao escopo auditado.

## JaCoCo

Inclui DTOs, adaptadores e metamodelos JPA gerados, sem exclusões para aumentar o percentual.

| Contador | Coberto | Total | Percentual |
|---|---:|---:|---:|
| LINE | 1719 | 1886 | 91.15% |
| BRANCH | 477 | 654 | 72.94% |
| INSTRUCTION | 6995 | 8042 | 86.98% |
| CLASS | 154 | 172 | 89.53% |

Relatórios locais: target/site/jacoco-authentication/index.html e jacoco.xml.

## PIT focal

Resultado final: {"KILLED": 217}.

Comandos reproduzíveis (bash: ./mvnw; PowerShell: .\mvnw.cmd):

```powershell
.\mvnw.cmd -Pauthentication-mutation org.apache.maven.plugins:maven-help-plugin:3.5.1:effective-pom -Doutput=target/authentication-effective-pom.xml
.\mvnw.cmd -Pauthentication-mutation org.pitest:pitest-maven:mutationCoverage
```

Configuração efetiva conferida em target/authentication-effective-pom.xml: includes, targetClasses, targetTests, excludedClasses e reportsDirectory. O perfil padrão de Dependents não é usado nesta evidência.
Relatórios: target/pit-reports/authentication/index.html e mutations.xml.

Nenhum SURVIVED, NO_COVERAGE ou TIMED_OUT na rodada focal final.

A rodada ampla inicial foi interrompida após timeouts de testes Spring. A primeira rodada focal teve 183 KILLED / 28 SURVIVED / 6 NO_COVERAGE. Os guards e estados persistidos relevantes receberam testes; a rodada final terminou sem excluir métodos ou desabilitar mutadores.

## Limites e cobertura residual

H2 é a infraestrutura exigida por BACK-253. As transações e locks foram exercitados com H2; não certificam todas as particularidades do MySQL. E-mail externo não foi enviado. Nenhum arquivo de produção foi alterado.

JaCoCo não representa cobertura exaustiva. Os maiores grupos de linhas não executadas são registrados abaixo. A matriz descreve os fluxos realmente exercitados e o PIT considera somente as classes selecionadas.

| Classe | Linhas não executadas | Branches não executados |
|---|---:|---:|
| PasswordChangeChallenge | 17 | 17 |
| SessionExceptionHandler | 14 | 0 |
| AuthenticationAccountExceptionHandler | 10 | 0 |
| SessionService | 10 | 10 |
| CreateUserWithPendingFirstAccessAdapter | 8 | 0 |
| RefreshTokenRepositoryAdapter | 7 | 6 |
| PasswordChangeChallengeExceptionHandler | 6 | 0 |
| AuthenticationAccount | 6 | 6 |

Handlers alternativos e validações defensivas não são apresentados como integralmente cobertos. A matriz e os testes sustentam os fluxos centrais e rejeições exigidos. Não foi identificado bug funcional de produção nesta auditoria.

## Arquivos da complementação

- docs/back-245-audit.md
- docs/back-245-jira-requirements.md
- docs/back-245-validation.json
- pom.xml
- src/test/java/com/jeepclub/backend/authentication/api/controller/PasswordRecoveryHttpIntegrationTest.java
- src/test/java/com/jeepclub/backend/authentication/api/security/AuthenticationSecurityIntegrationTest.java
- src/test/java/com/jeepclub/backend/authentication/core/application/service/RefreshTokenTransactionIntegrationTest.java
- src/test/java/com/jeepclub/backend/authentication/core/domain/model/AuthenticationBoundaryInvariantTest.java
- src/test/java/com/jeepclub/backend/authentication/infra/persistence/adapter/PasswordRecoveryRequestRepositoryAdapterTest.java
- src/test/java/com/jeepclub/backend/authentication/infra/persistence/adapter/RefreshTokenRepositoryAdapterTest.java
- src/test/java/com/jeepclub/backend/authentication/infra/persistence/adapter/SessionRepositoryAdapterTest.java

Fingerprint local de pom.xml + src/**/*.java: 2377154d9b7209e32c2d66154a00768ca54b950b0f0e7d1ba66b277c261921d8.
Logs brutos no worktree: target/audit-evidence. Resumo estruturado versionado: back-245-validation.json.
