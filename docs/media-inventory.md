# Inventário de mídia e contrato global

Auditoria em `src/main/java` (entidades, DTOs, serviços e controladores) e nos recursos de configuração.

| Consumidor | Antes | Contrato atual |
| --- | --- | --- |
| Identity `User` | `profilePhotoUrl` string e coluna `profile_photo_url` | `profilePhotoStorageKey` em `profile_photo_storage_key`; `PATCH /identity/me/photo` associa chave validada |
| Vehicles `Vehicle` e histórico | `photo` string com URL/caminho na coluna `photo` | `photo` contém somente chave existente, persistida em `photo_storage_key`; create/update de membro e admin verificam a referência |
| Tools `Tool` e histórico | Nenhum campo de imagem | `photoStorageKey` em `photo_storage_key`; `PATCH /tools/{id}/photo` e `/admin/tools/{id}/photo` |
| Billing `MemberPayment` | Já guarda `receipt_storage_key` | Mantido: multipart, validação própria de comprovante (PDF e imagens), `FileStorage`, rollback e rota autorizada de download |
| Outros módulos V2 | Nenhum campo de imagem/comprovante persistido encontrado nesta revisão | Novos campos devem seguir o mesmo contrato; Publications não está presente neste checkout |

`FileStorage` é o contrato público transversal (`shared.storage`): `store(StorageFile, namespace)` devolve `StoredFile.storageKey()`, `load(key)` devolve conteúdo e `delete(key)` remove o objeto. A implementação local `LocalFileStorage` usa chaves lógicas com namespace, data e UUID, sem expor path físico. `storage.provider=local` e `storage.local.root-directory` configuram a instância; em produção o diretório precisa de volume persistente. Billing já aplica `PaymentReceiptValidator` com limite de 10 MB e tipos PDF/JPEG/PNG/WebP e usa `PaymentReceiptLifecycle` para compensar rollback e limpar o comprovante substituído após commit.

Para fotos, `POST /media/images` recebe `multipart/form-data` (`file`), aceita JPEG/PNG/WebP até 5 MB, compara extensão/MIME/assinatura e retorna `{storageKey,url}`. `GET /media/images?key=...` resolve exclusivamente o namespace `images/` para usuário autenticado. Os campos de foto e respostas retornam a chave, não a URL de um provider; o cliente pode usar a rota de leitura retornada no upload. `null` em rotas de atualização de foto desassocia; em Vehicles, `null` explícito limpa o campo, e omissão preserva.

O upload precede a associação. Antes de persistir uma chave, o serviço verifica que o objeto existe. A exclusão de User/Vehicle/Tool e a substituição de foto **retêm o objeto**: uma chave pode ser reutilizada por vários recursos, então a aplicação não presume propriedade exclusiva. Uploads sem associação e chaves substituídas são possíveis órfãos; uma limpeza futura deverá verificar referências em todos os consumidores antes de remover o objeto. O endpoint de mídia não oferece delete público.

## Migração de dados existentes

`hibernate.ddl-auto=update` cria as novas colunas, mas **não converte** valores antigos de URL em chaves. Se houver fotos antigas em `profile_photo_url` ou `photo`, elas devem ser migradas por operação de dados separada: obter o objeto original, enviar pela API de mídia e associar a chave ao recurso correspondente; URLs temporárias ou inacessíveis não podem ser transformadas automaticamente. Até essa migração, as fotos antigas não aparecem nos novos campos. Nenhum esquema de Billing foi alterado.
