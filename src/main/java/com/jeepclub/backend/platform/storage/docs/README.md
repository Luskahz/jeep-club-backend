# Contrato global de armazenamento de arquivos

## Objetivo e fronteiras

A capacidade global de storage armazena bytes sem conhecer a semântica do
arquivo. Ela não é um bounded context, uma API HTTP de upload, um catálogo de
metadados nem um módulo de imagens.

O fluxo arquitetural é:

```text
módulo consumidor -> shared.storage -> platform.storage -> provider/filesystem
```

O módulo consumidor valida MIME, extensão permitida, tamanho, autorização,
ownership e lifecycle. `shared.storage` expõe o contrato neutro e
`platform.storage` faz a composição Spring e implementa os detalhes físicos.

Módulos persistem somente a `storageKey`, nunca URL, path físico, root directory,
bucket, hostname ou provider. Trocar o provider não pode mudar a identidade já
persistida.

## Contratos compartilhados

`FileStorage` oferece exclusivamente:

- `store(StorageFile, namespace)`;
- `load(storageKey)`;
- `delete(storageKey)`.

`StorageFile` transporta `originalFilename`, `contentType`, a extensão já
normalizada pelo consumidor e os bytes. O nome original nunca define o nome
físico. O storage valida somente invariantes técnicas da extensão; ele não decide
se `pdf`, `jpg` ou qualquer outro formato serve para um caso de uso. Conteúdo
`null` é inválido, mas um arquivo com zero bytes é tecnicamente armazenável.

`StoredFile` retorna apenas a `storageKey`. `StorageResource` retorna a chave e os
bytes, além do tamanho derivado dos próprios bytes. MIME e nome original não são
adivinhados no carregamento, pois esta capacidade não cria sidecar, tabela ou
metadado persistente para recuperá-los com fidelidade. Os arrays são copiados
defensivamente nos dois contratos que transportam conteúdo.

Os contratos não dependem de Spring Web, JPA, filesystem ou módulos de negócio.

## Provider local e storageKey

O único provider suportado é `LOCAL`. A composição Spring expõe
`LocalFileStorage` pelo tipo `FileStorage`; consumidores não selecionam provider.
Qualquer outro valor de `storage.provider` falha durante o binding da configuração,
sem fallback silencioso.

A chave gerada usa separador lógico `/`:

```text
{namespace}/{yyyy}/{MM}/{dd}/{uuid}.{extension}
```

Exemplo:

```text
identity/profile-photos/2026/09/13/550e8400-e29b-41d4-a716-446655440000.webp
```

O namespace é fornecido pelo consumidor e pode ter múltiplos segmentos. Cada
segmento deve ser relativo e portátil. Valores nulos, blank, `.`, `..`, segmentos
vazios, barras invertidas, paths absolutos Unix/Windows e traversal são rejeitados;
eles não são corrigidos silenciosamente. A extensão deve estar normalizada em
minúsculas e conter somente letras ASCII, números, `_` ou `-`, com até 32
caracteres.

A data usa o `Clock` global injetável e o identificador usa UUID. O gerador possui
um ponto interno mínimo de injeção para testes determinísticos de colisão.

## Configuração

Propriedades da aplicação:

```properties
storage.provider=${STORAGE_PROVIDER:local}
storage.local.root-directory=${STORAGE_LOCAL_ROOT_DIRECTORY:./storage}
```

Exemplo de ambiente:

```env
STORAGE_PROVIDER=local
STORAGE_LOCAL_ROOT_DIRECTORY=./storage
```

O root é normalizado como `Path` somente dentro de `platform.storage`. O diretório
runtime `./storage` fica ignorado pelo Git.

## Operações, erros e segurança

`store` valida entrada/namespace/extensão, gera a chave, confirma confinamento,
cria diretórios e grava com `CREATE_NEW`. Uma colisão nunca sobrescreve o objeto
existente. `load` exige arquivo regular existente e devolve seus bytes. `delete`
exige arquivo regular existente e remove exatamente esse objeto. Ausência em
`load` ou `delete` produz `StorageObjectNotFoundException`; nenhum método retorna
`null` nem converte ausência em conteúdo vazio.

Todas as operações validam explicitamente a forma lógica antes de resolver
`rootDirectory + storageKey`, normalizam o resultado e confirmam que permanece no
root. Componentes existentes que sejam symbolic links são rejeitados e a leitura
usa `NOFOLLOW_LINKS`, impedindo escape trivial por symlink. Isso protege contra
entrada não confiável e alterações comuns no filesystem; não pretende defender
contra um administrador hostil capaz de trocar componentes em uma corrida no
filesystem.

As exceções neutras ficam em `shared.storage.exception`, pois atravessam
`FileStorage` e podem ser distinguidas pelos consumidores:

- `InvalidStorageFileException`, `InvalidStorageNamespaceException` e
  `InvalidStorageKeyException` para entradas tecnicamente inválidas;
- `StorageObjectNotFoundException` para ausência ou recurso que não é arquivo;
- `StorageCollisionException` para tentativa de sobrescrita;
- `StorageOperationException` para I/O inesperado, com operação `READ`, `WRITE`
  ou `DELETE` e causa técnica preservada.

As mensagens são controladas e não expõem root directory ou path físico. Não há
handler HTTP global: cada módulo consumidor traduz falhas para seu próprio
contrato quando necessário.

## Adicionar um provider futuro

Para adicionar um provider real no futuro:

1. implementar o contrato `FileStorage`;
2. adicionar a configuração e composição correspondentes no Platform;
3. manter a convenção e o significado da `storageKey`;
4. selecionar o bean na composição Spring.

Não é necessário alterar os consumidores nem criar outra abstração. Um provider
só deve ser anunciado quando seu adapter funcional existir.
