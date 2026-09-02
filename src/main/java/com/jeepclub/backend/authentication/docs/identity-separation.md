# Separacao entre identidade e autenticacao

## Responsabilidade da conta

`AuthenticationAccount` representa apenas o acesso e a seguranca de uma
identidade. O agregado possui o `identityId`, o hash da senha, o estado da
credencial, o bloqueio automatico, a habilitacao administrativa do acesso e os
timestamps relacionados a autenticacao.

Dados cadastrais, inclusive CPF, RG, nome, e-mail e telefone, pertencem a
`identity` e nao sao replicados na nova conta.

## Estados independentes

Existem tres decisoes diferentes, que nao devem ser condensadas em um unico
enum:

- `IdentityStatus`: `ACTIVE` ou `DISABLED`, usado por regras administrativas e
  de negocio, como elegibilidade para cobranca;
- `AuthenticationAccessStatus`: `ENABLED` ou `DISABLED`, usado para permitir ou
  revogar o acesso de forma administrativa;
- `AuthenticationStatus` e `CredentialStatus`: bloqueio por tentativas e ciclo
  da senha, incluindo primeiro acesso e troca obrigatoria.

Reabilitar o acesso administrativo nao desbloqueia automaticamente uma conta e
nao torna permanente uma credencial temporaria. Da mesma forma, desbloquear ou
trocar a senha nao reativa um acesso que foi desabilitado administrativamente.

## Identificador e persistencia

`authentication_accounts.identity_id` e uma chave primaria atribuida. Seu valor
e o ID criado por `identity_users`; a autenticacao nao gera outro ID.

A referencia e escalar e nao possui relacionamento JPA com a entidade de
`identity`. Isso evita acoplamento entre modelos de persistencia e preserva a
fronteira dos modulos. A consistencia entre a criacao da identidade e da conta
deve ser garantida pelo caso de uso transacional que coordenara o cutover.

## Estado transitorio

O agregado e a tabela novos estao prontos, mas ainda nao participam dos fluxos
da aplicacao. `User` e `authentication_users` continuam sendo a fonte funcional
unica enquanto login, cadastro, recuperacao, administracao e bootstrap do root
nao forem migrados conjuntamente.

Esse estado evita dual write e permite validar o novo modelo antes da troca. A
proxima etapa e introduzir os casos de uso transacionais que criam `Identity` e
`AuthenticationAccount` e, depois, mover as leituras de autenticacao para a
nova conta.
