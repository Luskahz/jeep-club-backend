# BACK-524 — Foto privada de dependente

Base temporária: BACK-523, PR #108, `cc81f6ede941f013d55f881545f99f69102861cb`.
Cadeia: develop → BACK-460 (#105) → BACK-510 (#106) → BACK-512 (#107) → BACK-523 (#108) → BACK-524 → BACK-525.

## Investigação e decisões

Foram lidas a história BACK-524, BACK-392/396/482, o PR #72 e os contratos atuais de Dependents, Platform Media e storage. O contrato integrado da BACK-510 prevalece sobre a proposta histórica de receber storageKey. A BACK-512 fornece ACTIVE/INACTIVE/ARCHIVED, locks e snapshot independente de exclusão. V6 é a última migration da ancestralidade; esta entrega usará V7. V1–V6 permanecem intactas.

O resolvedor global atual aceita qualquer usuário autenticado e somente chaves do namespace público `images`. Isso não é autorização suficiente para fotos de dependentes. A solução mantém o mesmo FileStorage/provider/root e o mesmo command transacional, com escrita privada em namespace separado. Referências privadas não são aceitas pelo resolvedor público nem pelos consumidores públicos de ImageReference. A leitura privada ocorre por query interna de Platform, chamada somente depois das verificações de titular/permissão em Dependents. Inclusive um arquivo órfão após falha de compensação continua inacessível no endpoint global; segurança não depende de UUID secreto ou de uma linha de ACL que possa sofrer rollback.

## Critérios e contratos implementados

- Foto opcional em criação multipart no endpoint de negócio; JSON existente sem foto continua válido. Part `dependent` é DTO estruturado e validado; `photo` é um arquivo opcional, até 5 MiB, JPEG/PNG/WebP conforme validação global.
- Atualização multipart com foto omitida preserva a associação; nova foto substitui a referência. PUT JSON de dados cadastrais preserva foto. PATCH dedicado com arquivo substitui; DELETE dedicado desassocia. Não aceitar storageKey, URL física, Base64 ou userId no DTO de escrita.
- Titular somente administra seu próprio dependente; ACTIVE permite mudanças de foto, INACTIVE/ARCHIVED devolvem conflito. BACK-544 substitui a regra original de leitura: exige dependent ACTIVE no snapshot; INACTIVE/ARCHIVED retornam 404. Leitura já autorizada antes da revogação pode concluir com a chave capturada. Administrador lê com DEPENDENTS_DEPENDENT_READ e altera/remove com DEPENDENTS_DEPENDENT_PHOTO_UPDATE, sem grants automáticos a roles CUSTOM.
- Respostas member/admin apresentam rota de leitura autorizada por ID, sem chave interna. Leitura é auditada e usa Cache-Control: no-store. Não registrar chaves, bytes, filenames ou dados do dependente em logs.
- Live e histórico persistem `photo_storage_key` nullable. Trocar, remover associação ou excluir dependente não apaga arquivos anteriores/compartilhados. Somente a imagem recém-criada é compensada em rollback, pela infraestrutura da BACK-510.
- Query pública usada por Health/Publications e exports mantém suas projeções de identificação: referências privadas não são adicionadas a relatórios ou integrações que não autorizam acesso à foto. Snapshot de exclusão preserva a referência internamente.
- Validar HTTP/permissions/ownership, criação sem/com foto, troca, remoção idempotente, update não relacionado, estados, inexistência, compartilhamento, histórico, falha de upload/JPA/auditoria/compensação, leitura global recusada, concorrência, OpenAPI e migrations reais. Executar verify, perfil MySQL e prova de migration futura; registrar resultados efetivos antes da publicação.

## HTTP e adaptação do frontend

| Operação | Rota | Autorização | Resultado |
| --- | --- | --- | --- |
| Criar sem foto | POST /dependents, JSON ou multipart | Usuário autenticado, titular vem do principal | 201, photoUrl nula |
| Criar com foto | POST /dependents, multipart dependent + photo | Titular ativo | 201, photoUrl protegida |
| Atualizar cadastro/foto | PUT /dependents/{id}, JSON ou multipart | Titular, dependente ACTIVE | 200; foto omitida preservada |
| Substituir foto | PATCH /dependents/{id}/photo, multipart photo | Titular e cadastro elegíveis | 200 |
| Desassociar | DELETE /dependents/{id}/photo | Titular e cadastro ACTIVE | 200, photoUrl nula; repetição não altera timestamp |
| Ler foto | GET /dependents/{id}/photo | Titular autenticado ativo; dependente ACTIVE no snapshot (BACK-544) | Binário, no-store e auditoria confirmada antes da resposta |
| Ler administrativamente | GET /users/{userId}/dependents/{id}/photo | DEPENDENTS_DEPENDENT_READ, par de IDs válido | Binário, no-store e auditoria |
| Manter administrativamente | PATCH ou DELETE /users/{userId}/dependents/{id}/photo | DEPENDENTS_DEPENDENT_PHOTO_UPDATE; titular/dependente elegíveis | 200, resposta com rota administrativa |

Nenhuma competência nova de criação administrativa geral foi introduzida. A manutenção administrativa de foto é separada da permissão de leitura. IDs técnicos mantêm o papel original das rotas de titular/dependente; não substituem matrícula de Member.

Exemplo de criação no frontend:

```javascript
const form = new FormData();
form.append("dependent", new Blob([JSON.stringify({
  name: "Dependente", cpf: "CPF_VALIDO", birthDate: "2015-01-01",
  relationshipType: "CHILD", phoneNumber: null
})], { type: "application/json" }));
if (photoFile) form.append("photo", photoFile);
await fetch("/dependents", { method: "POST", headers: { Authorization: "Bearer REDACTED" }, body: form });
```

O navegador deve definir o boundary de multipart; não fixar Content-Type manualmente. Enviar somente os campos de escrita do DTO. Campos desconhecidos, storageKey/photoStorageKey, URL/Base64 e tentativa de definir userId são rejeitados. PUT JSON permanece um contrato de dados cadastrais e preserva a foto; multipart com photo omitida também preserva. Nova photo substitui. A remoção explícita usa DELETE /photo, não `photo: null` nem string vazia no DTO.

Respostas mantêm os campos anteriores e acrescentam `photoUrl`, por exemplo `/dependents/12/photo` para titular e `/users/34/dependents/12/photo` para admin. Não há storageKey no HTTP. Para exibir, fazer fetch autenticado da rota e usar o Blob resultante; um img src isolado não injeta Authorization bearer. Respeitar no-store, não persistir conteúdo em service worker/analytics/cache compartilhado e revogar Object URLs da UI ao encerrar o uso. Não montar URL do endpoint global para estas fotos.

400 indica DTO, multipart ou arquivo inválido; 401 autenticação inelegível; 403 ownership/permissão; 404 cadastro/foto ausente ou par administrativo incorreto; 409 cadastro/titular inelegível ou concorrência; 503 indisponibilidade de storage. Erros seguem RFC 9457 e não carregam chaves, root, nome original ou detalhes de provider.

## Persistência e rollout

`V7__dependent_private_photo.sql` acrescenta `photo_storage_key VARCHAR(512) NULL` em `dependents_dependent` e `dependents_dependent_history`, sem unicidade na foto: referências legitimamente compartilhadas permanecem possíveis. A authority de manutenção é acrescentada ao final do ENUM físico, preservando a ordem anterior. O catálogo usa PermissionCode e PermissionDefinition; não há grants novos a roles CUSTOM. DDL aditiva e replay controlado na mesma ancestralidade preservam dados e referências. Não há backfill de fotos antes inexistentes.

O domínio preserva o campo em updates de cadastro/status e reconstituição; os mappers copiam a referência para JPA e snapshot. Criar com foto associa a imagem na mesma transação e registra updatedAt dessa associação; criação sem foto mantém a semântica anterior. Troca/remoção usam lock de Identity antes de Dependent; o estado atual é recarregado sob lock. Um competidor que já leu a instância antiga não pode sobrescrever outra edição recém-confirmada. Se exclusão vencer, a troca concorrente não recria o registro nem grava outra imagem.

Aplicar migration antes do código com Hibernate validate. A infraestrutura existente precisa permitir `private-images/` no mesmo root/volume. Não há outro provider, variável, volume ou job periódico novo. Arquivos substituídos/desassociados e do histórico são retidos conforme a política compartilhada; prever crescimento e reconciliação de falhas de limpeza/resultado UNKNOWN. Não limpar arquivos por ausência de referência operacional sem considerar histórico, compartilhamento e retenção aprovada.

As configurações versionadas `deploy/nginx.conf`, `deploy/nginx-https.conf` e `compose.yaml` só encaminham para a API e não montam uploads no proxy. Não foi encontrado resource handler Spring para o diretório de storage. Isso não é inspeção da configuração privada da VPS: o futuro agente de infraestrutura precisa confirmar que não existe alias/CDN/static route público do volume, restringir acesso ao volume/backup e preservar no-store. Nenhuma configuração privada da VPS foi acessada.

DDL MySQL não é uma transação única entre statements; estimar locks de metadados/ALTER e verificar backup/restauração antes da release. V1–V6 e baseline permanecem intactas. Downgrade para código anterior à V7 após gravar fotos pode perder a referência no snapshot de futuras exclusões, pois os mappers antigos desconhecem o campo; preferir correção para frente ou restauração coordenada, sem apagar coluna/histórico como rollback. Preservar instruções de #103/#104 e todas as restrições operacionais do Registry; nenhum deploy/VPS/importação é autorizado nesta entrega.

## Históricos, privacidade e limites

O snapshot de exclusão retém a referência internamente e não apaga o objeto. A rota operacional passa a retornar 404 depois da exclusão. Não foi criada uma nova rota de conteúdo de foto histórica: o acesso sensível a históricos continua nas superfícies administrativas existentes; CSV/PDF e query pública para Health/Publications não recebem chaves ou URLs privadas. BACK-482 (proteção ampla de dados/históricos) continua fora desta implementação; não se afirma homologação LGPD geral.

Não se aceitam chaves fornecidas pelo cliente, inclusive de outro dependente ou do upload avulso. A associação legítima nasce do arquivo novo pela BACK-510; referências já compartilhadas internamente são preservadas. Substituir/desassociar/excluir nunca chama delete do arquivo anterior. A compensação de rollback remove somente o objeto recém-criado. Se ela também falhar, o erro original permanece e o órfão privado continua recusado pelo resolvedor global; o log contém só categoria sanitizada, sem causa, chave ou filename.

Leitura privada gera auditoria atômica antes da resposta. Falha de auditoria impede retornar bytes. Permissão READ não concede PHOTO_UPDATE; não há bypass por ID. Privacidade depende das rotas autorizadas e da restrição do resolvedor público ao namespace público, não de tratar UUID como segredo.

## Validação e revisão

Resultados finais e tentativas corrigidas estão em `docs/back-524-test-evidence.json`: Full verify 2060 testes, 0 falhas, 0 erros, 1 skips; Complete MySQL profile 190 testes, 0 falhas, 0 erros, 0 skips; Future migration 190 testes, 0 falhas, 0 erros, 0 skips. O único skip local é a capacidade de symlink preexistente no Windows. Testes usam storage local real em diretório temporário e persistência transacional; o MySQL repete os cenários com Flyway e validate. Cobrem 20 cenários integrados de foto/segurança/compensação/exportação/concorrência e contratos de namespace privado, além da regressão geral. Conferir primeiro ownership/leitura administrativa, falhas de auditoria e rollback, preservação de arquivo compartilhado, migração do histórico e checklist de roteamento/storage do futuro deploy.
