# Achados que exigem decisão de contrato

Os itens abaixo foram identificados durante a padronização estrutural e não
foram corrigidos, pois alterariam autorização, validação ou respostas HTTP.

Este arquivo é um registro histórico de pendências, não uma fonte de verdade de
arquitetura ou contrato HTTP. Revalide cada item contra a `develop` e contra o
OpenAPI antes de planejar sua correção. O antigo achado de `@PreAuthorize`
produzir `500` foi removido: `GlobalExceptionHandler` já trata
`AccessDeniedException` como `403 ACCESS_DENIED`.

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

- A validação do owner de perfil médico e a política de retenção foram definidas
  em
  [`health/docs/medical-profile-owner-lifecycle.md`](../../src/main/java/com/jeepclub/backend/health/docs/medical-profile-owner-lifecycle.md).
- `tools` anuncia itens ativos, mas a listagem também inclui inativos; um item
  removido ainda pode ser consultado ou reativado. Corrigir essas regras muda
  os resultados dos endpoints.
- Em `vehicles`, ownership inválido é ocultado como `404` e os DTOs de edição
  usam campos primitivos em `PUT`. Alterar para `403`, parcialidade ou novas
  validações modifica o contrato atual.

Esses ajustes devem ser tratados em mudanças separadas, com testes de
caracterização e versionamento quando necessário.
