# Snapshot sanitizado da VPS

`deploy/snapshot-vps.sh` coleta metadados operacionais e estrutura do MySQL sem
alterar o banco ou reiniciar serviços. Não exporta linhas, variáveis secretas ou
logs brutos. Execute em um checkout privado da release ou copie o script e seus
irmãos `audit-schema.sh`, `compare-schema.py` e `snapshot-vps.py` para um
diretório privado na VPS. O usuário precisa ler Docker, systemd, o estado de
deploy e os arquivos de backup. O output deve ficar fora do repositório.

```bash
umask 077
bash deploy/snapshot-vps.sh /root/jeep-snapshot-$(date -u +%Y%m%dT%H%M%SZ) \
  /root/flyway-adoption-20261016/reference3-extended
```

O segundo argumento é a auditoria de referência **V3**, produzida em MySQL
isolado. Não reutilize essa referência após publicar V4 ou posterior: o script
retorna comparação `null` e exige uma nova referência para a versão publicada.
O quarto argumento opcional informa a versão da referência (padrão `3`).
Exemplo após V4: `bash deploy/snapshot-vps.sh /root/snapshot-v4
/root/reference-v4 /root/restore-evidence-v4.json 4`.
O terceiro argumento opcional é um JSON privado com `backup` (nome do diretório
do backup atual), `restoredTableCount`, `schemaMatch`, `rowsMatch`,
`uploadsRestored`, `releaseSha` e `flywayCliValidated`. Só o forneça depois de
executar a restauração isolada do SQL e dos uploads e
o `flyway info validate`; sem isso, a restauração e a validação CLI ficam `null`.

O diretório de saída contém `jeep-club-vps-snapshot.json`,
`jeep-club-vps-report.md` e `jeep-club-vps-checksums.txt`. Confira hashes com
`sha256sum -c` e revise os arquivos antes de compartilhá-los. O diretório bruto
temporário de `audit-schema.sh` é removido ao final; mantenha qualquer evidência
bruta adicional em área privada da VPS. O script não lê valores de `.env`, mas
consulta apenas nomes permitidos no ambiente do container em memória.

O checkout de `/opt/jeep-club-repo` pode continuar em um commit antigo por
desenho: o wrapper extrai a release aprovada em `/var/lib/jeep-club-deploy`.
Compare `masterSha`, `deployedSha`, a imagem e o diretório da release, não apenas
o HEAD do checkout. Um HTTP 401/403 de uma rota protegida prova que HTTPS e
proxy responderam; o health interno do backend deve estar `UP`.

O snapshot não cria cópia externa de backups, não executa restauração e não
substitui monitoramento contínuo. A chave SSH de deploy permanece limitada a
`deploy <SHA>` e `status`; não foi adicionado um comando de shell nem artifact
público do GitHub Actions para diagnóstico. Execute a coleta por acesso
administrativo controlado quando necessário.
