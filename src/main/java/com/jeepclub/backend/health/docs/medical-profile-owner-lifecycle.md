# Ciclo de vida do owner do perfil médico

## Decisão

`health` mantém a associação lógica de `medical_profiles` por
`owner_type + owner_id`. Não existe foreign key para tabelas de `identity` ou
`dependents`: esses dados pertencem a bounded contexts distintos e são
consultados por contratos públicos somente leitura.

Antes de consultar ou alterar um perfil, `health` classifica o owner como
ativo, inativo ou inexistente usando `UserQuery` e `DependentsQuery`.

| Estado do owner | Consulta/upsert de membro | Consulta/upsert administrativo | Exclusão administrativa do perfil |
| --- | --- | --- | --- |
| Ativo | Permitido, respeitando o vínculo do dependente | Permitido | Permitido |
| Inativo | Bloqueado, inclusive para exclusão pelo membro | Bloqueado | Permitido |
| Inexistente/removido | Bloqueado | Bloqueado | Permitido para saneamento |

IDs nulos ou não positivos são dados inválidos. Owner inexistente produz
`MEDICAL_PROFILE_OWNER_NOT_FOUND` (404), e owner inativo produz
`MEDICAL_PROFILE_OWNER_INACTIVE` (409). Um dependente ativo pertencente a outro
usuário continua produzindo `MEDICAL_PROFILE_ACCESS_DENIED` (403).

## Retenção e remoção

- A desativação não apaga dados clínicos. O perfil operacional é retido, mas
  deixa de aparecer nas consultas e não pode ser atualizado.
- A reativação torna o mesmo perfil novamente acessível; não é criada uma
  segunda linha.
- A remoção definitiva do owner torna qualquer perfil remanescente inacessível
  imediatamente, pois a validação passa a classificá-lo como inexistente.
- O perfil retido pode ser limpo pelo endpoint administrativo que recebe o ID do
  perfil, mesmo quando o owner está inativo ou inexistente. Até essa limpeza,
  ele permanece bloqueado e não é exposto pelas rotas operacionais.
- A exclusão por membro autorizado ou por administrador copia os dados para
  `medical_profiles_history` e remove a linha operacional na mesma transação.
  O histórico não é exposto pelas rotas de perfil.

## Consistência entre módulos

A validação por contrato impede novos órfãos nos fluxos de `health`, mas não é
uma transação distribuída com o lifecycle do owner. Uma foreign key ou transação
compartilhada só poderá ser introduzida por nova decisão arquitetural que reveja
os limites dos módulos.
