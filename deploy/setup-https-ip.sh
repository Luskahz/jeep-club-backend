#!/bin/bash
set -euo pipefail
umask 077
exec 9>/run/lock/jeep-club-https-setup.lock
flock -w 300 9
ip=${1:?Pass the public IPv4 address}
deploy_dir=${DEPLOY_DIR:-/opt/jeep-club-compose}
cd "$deploy_dir"
mkdir -p /var/lib/jeep-club-acme /etc/letsencrypt /etc/jeep-club
chmod 755 /var/lib/jeep-club-acme
install -m 644 deploy/nginx.conf /etc/jeep-club/nginx-http-acme.conf
install -m 644 deploy/nginx-https.conf /etc/jeep-club/nginx-https.conf
cp .env /etc/jeep-club/before-https.env
set_env() {
    local name=$1 value=$2 tmp
    tmp=$(mktemp .env.XXXXXX)
    awk -v name="$name" -v value="$value" 'index($0,name "=") != 1 { print } END { print name "=" value }' .env > "$tmp"
    chmod 600 "$tmp"
    mv "$tmp" .env
}
set_env NGINX_CONFIG /etc/jeep-club/nginx-http-acme.conf
docker compose config --quiet
docker compose up -d --no-deps proxy
docker compose exec -T proxy nginx -t
mkdir -p /var/lib/jeep-club-acme/.well-known/acme-challenge
chmod 755 /var/lib/jeep-club-acme/.well-known /var/lib/jeep-club-acme/.well-known/acme-challenge
printf '%s' 'acme-webroot-ready' > /var/lib/jeep-club-acme/.well-known/acme-challenge/jeep-club-probe
chmod 644 /var/lib/jeep-club-acme/.well-known/acme-challenge/jeep-club-probe
curl --fail --silent --show-error --output /dev/null http://127.0.0.1/.well-known/acme-challenge/jeep-club-probe
docker run --rm --name jeep-club-certbot \
    -v /etc/letsencrypt:/etc/letsencrypt \
    -v /var/lib/jeep-club-acme:/var/www/acme \
    certbot/certbot:latest certonly --non-interactive --agree-tos \
    --register-unsafely-without-email --preferred-profile shortlived \
    --webroot --webroot-path /var/www/acme --ip-address "$ip" \
    --cert-name jeep-club-api
set_env NGINX_CONFIG /etc/jeep-club/nginx-https.conf
set_env APP_SERVER_BASE_URL "https://$ip"
docker compose up -d --no-build --wait --wait-timeout 240 backend proxy
docker compose exec -T proxy nginx -t
docker compose exec -T proxy nginx -s reload
# Check TLS and the public gateway without depending on disabled documentation.
status=$(curl --silent --show-error --output /dev/null --write-out '%{http_code}' "https://$ip/identity/me")
[[ "$status" == 401 || "$status" == 403 ]]
echo "HTTPS ready at https://$ip"
