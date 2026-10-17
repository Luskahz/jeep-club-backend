# BACK-284 — requisitos consultados no Jira

Consulta em 15/10/2026 (America/Sao_Paulo). Fonte: https://jeep-club-developers.atlassian.net/browse/BACK-284

## Contexto atualizado — baseline develop 20/09/2026

Esta task foi revalidada contra `develop@80ddc7de896fa52a75b67dcc45322c9b3355a5c5`.

A pré-condição antiga vinculada à BACK-275 está obsoleta. Membership evoluiu e já possui testes de service, applicant block, persistência, controller administrativo, exception handler e OpenAPI. Mudanças recentes também consolidaram activation/onboarding e integração com Identity/Authentication.

## Objetivo

Auditar a cobertura **atual** de Membership/onboarding e completar somente gaps reais, sem recriar testes já existentes.

## Fluxos atuais a auditar

* criação de MembershipApplication pública conforme contrato vigente;
* bloqueio/desbloqueio de applicant e rejeição com block;
* consulta/filtros/paginação administrativa;
* aprovação e efeitos necessários para Identity/Authentication;
* activation completion conforme fluxo atual;
* idempotência/conflitos de CPF/e-mail quando aplicável;
* estados e transições atuais de MembershipApplication;
* autorização administrativa e exposição pública estritamente necessária;
* RFC 9457 e OpenAPI;
* boundaries com Identity/Authentication sem duplicar testes que pertencem exclusivamente a esses módulos.

## Estratégia

1. Mapear classes/rotas atuais e testes existentes.
2. Produzir matriz coberto/parcial/ausente/não aplicável.
3. Atualizar as subtasks BACK-285..291 na execução conforme essa matriz.
4. Implementar somente gaps.
5. Priorizar regressões do onboarding/activation recente e integrações entre bounded contexts.
6. JaCoCo/PIT são ferramentas de diagnóstico; não criar testes artificiais para percentual.

## Critérios de aceite

* \[ \] matriz reflete a develop atual e substitui premissas antigas;
* \[ \] cobertura existente é reaproveitada;
* \[ \] activation/onboarding vigente possui cobertura end-to-end proporcional e determinística;
* \[ \] applicant block, aprovação, conflitos e idempotência auditados;
* \[ \] segurança das rotas admin/públicas auditada;
* \[ \] boundaries Identity/Authentication testados no nível correto;
* \[ \] JaCoCo e PIT analisados no escopo aplicável;
* \[ \] `./mvnw test` e `./mvnw verify` verdes;
* \[ \] evidências e PR vinculados.

Bug funcional encontrado deve ser explicitado e acompanhado de teste de regressão; não alterar produção apenas para satisfazer o teste.

## BACK-291 — Finalizar evidências do Membership

## Superseded

O escopo foi absorvido pela BACK-290: evidência final passa a fazer parte da validação da árvore final, evitando uma subtask puramente documental.

Mantida apenas para rastreabilidade histórica.

## BACK-290 — Executar PIT e validar árvore final do Membership

## Objetivo

Executar PIT focal em domínio/serviços selecionados e concluir a validação da árvore final.

## Aceite

* PIT com SURVIVED/NO_COVERAGE relevantes analisados;
* gaps reais tratados ou justificados;
* `mvnw test` e `mvnw verify` verdes;
* JaCoCo final comparado à BACK-285;
* evidência consolidada com matriz, comandos, limitações e PR/commit.



## BACK-289 — Analisar cobertura JaCoCo do Membership

## Superseded

Escopo absorvido pela BACK-285: JaCoCo passa a fazer parte da auditoria e baseline do módulo.

Mantida apenas para rastreabilidade histórica.

## BACK-288 — Validar onboarding e activation cross-module

## Objetivo

Caracterizar os gaps reais do fluxo MembershipApplication → aprovação → activation → Identity/Authentication.

## Baseline físico

Membership na develop:

* LINE 77,51%;
* BRANCH 62,09%.

## Prioridades comprovadas

* activation service e seus branches;
* `IdentityActivationRecipientAdapter`, atualmente sem cobertura de linha;
* token de activation, mapper/persistência e primeiro acesso;
* conflitos/idempotência entre aprovação, activation e provisioning.

## Regras

Reaproveitar `PaidMembershipFlowIntegrationTest` e demais fluxos existentes. Não repetir a mesma evidência de activation em BACK-286 e BACK-288: cada cenário deve ter um único objetivo/rastreabilidade.

## Aceite

Fluxo representativo e determinístico, cobrindo somente os gaps do boundary cross-module.

## BACK-287 — Implementar testes de integração do Membership

## Superseded

Escopo absorvido pela BACK-286 e BACK-288: integrações serão implementadas somente onde a matriz apontar gap real.

Mantida apenas para rastreabilidade histórica.

## BACK-286 — Fechar gaps comportamentais do Membership

## Objetivo

Implementar gaps reais de Membership que não pertencem especificamente ao fluxo cross-module de onboarding.

## Baseline físico

Develop `0d8542fb3d90768573dc738cea85b23c1701b44c`:

* LINE 77,51%;
* BRANCH 62,09%.

## Prioridades comprovadas

* superfícies HTTP administrativas e públicas pouco executadas;
* applicant block e activation controllers onde houver 0%/baixa cobertura;
* persistência/mapping do activation token;
* filtros, paginação, conflitos e idempotência;
* HTTP, RFC 9457, segurança e OpenAPI onde a suíte não caracterizar o comportamento.

## Redução de escopo

O domínio principal e vários services já possuem cobertura relevante. Não duplicar testes de service/persistência já existentes.

## Aceite

Somente gaps comprovados recebem testes; `mvnw test` verde; nenhuma duplicação sem justificativa.

## BACK-285 — Auditar Membership e mapear gaps reais da suíte

## Objetivo

Revalidar Membership/onboarding contra a `develop` atual e medir o baseline real da suíte.

## Escopo

* mapear Applications, applicant block, billing configuration, activation/onboarding e boundaries;
* matriz produção → teste existente → coberto/parcial/ausente/não aplicável;
* JaCoCo LINE/BRANCH do módulo Membership;
* reaproveitar os testes existentes de services, controllers, OpenAPI, persistência e paid membership.

## Aceite

Matriz e baseline registrados, sem duplicar testes existentes, com gaps objetivos para BACK-286 e BACK-288.

## BACK-253 — Padronizar infraestrutura de testes automatizados do backend

## Objetivo

Definir uma configuração única de testes para todos os módulos vinculados ao Epic BACK-244.

## Bibliotecas adotadas

* JUnit 5;
* Mockito;
* AssertJ;
* Spring Boot Test;
* Spring MVC Test e MockMvc;
* Spring REST Docs;
* Spring Data JPA Test;
* H2 para o profile de teste;
* JaCoCo para cobertura;
* PIT para mutação;
* Maven Wrapper para execução.

## Regras

* Testes unitários não sobem contexto Spring.
* A estrutura de `src/test/java` espelha `src/main/java`.
* Datas usam valores determinísticos ou `Clock.fixed`.
* Nenhum teste depende do banco de desenvolvimento, internet, ordem de execução ou preparação manual.
* Bibliotecas adicionais só podem ser incluídas após revisão desta tarefa.
* Testcontainers não deve ser adotado isoladamente por um módulo; qualquer mudança de H2 para MySQL em container deve ser compartilhada por todo o backend.

## Critérios de aceite

* \[ \] Dependências de teste revisadas no `pom.xml`.
* \[ \] JaCoCo configurado no Maven.
* \[ \] PIT configurado no Maven.
* \[ \] Profile comum de teste documentado.
* \[ \] Comandos `./mvnw test` e `./mvnw verify` reproduzíveis.
* \[ \] README contém bibliotecas, convenções e comandos.
* \[ \] Tasks de Authentication, Health, Tools, Dependents, Vehicles e Membership referenciam este padrão.



