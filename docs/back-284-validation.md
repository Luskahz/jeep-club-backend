# BACK-284 — resultados de validação

Execução em 15/10/2026: Windows, Microsoft OpenJDK 17.0.20 e Maven Wrapper 3.9.14.
Resultados da CI do HEAD publicado são registrados no PR após o envio.

## Maven

| Comando e log | Testes | Falhas | Erros | Ignorados | Resultado |
|---|---:|---:|---:|---:|---|
| .\mvnw.cmd test (final-tree-test.log) | 1696 | 0 | 0 | 1 | BUILD SUCCESS |
| .\mvnw.cmd verify (final-tree-verify.log) | 1697 | 0 | 0 | 1 | BUILD SUCCESS |

O test completo de 1.696 cenários precedeu a última inclusão do caso JPA de paginação. O verify final inclui esse caso (1.697 cenários). O slice JPA também passou isoladamente após corrigir o telefone obrigatório da fixture.

O único teste ignorado no Windows é LocalFileStorageTest.shouldRejectSymbolicLinkInsteadOfFollowingItOutsideRoot (privilégio para criar symlink). Não pertence ao escopo auditado.

## JaCoCo

Inclui DTOs, adaptadores e metamodelos JPA gerados, sem exclusões para aumentar o percentual.

| Contador | Coberto | Total | Percentual |
|---|---:|---:|---:|
| LINE | 747 | 771 | 96.89% |
| BRANCH | 156 | 213 | 73.24% |
| INSTRUCTION | 3173 | 3373 | 94.07% |
| CLASS | 80 | 85 | 94.12% |

Relatórios locais: target/site/jacoco-membership/index.html e jacoco.xml.

## PIT focal

Resultado final: {"KILLED": 105, "SURVIVED": 1}.

Comandos reproduzíveis (bash: ./mvnw; PowerShell: .\mvnw.cmd):

```powershell
.\mvnw.cmd -Pmembership-mutation org.apache.maven.plugins:maven-help-plugin:3.5.1:effective-pom -Doutput=target/membership-effective-pom.xml
.\mvnw.cmd -Pmembership-mutation org.pitest:pitest-maven:mutationCoverage
```

Configuração efetiva conferida em target/membership-effective-pom.xml: includes, targetClasses, targetTests, excludedClasses e reportsDirectory. O perfil padrão de Dependents não é usado nesta evidência.
Relatórios: target/pit-reports/membership/index.html e mutations.xml.

- com.jeepclub.backend.memberships.core.application.service.membershipapplication.AdminMembershipApplicationService.rejectApplication:148 (SURVIVED): retorno privado ignorado pelo chamador; equivalente sem efeito observável. Não alteramos produção nem usamos reflexão para eliminá-lo.

Primeira rodada: 93 KILLED / 6 SURVIVED / 7 NO_COVERAGE. Após reforçar notificações, validação precoce de tokens e dados ausentes: 105 KILLED e 1 equivalente.

## Limites e cobertura residual

H2 é a infraestrutura exigida por BACK-253. As transações e locks foram exercitados com H2; não certificam todas as particularidades do MySQL. E-mail externo não foi enviado. Nenhum arquivo de produção foi alterado.

JaCoCo não representa cobertura exaustiva. Os maiores grupos de linhas não executadas são registrados abaixo. A matriz descreve os fluxos realmente exercitados e o PIT considera somente as classes selecionadas.

| Classe | Linhas não executadas | Branches não executados |
|---|---:|---:|
| MemberActivationTokenService | 4 | 6 |
| AdminMembershipApplicationService | 2 | 4 |
| SmtpMemberActivationMailSender | 2 | 0 |
| DummyMemberActivationMailSender | 2 | 0 |
| PendingFirstAccessUser | 2 | 4 |
| MembershipAccessQueryService | 1 | 2 |
| MembershipApplicationService | 1 | 2 |
| MembershipApplicationPageResponseSchema | 1 | 0 |

Handlers alternativos e validações defensivas não são apresentados como integralmente cobertos. A matriz e os testes sustentam os fluxos centrais e rejeições exigidos. Não foi identificado bug funcional de produção nesta auditoria.

## Arquivos da complementação

- docs/back-284-audit.md
- docs/back-284-jira-requirements.md
- docs/back-284-validation.json
- pom.xml
- src/test/java/com/jeepclub/backend/memberships/MembershipActivationFlowIntegrationTest.java
- src/test/java/com/jeepclub/backend/memberships/api/security/MembershipAdminSecurityIntegrationTest.java
- src/test/java/com/jeepclub/backend/memberships/core/application/service/memberactivationtoken/MemberActivationTokenServiceTest.java
- src/test/java/com/jeepclub/backend/memberships/core/application/service/membershipapplication/AdminMembershipApplicationServiceTest.java
- src/test/java/com/jeepclub/backend/memberships/core/application/service/membershipapplication/MembershipApplicationServiceTest.java
- src/test/java/com/jeepclub/backend/memberships/infra/persistence/jpa/MembershipApplicantBlockJpaRepositoryTest.java

Fingerprint local de pom.xml + src/**/*.java: 692959878b3ebf3e1f5447ab0aa7a8007d747dd14ef754b68d16124a19a8e54d.
Logs brutos no worktree: target/audit-evidence. Resumo estruturado versionado: back-284-validation.json.
