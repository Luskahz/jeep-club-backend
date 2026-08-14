# Jeep Club Backend

Backend oficial do sistema **Jeep Club**, desenvolvido em Java utilizando Spring Boot.

O projeto fornece uma API REST responsável pelo gerenciamento de associados, dependentes, autenticação, permissões, eventos e demais recursos do sistema.

---

# Tecnologias

- Java 17
- Spring Boot 4
- Spring Security
- JWT
- Spring Data JPA
- MySQL
- Maven
- Swagger / OpenAPI

---

# Arquitetura

O projeto segue uma arquitetura inspirada em **Arquitetura Hexagonal (Ports & Adapters)**, separando claramente:

- API
- Application
- Domain
- Infrastructure

Essa divisão facilita manutenção, testes e evolução do sistema.

O padrão adotado para controllers, services e limites entre módulos está
detalhado em [docs/architecture/module-organization.md](docs/architecture/module-organization.md).

---

# Requisitos

Antes de executar o projeto é necessário possuir instalado:

- Java 17+
- Maven 3.9+ (opcional, pois existe Maven Wrapper)
- MySQL 8+

---

# Instalação

## 1. Clone o repositório

```bash
git clone https://github.com/Luskahz/jeep-club-backend.git

cd jeep-club-backend
```

---

## 2. Configure o banco

Crie um banco MySQL.

Exemplo:

```sql
CREATE DATABASE jeepclub;
```

---

## 3. Configure o application.properties

Edite:

```
src/main/resources/application.properties
```

Configure:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/jeepclub

spring.datasource.username=root

spring.datasource.password=senha
```

---

## 4. Configure o JWT

Copie

```
jwt-secrets.properties.sample
```

para

```
jwt-secrets.properties
```

e preencha as chaves necessárias.

---

## 5. Execute

Linux/macOS

```bash
./mvnw spring-boot:run
```

Windows

```cmd
mvnw.cmd spring-boot:run
```

---

# Documentação da API

Após iniciar a aplicação:

```
http://localhost:8080/swagger-ui.html
```

ou

```
http://localhost:8080/swagger-ui/index.html
```

---

# Estrutura do Projeto

```
src
 ├── authentication
 ├── dependents
 ├── platform
 ├── ...
```

Cada módulo é organizado em camadas seguindo os princípios da Arquitetura Hexagonal.

---

# Licença

Este projeto é distribuído sob a licença GNU General Public License v3.0.

Veja o arquivo LICENSE.
