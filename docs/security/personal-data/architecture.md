# Proposta arquitetural de proteção em repouso

Relacionado à [auditoria](README.md). **Proposta sujeita a revisão antes de
qualquer implementação.** As escolhas abaixo são decisões técnicas propostas,
não uma alegação de exigência legal por algoritmo/campo.

## Ameaças e camadas

| Ameaça | Mitigação e limite |
| --- | --- |
| Disco/volume, snapshot físico ou backup perdido | Criptografia de infraestrutura, acesso restrito e chaves externas; verificar configuração real |
| Dump lógico/consulta SQL indevida sem acesso às chaves | Criptografia em aplicação protege valores selecionados; HMAC protege contra enumeração offline sem chave; relações, tamanhos e igualdade continuam visíveis |
| Arquivos de storage vazados | Volume/objeto protegido; para comprovantes privados, envelope criptográfico dos bytes com chave externa |
| Credencial SQL obtida junto com acesso ao KMS/runtime | Segregação IAM e mínimos privilégios; criptografia de campo não protege um processo autorizado a descriptografar comprometido |
| Acesso indevido por API, funcionário ou conta tomada | Ownership, permissions, autenticação, auditoria e minimização continuam necessários |
| Logs, SMTP, tracing, cache de navegador ou exportação | Redaction, política de retenção e perímetro externo; cifrar banco não protege cópias descriptografadas |

IDs e vínculos em claro são pseudônimos relacionáveis, não dados anonimizados.
A existência de um perfil médico associado a `ownerId`, por exemplo, ainda pode
ser inferida. Proteger todo o grafo exigiria tokenização/reestruturação de
queries e é uma decisão separada; registrar esse risco residual na aprovação.

## Boundary e representação

Manter campos canônicos no domínio e nos contratos autorizados. Adapters de
persistência do módulo fazem `canonicalizar -> cifrar/indexar -> entity` e
`entity -> validar/descriptografar -> reconstituir domínio`. O módulo continua
owner de finalidade, seleção de campos e consultas. Propor uma capacidade
técnica compartilhada com port neutro em Shared e provider/configuração em
Platform, conforme a arquitetura existente; não mover cadastro para
Authentication nem permitir acesso cross-module a repositories internos.

Não aplicar um `AttributeConverter` genérico a todas as Strings: projections,
queries Criteria, constraints e snapshots precisam de tratamento explícito.
`AdminUserJpaQueryRepository` lê Tuples diretamente e precisa de mapper de leitura
protegida; criptografar apenas o repository de escrita deixaria esse caminho
incompatível. Cópias de histórico devem ser cifradas com contexto próprio, sem
dupla cifra e sem manter o valor atual cifrado e o snapshot em claro.

Para cada célula cifrada, usar envelope versionado com `formatVersion`,
`algorithm`, `keyId`, `nonce`, `ciphertext` e `tag`. Proposta inicial:
**AES-256-GCM**, nonce de 96 bits, tag de 128 bits, implementação de biblioteca
mantida/provider JCA compatível, sem algoritmo próprio. Gerar nonce por CSPRNG
para toda escrita; nunca derivá-lo de CPF/ID/tempo, reutilizar em atualização ou
copiar par nonce/ciphertext entre contextos. Dimensionar limite de operações por
chave conforme tamanho/carga e análise de colisão; rotacionar antes do limite
aprovado. Não assumir que nonce aleatório permita quantidade ilimitada de usos.
Referência do modo autenticado:
[NIST SP 800-38D](https://csrc.nist.gov/pubs/sp/800/38/d/final).

O ciphertext/nonce/tag pode ficar no banco; chave secreta não. Dimensionar novas
colunas binárias ou TEXT pelo envelope e crescimento Base64, incluindo campos
hoje curtos/enum/date. Não sobrescrever CPF `VARCHAR(11)`, e-mail `VARCHAR(180)`
ou `LocalDate` com ciphertext mantendo mappings antigos.

AAD canônico inclui ambiente lógico, módulo, tabela, campo, versão do formato,
algoritmo, keyId, PK física esperada e `cryptoRecordId` imutável. O UUID é alocado
antes do INSERT; a PK autogerada exige desenho explícito: inserir somente uma
linha técnica sem PII nas novas colunas protegidas, obter o ID, cifrar e completar
a linha na mesma transação, revertendo tudo se qualquer etapa falhar. Schema e
writers devem suportar esse estado transitório não publicável. Alternativa é
pré-alocação de IDs aprovada; não assumir sequences em MySQL nem alterar IDs
públicos para facilitar o cipher. Nenhuma etapa grava plaintext para obter a PK.
Snapshot recebe outra PK, outro `cryptoRecordId` e outro contexto.
Definir encoding sem ambiguidades (campos de comprimento prefixado) e vetores
de teste. AAD não é segredo; não inserir dado clínico, CPF ou outro identificador
direto no header. Na leitura, o adapter fornece a PK/contexto esperado, não
aceita identidade de linha informada pelo envelope. Usar apenas um UUID copiado
da própria linha como AAD permitiria mover UUID e ciphertext juntos. Troca de
payload entre linhas/campos, alteração de tag/header ou chave errada deve falhar
sem devolver texto parcial. Não alegar detecção de rollback para uma versão
antiga válida na mesma linha: proteção contra replay exige mecanismo separado.

## Consultas, índices e constraints

Usar **HMAC-SHA-256 com chave de índice separada**, sobre valor canônico e
contexto de propósito/versão, para igualdade/deduplicação. Guardar digest completo
binário de 32 bytes e versão, com constraints equivalentes às regras atuais.
Separar escopos por módulo/campo: evitar um fingerprint universal de CPF que
permita correlacionar toda a base. APIs intermodulares continuam recebendo o
valor autorizado e executam seus próprios lookups. Normalização deve reutilizar
regras atuais e ser versionada; null não recebe um digest fixo comum.

CPF/telefone têm espaço previsível: SHA-256 sem chave ou salt público não é
proteção adequada contra enumeração. Senhas permanecem hash adaptativo (BCrypt
atual, custo a avaliar separadamente); tokens aleatórios de 64 bytes já são
persistidos por hash e não precisam ser reversíveis. HMAC não substitui hashing
de senha. Blind indexes vazam igualdade/frequência e não fornecem autenticidade
da linha; verificar, após descriptografar, correspondência entre valor e digest.

| Fluxo vigente | Preservação proposta / decisão exigida |
| --- | --- |
| Identity login/registro via CPF; e-mail/RG únicos | Lookup por HMAC e ciphertext; manter constraints nomeadas/tradução e nullable semantics |
| Identity filtros CPF/RG e nascimento por igualdade | Índices de igualdade separados; nascimento não é único |
| Identity `name`, `email`, `phoneNumber` por substring; `q` inclui nome/e-mail/CPF/RG/telefone | **Bloqueador:** HMAC de igualdade não preserva substring. Revisar contrato antes de cifrar esses campos; não remover filtros silenciosamente |
| Identity sort por nome/nascimento/e-mail/CPF/RG/telefone/key de foto e projections/sparse fields | **Bloqueador:** ordenar ciphertext não corresponde ao valor. Revisão específica de contrato/UX e plano de consulta paginada |
| Memberships CPF e e-mail por existência/status; block ativo por CPF | HMAC por escopo; `activeCpf` vira índice ativo nullable; conservar unicidade somente de bloqueio ativo e histórico de desbloqueio |
| Dependents CPF único; listagem por userId/status | HMAC CPF na tabela operacional; históricos mantêm ciphertext sem impor unicidade de CPF histórico |
| Vehicles plate/RENAVAM únicos | Canonicalizações atuais, dois HMACs e duas constraints operacionais; histórico sem unicidade de identificador atual |
| Event guests CPF por igualdade, IN, GROUP BY e unique(eventId,cpf) | Trocar consultas por digest no módulo, mapear resultado autorizado; manter unique(eventId,cpfIndex) e deduplicação em memória |
| Health lookup/unique(ownerType,ownerId), lotes e resumo por ID | Manter chaves relacionais em claro; campos clínicos não têm pesquisa clínica dedicada no repository atual |
| Textos privados (mensagens, notas, motivos), telefone de serviço | AEAD sem índice quando não há filtro específico; validar opções de Pageable/sort na implementação antes de mudar representação |

Opções para os bloqueadores: aprovar busca exata + ordenação por metadados
não cifrados; ou manter temporariamente campos específicos em claro com
aceitação de risco e plano datado. Não escolher índices de n-grams/prefixos ou
criptografia que preserve ordem por conveniência: revelam informação adicional
e exigem análise própria. Descriptografar todos os registros para filtrar/sort
em memória compromete paginação, performance e exposição; não é solução padrão.
A recomendação da matriz para Identity permanece condicionada a resolver isso.

## Chaves por ambiente e responsabilidades

Produção: KMS/secret manager aprovado, autenticação por workload identity e
permissões mínimas. Propor envelope de chaves: KEK não exportável em KMS;
DEKs versionadas por finalidade/módulo guardadas **envelopadas fora do banco**
em secret manager. Proteger índice com chave distinta da DEK e da assinatura
JWT. O processo recebe somente as versões necessárias; operadores de DB e
backup não recebem decrypt/unwrap ou leitura de segredos. Separar quem pode
administrar chaves de quem acessa dados e auditar operações do provedor.

HML: mesmo modelo, identidades e keyrings separados de produção, dados
sintéticos por padrão. Dev: segredo aleatório injetado em ambiente/arquivo local
fora do Git; jamais chave padrão ou derivada de senha/CPF/JWT. Teste: keyring
efêmero isolado; vetores sintéticos explícitos em testes são permitidos, sem
reutilizar material de ambientes reais. Variáveis só no desenvolvimento/teste;
produção deve preferir identidade do workload ao segredo durável em ENV.

O provedor de produção ainda não está definido no repositório. O backlog
compara custo, latência/quotas, suporte de versões/HMAC, auditoria, região,
recuperação, IAM e operação antes de escolher o serviço. Não incluir chave em
application.properties, banco, dump, logs ou documentação.
Referências de gestão:
[OWASP Cryptographic Storage](https://cheatsheetseries.owasp.org/cheatsheets/Cryptographic_Storage_Cheat_Sheet.html)
e [OWASP Key Management](https://cheatsheetseries.owasp.org/cheatsheets/Key_Management_Cheat_Sheet.html).

## Rotação, revogação e falha segura

Keyring distingue chave atual de escrita e versões antigas somente de leitura.
Rotação de DEK: publicar versão nova, escrever nela, reencriptar em lotes com
controle de concorrência e checkpoints, confirmar todas as cópias/arquivos,
desabilitar leitura antiga após janela de rollback/retention. Rotação de KEK
pode re-envelopar DEKs sem recifrar todos os dados; rotação de DEK exige
reencriptação. Não confundir as duas.

Rotação HMAC exige plano separado: adicionar índices v2, preencher todos os
registros e manter **todos os writers** calculando v1/v2 na coexistência; reads
aceitam as duas versões, constraints v1 continuam impedindo duplicatas até v2
estar completa e única. Validar nullable activeCpf, concorrência e unicidade
cruzada entre writers antes de retirar v1. Simples OR de versões sem essa
disciplina admite duplicata na migração.

Missing key/KMS indisponível: falhar startup/readiness para capacidade afetada e
falhar leitura/escrita protegida sem plaintext fallback; operação pode usar
DEK já desbloqueada em memória apenas dentro de TTL/política de revogação
aprovados. Não guardar keyring/ciphertext descriptografado em cache distribuído.
Erro de autenticação do ciphertext é corrupção/incidente, não dado legado.
Traduzir falha operacional para RFC 9457 controlado (503 em indisponibilidade;
erro interno controlado em corrupção), sem dado, SQL ou chave na resposta/log.
Operações emergenciais Health precisam de disponibilidade do keyring e da
auditoria obrigatória; não usar bypass sem cifra como solução de contingência.

Revogação urgente bloqueia novas operações com a versão e invalida caches de
chave; desencadeia re-envelopamento/reencriptação conforme comprometimento de
KEK/DEK/HMAC. Rotação não desfaz dados já exfiltrados com chave antiga.
Destruição de uma chave compartilhada não é apagamento individual de titular.
Retenção/eliminações precisam de procedimento próprio.

DR: backups cifrados e protegidos, metadata keyId preservada e recuperação de
versões pelo KMS/secret manager com controle dual e acesso temporário auditado.
Não copiar chaves brutas junto com dump. Testar restore de DB + storage +
keyring em ambiente isolado; perda definitiva da chave perde dados. Manter
versões necessárias a backups retidos e política para dados eliminados que
reapareçam em restore. Definir RPO/RTO e teste de indisponibilidade de KMS.

## Rollout compatível com a política de schema

Nenhuma introdução automática de Flyway/Liquibase. A política atual usa entities
e Hibernate (`ddl-auto=update` padrão; Flyway desabilitado), o que não migra
semântica de dados nem prova o estado de produção.

**Cenário preferencial, banco ainda vazio:** confirmar por evidência operacional
que não há dados reais, revisar proposta/queries, preparar schema e providers
antes da primeira carga, validar restore e só então permitir go-live. Não assumir
banco vazio pelo fato de a V2 ainda estar em preparação. Importações/fixtures
entram pelos mesmos writers protegidos; nenhum seed plaintext paralelo.

**Cenário com dados existentes:** schema aditivo explícito revisado (colunas
ciphertext, versão/cryptoRecordId e índices), inventário de contagens/dados
duplicados, backup validado, job de backfill autorizado, idempotente, em lotes
e com lock/versionamento contra alterações simultâneas. Tratar todas as
tabelas operacionais, snapshots, coleções, bytes e cópias SMTP/exportadas.
Não executar job em startup nem enviar dados a logs.

Durante transição, leitura plaintext permitida somente quando o marcador de
formato indica **LEGACY** e a flag de janela está ativa. Registro marcado
ENCRYPTED nunca é interpretado como legado se chave/tag falhar. Novas escritas
já são cifradas; evitar dual-write que reintroduza plaintext. Proibir ciphertext
desconhecido, medir pendências por módulo/versão sem valores pessoais, validar
índices e comparações por amostragem protegida. Ao final: desligar legacy reads,
remover colunas/cópias antigas pelo procedimento de schema aprovado e tratar
backups/logs antigos pela política de retenção. Flags têm owner e prazo.

Rollback conserva versão antiga capaz de ler ciphertext e índices v1/v2;
não reverter para binário que grava/lê somente plaintext após o cutover.
Preservar evidência de sucesso, plano de contingência e restore ensaiado.

## Validação exigida na futura implementação

Round-trip por tipo/null, canonicalização atual, envelope inválido, AAD trocado,
nonce distinto, versão desconhecida, indisponibilidade/revogação, rotação e
legado explícito; unicidade real concorrente em MySQL para CPF/RG/e-mail/
plate/RENAVAM e guest/block ativo. Testar projections, filtros, sort e paginação,
traduções de constraint, snapshots, rollback e bytes de comprovante.
Examinar dump de massa sintética e logs para provar ausência dos valores
selecionados, inclusive backups/restore, sem expor dados reais. Medir custo,
latência, tamanho e falhas nos limites dos endpoints clínicos e emergenciais.
