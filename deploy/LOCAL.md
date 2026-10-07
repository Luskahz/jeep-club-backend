# Ambiente local: banco no Docker

O fluxo recomendado para desenvolver é:

| Componente | Execução | Endereço |
| --- | --- | --- |
| MySQL 8.0 | Docker Compose | `127.0.0.1:3307` |
| Spring Boot | IntelliJ, perfil `dev` | `http://localhost:8080` |
| Next.js | `npm run dev` | `http://localhost:3000` |

O Dockerfile do backend empacota um JAR. Para editar e depurar Java no dia a
dia, execute pelo IntelliJ. O frontend usa seu servidor de desenvolvimento.
`compose.local.yaml` sobe somente o MySQL, em um projeto chamado
`jeep-club-local`, com volume próprio. Não combine esse arquivo com
`compose.yaml`, que contém a stack de deploy.

## 1. Preparar o Docker

No Windows, instale o [Docker Desktop](https://docs.docker.com/desktop/setup/install/windows-install/)
e abra o aplicativo. Aguarde o engine ficar disponível. No PowerShell:

```powershell
docker version
docker compose version
Set-Location C:\Users\joao.beserra\Documents\jeep-club-backend
```

Se `docker` não for reconhecido, reabra o terminal após instalar. Se aparecer
erro de conexão com o engine, confira que o Docker Desktop está aberto.

## 2. Configurar as senhas locais

Copie o exemplo para `.env.local` na raiz do backend. Preserve um arquivo
existente e ajuste os valores nele:

```powershell
if (!(Test-Path .env.local)) {
    Copy-Item deploy/mysql-local.env.example .env.local
}
```

Edite `.env.local` e substitua ambos os `CHANGE_ME` por senhas locais diferentes:

```dotenv
MYSQL_LOCAL_PORT=3307
MYSQL_PASSWORD=CHANGE_ME
MYSQL_ROOT_PASSWORD=CHANGE_ME
```

O Compose cria o banco `jeepclub` e o usuário `jeepclub` na primeira
inicialização. A aplicação usa `MYSQL_PASSWORD`; a senha root fica reservada
para administração. Essas variáveis não alteram as senhas de um banco já
inicializado. [Comportamento da imagem MySQL](https://hub.docker.com/_/mysql)

`.env.local` é ignorado pelo Git. Ele é lido pelo Docker com `--env-file`;
Spring Boot no IntelliJ precisa de sua própria configuração no próximo passo.

## 3. Subir o banco

Na raiz do backend:

```powershell
# Valida a configuração sem imprimir as senhas.
docker compose --env-file .env.local -f compose.local.yaml config --quiet

# Baixa a imagem na primeira execução e aguarda o healthcheck do banco.
docker compose --env-file .env.local -f compose.local.yaml up -d --wait

# Deve mostrar mysql com status healthy.
docker compose --env-file .env.local -f compose.local.yaml ps
```

Se a inicialização falhar, consulte:

```powershell
docker compose --env-file .env.local -f compose.local.yaml logs --tail=100 mysql
```

A porta `3307` evita conflitos com instalações comuns de MySQL na `3306`.
Se ela já estiver ocupada, altere `MYSQL_LOCAL_PORT` e também a URL JDBC abaixo.

## 4. Rodar o Spring Boot pelo IntelliJ

Copie o exemplo local se ainda não houver um arquivo configurado:

```powershell
if (!(Test-Path src/main/resources/application-dev.properties)) {
    Copy-Item src/main/resources/application-dev.properties.example src/main/resources/application-dev.properties
}
```

Em `src/main/resources/application-dev.properties`, configure:

```properties
spring.datasource.url=jdbc:mysql://127.0.0.1:3307/jeepclub?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
spring.datasource.username=jeepclub
# Mesma MYSQL_PASSWORD de .env.local.
spring.datasource.password=CHANGE_ME

# Chave privada local, com pelo menos 32 bytes.
security.jwt.secret=CHANGE_ME
# Senha do administrador LOCAL; diferente das credenciais da VPS.
security.bootstrap.admin.password=CHANGE_ME
```

Os outros campos do bootstrap já estão comentados no exemplo. Para gerar uma
chave aleatória usando o Node.js instalado para o frontend:

```powershell
node -e "console.log(require('crypto').randomBytes(32).toString('hex'))"
```

Cole a chave em `security.jwt.secret`. Escolha também a senha do administrador
local. O arquivo `application-dev.properties` é ignorado pelo Git.

No IntelliJ, use Java 17 ou superior, a raiz do backend como working directory
e execute a classe principal com `@SpringBootApplication`. Nas VM options,
defina `-Dspring.profiles.active=dev` (ou selecione `dev` em Active profiles,
se a configuração oferecer esse campo). Confira que a configuração de execução
não sobrescreve a URL ou as credenciais com valores antigos.

O MySQL cria o banco; as tabelas são criadas/atualizadas pelo backend com a
configuração JPA de desenvolvimento. No banco vazio, o bootstrap cadastra o
administrador com CPF `12345678909` e a senha local escolhida. Alterar a senha
do bootstrap depois não é um procedimento de troca de senha de uma conta já
criada.

Abra http://localhost:8080/swagger-ui/index.html para conferir a API. SMTP
pode permanecer vazio no perfil `dev`.

## 5. Conectar pelo MySQL Workbench ou IntelliJ

Use conexão TCP/IP com:

| Campo | Valor |
| --- | --- |
| Host | `127.0.0.1` |
| Porta | `3307` |
| Banco/schema | `jeepclub` |
| Usuário | `jeepclub` |
| Senha | A `MYSQL_PASSWORD` de `.env.local` |

A publicação está limitada ao endereço local da máquina. Para abrir o cliente
MySQL do próprio container, com senha solicitada interativamente:

```powershell
docker compose --env-file .env.local -f compose.local.yaml exec mysql mysql -u jeepclub -p jeepclub
```

## 6. Rodar o frontend

Na raiz do frontend, crie/ajuste `.env.local`:

```dotenv
API_URL=http://localhost:8080
NEXT_PUBLIC_API_URL=http://localhost:8080
NODE_SECURE=HTTP
# Gere uma chave privada local, diferente da chave JWT do backend.
ACCESS=CHANGE_ME
```

Depois execute:

```powershell
Set-Location C:\Users\joao.beserra\Documents\front-end-web-application
npm.cmd ci
npm.cmd run dev
```

Abra http://localhost:3000 e entre com as credenciais do bootstrap local.
Reinicie o servidor Next.js quando mudar `.env.local`.

Se o Windows bloquear os wrappers dos scripts por política de grupo, a
alternativa usada na validação deste projeto foi instalar sem scripts e chamar
o executável JavaScript do Next.js diretamente:

```powershell
npm.cmd ci --ignore-scripts
node node_modules/next/dist/bin/next dev
```

## 7. Parar e voltar depois

Na raiz do backend:

```powershell
# Para o MySQL preservando os dados.
docker compose --env-file .env.local -f compose.local.yaml stop

# Sobe novamente usando o mesmo volume.
docker compose --env-file .env.local -f compose.local.yaml up -d --wait

# Remove os containers e a rede, preservando o volume do banco.
docker compose --env-file .env.local -f compose.local.yaml down
```

O volume é `jeep-club-local_mysql-local-data`. Não acrescente `-v` ou
`--volumes` ao comando `down` se quiser manter o banco. Cada desenvolvedor tem
seu banco local; essas instruções não importam nem modificam os dados da VPS.
