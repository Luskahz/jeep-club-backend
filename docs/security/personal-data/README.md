# Auditoria de dados pessoais — BACK-425 / BACK-409

**Status: proposta para revisão; nenhuma criptografia ou regra funcional foi implementada.**

Tasks: [BACK-425](https://jeep-club-developers.atlassian.net/browse/BACK-425) e
[BACK-409](https://jeep-club-developers.atlassian.net/browse/BACK-409).
Auditoria em 29/09/2026, sobre `develop@b051da139395708aba61a2895ebdce3d51cee967`.

## Entregáveis e conclusão

- [Matriz por campo e inventário de armazenamento](matrix.md).
- [Proposta de proteção, consultas, chaves e rollout](architecture.md).
- [Findings e backlog estimável](backlog.md).

Priorizar criptografia autenticada dos dados clínicos de Health **e dos seus
históricos**, seguida de identificadores de alto impacto e textos privados.
CPF, RG, e-mail, placa e RENAVAM precisam de avaliação conjunta das consultas e
constraints; blind indexes servem para igualdade, não para `LIKE` ou ordenação.
Metadados relacionais e financeiros necessários ao domínio podem permanecer em
claro com controles de acesso e infraestrutura, assumindo o risco residual de
correlação. Arquivos exigem proteção dos bytes, além da referência no banco.

Há cópias de dados pessoais em Memberships, Dependents, Vehicles, Tools,
Publications, auditoria e SMTP. Proteger apenas `identity_users` não resolve o
problema. Remoção operacional normalmente preserva snapshots; não representa
eliminação LGPD. Endereço e perfil profissional da BACK-408 ainda não têm campos
estruturados nesta baseline; entram como requisito futuro, não como armazenamento
atual fictício. Publications e Events, por outro lado, já estão implementados.

## Método e limite da evidência

Revisão estática repo-wide: entidades JPA (inclusive herança JOINED e
`ElementCollection`), modelos, mappers/adapters, repositories e consultas,
DTOs/controllers, portas intermodulares, configurações, bootstrap, logs, storage,
notificações e documentação de lifecycle. As 50 entidades e oito tabelas de
coleções estão relacionadas na matriz. Configurações sem dados pessoais também
foram examinadas e distinguidas dos dados associados a titulares.

Buscas reproduzíveis, a partir da raiz do backend:

```powershell
rg -l '@Entity|@Embeddable|@ElementCollection|@CollectionTable' src/main/java
rg -n 'findBy|existsBy|@Query|Specification|UniqueConstraint|@Index' src/main/java
rg -n 'log\.|LOGGER\.|MDC\.|printStackTrace|@Cache|CacheManager|Redis|Caffeine' src/main/java
rg -n 'Cipher|AttributeConverter|@Convert|MessageDigest|BCrypt|SecureRandom' src/main/java
rg -n 'storageKey|receiptStorageKey|FileStorage|CacheControl|recordRequired' src/main/java
rg --files -g '*.sql' -g '*backup*' -g '*dump*' -g '*fixture*' -g '*compose*'
```

Não foi acessado banco real, arquivo enviado por usuários, segredo local,
console de infraestrutura, provedor SMTP ou backup. O código permite afirmar
como a aplicação grava/lê; não comprova configuração de volume, TLS, IAM,
retenção de logs externos, existência de dados reais ou proteção de dumps.
Não foi encontrada política versionada de backup/restore ou cache distribuído
de dados pessoais. Isso é uma lacuna de evidência, não uma prova de ausência
na infraestrutura. A fila de System Logs e caches de cliente são tratados
explicitamente na matriz.

O campo `User.profilePhotoStorageKey` usa a coluna legada `profile_photo_url`;
o nome físico não significa que o contrato vigente aceite URL arbitrária.
Históricos de domínio auditados são snapshots de exclusão, não necessariamente
um histórico de cada atualização. Billing retém o fato financeiro e snapshots
de configuração; Memberships retém o processo de revisão/bloqueio no próprio
registro. Essas diferenças orientam o rollout e a retenção.

Os DTOs expõem os dados necessários às operações: cadastro próprio/admin,
dependentes, perfil clínico completo e resumo por owner, processo de admissão,
pagamentos/reembolsos, veículos, ferramentas e conteúdo social. O DTO não é
outro armazenamento no servidor, mas amplia o perímetro para cliente, proxy,
tracing e notificações. Futuras implementações devem manter autorização e
contratos públicos e impedir caches/logs indevidos após descriptografar.

## Requisito legal, risco e decisão técnica

**Legal:** a LGPD define dado pessoal e categorias específicas de dado sensível
no art. 5º; prevê necessidade/minimização no art. 6º e medidas de segurança
adequadas no art. 46. A lei não determina criptografia de toda coluna textual.
Dados de dependentes podem envolver crianças/adolescentes: a avaliação de
tratamento e finalidade também deve considerar o art. 14.
[Texto compilado da LGPD](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709compilado.htm).

**Risco:** identificadores civis, contatos, nascimento, dívida, documentos e
associações familiares têm potencial de fraude, exposição e correlação mesmo
quando não integram as categorias jurídicas de dado sensível. Texto livre e
imagens podem conter essas categorias, mas sua presença não pode ser presumida
para todo registro. Foto comum não é automaticamente biometria; eventual uso
para identificação biométrica exige nova classificação.

**Decisão proposta:** selecionar proteção por finalidade, exposição e workload;
separar criptografia, índices, controle de acesso, minimização e retenção.
A ANPD apresenta medidas técnicas e administrativas orientadas a risco, entre
elas mecanismos de proteção de dados. Seu guia é referência orientativa;
não assumimos que o Jeep Club se enquadre como agente de pequeno porte.
[Guia oficial da ANPD](https://www.gov.br/anpd/pt-br/centrais-de-conteudo/materiais-educativos-e-publicacoes/anonimizado___guia_orientat-_seg_da_inf_p_atpp.pdf).

Esta auditoria técnica não decide bases legais, prazos legais de retenção ou
conformidade jurídica definitiva. Essas decisões aparecem com owner e aceite
próprios no backlog, sem bloquear a entrega da investigação.

## Critérios de aceite e evidência

| Critério | Evidência |
| --- | --- |
| Inventário repo-wide e categorias distintas | Matriz com campos operacionais, metadados, herança, coleções e superfícies externas |
| Health prioritário, snapshots e dados de alto impacto | Seções Health, Identity, Dependents, Memberships e Vehicles da matriz |
| Impacto, busca, unicidade e recomendação por campo | Colunas da matriz; bloqueadores de contrato detalhados na proposta |
| Logs, arquivos, comprovantes, caches, bootstrap e backups | Matriz de superfícies e findings F03–F08 |
| Algoritmo, integridade, nonce, AAD, versões, rotação e indisponibilidade | Proposta arquitetural, inclusive índices e recuperação |
| Rollout sem migration improvisada | Gates de banco vazio e dados existentes; job explícito e validado |
| Stories/Tasks separadas e estimáveis | Backlog local pronto para transcrição no Jira, com dependências e aceite |
| Distinção entre lei, risco e decisão | Seção acima e legenda da matriz |

Validação desta entrega: conferência dos campos contra os mappings JPA,
repositories e boundaries; links locais e `git diff --check`. Não houve mudança
de runtime; a suíte Maven não é evidência de efetividade criptográfica e não
foi executada para uma alteração exclusivamente documental.
