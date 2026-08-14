# Achados que exigem decisão de contrato

Os itens abaixo foram identificados durante a padronização estrutural e não
foram corrigidos, pois alterariam autorização, validação ou respostas HTTP.

## Segurança e catálogo de permissões

- Uma negação de `@PreAuthorize` lançada pela segurança de método chega ao
  handler global como erro inesperado e produz `500`, embora a resposta
  semanticamente esperada seja `403`. A correção muda status, código e corpo
  de erro.
## Memberships

- O fluxo de `MemberActivationToken` possui validação pública, mas não há
  emissão do token no fluxo atual. A aprovação por link gera um token de
  recuperação de senha de `authentication`; implementar ou remover a ativação
  muda comportamento.
- O e-mail da solicitação não é obrigatório no DTO, mas é não nulo no banco.
  Torná-lo obrigatório altera a validação do request.
- A consulta administrativa por ID retorna `404` sem corpo quando não encontra
  a solicitação. Padronizar esse erro com `ApiErrorResponse` altera o response.

## Health, Tools e Vehicles

- O upsert administrativo de perfil médico não verifica a existência do dono;
  adicionar a verificação cria novos `400/404` para IDs hoje aceitos.
- `tools` anuncia itens ativos, mas a listagem também inclui inativos; um item
  removido ainda pode ser consultado ou reativado. Corrigir essas regras muda
  os resultados dos endpoints.
- Em `vehicles`, ownership inválido é ocultado como `404` e os DTOs de edição
  usam campos primitivos em `PUT`. Alterar para `403`, parcialidade ou novas
  validações modifica o contrato atual.

Esses ajustes devem ser tratados em mudanças separadas, com testes de
caracterização e versionamento quando necessário.
