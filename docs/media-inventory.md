# Inventário de mídia e contrato global

Auditoria em `src/main/java` (entidades, DTOs, serviços e controladores) e nos recursos de configuração.

| Consumidor | Antes | Contrato atual |
| --- | --- | --- |
| Identity `User` | `profilePhotoUrl` string e coluna `profile_photo_url` | Novas associações usam `profilePhotoStorageKey`, mantendo fisicamente `profile_photo_url` para preservar registros existentes; `PATCH /identity/me/photo` só aceita chave validada |
| Vehicles `Vehicle` e histórico | `photo` string com URL/caminho na coluna `photo` | BACK-510: criação member/admin multipart com JSON vehicle e photo opcional, command interno e compensação; DTO de criação sem chave. Edição conserva referência validada e legado até galeria |
| Tools `Tool` e histórico | Nenhum campo de imagem | `photoStorageKey` em `photo_storage_key`; `PATCH /tools/{id}/photo` e `/admin/tools/{id}/photo` |
| Billing `MemberPayment` | Já guarda `receipt_storage_key` | Mantido: multipart, validação própria de comprovante (PDF e imagens), `FileStorage`, rollback e rota autorizada de download |
| Publications | Galleries de Notice/Event/Service, requests/change requests e comentários | Consumidor real de referências via ImageMediaService; upload avulso preservado para não quebrar esses contratos |
| Dependents | Ainda não possui referência de foto | BACK-524 deve reutilizar o command da BACK-510; sua funcionalidade não é antecipada aqui |

`FileStorage` é o contrato público transversal (`shared.storage`): `store(StorageFile, namespace)` devolve `StoredFile.storageKey()`, `load(key)` devolve conteúdo e `delete(key)` remove o objeto. A implementação local `LocalFileStorage` usa chaves lógicas com namespace, data e UUID, sem expor path físico. `storage.provider=local` e `storage.local.root-directory` configuram a instância; em produção o diretório precisa de volume persistente. Billing já aplica `PaymentReceiptValidator` com limite de 10 MB e tipos PDF/JPEG/PNG/WebP e usa `PaymentReceiptLifecycle` para compensar rollback e limpar o comprovante substituído após commit.

Para os consumidores legados, `POST /media/images` recebe `multipart/form-data` (`file`), aceita JPEG/PNG/WebP até 5 MB, compara extensão/MIME/assinatura e retorna `{storageKey,url}`. `GET /media/images?key=...` resolve exclusivamente o namespace `images/` para usuário autenticado. Os campos de foto e respostas retornam a chave, não a URL de um provider; o cliente pode usar a rota de leitura retornada no upload. `null` em rotas de atualização de foto desassocia; em Vehicles, `null` explícito limpa o campo, e omissão preserva.

No contrato legado, upload precede associação e a referência é validada antes de persistir. Vehicles creation agora usa `ImageMediaCommand.storeInTransaction`: guarda arquivo novo, persiste via JPA, mantém no commit e compensa apenas esse arquivo no rollback. O command não aceita uma chave antiga para deletar. A exclusão de User/Vehicle/Tool e substituição/desassociação de foto **retêm objetos compartilhados**. Uploads avulsos e referências substituídas podem deixar órfãos; limpeza posterior deve conferir todos os consumidores e históricos. Falha de compensação é sanitizada e não mascara a falha original. Commit de resultado UNKNOWN retém mídia para reconciliação, evitando apagar uma referência possivelmente confirmada. Não há delete público de mídia.

## Compatibilidade com dados existentes

O projeto usa Flyway para DDL e Hibernate `validate`. A BACK-510 não altera schema nem migrations V1–V4: `identity_users.profile_photo_url` e `vehicles_vehicle.photo`, inclusive histórico, são preservadas. Detalhes de contratos, transporte, consumidores e rollout: [BACK-510](back-510-media-orchestration.md).

A partir desta versão, novas associações continuam obrigatoriamente passando por `ImageMediaService` e gravam referências `images/...`. Valores antigos em formato URL permanecem legíveis até substituição explícita; nenhuma nova URL de provider é aceita pelos fluxos de criação/alteração de foto.

Em Vehicles, update parcial valida storage somente quando `photo` foi realmente enviado com um novo valor. Campo omitido preserva o valor existente — inclusive legado — e `null` explícito limpa a associação. Assim uma edição de outro atributo não falha apenas porque o registro foi criado antes da adoção do storage global.

Em Identity, create/update de foto permanecem estritos; somente a reconstituição de uma linha já persistida tolera a antiga URL. Ao substituir a foto, o estado passa naturalmente para storageKey.

Nenhum esquema de Billing foi alterado.
