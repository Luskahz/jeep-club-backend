# Identity

Criação administrativa de User e integração com Authentication: [BACK-511](../../../../../../../../../docs/back-511-admin-user-creation.md).

Leia primeiro a [governança global](../../../../../../../../../docs/architecture/README.md), a
[organização dos módulos](../../../../../../../../../docs/architecture/module-organization.md)
e as [regras de desenvolvimento](../../../../../../../../../docs/architecture/feature-development-rules.md).
Este documento descreve somente o bounded context `iam.identity`. O OpenAPI
gerado pelos controllers e DTOs do módulo é a fonte de verdade do contrato HTTP.

## Responsabilidade e limites

Identity é proprietário do agregado `User`: identificador estável, nome,
`birthDate`, e-mail, CPF, RG, telefone, referência global de foto de perfil, estado
administrativo e timestamps de criação, desativação e atualização. Também é
proprietário do cadastro, das consultas cadastrais e do lifecycle administrativo
do usuário.

Identity não é proprietário de senha, hash, login, lock, sessões, access token
ou refresh token: esses conceitos pertencem a Authentication. Tampouco possui
roles, permissions ou authorities, que pertencem a Authorization. Para proteger o
sistema contra lockout administrativo, o lifecycle administrativo de Identity consulta
a porta consumer-owned `UserAuthorizationProtectionPort` (adaptada via `RoleQuery`
de Authorization) e impede a desativação de usuário portador da role estrutural
ROOT, retornando 409 CONFLICT com código `USER_ROOT_CANNOT_BE_DISABLED` sem produzir
efeitos em Identity ou Authentication. O `enable` administrativo continua permitido
para recuperação de estado.

`User` é criado com `UserStatus.ACTIVE`. O único outro estado é `DISABLED`:
`disable` define `disabledAt` e `updatedAt`; `enable` remove `disabledAt` e
atualiza `updatedAt`. Desabilitar um usuário já desabilitado e habilitar um
usuário ativo são conflitos. O agregado não conhece lock de autenticação nem
estado de credencial.

## Dados cadastrais e normalização

Nome e textos opcionais são aparados; e-mail é aparado e convertido para
minúsculas. CPF, RG e telefone são persistidos somente com dígitos. CPF tem
exatamente 11 dígitos e CPF, e-mail e RG são únicos na persistência. Campos
opcionais em branco se tornam ausentes.

O User pode existir permanentemente sem e-mail. O próprio usuário autenticado
pode cadastrar ou substituir um endereço por `PATCH /identity/me/email`; a
operação usa o `UserPrincipal`, exige um e-mail válido, normaliza o valor,
protege a unicidade e atualiza `updatedAt` sem alterar Membership ou Authentication.

### Atualização cadastral própria — BACK-424 / BACK-391

O perfil próprio permite editar nome, nascimento, RG, telefone e
`profilePhotoStorageKey`, usando exclusivamente o ID do `UserPrincipal` autenticado.
A edição pertence a Identity e não reescreve a solicitação histórica de Membership,
credenciais, sessões ou vínculos de Authorization. Alterar o nome não exige novos
tokens: o claim `userName` é apenas contexto de exibição; a consulta de perfil lê o
cadastro atualizado pelo ID estável.

Campos omitidos preservam o valor atual; `null` remove nascimento, RG, telefone ou a
associação da foto. Nome não aceita `null` nem branco. RG/telefone em branco continuam
normalizados para ausência; pontuação é removida e o limite vigente de 20 dígitos
canônicos é preservado. Nome é aparado e limitado a 150 caracteres. Nascimento usa
data ISO e conserva a semântica atual do modelo: não se introduz regra nova de idade
ou bloqueio de datas futuras. Um objeto vazio não salva nem altera `updatedAt`.

CPF, IDs, estado administrativo, roles, permissions e timestamps não são editáveis.
O novo contrato rejeita campos desconhecidos/proibidos, inclusive `email` (que possui
fluxo próprio) e a antiga `profilePhotoUrl`, evitando ignorar silenciosamente uma
tentativa de escrita. O schema OpenAPI é a fonte dos campos, erros e exemplos HTTP.

O service toma lock pessimista do próprio User antes de resolver os campos omitidos.
RG canônico é verificado contra outros usuários; a constraint de unicidade no banco
permanece como proteção contra corrida. Conflitos de edição retornam 409
`USER_RG_ALREADY_IN_USE`. Todos os valores são validados antes da mutação do agregado.

Fotos referenciam a chave retornada pela API global de mídia, sem upload específico
em Identity. `ProfileImagePort` isola a necessidade de verificar a imagem existente;
seu adapter usa `platform.storage.image.ImageMediaService`. Uma chave enviada é
validada e resolvida antes de salvar o perfil; URLs arbitrárias ou chaves inválidas
não são aceitas. Omitir a foto preserva a associação sem consultar o arquivo;
`null` a remove sem excluir bytes potencialmente compartilhados. O endpoint de foto
dedicado preexistente continua disponível. Essa capacidade depende da integração
global de mídia entregue no PR #69.

O nome do campo HTTP de nascimento é `birthDate`. Não há alias `birthData` no
DTO ou na serialização atual. Formatos aceitos, limites e exemplos do request,
bem como o formato canônico retornado, pertencem aos schemas OpenAPI.

## Edição administrativa — BACK-522

A superfície administrativa reutiliza `ProfileUpdate.Field` para distinguir omissão
(deixar como está), null (remover opcional) e valor informado. `AdminUserUpdate`
compõe esse comando com CPF e e-mail. `User.updateAdministratively` valida antes de
mutar; IDs, status e timestamps de lifecycle permanecem sob seus fluxos próprios.
CPF enviado exige formato e dígitos verificadores válidos; edição de outros campos
preserva CPF legado sem impor backfill. Identificadores únicos são verificados
excluindo o próprio ID e protegidos pelas constraints já existentes. `{}` e valores
que normalizam para o cadastro atual não salvam nem alteram `updatedAt`.

`AdminUserService` bloqueia User, salva/flush, coordena os efeitos por SPI e registra
um evento `IDENTITY_USER_UPDATE:<campo>` por campo realmente alterado, na mesma
transação. O evento contém ator, alvo no path, operação e instante; não contém
valores pessoais. `recordInTransaction` faz a alteração e auditoria confirmarem ou
reverterem juntas, conforme retenção de Platform. O perfil complementar usa as
mesmas regras e DTOs do self-service; a contraparte administrativa delega a
`UserProfileService` e audita os nove campos com `IDENTITY_USER_PROFILE_UPDATE`.

Troca efetiva de CPF ou e-mail revoga sessões, refresh tokens e challenges por
`UserAuthenticationAdministrationPort.identityChanged`. Troca de e-mail também
invalida convite administrativo e recuperação por e-mail em Authentication, e
convite de Membership por `UserContactChangePort`, sem reescrever a solicitação
histórica. Não envia um novo convite automaticamente: o administrador usa o fluxo
de reenvio do proprietário. CPF continua apenas em Identity e o login consulta o
valor atual; FK/vínculos continuam usando o ID estável. Senha, estado da credencial,
lock automático, tentativas e status administrativo são preservados.

A política ROOT permite CPF/e-mail somente quando o ator é o próprio alvo, já
portador de `IDENTITY_USER_UPDATE`; outro administrador recebe 409. Os demais
campos podem ser corrigidos por um administrador autorizado. Usuários DISABLED
podem ser editados sem reativação. BACK-512/BACK-514 estão pendentes: não se antecipa
ACTIVE/INACTIVE/ARCHIVED nesta feature; quando integradas, devem continuar isoladas
da edição cadastral.

`UserIdentityLock` é leitura pública transacional (`MANDATORY`) para serializar
login, emissão/consumo de convites e recuperação por CPF com correções cadastrais.
A ordem é User antes de conta/token; não expõe entity ou repository. O envio de
convite administrativo verifica e bloqueia o cadastro em transação nova após o
commit para impedir entrega a um e-mail substituído. Timeout/deadlock/concorrência
traduzidos em `USER_CONCURRENT_MODIFICATION` (409) devem ser repetidos pelo cliente;
violação de unicidade/ROOT usa `USER_UPDATE_CONFLICT` e conta ausente usa
`USER_IDENTITY_CHANGE_CONFLICT`.

Orientações de integração: [edição administrativa](../../../../../../../../../docs/back-522-admin-user-update.md).
O contrato HTTP completo permanece no OpenAPI.

## Lifecycle composto com Authentication

Identity define as necessidades de Authentication em
`api.module.spi.UserAuthenticationProvisioningPort` e
`UserAuthenticationAdministrationPort`. Os adapters de Authentication em
`authentication.infra.integration.identity` implementam essas portas; Identity
não acessa service, repository, entity ou hash interno daquele módulo.

- `UserRegistration` cria o User e solicita provisionamento de credencial.
  Registro HTTP provisiona e autentica; os fluxos internos também podem criar
  credencial permanente ou pendente de primeiro acesso.
- `UserAdministration.disable` bloqueia o User e solicita que Authentication
  desabilite o acesso e revogue credenciais ativas. A operação é transacional:
  falha no efeito de Authentication desfaz a transição de Identity.
- `UserAdministration.enable` reativa o User e solicita habilitação de acesso.
  Não desbloqueia a conta, não zera tentativas, não altera senha ou estado da
  credencial e não recria sessões/tokens.

O acesso administrativo de Authentication, lock e `CredentialStatus` continuam
estados independentes. Um User administrativamente ativo pode estar bloqueado
ou pendente de primeiro acesso e ainda ser ativo para os consumidores de
`UserQuery`.

## Contratos e integrações públicas

`api.module` é a fronteira Java pública. Suas assinaturas continuam no código;
os contratos são:

- `UserQuery`: consultas somente leitura de User, existência por ID/CPF/e-mail
  e atividade administrativa, inclusive consultas em lote. É consumido por
  Authentication e por adapters de Authorization, Billing, Memberships,
  Dependents, Vehicles e Health.
- `UserProfileQuery` (BACK-408): contrato de Identity que expõe somente a
  consulta informativa de completude do perfil complementar. Valida a existência
  do User, inclusive quando chamado isoladamente; User inexistente é erro,
  enquanto perfil inexistente implica pendência. Consumidores não acessam
  repository/entity internos para obter esses dados.
- `UserRegistration` e `UserRegistrationData`: criação composta de User com
  credencial permanente, pendente de primeiro acesso ou já autenticada. O
  bootstrap de desenvolvimento o consome diretamente; o fluxo de Memberships
  chega a ele pelo adapter de Authentication que implementa o contrato do
  consumidor.
- `UserAdministration`: transições administrativas compostas de disable/enable
  e seus efeitos em Authentication; o controller administrativo o usa por meio
  de `AdminUserService`.
- `UserDetails`, `UserAuthenticationTokens` e `UserStatus`: valores de
  transporte dos contratos acima, sem expor entity, repository ou modelo JPA.
- `UserAuthenticationProvisioningPort` e
  `UserAuthenticationAdministrationPort`: SPIs proprietários de Identity que
  Authentication implementa para provisionamento e administração de acesso.

`DevelopmentAdminUserBootstrapService` usa `UserQuery` e `UserRegistration`
para assegurar o usuário administrativo configurado. O bootstrap de role ROOT
é responsabilidade de Authorization; essa composição não muda o ownership de
User, conta ou role.

## Consulta administrativa e contrato HTTP

O OpenAPI descreve registro público, `/identity/me` autenticado e a superfície
administrativa de usuários. As operações administrativas usam `@PreAuthorize`
para enforcement e `@RequiredPermission` apenas para publicar a mesma authority
no OpenAPI: `IDENTITY_USER_READ`, `IDENTITY_USER_CREATE`, `IDENTITY_USER_UPDATE`,
`IDENTITY_USER_DISABLE` e `IDENTITY_USER_ENABLE`.

A listagem administrativa retorna o envelope paginado transversal
`PageResponse<AdminUserResponseDTO>`. Aceita
paginação zero-based (`page`, `size`; padrão 20 e máximo 50), `sort` com os
campos permitidos, filtros cadastrais, busca `q` e `fields`. Quando `fields`
é omitido ou vazio, todos os campos de `AdminUserField` são selecionados;
campos não solicitados podem ser omitidos do item retornado. Filtros, seleção,
ordenação e paginação são executados na consulta de Identity, não por leituras
de Authentication ou outros contextos.

`/identity/me` usa o `UserPrincipal` já autenticado para obter o ID e consulta
Identity somente para os dados cadastrais. A documentação detalhada de paths,
parâmetros, schemas, validações, respostas e erros RFC 9457 está no OpenAPI;
não há catálogo Markdown concorrente.

## Persistência e concorrência

### Perfil complementar — BACK-408

O perfil complementar pertence exclusivamente a Identity e possui associação
opcional 1:1 com User. `UserProfile` reúne `WorkProfile` e `ResidentialAddress`,
sem dependências HTTP/JPA. Sua ausência não altera onboarding, autenticação,
Membership ou o lifecycle ACTIVE/DISABLED.

A pendência é derivada: ocupação e local de trabalho devem estar preenchidos,
assim como CEP, logradouro, número (ou S/N), bairro, cidade e UF; complemento
é opcional. Dados parciais são aceitos. Textos são aparados e branco vira null;
CEP brasileiro aceita oito dígitos com hífen opcional, persistindo só dígitos;
UF válida é normalizada para maiúsculas. Os limites de texto são aplicados após
aparar, e não ao payload bruto. Na revisão de 2026-10-02, o frontend acessível
[jeep-club-tamoios-caragua](https://github.com/Vitorram/jeep-club-tamoios-caragua/tree/da893d9502d387bf549211924358c179e4252a74)
continuava com `src/schemas/index.ts` e `src/services/index.ts` vazios, sem
contrato de perfil/endereço nos tipos ou componentes. Mantém-se este conjunto
mínimo, sem campos adicionais por inferência.

O bloco complementar tem recurso próprio de leitura e substituição integral,
separado da edição parcial dos dados básicos existente na BACK-391. Ambos usam
somente o principal autenticado. No PUT, omitir `workProfile` ou `address`, ou
enviar o bloco como `null`, remove o respectivo bloco. Campos internos omitidos,
`null` ou em branco são removidos; `{}` limpa todo o perfil complementar.
Preenchimento parcial mantém a pendência. Não se preservam valores omitidos
como no PATCH da BACK-391. O OpenAPI detalha esse contrato.

A resposta de `/identity/me` consulta `UserQuery` e depois `UserProfileQuery`,
que verifica novamente a existência do User. Essa leitura redundante foi mantida:
o contrato público deve validar chamadas isoladas, e uma composição adicional ou
uma variante que dispense validação apenas para economizar essa consulta não se
justifica nesta Story. Não se introduz novo contrato público.

`identity_user_profiles` usa o user_id como PK e FK, garantindo um único perfil
por User. O upsert bloqueia o User na mesma transação antes de consultar/criar
o perfil, inclusive no primeiro preenchimento concorrente. Não há booleano de
completude persistido. Ler um perfil ausente não cria uma linha no banco.

### Cadastro básico

`UserEntity`, repository JPA, mapper, adapter e read model administrativo
ficam em `infra.persistence`; o domínio não depende de JPA. `identity_users`
aplica unicidade a CPF, e-mail e RG. Alterações de lifecycle tomam lock
pessimista em User e participam da mesma transação do efeito em Authentication.

Há uma associação física legada de chave compartilhada entre User e
`AuthenticationAccount`, necessária à PK/FK existente. Ela não permite acesso
cross-module a modelos internos. A política de schema é a global atual: entities
e Hibernate representam mudanças e não há migrations versionadas obrigatórias.

## Testes

Os testes do módulo caracterizam serialização de `birthDate`, registro,
normalização, lifecycle transacional, read model administrativo, paginação,
filtros, sparse fields, sort e persistência. Alterações futuras devem escolher
cobertura proporcional conforme as
[regras globais](../../../../../../../../../docs/architecture/feature-development-rules.md#testes-m%C3%ADnimos).


## Exportações CSV/PDF — BACK-410

UserQuery.findByIds e findAfterId oferecem leitura administrativa em lote de até 500, incluindo cadastros desativados para auditoria. Export mantém filtros AdminUserFilter existentes; foto aparece somente como booleano. IDENTITY_USER_EXPORT é independente de READ.

Contratos HTTP, limites, segurança e evidências estão em `docs/exports/` na raiz do repositório. As exportações são administrativas, sem paginação HTTP, com auditoria síncrona e `Cache-Control: no-store`.
