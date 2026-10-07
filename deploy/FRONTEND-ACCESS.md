# Acesso exclusivo pelo servidor do frontend

O Docker habilita `SECURITY_FRONTEND_ACCESS_ENABLED=true`. Todas as rotas da API,
inclusive login e cadastro, exigem `X-Frontend-Key` antes de validar o JWT do usuário.
Copiar `Origin`, usar um JWT sozinho ou omitir `Origin` não libera acesso.
CORS continua sendo uma política do navegador, não a autenticação entre servidores.

Na Vercel, configure `API_FRONTEND_KEY` como variável privada e criptografada.
O Next.js acrescenta o header apenas em chamadas do servidor, incluindo refresh,
uploads e imagens. Nunca use prefixo `NEXT_PUBLIC_` para essa credencial.
Usuários continuam precisando de login, JWT e permissões para operações protegidas.
O frontend continua acessível na internet e suas ações públicas podem ser chamadas
por clientes HTTP; esta proteção impede chamadas diretas à API sem a credencial.

Uma chave independente pode ser definida com `SECURITY_FRONTEND_ACCESS_SECRET`.
Se vazia, o backend usa uma credencial derivada do segredo existente de JWT:

```
API_FRONTEND_KEY = base64url_sem_padding(
    HMAC_SHA256(UTF8(SECURITY_JWT_SECRET), UTF8("jeep-club/frontend-access/v1"))
)
```

Isso permite ativar a proteção pelo Git/Compose sem alterar arquivos da VPS.
A chave derivada não é o segredo que assina JWT e não permite calculá-lo.
Ao trocar `SECURITY_JWT_SECRET`, atualize também a credencial privada na Vercel.
Não registre nenhuma dessas chaves em logs, commits, exemplos ou bundles do browser.
Publique primeiro o frontend preparado com a nova chave, depois o backend protegido.

`application.properties` desabilita OpenAPI e Swagger com `false`, inclusive em dev.
O filtro também bloqueia a documentação estática `/openapi-custom` e `/docs` com 404.
Os testes de contrato optam explicitamente por habilitar OpenAPI no perfil de teste.

O healthcheck usa GET `/actuator/health` na porta 8081, vinculada a 127.0.0.1 dentro
do container. Essa porta não é publicada, e a porta da API não tem essa exceção.
MySQL e backend continuam sem portas públicas; Nginx publica apenas HTTP/HTTPS.

No perfil `dev`, o indicador de saúde de SMTP fica desativado, pois o ambiente
usa envio de e-mail simulado e não tem servidor SMTP configurado. A saúde do
banco e da aplicação continua sendo verificada. Esse ajuste não desativa o
indicador de SMTP em outros perfis.

Para IDE local a proteção vem desabilitada por padrão. Para desenvolvimento local
com Compose, use uma chave privada em ambos os serviços ou defina explicitamente
`SECURITY_FRONTEND_ACCESS_ENABLED=false` apenas no ambiente local.
