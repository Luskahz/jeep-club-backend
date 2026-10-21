# Inventário de mídia e contrato global

Auditoria em `src/main/java` (entidades, DTOs, serviços e controladores) e nos recursos de configuração.

| Consumidor | Antes | Contrato atual |
| --- | --- | --- |
| Identity `User` | `profilePhotoUrl` string e coluna `profile_photo_url` | Novas associações usam `profilePhotoStorageKey`, mantendo fisicamente `profile_photo_url` para preservar registros existentes; `PATCH /identity/me/photo` só aceita chave validada |
| Vehicles `Vehicle` e histórico | `photo` string com URL/caminho na coluna `photo` | BACK-525: galeria 0..5, multipart vehicle/photos/gallery, command BACK-510, capa, revisão esperada, snapshot completo e backfill V8. photo é alias temporário; coluna preservada |
| Tools `Tool` e histórico | Nenhum campo de imagem | `photoStorageKey` em `photo_storage_key`; `PATCH /tools/{id}/photo` e `/admin/tools/{id}/photo` |
| Billing `MemberPayment` | Já guarda `receipt_storage_key` | Mantido: multipart, validação própria de comprovante (PDF e imagens), `FileStorage`, rollback e rota autorizada de download |
| Publications | Galleries de Notice/Event/Service, requests/change requests e comentários | Consumidor real de referências via ImageMediaService; upload avulso preservado para não quebrar esses contratos |
| Dependents e histórico | Sem foto | BACK-524: photo_storage_key nullable, criação/update multipart no negócio, compensação BACK-510, namespace privado no mesmo storage e leitura por ID com ownership/READ; sem chave no HTTP |

`FileStorage` é o contrato público transversal (`shared.storage`): `store(StorageFile, namespace)` devolve `StoredFile.storageKey()`, `load(key)` devolve conteúdo e `delete(key)` remove o objeto. A implementação local `LocalFileStorage` usa chaves lógicas com namespace, data e UUID, sem expor path físico. `storage.provider=local` e `storage.local.root-directory` configuram a instância; em produção o diretório precisa de volume persistente. Billing já aplica `PaymentReceiptValidator` com limite de 10 MB e tipos PDF/JPEG/PNG/WebP e usa `PaymentReceiptLifecycle` para compensar rollback e limpar o comprovante substituído após commit.

Para os consumidores legados, `POST /media/images` recebe `multipart/form-data` (`file`), aceita JPEG/PNG/WebP até 5 MB, compara extensão/MIME/assinatura e retorna `{storageKey,url}`. `GET /media/images?key=...` resolve exclusivamente o namespace `images/` para usuário autenticado. Os campos de foto e respostas retornam a chave, não a URL de um provider; o cliente pode usar a rota de leitura retornada no upload. `null` em rotas de atualização de foto desassocia; em Vehicles, images=[] limpa a galeria e edição cadastral omissa preserva; photo legado só altera galeria de até uma imagem e revisão zero; depois exige o contrato versionado.

No contrato legado, upload precede associação e a referência é validada antes de persistir. Vehicles creation agora usa `ImageMediaCommand.storeInTransaction`: guarda arquivo novo, persiste via JPA, mantém no commit e compensa apenas esse arquivo no rollback. O command não aceita uma chave antiga para deletar. A exclusão de User/Vehicle/Tool e substituição/desassociação de foto **retêm objetos compartilhados**. Uploads avulsos e referências substituídas podem deixar órfãos; limpeza posterior deve conferir todos os consumidores e históricos. Falha de compensação é sanitizada e não mascara a falha original. Commit de resultado UNKNOWN retém mídia para reconciliação, evitando apagar uma referência possivelmente confirmada. Não há delete público de mídia.

## Compatibilidade com dados existentes

O projeto usa Flyway para DDL e Hibernate `validate`. A BACK-510 não altera schema nem migrations V1–V4: `identity_users.profile_photo_url` e `vehicles_vehicle.photo`, inclusive histórico, são preservadas. Detalhes de contratos, transporte, consumidores e rollout: [BACK-510](back-510-media-orchestration.md).

Nos consumidores públicos, novas associações continuam passando por `ImageMediaService` e gravam referências `images/...`. Dependents usa o mesmo provider com referências `private-images/...`, somente através do command privado e das rotas autorizadas descritas abaixo. Valores antigos em formato URL permanecem legíveis até substituição explícita; nenhuma nova URL de provider é aceita pelos fluxos de criação/alteração de foto.

Em Vehicles, update parcial valida storage somente quando `photo` foi realmente enviado com um novo valor. Campo omitido preserva o valor existente — inclusive legado — e null explícito limpa somente quando o alias legado ainda é elegível (até uma imagem, revisão zero); depois exige o contrato versionado. Assim uma edição de outro atributo não falha apenas porque o registro foi criado antes da adoção do storage global.

Em Identity, create/update de foto permanecem estritos; somente a reconstituição de uma linha já persistida tolera a antiga URL. Ao substituir a foto, o estado passa naturalmente para storageKey.

Nenhum esquema de Billing foi alterado.

## Fotos privadas de Dependents — BACK-524

`ImageMediaCommand.storePrivateInTransaction` usa a mesma validação JPEG/PNG/WebP
e o mesmo mecanismo de compensação do command público, com namespace separado.
`ImageReference` continua restrita a `images/`; global GET e associação em outros
consumidores públicos recusam a referência privada, mesmo quando o arquivo existe.
O órfão de rollback com limpeza malsucedida também permanece inacessível no global.

`PrivateImageMediaQuery` é leitura interna após autorização pelo consumidor e
retorna bytes/MIME sem chave, path, provider ou filename. Dependents expõe somente
photoUrl por ID, com ownership ou DEPENDENTS_DEPENDENT_READ e auditoria/no-store.
Manutenção administrativa exige DEPENDENTS_DEPENDENT_PHOTO_UPDATE. DTOs de escrita
não aceitam referência arbitrária; multipart substitui e DELETE /photo remove a
associação. Cadastro/status não relacionado preserva a foto. Não há delete de
arquivo antigo/compartilhado em substituição, desassociação ou exclusão do cadastro.

V7 acrescenta a referência nullable ao cadastro e histórico. Dados existentes sem
foto continuam sem foto. Não há novo storage, volume ou variável. No futuro deploy,
confirmar ausência de alias público/CDN do volume e proteção de acesso ao namespace
privado; as configurações versionadas do proxy não montam uploads. Não foi acessada
configuração privada da VPS. Contratos, exemplos e limites:
[BACK-524](back-524-dependent-photo.md).

## Vehicles: galeria canônica — BACK-525

Mesmo provider público images/, zero a cinco imagens. POST de negócio recebe
arquivos diretamente; upload avulso não é requisito do frontend novo. PUT de
galeria member/admin exige expectedRevision, ordem/principal e lista completa.
Retenções próprias conservam legado; novas referências precisam ser públicas e
existir. Referências privadas de Dependents são recusadas. V8 cria coleções
operacional/histórica e backfill sem perda/drop. Histórico exige autoridade de
exportação. Arquivos antigos/compartilhados são retidos; só novos são compensados.
Request/proxy total passa a 30 MiB, imagem continua 5 MiB e recibo Billing 10 MiB.
Sem provider/volume/flag novo. Escritores antigos não podem coexistir com galeria.
Frontend, rollout e rollback: [BACK-525](back-525-vehicle-gallery.md).
