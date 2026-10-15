# BACK-245 — requisitos consultados no Jira

Consulta em 15/10/2026 (America/Sao_Paulo). Fonte: https://jeep-club-developers.atlassian.net/browse/BACK-245

## Contexto atualizado — baseline develop 20/09/2026

Esta task foi revalidada contra a `develop` no commit `80ddc7de896fa52a75b67dcc45322c9b3355a5c5`.

O módulo Authentication **já possui uma suíte relevante**. Foram identificados, entre outros: `LoginControllerTest`, `LoginControllerRestDocsTest`, `PasswordRecoveryControllerTest`, `AdminPasswordRecoveryControllerTest`, `AuthenticationSecurityIntegrationTest`, `AuthenticationAccountTest`, `JwtServiceImplTest`, `SessionServiceMeTest`, `ConcurrentLoginIntegrationTest`, testes de revogação, persistência, provisioning e characterization de DTO.

Portanto, **não implementar uma nova suíte do zero e não duplicar cenários já cobertos**.

## Objetivo

Auditar a suíte atual de Authentication contra o comportamento e contrato vigentes na develop, produzir uma matriz requisito → teste existente → gap e implementar somente gaps reais.

## Escopo atual a auditar

* login e criação/rotação/revogação de sessão;
* `/authentication/me` e identidade autenticada;
* JWT: emissão, validação, expiração e rejeições;
* alteração obrigatória de senha e challenges vigentes;
* recuperação/redefinição de senha, inclusive rotas administrativas;
* concorrência de login/sessão;
* provisioning/integração com Identity;
* segurança HTTP, RFC 9457 e contratos OpenAPI/DTO relevantes.

Registro de usuário não deve ser tratado como responsabilidade de Authentication se o fluxo vigente estiver em Identity/Membership. Rotas removidas ou transferidas devem ser marcadas como não aplicáveis na matriz.

## Execução

1. Inventariar produção + testes atuais.
2. Mapear cada regra/endpoint para testes existentes.
3. Classificar: coberto / parcial / ausente / não aplicável.
4. Implementar apenas gaps de risco real.
5. Executar JaCoCo e PIT sobre o escopo aplicável; não criar testes artificiais apenas para perseguir percentual.
6. Garantir determinismo e CI verde.

## Critérios de aceite

* \[ \] matriz baseada na develop atual;
* \[ \] nenhum cenário duplicado sem justificativa;
* \[ \] principais fluxos e rejeições de Authentication possuem cobertura proporcional ao risco;
* \[ \] segurança, persistência e erros HTTP relevantes auditados;
* \[ \] concorrência e lifecycle de sessão considerados;
* \[ \] JaCoCo analisado e gaps relevantes tratados ou justificados;
* \[ \] PIT executado nas classes de domínio/serviço selecionadas e sobreviventes relevantes analisados;
* \[ \] `./mvnw test` e `./mvnw verify` passam;
* \[ \] PR descreve claramente o que já existia e o que foi acrescentado;
* \[ \] evidências e limitações registradas.

## Regra de escopo

Esta task é de **hardening da suíte existente**. Não alterar comportamento de produção apenas para facilitar testes. Bug funcional descoberto deve ser documentado e corrigido de forma explícita, com teste de regressão.

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



## BACK-252 — Validar árvore final e consolidar evidências do Authentication

## Objetivo

Encerrar o hardening de Authentication com evidência reproduzível sobre a árvore final.

## Aceite

* `mvnw test` e `mvnw verify` verdes;
* JaCoCo final comparado ao baseline da BACK-246;
* PIT final registrado;
* matriz produção→teste→gap atualizada;
* limitações/não aplicáveis documentados;
* PR/commit referencia o trabalho realizado.



## BACK-251 — Executar PIT focal e analisar mutações do Authentication

## Objetivo

Executar mutation testing apenas nas classes de domínio/serviço de Authentication onde a mutação tem valor semântico.

## Escopo

Priorizar AuthenticationAccount, Session, RefreshToken, PasswordChangeChallenge, PasswordRecoveryRequest e serviços críticos selecionados pela auditoria.

## Aceite

* relatório PIT reproduzível;
* SURVIVED/NO_COVERAGE relevantes identificados por classe/método;
* gaps reais recebem teste;
* equivalentes ou caminhos defensivos são justificados;
* não perseguir 100% artificialmente.



## BACK-250 — Analisar cobertura de testes do Authentication

## Superseded

O escopo foi absorvido pela BACK-246. JaCoCo passa a fazer parte da auditoria e do baseline do módulo, não de uma etapa isolada.

Mantida apenas para rastreabilidade histórica.

## BACK-249 — Implementar teste de sistema do fluxo login até /me

## Superseded

O escopo foi absorvido pela BACK-247. Fluxo de sistema só será criado se a matriz provar gap real; Authentication já possui cobertura HTTP/segurança vigente.

Mantida apenas para rastreabilidade histórica.

## BACK-248 — Implementar testes de integração do Authentication

## Superseded

O escopo foi absorvido pela BACK-247. Os gaps de integração serão tratados junto aos demais gaps comportamentais, sem duplicar a suíte existente.

Mantida apenas para rastreabilidade histórica.

## BACK-247 — Fechar gaps comportamentais do Authentication

## Objetivo

Implementar somente os gaps comprovados pela auditoria e pelo JaCoCo físico da develop, independentemente de serem unitários, integração ou sistema.

## Baseline físico

Develop `0d8542fb3d90768573dc738cea85b23c1701b44c`:

* LINE 53,19%;
* BRANCH 43,52%.

## Prioridades comprovadas

* `RefreshTokenService`, `AdminRefreshTokenService`, `AdminSessionService` e `AdminPasswordRecoveryService`, atualmente sem cobertura de linha;
* rotação, expiração e revogação de refresh/session;
* recovery: adapters, mapper, persistência e fluxos administrativos;
* boundaries de primeiro acesso/provisioning;
* domínio: RefreshToken, PasswordRecoveryRequest e Session, com branches relevantes descobertos;
* HTTP administrativo, handlers, RFC 9457, segurança e OpenAPI onde houver gap real.

## Regras

Reaproveitar a suíte atual. Não recriar happy paths já cobertos nem alterar produção apenas para elevar cobertura. Bugs reais exigem regressão explícita.

## Aceite

* gaps relevantes acima tratados ou justificados;
* testes determinísticos;
* `mvnw test` verde;
* incremento de cobertura medido sem objetivo artificial de percentual.



## BACK-246 — Auditar Authentication e mapear gaps reais da suíte

## Objetivo

Revalidar o módulo Authentication contra a `develop` atual e substituir a decomposição antiga baseada em “tipos de teste” por uma matriz de comportamento real.

## Escopo

* inventariar `api/core/infra`, endpoints, serviços, domínio e boundaries;
* mapear produção → teste existente → coberto/parcial/ausente/não aplicável;
* medir JaCoCo do pacote `iam.authentication` como baseline;
* identificar gaps de risco real sem duplicar LoginControllerTest, SecurityIntegration, Session, JWT, recovery, provisioning e persistência já existentes.

## Aceite

* matriz baseada no HEAD atual da develop;
* JaCoCo LINE/BRANCH registrado;
* fluxos obsoletos marcados como não aplicáveis;
* lista objetiva de gaps para BACK-247.



