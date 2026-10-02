# Regras globais para features e fixes

Este guia resume como consumir a arquitetura transversal já existente. A
estrutura e as fronteiras estão em
[module-organization.md](module-organization.md); detalhes de um domínio ficam
nos docs do módulo proprietário.

## Checklist de início e conclusão

Antes de implementar:

1. leia o [índice global](README.md) e este guia;
2. identifique o bounded context proprietário da regra e do dado;
3. leia `<module>/docs/README.md` e somente as notas específicas necessárias;
4. inspecione o código vigente, os contratos intermodulares e o OpenAPI afetado;
5. confirme que o escopo não depende de comportamento apenas planejado.

Antes de concluir:

1. valide regra, contrato HTTP, autorização, erros e integração transversal que
   a mudança realmente afetou;
2. preserve a suíte existente;
3. atualize somente a fonte documental proprietária;
4. trate mudança observável de API como evolução de contrato, não como simples
   ajuste interno.

## Usuário autenticado

`platform.security` valida o access token e publica um `UserPrincipal` no
`SecurityContext`. Esse é o mecanismo padrão para obter a identidade já
autenticada na requisição. O principal contém:

- `userId`, identidade interna canônica;
- `sessionId`;
- `accessTokenExpiresAt`;
- `userName`, contexto humano não autoritativo que pode ser nulo em token
  legado e não participa de autorização ou lookup de negócio.

Controllers recebem o principal com `@AuthenticationPrincipal` ou usam o
`Authentication` fornecido pelo Spring quando precisam, de forma explícita, das
authorities. Uma feature não reprocessa JWT, não cria principal próprio por
módulo e não consulta outro módulo para reconstruir `userId`, `sessionId`, nome
ou expiração já disponíveis.

Se o caso de uso precisar de informação adicional — atividade administrativa,
ownership, perfil ou outro estado de domínio — consulte o módulo proprietário
por `api.module` ou por porta/adapters conforme a fronteira definida. O
principal não substitui uma consulta de domínio.

## Autenticação, autorização e ownership

`SecurityConfig` mantém a API stateless, autentica as rotas não públicas e
habilita method security. Para uma operação protegida por authority global, o
enforcement vigente é `@PreAuthorize`, normalmente com
`hasAuthority('MODULO_RECURSO_ACAO')`. Evite repetir a mesma checagem manual em
controllers ou services quando method security resolve o caso.

`@RequiredPermission` pertence a `platform.openapi`: o customizer adiciona
`bearerAuth`, `x-required-permissions` e texto de permission à operação
OpenAPI. A anotação é **documental** e não concede, nega nem valida acesso. Em
uma rota com permission fixa, seu valor deve refletir exatamente o
`@PreAuthorize` que faz enforcement. Rotas públicas não anunciam permission e
devem remover o requisito global no `@Operation` com `security = {}`.

São responsabilidades diferentes:

- authority global: acesso à categoria de operação, aplicado por method
  security;
- ownership/regra de objeto: decisão do caso de uso com base no `userId` e no
  estado do recurso, aplicada no core do módulo proprietário.

Recursos exclusivos de membros podem declarar `@RequiresMembership`. A
annotation usa um advisor próprio de Spring Method Security, obtém
`UserPrincipal.userId` e delega ao contrato público de Membership. Ela compõe
com `@PreAuthorize`: autenticação, membritude e permission continuam condições
independentes. O advisor não consulta Billing nem contém regra financeira;
Membership faz essa integração por contrato público. O customizer OpenAPI da
annotation documenta os possíveis erros 402 e 503 sem participar do enforcement.

Uma rota deliberadamente compartilhada entre o dono do recurso e um usuário
privilegiado pode precisar das authorities para escolher o caminho autorizado,
como ocorre no download de comprovante de Billing. Isso é um contrato explícito
e testado, não um incentivo a espalhar verificações manuais.

## Erros HTTP e RFC 9457

O contrato de erro usa `ProblemDetail`/RFC 9457 com media type
`application/problem+json`. `ApiErrorResponse` acrescenta `code`, `timestamp` e,
para validação de campos, `errors`.

- `GlobalExceptionHandler` trata validação e falhas comuns ou inesperadas;
- `ApiProblemFactory` cria o corpo localizado e usa o `Clock` global;
- `ApiProblemResponseWriter` leva o mesmo contrato a 401/403 produzidos nos
  filtros de segurança;
- handlers específicos de módulo estendem `ApiExceptionHandler` e traduzem
  outcomes daquele bounded context para status e códigos controlados.

Não crie outro envelope de erro, não exponha mensagem técnica/SQL/stack trace e
não deixe exceção de infraestrutura definir o contrato HTTP. Erros relevantes
da operação devem aparecer no OpenAPI com status e schema
`ApiErrorResponse`; detalhes de tradução pertencem ao módulo que conhece a
semântica.

## Tempo determinístico

`platform.time.TimeConfig` fornece um `Clock` UTC. Regras de negócio ou de
aplicação dependentes do “agora” recebem esse `Clock` e usam, por exemplo,
`Instant.now(clock)`, `LocalDate.now(clock)` ou recebem o instante/data como
argumento de domínio. Isso torna expiração, lifecycle e testes determinísticos.

Não transforme a regra em proibição mecânica para código estritamente técnico
ou fallback isolado em teste. O objetivo é impedir relógio global oculto em
decisões de negócio.

## Logging e MDC

`platform.logging` é o proprietário único da observabilidade HTTP transversal.
Ele cria/valida `X-Request-Id`, classifica o cliente, enriquece e limpa o MDC,
inclui a identidade autenticada quando disponível e registra a conclusão do
request. Consulte o
[contrato especializado de logging](../../src/main/java/com/jeepclub/backend/platform/logging/docs/README.md).

Regras de consumo:

- módulos usam SLF4J normalmente para fatos técnicos ou de domínio;
- não passe `requestId` entre camadas apenas para logging;
- não crie filtro, interceptor ou wrapper HTTP por módulo;
- `SystemLogService` é para eventos com decisão explícita de persistência e
  retenção, não o logger operacional comum;
- não registre token, senha, cookie, body, dado médico ou pessoal sensível;
- falha ao emitir logging não altera status, body ou resultado funcional.

## Storage global e `storageKey`

O contrato vigente é:

```text
módulo consumidor -> shared.storage -> platform.storage -> provider/filesystem
```

`shared.storage` expõe `FileStorage` e tipos/exceções neutros. `platform.storage`
compõe o bean Spring, seleciona o provider e concentra filesystem, I/O,
confinamento de path e segurança técnica. O provider implementado atualmente é
`LOCAL`. Consulte o
[contrato especializado de storage](../../src/main/java/com/jeepclub/backend/platform/storage/docs/README.md).

O módulo consumidor mantém validação semântica (MIME, extensão e tamanho
permitidos), autorização, ownership, namespace, lifecycle e compensações. Ele
persiste a `storageKey` como identidade do objeto. Não persiste path físico,
root directory, provider, bucket, hostname ou URL física no lugar da chave.

## Persistência e integrações

- use o repository do core do módulo proprietário;
- mantenha entity JPA, Spring Data repository, mapper e adapter em
  `infra.persistence`;
- não deixe o domínio depender de JPA ou de DTO HTTP;
- não consulte repository, entity, service ou tabela interna de outro módulo;
- use `api.module`/`*Query` para um contrato público simples ou porta
  consumer-owned + `infra.integration` para traduzir/isolar a necessidade;
- relação física no banco não elimina boundaries lógicos.

Não há política global de migrations versionadas nesta fase. Não adicione
Flyway, Liquibase ou migrations obrigatórias sem decisão arquitetural e escopo
explícitos.

## Testes mínimos

Não existe uma pirâmide ou nomenclatura única imposta. Escolha o menor conjunto
que prove o comportamento alterado. Quando aplicável, cubra:

- regra de domínio/aplicação, incluindo estados e limites temporais;
- contrato HTTP, validação, status e corpo;
- autenticação, authority e ownership;
- tradução de erros relevantes;
- repository/adapter ou integração transversal afetada;
- concorrência ou compensação quando fizer parte da regra.

Preserve os testes existentes. Se um teste contradisser o contrato real, não
deforme produção apenas para fazê-lo passar: caracterize a divergência e
corrija a fonte errada dentro do escopo autorizado.

## OpenAPI como contrato HTTP

`platform.openapi` define `bearerAuth` e customizações comuns. Controllers e
DTOs completam o contrato observado pelo frontend:

- `@Tag` identifica e descreve a superfície;
- cada operação usa `@Operation` com summary preciso e description quando ela
  acrescentar comportamento relevante;
- path/query parameters, paginação e restrições observáveis devem ficar
  visíveis, por inferência correta ou `@Parameter`/`@ParameterObject`;
- requests e responses usam os DTOs reais, e `@Schema` documenta significado,
  formato, valores, exemplos e limites coerentes com Bean Validation;
- `@ApiResponse` registra sucessos e erros relevantes, com status, media type e
  schema corretos; erros estruturados usam `ApiErrorResponse`;
- autenticação e permission devem coincidir com `SecurityConfig` e
  `@PreAuthorize`; `@RequiredPermission` apenas publica esse requisito;
- operações públicas sobrescrevem a segurança bearer global com
  `security = {}`;
- `@SwaggerOperationGroup` pode organizar a UI em grupos já usados pelo projeto,
  mas não substitui nenhuma parte do contrato.

Documente o comportamento real, inclusive status e validações observáveis. Não
invente response, exemplo ou permission; não copie esse catálogo para Markdown.
Ao mudar o contrato, ajuste implementação, OpenAPI e testes na mesma entrega.

## Paginação HTTP

Endpoints paginados usam `platform.web.pagination.PageResponse<T>` na fronteira
HTTP. `Page<T>` e `Pageable` do Spring Data podem continuar nas camadas internas,
mas `Page`/`PageImpl` não são serializados diretamente como resposta pública.

O envelope estável contém `content`, `number`, `size`, `totalElements`,
`totalPages`, `numberOfElements`, `first`, `last` e `empty`. Estruturas internas
do Spring Data, como `pageable` e o objeto complexo `sort`, não fazem parte do
contrato de resposta. Os parâmetros de consulta `page`, `size` e `sort`, seus
defaults e limites continuam definidos por cada endpoint e pela configuração
global vigente.

O retorno parametrizado do controller deve permitir ao Springdoc gerar um
schema específico cujo `content.items` referencia o DTO real do endpoint. Os
testes de OpenAPI e MVC devem proteger, respectivamente, esse tipo e o JSON
efetivamente serializado.
