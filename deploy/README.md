# Docker deployment

This stack runs the Java 17 Spring Boot backend, MySQL 8.0 and Nginx. Only the proxy HTTP port is published. Database and uploaded files use named volumes; container restarts and rebuilds keep their data.

## First start

Copy `.env.example` to `.env`, fill the database passwords, JWT secret and administrator password, then run:

```bash
docker compose config --quiet
docker compose up -d --build --wait
```

Generate independent random passwords. The JWT HMAC secret must contain at least 32 random bytes; for example, use `openssl rand -base64 64`. Keep `.env` private; it is excluded from Git and image build contexts.

The default profile is `dev`: SMTP remains empty and notifications are simulated by the application. Enable `SECURITY_BOOTSTRAP_ADMIN_ENABLED=true` for the first administrator; after creation, set it to `false` and recreate the backend. Bootstrap identity defaults are fictitious test values. Login uses CPF and the JSON field `senha`.

```bash
docker compose ps
docker compose logs --tail=100 backend
docker compose up -d --build --wait
```

The Docker build packages the application without running tests. Run `mvn verify` separately before releasing a change. Do not use `docker compose down -v` when you need to preserve the database or uploaded files.

## Backup and migration

Copy this project and the private `.env` to the new server. To transfer data, stop writes first, take a MySQL dump and archive uploaded files:

```bash
docker compose stop proxy backend
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysqldump -u jeepclub --single-transaction --no-tablespaces jeepclub' > database.sql
docker compose run --rm --no-deps --entrypoint tar backend -czf - -C /app/storage . > uploads.tar.gz
docker compose start backend proxy
```

On the new server, initialize MySQL, restore the dump, restore uploads as the application's user, then start the stack:

```bash
docker compose up -d mysql --wait
docker compose exec -T mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" mysql -u jeepclub jeepclub' < database.sql
docker compose run --rm --no-deps --entrypoint tar backend -xzf - -C /app/storage < uploads.tar.gz
docker compose up -d --build --wait
```

Build the backend image first on the new server (`docker compose build backend`) before using `docker compose run` to restore uploads. Store backup files securely and keep a copy outside the VPS. Change `APP_SERVER_BASE_URL` and DNS when the address changes.

For production, configure HTTPS, real SMTP, frontend URLs and CORS, switch to `prod`, and use real administrator identity data. The existing production configuration validator requires SMTP and external frontend URLs.
