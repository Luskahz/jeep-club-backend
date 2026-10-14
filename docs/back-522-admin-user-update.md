# BACK-522 — integração da edição administrativa

O contrato de paths, DTOs, status e erros é publicado no OpenAPI de
`AdminUserController`; o domínio/concorrência está no README de Identity. Este
guia explica as decisões de formulário e os efeitos entre fluxos que não cabem
somente nos schemas.

## Formulário de cadastro

O frontend lê o detalhe administrativo, mantém o `id` estável e envia os campos
alterados a `PATCH /identity/admin/users/{userId}`. É necessária a authority
`IDENTITY_USER_UPDATE`; `IDENTITY_USER_READ` permite consulta, sem escrita.
A resposta é `AdminUserResponse` atualizada. Não reenviar o DTO de resposta como
body: status, IDs, timestamps, roles e credenciais são rejeitados.

Exemplo de correção:

```json
{"name":"Marina da Silva","cpf":"111.444.777-35","email":"marina@example.com","rg":null}
```

Campos omitidos preservam os dados. `null` remove opcionais; nome/CPF não podem
ser removidos. Strings são normalizadas como no cadastro existente; CPF exige
formato e checksum válidos. `{}` e valores canônicos iguais são no-op. Nascimento
é uma data ISO (sem hora); foto usa a `storageKey` obtida no upload global de mídia,
sem URL arbitrária. Remover a associação não remove o arquivo compartilhável.
Conflitos de unicidade usam 409; dados inválidos, 400; alvo inexistente, 404.
As respostas desses endpoints usam `Cache-Control: no-store`.

## Perfil complementar

Consultar `GET /identity/admin/users/{userId}/profile` exige READ; substituir
`PUT /identity/admin/users/{userId}/profile` exige UPDATE. O request reutiliza
`IdentityUserProfileData` (`workProfile` e `address`) e a resposta sinaliza
`profileCompletionPending`. O PUT substitui integralmente: omissão, null ou branco
removem dados e `{}` limpa. O formulário deve enviar o perfil completo que quer
preservar, inclusive ambos os blocos. Dados parciais não bloqueiam login.

```json
{"workProfile":{"occupation":"Motorista","workplace":"Clube"},"address":{"postalCode":"12345-678","street":"Rua A","number":"S/N","complement":null,"neighborhood":"Centro","city":"Caraguatatuba","state":"SP"}}
```

## CPF, e-mail e convites

CPF/e-mail efetivamente alterados encerram sessões e refresh e invalidam challenges
ativos do alvo na mesma transação. Se o administrador edita a própria conta,
deverá autenticar novamente com o CPF vigente e a mesma senha. Correção de nome,
RG, telefone, nascimento ou foto não exige novo login. IDs, vínculos e históricos
não mudam; senha, lock, estado da credencial e status não são atribuídos pelo PATCH.

E-mail corrigido invalida links pendentes de ativação administrativa, todas as
recuperações por e-mail abertas e Membership. O status do convite administrativo passa a `INVALIDATED`;
não confundir com falha SMTP. Após confirmar o e-mail, usar o reenvio administrativo
de Authentication para um usuário pendente, ou o reenvio da solicitação de
Membership quando o convite pertencer àquele fluxo. Não há reenvio automático,
nem entrega a partir de um evento com endereço anterior, token substituído ou
expirado, ou cuja conta já tenha deixado o primeiro acesso pendente. Remover o e-mail é
permitido, mas impede novos convites até preencher um endereço válido.

Outro administrador não pode mudar CPF/e-mail de ROOT. O titular ROOT autorizado
pode corrigir os próprios dados; a alteração exige novo login. O bootstrap dev
preserva o vínculo ROOT existente pelo ID, sem transferir privilégios ao CPF
anterior caso ele seja usado em outro cadastro. Os demais campos
continuam disponíveis ao administrador autorizado. Cadastros DISABLED permanecem
desativados após edição. A migração INACTIVE/ARCHIVED é responsabilidade da
BACK-512/BACK-514, ainda não integrada nesta base.

A operação é atômica com auditoria: falha reverte cadastro, revogações e links.
Audit registra ator, alvo, instante e nomes dos campos efetivamente modificados,
sem valores pessoais. Para `USER_CONCURRENT_MODIFICATION` (409), refazer a leitura e
repetir a intenção de edição; não substituir cegamente todos os campos após
conflito. Para `USER_UPDATE_CONFLICT`, corrigir duplicidade/política ROOT; para
`USER_IDENTITY_CHANGE_CONFLICT`, investigar conta ausente antes de repetir.
