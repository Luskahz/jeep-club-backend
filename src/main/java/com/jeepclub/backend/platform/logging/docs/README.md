# Contrato global de logging e observabilidade HTTP

## Responsabilidade

`platform.logging` é o único dono da observabilidade HTTP transversal. Ele cria a
correlação, classifica a plataforma cliente, enriquece o MDC com a identidade já
autenticada, emite uma linha operacional ao final do request e limpa o contexto.

Controllers, services, repositories e adapters continuam usando somente o SLF4J
da própria classe para fatos técnicos ou de domínio. Nenhum módulo deve criar um
filtro HTTP, wrapper de logger ou transporte manual de `requestId` para reproduzir
este pipeline.

## Dois contratos distintos

### Logging operacional HTTP

`SystemRequestLoggingFilter` cobre os requests não excluídos, mede sua duração e
chama `HttpRequestLogWriter`. O resultado é uma única linha SLF4J, sem fila, banco
ou worker:

```text
http_request requestId=82fd91 method=GET path=/vehicles/{vehicleId} status=200 durationMs=37 device=WEB userId=7 userName="Lucas Alves"
```

O `path` usa o pattern resolvido pelo Spring MVC quando disponível e, caso
contrário, a URI sem query string. Campos de identidade ausentes usam `-` e
device ausente ou inválido usa `UNKNOWN`.

Severidade:

- `INFO`: 2xx e 3xx;
- `WARN`: 4xx;
- `ERROR`: 5xx e falhas não tratadas, inclusive antes de o container consolidar
  o status 500.

Uma falha interna ao escrever o log operacional é isolada e não muda status,
headers ou body da resposta de negócio.

### System Logs persistidos

`SystemLogService`, `SystemLogEvent`, `SystemLogServiceAdapter`, a fila e
`SystemLogPersistenceWorker` formam outro contrato. Ele existe para eventos cuja
retenção em banco seja uma decisão explícita. A conclusão de um request HTTP
comum não chama esse serviço.

O adapter mantém fila em memória limitada a 5.000 eventos. O worker drena lotes
de até 100, por padrão a cada 1.000 ms, e os persiste em `platform_logs`. Falhas
de persistência recolocam o lote enquanto houver capacidade. A retenção padrão é
90 dias e o cleanup roda diariamente após o atraso inicial configurado.

Propriedades:

- `platform.logging.persistence-delay-ms` (default `1000`);
- `platform.logging.retention-days` (default `90`);
- `platform.logging.cleanup-initial-delay-ms` (default `60000`);
- `platform.logging.cleanup-delay-ms` (default `86400000`).

Use `SystemLogService` somente quando o produto ou requisito técnico exigir
persistência, consulta e retenção do evento. Não o use como substituto de
`log.info`, `log.warn` ou `log.error`.

## Request ID e `X-Request-Id`

O request ID é criado no início do request e devolvido no header
`X-Request-Id`, inclusive para requests anônimos e respostas 401/403 quando o
filtro é aplicável.

Um valor externo só é reutilizado quando:

- não está vazio;
- possui no máximo 100 caracteres;
- contém apenas letras ASCII, números, `.`, `_`, `:`, ou `-`.

CR, LF, espaços, outros caracteres de controle e qualquer valor fora desse
conjunto invalidam o header. Nesse caso o backend gera um UUID novo e devolve o
valor seguro. Request ID é correlação, não identidade ou autorização.

## MDC e ordem dos filtros

O pipeline possui duas fases coordenadas:

1. `SystemRequestLoggingFilter`, com maior precedência de servlet filter, limpa
   eventual resíduo da thread, resolve `requestId` e `device`, os coloca no MDC e
   envolve a cadeia do Spring Security;
2. `RequestContextEnrichmentFilter`, registrado apenas dentro da security chain
   logo após `JwtAuthenticationFilter`, adiciona `userId` e `userName` quando o
   `UserPrincipal` existe.

Assim, `requestId` e `device` já existem durante autenticação, autorização, 401,
403 e processamento MVC. A identidade só aparece depois de autenticação válida.
O filtro de enriquecimento remove as chaves de identidade ao retornar; o filtro
externo restaura os valores internos apenas para a linha final e executa
`MDC.clear()` em `finally` em sucesso, 4xx, 5xx ou exceção.

Chaves disponíveis durante o trecho aplicável:

- `requestId`;
- `device`;
- `userId` para request autenticado;
- `userName` para request autenticado.

O pattern global de console inclui essas chaves com placeholders controlados,
portanto logs SLF4J internos herdam a correlação sem integração por módulo.

## Identidade autenticada e JWT

`userId` continua sendo a identidade interna canônica. O access token usa o
subject para esse ID e preserva a claim de sessão `sid`, tipo `typ`, emissão e
expiração.

O access token também contém a claim documentada `name`. `JwtTokenParser`
recupera essa claim e `JwtAuthenticationFilter` a propaga por
`JwtAuthenticatedUser` até `UserPrincipal.userName`.

`userName` é exclusivamente contexto humano e observacional:

- não participa de autorização ou regra de negócio;
- não substitui `userId` em lookup;
- pode ficar desatualizado até a emissão ou renovação normal do access token;
- não justifica consulta adicional durante cada request.

O refresh token não foi alterado. CPF, RG, telefone, e-mail e outros dados
pessoais não são adicionados ao JWT para logging.

## Client/device e `X-Client-Platform`

Clientes podem enviar `X-Client-Platform`. A classificação é case-insensitive e
aceita somente:

- `WEB`;
- `ANDROID`;
- `IOS`.

Ausência, valor desconhecido, vazio ou maior que 16 caracteres resulta em
`UNKNOWN`, sem falhar o request. Não há fallback por `User-Agent`; o valor
integral desse header nunca é copiado para o MDC ou log operacional. Device é
metadado observacional e não participa de autenticação, autorização, antifraude
ou regra de negócio. A classificação não executa I/O.

## Privacidade

O pipeline padrão não lê nem registra:

- `Authorization`, JWT, access token ou refresh token;
- cookies ou token de sessão;
- senha;
- request/response body;
- query string integral;
- CPF, RG, telefone ou e-mail;
- activation/recovery token;
- `User-Agent` integral.

Paths não incluem query string. Valores controlados usados na linha são
limitados e caracteres de controle são neutralizados para impedir quebra de
linha. As categorias Web/Hibernate capazes de expor payload ou bind parameter
permanecem protegidas na configuração global.

## Uso correto nos módulos

```java
private static final Logger log = LoggerFactory.getLogger(VehicleService.class);

public void disable(Long vehicleId) {
    // regra de negócio
    log.info("vehicle_disabled vehicleId={}", vehicleId);
}
```

Durante um request coberto, o pattern global anexará o MDC a esse log. O módulo
registra somente o fato que conhece; o Platform fornece o contexto transversal.

Para um evento que precisa ser consultado e retido como System Log, injete
`SystemLogService` e construa um `SystemLogEvent` intencionalmente. Essa decisão
deve vir do requisito do evento, não do simples fato de existir um request.

## Anti-patterns

Não faça:

```java
service.execute(command, requestId); // requestId passado apenas para logging
platformLogger.info(...);            // wrapper local do logger global
systemLogService.record(...);        // para toda conclusão HTTP comum
```

Também são proibidos filtro/interceptor HTTP por módulo, parsing local de JWT,
consulta ao banco apenas para enriquecer log, body logging global e uso de MDC
como entrada de autorização ou regra de negócio.

## Async e executors

MDC usa estado associado à thread e não é propagado automaticamente para
`@Async`, `CompletableFuture`, pools ou executors. Código assíncrono não deve
presumir que as chaves do request estarão presentes. Quando uma correlação
assíncrona for realmente necessária, ela exige solução explícita, com cópia e
cleanup do contexto no executor; este contrato não fornece essa propagação.

## Configuração por ambiente e SQL

`spring.jpa.show-sql` e `hibernate.format_sql` são `false` por padrão. SQL não é
o mecanismo de observabilidade da aplicação e pode conter dados sensíveis.
Desenvolvimento pode habilitá-los deliberadamente com
`SPRING_JPA_SHOW_SQL=true` e `SPRING_JPA_FORMAT_SQL=true`, mantendo bind
parameters desligados.

O pattern de console pode ser sobrescrito por configuração externa normal do
Spring Boot. Qualquer override deve preservar os campos MDC e as proteções de
privacidade. Nenhuma configuração específica por módulo é necessária.

Endpoints técnicos atualmente excluídos do log operacional são Swagger/OpenAPI,
`/favicon.ico` e `/actuator/health`; mudanças nessa lista precisam ser explícitas
e testadas.
