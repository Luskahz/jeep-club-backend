# Inventário de mídia e contrato global

Auditoria em `src/main/java` (entidades, DTOs, serviços e controladores) e nos recursos de configuração.

| Consumidor | Antes | Contrato atual |
| --- | --- | --- |
| Identity `User` | `profilePhotoUrl` string e coluna `profile_photo_url` | Novas associações usam `profilePhotoStorageKey`, mantendo fisicamente `profile_photo_url` para preservar registros existentes; `PATCH /identity/me/photo` só aceita chave validada |
| Vehicles `Vehicle` e histórico | `photo` string com URL/caminho na coluna `photo` | Novas associações usam somente storageKey validada, preservando fisicamente a coluna `photo` e seus valores legados até substituição |
| Tools `Tool` e histórico | Nenhum campo de imagem | `photoStorageKey` em `photo_storage_key`; `PATCH /tools/{id}/photo` e `/admin/tools/{id}/photo` |
| Billing `MemberPayment` | Já guarda `receipt_storage_key` | Mantido: multipart, validação própria de comprovante (PDF e imagens), `FileStorage`, rollback e rota autorizada de download |
| Outros módulos V2 | Nenhum campo de imagem/comprovante persistido encontrado nesta revisão | Novos campos devem seguir o mesmo contrato; Publications não está presente neste checkout |

`FileStorage` é o contrato público transversal (`shared.storage`): `store(StorageFile, namespace)` devolve `StoredFile.storageKey()`, `load(key)` devolve conteúdo e `delete(key)` remove o objeto. A implementação local `LocalFileStorage` usa chaves lógicas com namespace, data e UUID, sem expor path físico. `storage.provider=local` e `storage.local.root-directory` configuram a instância; em produção o diretório precisa de volume persistente. Billing já aplica `PaymentReceiptValidator` com limite de 10 MB e tipos PDF/JPEG/PNG/WebP e usa `PaymentReceiptLifecycle` para compensar rollback e limpar o comprovante substituído após commit.

Para fotos, `POST /media/images` recebe `multipart/form-data` (`file`), aceita JPEG/PNG/WebP até 5 MB, compara extensão/MIME/assinatura e retorna `{storageKey,url}`. `GET /media/images?key=...` resolve exclusivamente o namespace `images/` para usuário autenticado. Os campos de foto e respostas retornam a chave, não a URL de um provider; o cliente pode usar a rota de leitura retornada no upload. `null` em rotas de atualização de foto desassocia; em Vehicles, `null` explícito limpa o campo, e omissão preserva.

O upload precede a associação. Antes de persistir uma chave, o serviço verifica que o objeto existe. A exclusão de User/Vehicle/Tool e a substituição de foto **retêm o objeto**: uma chave pode ser reutilizada por vários recursos, então a aplicação não presume propriedade exclusiva. Uploads sem associação e chaves substituídas são possíveis órfãos; uma limpeza futura deverá verificar referências em todos os consumidores antes de remover o objeto. O endpoint de mídia não oferece delete público.

## Compatibilidade com dados existentes

O projeto não possui política de migrations versionadas e a arquitetura atual orienta não introduzir Flyway/Liquibase sem decisão própria. Esta integração, portanto, não renomeia fisicamente as colunas já implantadas: `identity_users.profile_photo_url` e `vehicles_vehicle.photo` (inclusive o snapshot histórico) são preservadas.

A partir desta versão, novas associações continuam obrigatoriamente passando por `ImageMediaService` e gravam referências `images/...`. Valores antigos em formato URL permanecem legíveis até substituição explícita; nenhuma nova URL de provider é aceita pelos fluxos de criação/alteração de foto.

Em Vehicles, update parcial valida storage somente quando `photo` foi realmente enviado com um novo valor. Campo omitido preserva o valor existente — inclusive legado — e `null` explícito limpa a associação. Assim uma edição de outro atributo não falha apenas porque o registro foi criado antes da adoção do storage global.

Em Identity, create/update de foto permanecem estritos; somente a reconstituição de uma linha já persistida tolera a antiga URL. Ao substituir a foto, o estado passa naturalmente para storageKey.

Nenhum esquema de Billing foi alterado.
