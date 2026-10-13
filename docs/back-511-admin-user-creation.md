# BACK-511 — criação administrativa de usuários

## Contratos

| Método | Caminho | Acesso | Corpo | Sucesso |
| --- | --- | --- | --- | --- |
| POST | `/identity/admin/users` | `IDENTITY_USER_CREATE` | `name`, `birthDate?`, `email?`, `cpf`, `rg?`, `phoneNumber?`, `activationMethod` | `201`, `Location`, `Cache-Control: no-store` |
| POST | `/authentication/activation-invitations/validation` | público | `token` | `204`, sem consumir |
| POST | `/authentication/account-activations` | público | `token`, `newPassword` | `204`, sem sessão; efetuar login |
| POST | `/authentication/admin/users/{userId}/activation-invitations` | `IDENTITY_USER_CREATE` | vazio | `202`, `userId`, `invitationStatus: QUEUED` |
| GET | `/authentication/admin/users/{userId}/activation-invitations` | `IDENTITY_USER_CREATE` | nenhum | `200`, status de entrega |

O `activationMethod` deve ser `TEMPORARY_PASSWORD` ou `EMAIL_INVITATION`. Ausência ou valor desconhecido resulta em `400`. Identity normaliza nome, CPF, RG, telefone e e-mail. E-mail é opcional para senha provisória e obrigatório para convite. CPF, e-mail e RG possuem restrição de unicidade no banco; conflito resulta em `409`. Repetir o POST com o mesmo CPF resulta em conflito, evitando um segundo cadastro em cliques repetidos. `birthDate` e `rg` são persistidos.

### Senha provisória

Exemplo de criação:

```json
{"name":"Marina da Silva","birthDate":"2000-02-12","email":null,"cpf":"529.982.247-25","rg":null,"phoneNumber":"(11) 91234-5678","activationMethod":"TEMPORARY_PASSWORD"}
```

A resposta contém `userId`, `activationMethod` e `temporaryPassword`. A senha aparece somente nesse retorno e apenas o hash é persistido. O administrador entrega a senha por canal adequado. `POST /authentication/login` retorna `PASSWORD_CHANGE_REQUIRED` e um `passwordChangeToken`; `POST /authentication/login/password-change` define a senha permanente. Até essa conclusão não há acesso normal. O fluxo atual de Membership usa os mesmos mecanismos de autenticação e permanece independente.

### Convite por e-mail

A criação com `EMAIL_INVITATION` persiste User e AuthenticationAccount `PENDING_FIRST_ACCESS`, registra convite com hash do token e responde `userId`, `activationMethod` e `invitationStatus: QUEUED`. Nenhum token, link ou senha é enviado ao administrador. O e-mail é enviado após commit. O estado consultável é `QUEUED`, `SENT` ou `DELIVERY_FAILED`. Em `DELIVERY_FAILED` ou `QUEUED` persistente, o administrador usa o POST de reenvio; o novo token invalida o anterior. Essa é a recuperação operacional quando SMTP ou o processo de envio falha. O token anterior também fica inválido quando a conta se torna permanente. Não há atribuição de roles nem criação de MembershipApplication.

O usuário pode chamar a validação repetidamente sem consumir o convite. A conclusão exige token válido, conta ativa e pendente, e senha com 8 a 72 caracteres; atualiza conta e token na mesma transação. Convite inválido, expirado ou usado recebe `410` genérico. Reenvio para conta permanente, bloqueada, desativada ou sem e-mail recebe `409`. A atualização usa bloqueio da conta e do convite para impedir dois consumos simultâneos. A conclusão retorna `204`, e o frontend deve enviar o usuário ao login.

## Segurança, configuração e operação

`IDENTITY_USER_CREATE` já consta no catálogo de Authorization. A API aplica `@PreAuthorize`; `@RequiredPermission` documenta a mesma authority. A criação de User e conta está em transação única. Token de convite tem 64 bytes gerados por `SecureRandom`, hash SHA-256 em banco, validade padrão de 24 horas e uso único. A senha provisória usa o gerador seguro e o hash existentes. O log de auditoria registra ator, ID criado, modalidade e instante, sem senha ou token. Respostas sensíveis usam `Cache-Control: no-store`.

O logger de diagnóstico SQL do Hibernate fica desativado porque mensagens de violação de unicidade podem incluir CPF ou e-mail. O erro HTTP continua sendo `409` estruturado.

Configurar `APP_ACCOUNT_ACTIVATION_URL` com a página de definição da senha no frontend, `APP_ACCOUNT_ACTIVATION_TTL` como `Duration` ISO-8601 (ex.: `PT24H`), e as configurações SMTP existentes (`SPRING_MAIL_*`, `APP_MAIL_FROM`). Em produção a URL não pode apontar para localhost. A entrega após commit evita afirmar que SMTP faz parte da transação do banco. O estado `QUEUED` pode ficar pendente após reinício do processo antes do envio; nesse caso o reenvio administrativo rotaciona o token e recupera a conta.

## Integração do frontend

O formulário administrativo deve exigir seleção explícita de modalidade e enviar os campos cadastrados a `POST /identity/admin/users`. Na modalidade de senha provisória, exibir a senha uma única vez em tela segura, sem persistir em storage nem telemetry. Na modalidade e-mail, exibir estado de envio e disponibilizar consulta e reenvio administrativo. A página de ativação lê o token recebido pelo link, envia o token no corpo de `/authentication/activation-invitations/validation` e, após o usuário escolher a senha, chama `/authentication/account-activations`; depois redireciona ao login. Não usar rota de criação `/identity/admin/users/first-access`.
