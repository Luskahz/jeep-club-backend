#!/bin/bash
set -euo pipefail
docker run --rm --name jeep-club-certbot \
    -v /etc/letsencrypt:/etc/letsencrypt \
    -v /var/lib/jeep-club-acme:/var/www/acme \
    certbot/certbot:latest renew --non-interactive --quiet
cd /opt/jeep-club-compose
docker compose exec -T proxy nginx -s reload
