# Docker deployment

## HTTPS on an IP address

The default proxy configuration serves HTTP and the ACME webroot. For a public IP, run `sudo bash deploy/setup-https-ip.sh <public IPv4>` on the VPS deployment directory. This requests a trusted short-lived certificate with Certbot, changes the proxy to `deploy/nginx-https.conf`, and redirects HTTP to HTTPS. It registers an ACME account without a contact email.

The certificate is named `jeep-club-api`; files persist under `/etc/letsencrypt`, and ACME challenge files under `/var/lib/jeep-club-acme`. These are host bind mounts. Keep automated renewal running: install `deploy/renew-https.sh` and invoke it twice daily using a systemd timer. IP certificates require frequent renewal.

For the existing VPS, `NGINX_CONFIG=/etc/jeep-club/nginx-https.conf` and `APP_SERVER_BASE_URL=https://144.91.73.3` live in the private environment file. Keep the HTTP ACME challenge route reachable during renewal. Migrating an IP deployment requires issuing a certificate for the new IP.

## GitHub Actions deployment

The existing Backend CI workflow runs Maven verification for pull requests. On a push to `master`, or a manual run selecting `master`, the deployment job starts only after verification succeeds. Pull requests and other branches do not deploy.

Required repository secrets: `JEEPCLUB_DEPLOY_HOST`, `JEEPCLUB_DEPLOY_USER`, `JEEPCLUB_DEPLOY_SSH_KEY`, and `JEEPCLUB_DEPLOY_KNOWN_HOSTS`. Use a dedicated SSH key constrained with `restrict,command="/usr/local/sbin/jeep-club-deploy"` in `authorized_keys`. The key permits only `status` and `deploy <40-character commit SHA>`, and the server validates that the commit belongs to `origin/master`.

Install `deploy/vps-deploy.sh` as `/usr/local/sbin/jeep-club-deploy` with root ownership and mode `700`. It uses the clone at `/opt/jeep-club-repo` and the private environment file at `/opt/jeep-club-compose/.env`.

Deployment builds an image tagged with the tested commit, backs up the existing database/uploads, and waits for container health checks. Concurrent deployments are serialized. If startup fails, the script attempts to restore the previous application image; database schema changes are not reversed automatically. Keep the pre-deployment database backup for recovery.

After a successful deployment, the image tag is saved in the private environment file and `/var/lib/jeep-club-deploy`. Do not remove these image tags until the corresponding release is no longer needed for rollback. Database credentials remain on the VPS and are not needed in GitHub Actions.

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
