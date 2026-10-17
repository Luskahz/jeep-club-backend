#!/usr/bin/env python3
"""Read-only, allowlisted VPS collection. Never serialize environment or raw logs."""
import datetime as dt
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request

SAFE_NAME = re.compile(r"^[A-Za-z0-9_.-]{1,128}$")
SHA = re.compile(r"^[0-9a-f]{40}$")
SERVICES = ("jeep-club-backend-1", "jeep-club-mysql-1", "jeep-club-proxy-1")
VOLUMES = ("jeep-club_mysql-data", "jeep-club_uploads")
STATE = Path("/var/lib/jeep-club-deploy")
REPO = Path("/opt/jeep-club-repo")


def command(*args, input_text=None, timeout=30):
    try:
        p = subprocess.run(args, input=input_text, text=True, capture_output=True, timeout=timeout)
        return p.returncode, p.stdout
    except (OSError, subprocess.TimeoutExpired):
        return -1, ""


def read(path):
    try:
        return Path(path).read_text().strip()
    except OSError:
        return ""


def safe_name(value):
    return value if SAFE_NAME.fullmatch(value or "") else "[redacted]"


def cpu_percent():
    def sample():
        numbers = [int(x) for x in Path("/proc/stat").read_text().splitlines()[0].split()[1:]]
        return sum(numbers), numbers[3] + numbers[4]
    try:
        a, b = sample()
        time.sleep(0.25)
        c, d = sample()
        return round(100 * (1 - (d - b) / (c - a)), 1) if c > a else None
    except (OSError, ValueError, ZeroDivisionError):
        return None


def docker_inspect(name):
    rc, out = command("docker", "inspect", name)
    try:
        return json.loads(out)[0] if rc == 0 else None
    except (ValueError, IndexError):
        return None


def same_file_content(left, right):
    try:
        return Path(left).read_bytes() == Path(right).read_bytes()
    except OSError:
        return False


def iso_mtime(path):
    try:
        return dt.datetime.fromtimestamp(Path(path).stat().st_mtime, dt.timezone.utc).isoformat().replace("+00:00", "Z")
    except OSError:
        return None


def audit_lines(path):
    try:
        return Path(path).read_text().splitlines()
    except OSError:
        return []


def recent_log_counts(name, since):
    # docker logs normally emits application output on stderr; retrieve it privately.
    try:
        p = subprocess.run(["docker", "logs", "--since", since, name], capture_output=True, text=True, timeout=20)
        raw = p.stdout + "\n" + p.stderr
    except (OSError, subprocess.TimeoutExpired):
        return None
    patterns = {
        "sqlExceptions": r"SQLSyntaxErrorException|SQLIntegrityConstraintViolationException|SQLException|Schema-validation|FlywayException",
        "outOfMemory": r"OutOfMemoryError|Killed process",
        "smtpFailures": r"MailSendException|SMTP.{0,30}(?:error|fail)",
        "flywayCurrent": r"Schema.{0,80}up to date|Current version of schema",
        "flywayValidated": r"Successfully validated|Validated \d+ migrations",
        "jpaInitialized": r"Initialized JPA EntityManagerFactory",
        "http5xx": r'"\s+(?:500|502|503|504)\s+',
    }
    return {key: len(re.findall(pattern, raw, re.I)) for key, pattern in patterns.items()}


def https_probe(env):
    url = env.get("APP_SERVER_BASE_URL", "")
    parsed = urllib.parse.urlparse(url)
    if parsed.scheme != "https" or not parsed.hostname:
        return {"tlsVerified": None, "httpStatus": None, "protectedEndpointExpected": None}
    try:
        with urllib.request.urlopen(url.rstrip("/") + "/", timeout=10) as response:
            status = response.status
        return {"tlsVerified": True, "httpStatus": status, "protectedEndpointExpected": status in (200, 401, 403, 404)}
    except urllib.error.HTTPError as error:
        return {"tlsVerified": True, "httpStatus": error.code, "protectedEndpointExpected": error.code in (401, 403, 404)}
    except Exception:
        return {"tlsVerified": False, "httpStatus": None, "protectedEndpointExpected": False}


def reference_comparison(reference, audit, script_dir, latest_version, reference_version):
    if not reference or not Path(reference).is_dir() or not Path(audit).is_dir():
        return None, ["Referência estrutural ou auditoria indisponível."], None
    # Refuse to compare different migration versions, even if metadata happens to match.
    if latest_version != reference_version:
        return None, [f"A referência V{reference_version} não representa a versão publicada."], reference_version
    rc, output = command("python3", str(Path(script_dir) / "compare-schema.py"), reference, audit)
    if rc == 0:
        return True, [], reference_version
    if rc == 1:
        names = sorted(set(re.findall(r"(?:reference|actual)/([A-Za-z0-9-]+\.tsv)", output)))
        return False, ["Metadados divergentes em " + ", ".join(names or ["arquivos não identificados"])], reference_version
    return None, ["Comparação estrutural falhou."], reference_version


def collect(output, audit, reference, restore_evidence, audit_ok, script_dir, reference_version):
    now = dt.datetime.now(dt.timezone.utc).isoformat().replace("+00:00", "Z")
    rc, os_text = command("sh", "-c", ". /etc/os-release; printf '%s %s' \"$NAME\" \"$VERSION_ID\"")
    rc, uptime = command("uptime", "-p")
    rc, memory = command("free", "-m")
    mem = memory.splitlines()[1].split() if len(memory.splitlines()) > 1 else []
    rc, disks = command("df", "-B1", "--output=source,size,used,avail,pcent", "/")
    disk_fields = disks.splitlines()[-1].split() if len(disks.splitlines()) > 1 else []
    rc, clock = command("timedatectl", "show", "-p", "Timezone", "-p", "NTPSynchronized")
    time_props = dict(line.split("=", 1) for line in clock.splitlines() if "=" in line)
    rc, failed_units = command("systemctl", "--failed", "--no-legend", "--no-pager")
    failed_names = [x.split()[1] for x in failed_units.splitlines() if len(x.split()) > 1 and SAFE_NAME.fullmatch(x.split()[1])]
    rc, journal = command("journalctl", "-p", "0..3", "--since", "24 hours ago", "--no-pager", "-o", "json")
    journal_units = {}
    for line in journal.splitlines():
        try:
            unit = json.loads(line).get("_SYSTEMD_UNIT", "other")
            unit = safe_name(unit)
            journal_units[unit] = journal_units.get(unit, 0) + 1
        except ValueError:
            pass
    rc, master = command("git", "ls-remote", "https://github.com/Luskahz/jeep-club-backend.git", "refs/heads/master", timeout=15)
    master_sha = master.split()[0] if rc == 0 and master.split() and SHA.fullmatch(master.split()[0]) else None
    deployed_sha = read(STATE / "current-commit")
    deployed_sha = deployed_sha if SHA.fullmatch(deployed_sha) else None
    image_marker = read(STATE / "current-image")
    release_path = read(STATE / "current-release")
    release = Path(release_path) if release_path.startswith(str(STATE / "release-")) else None
    rc, repo_head = command("git", "-C", str(REPO), "rev-parse", "HEAD")
    rc, repo_master = command("git", "-C", str(REPO), "rev-parse", "refs/remotes/origin/master")
    containers, unhealthy, restarts = [], [], []
    inspected = {}
    for name in SERVICES:
        item = docker_inspect(name)
        if not item:
            unhealthy.append(name)
            continue
        inspected[name] = item
        state = item.get("State", {})
        health = (state.get("Health") or {}).get("Status")
        row = {"service": name, "image": item.get("Config", {}).get("Image"),
               "running": state.get("Running"), "health": health,
               "restartCount": item.get("RestartCount"),
               "restartPolicy": item.get("HostConfig", {}).get("RestartPolicy", {}).get("Name"),
               "mounts": [{"type": m.get("Type"), "volume": m.get("Name"), "destination": m.get("Destination")}
                          for m in item.get("Mounts", []) if m.get("Type") == "volume"],
               "networks": sorted(item.get("NetworkSettings", {}).get("Networks", {}).keys())}
        containers.append(row)
        if not state.get("Running") or health == "unhealthy":
            unhealthy.append(name)
        if item.get("RestartCount", 0):
            restarts.append({"service": name, "count": item["RestartCount"]})
    volumes, volume_bytes = [], {}
    for name in VOLUMES:
        volume = docker_inspect(name)
        if volume:
            volumes.append(name)
            mountpoint = volume.get("Mountpoint")
            if mountpoint:
                rc, size = command("du", "-sb", mountpoint)
                if rc == 0 and size.split() and size.split()[0].isdigit():
                    volume_bytes[name] = int(size.split()[0])
    rc, docker_storage = command("docker", "system", "df", "--format", "{{.Type}}|{{.Size}}")
    storage_summary = dict(x.split("|", 1) for x in docker_storage.splitlines() if "|" in x)
    backend = inspected.get("jeep-club-backend-1", {})
    env = dict(x.split("=", 1) for x in backend.get("Config", {}).get("Env", []) if "=" in x)
    allowed = {key: env.get(key) for key in (
        "SPRING_PROFILES_ACTIVE", "SPRING_JPA_HIBERNATE_DDL_AUTO", "SPRING_FLYWAY_ENABLED",
        "SPRING_FLYWAY_BASELINE_ON_MIGRATE", "SPRING_FLYWAY_CLEAN_DISABLED",
        "SPRING_FLYWAY_VALIDATE_ON_MIGRATE", "SPRING_FLYWAY_OUT_OF_ORDER")}
    mail_mode = "dummy" if allowed["SPRING_PROFILES_ACTIVE"] == "dev" else "smtp" if allowed["SPRING_PROFILES_ACTIVE"] else None
    smtp_configured = all(env.get(key) for key in ("SPRING_MAIL_HOST", "SPRING_MAIL_USERNAME", "SPRING_MAIL_PASSWORD", "APP_MAIL_FROM"))
    admin_bootstrap_running = env.get("SECURITY_BOOTSTRAP_ADMIN_ENABLED") == "true"
    admin_bootstrap_on_disk = None
    for line in read("/opt/jeep-club-compose/.env").splitlines():
        if line.startswith("SECURITY_BOOTSTRAP_ADMIN_ENABLED="):
            admin_bootstrap_on_disk = line.split("=", 1)[1] == "true"
            break
    java_version = None
    if backend:
        try:
            p = subprocess.run(["docker", "exec", "jeep-club-backend-1", "java", "-version"], capture_output=True, text=True, timeout=10)
            match = re.search(r'openjdk version "([0-9.]+)"', p.stderr + p.stdout)
            java_version = match.group(1) if match else None
        except (OSError, subprocess.TimeoutExpired):
            pass
    rc, health_text = command("docker", "exec", "jeep-club-backend-1", "wget", "-qO-", "http://127.0.0.1:8081/actuator/health")
    try:
        health = json.loads(health_text).get("status") if rc == 0 else None
    except ValueError:
        health = None
    https = https_probe(env)
    log_counts = recent_log_counts("jeep-club-backend-1", "24h") or {}
    proxy_logs = recent_log_counts("jeep-club-proxy-1", "24h") or {}
    backend_started = backend.get("State", {}).get("StartedAt")
    proxy_since_release = recent_log_counts("jeep-club-proxy-1", backend_started) if backend_started else None
    proxy_to_backend = None
    try:
        p = subprocess.run(["docker", "exec", "jeep-club-proxy-1", "wget", "-S", "-O", "/dev/null", "http://backend:8080/"],
                           capture_output=True, text=True, timeout=10)
        match = re.search(r"HTTP/1\.1\s+(\d{3})", p.stdout + p.stderr)
        proxy_to_backend = int(match.group(1)) if match else None
    except (OSError, subprocess.TimeoutExpired):
        pass
    server_line = audit_lines(Path(audit) / "server.tsv")
    server_cols = server_line[0].split("\t") if server_line else []
    tables = audit_lines(Path(audit) / "tables.tsv")
    views = audit_lines(Path(audit) / "views.tsv")
    fks = audit_lines(Path(audit) / "foreign-keys.tsv")
    index_rows = audit_lines(Path(audit) / "indexes.tsv")
    indexes = {(x.split("\t")[0], x.split("\t")[1]) for x in index_rows if len(x.split("\t")) > 1}
    counts = {}
    for line in audit_lines(Path(audit) / "counts.tsv"):
        parts = line.split("\t")
        if len(parts) == 2 and SAFE_NAME.fullmatch(parts[0]) and parts[1].isdigit():
            counts[parts[0]] = int(parts[1])
    history = []
    for line in audit_lines(Path(audit) / "flyway-history.tsv"):
        p = line.split("\t")
        if len(p) == 7:
            history.append({"installed_rank": int(p[0]), "version": safe_name(p[1]),
                            "description": safe_name(p[2]), "type": safe_name(p[3]),
                            "script": safe_name(p[4]), "checksum": int(p[5]) if p[5].lstrip("-").isdigit() else None,
                            "success": p[6] == "1"})
    baseline = next((x["version"] for x in history if x["type"] == "BASELINE"), None)
    failed_migrations = [x["version"] for x in history if not x["success"]]
    migration_files = sorted((release / "src/main/resources/db/migration").glob("V*.sql")) if release else []
    versions = [int(m.group(1)) for path in migration_files if (m := re.match(r"V(\d+)__", path.name))]
    applied_versions = [int(x["version"]) for x in history if x["version"].isdigit() and x["success"]]
    current_version = max(applied_versions, default=0)
    pending = sorted(v for v in versions if v > current_version and v not in applied_versions)
    schema_match, schema_diff, reference_version = reference_comparison(reference, audit, script_dir, max(versions, default=0), reference_version)
    latest_backup = read(STATE / "latest-backup")
    backup_path = Path(latest_backup) if latest_backup.startswith("/var/backups/jeep-club/") else None
    backup_ok = None
    backup_sizes = {}
    if backup_path and backup_path.is_dir():
        try:
            p = subprocess.run(["sha256sum", "-c", "SHA256SUMS"], cwd=backup_path, capture_output=True, text=True, timeout=120)
            backup_ok = p.returncode == 0
        except (OSError, subprocess.TimeoutExpired):
            backup_ok = False
        for name in ("database.sql.gz", "uploads.tar.gz", "config.tar.gz"):
            try:
                backup_sizes[name] = (backup_path / name).stat().st_size
            except OSError:
                backup_sizes[name] = None
    restore_ok = None
    cli_validation = None
    admin_change_evidenced = False
    if restore_evidence:
        try:
            proof = json.loads(Path(restore_evidence).read_text())
            if (backup_path and proof.get("backup") == backup_path.name and
                    proof.get("restoredTableCount") == len(tables) + 1 and
                    proof.get("schemaMatch") is True and proof.get("rowsMatch") is True and
                    proof.get("uploadsRestored") is True):
                restore_ok = True
            if proof.get("releaseSha") == deployed_sha and proof.get("flywayCliValidated") is True:
                cli_validation = True
            if proof.get("adminBootstrapDisabledOnDisk") is True and admin_bootstrap_on_disk is False:
                admin_change_evidenced = True
        except (OSError, ValueError):
            pass
    rc, timer = command("systemctl", "show", "jeep-club-backup.timer", "-p", "ActiveState", "-p", "UnitFileState")
    timer_state = dict(x.split("=", 1) for x in timer.splitlines() if "=" in x)
    wrapper_match = bool(release and same_file_content("/usr/local/sbin/jeep-club-deploy", release / "deploy/vps-deploy-wrapper.sh"))
    backup_script_match = bool(release and same_file_content("/usr/local/sbin/jeep-club-backup", release / "deploy/vps-backup.sh"))
    restricted_key = 'command="/usr/local/sbin/jeep-club-deploy"' in read("/root/.ssh/authorized_keys")
    rc, tagged_image_id = command("docker", "image", "inspect", f"jeep-club-backend:{deployed_sha}", "--format", "{{.Id}}") if deployed_sha else (-1, "")
    image_match = bool(deployed_sha and backend.get("Config", {}).get("Image") == f"jeep-club-backend:{deployed_sha}" and
                       image_marker == f"jeep-club-backend:{deployed_sha}" and rc == 0 and backend.get("Image") == tagged_image_id.strip())
    release_match = bool(release and deployed_sha and release.name.startswith(f"release-{deployed_sha}-")
                           and backend.get("Config", {}).get("Labels", {}).get("com.docker.compose.project.working_dir") == str(release))
    flyway_ready = bool(audit_ok and schema_match and not failed_migrations and not pending and
                        allowed["SPRING_FLYWAY_ENABLED"] == "true" and allowed["SPRING_FLYWAY_BASELINE_ON_MIGRATE"] == "false" and
                        allowed["SPRING_FLYWAY_CLEAN_DISABLED"] == "true" and allowed["SPRING_FLYWAY_VALIDATE_ON_MIGRATE"] == "true" and
                        allowed["SPRING_FLYWAY_OUT_OF_ORDER"] == "false" and allowed["SPRING_JPA_HIBERNATE_DDL_AUTO"] == "validate" and
                        log_counts.get("flywayValidated", 0) > 0)
    production_healthy = bool(health == "UP" and not unhealthy and https["tlsVerified"] is True and
                              https["protectedEndpointExpected"] and not log_counts.get("sqlExceptions") and not log_counts.get("outOfMemory"))
    deploy_ready = bool(flyway_ready and wrapper_match and restricted_key and release_match and image_match and
                        master_sha == deployed_sha and backup_ok)
    findings = []
    if not schema_match: findings.append("Comparação estrutural não comprovada ou divergente.")
    if not production_healthy: findings.append("Saúde de produção não comprovada.")
    if not deploy_ready: findings.append("Cadeia de deploy não comprovada integralmente.")
    findings += ["Backups verificados localmente; cópia fora da VPS não foi comprovada.",
                 "SSH recebeu tentativas externas de autenticação; rever acesso com plano de continuidade."]
    if failed_names: findings.append("Unidades systemd falhas: " + ", ".join(failed_names))
    if mail_mode == "dummy":
        findings.append("Perfil Spring dev ativa remetentes simulados: ativação, associação e recuperação de senha não enviam e-mails reais.")
    if not smtp_configured:
        findings.append("Configuração SMTP de produção incompleta; mudar o perfil agora não é seguro.")
    if admin_bootstrap_running and admin_bootstrap_on_disk is False:
        findings.append("Bootstrap de administrador ainda ativo no processo atual; já desativado no .env para o próximo deploy.")
    if restore_ok is None: findings.append("Restauração do backup atual não comprovada neste snapshot.")
    status = "SAUDÁVEL COM RESSALVAS" if production_healthy and deploy_ready and mail_mode != "dummy" else "AÇÃO NECESSÁRIA"
    data = {
        "snapshot": {"generatedAtUtc": now, "environment": "production", "status": status, "collectedVia": "ssh", "readOnly": True},
        "server": {"os": os_text.strip() or None, "uptime": uptime.strip() or None, "cpuUsage": {"percent": cpu_percent(), "logicalCpus": os.cpu_count()},
                   "memoryUsage": {"totalMiB": int(mem[1]), "usedMiB": int(mem[2]), "availableMiB": int(mem[6])} if len(mem) > 6 else None,
                   "diskUsage": {"sizeBytes": int(disk_fields[1]), "usedBytes": int(disk_fields[2]), "availableBytes": int(disk_fields[3]), "usedPercent": disk_fields[4]} if len(disk_fields) >= 5 else None,
                   "timeSync": {"timezone": time_props.get("Timezone"), "ntpSynchronized": time_props.get("NTPSynchronized") == "yes"},
                   "failedServices": failed_names, "criticalJournalEntries24hByUnit": journal_units},
        "git": {"repository": "Luskahz/jeep-club-backend", "masterSha": master_sha, "deployedSha": deployed_sha,
                "imageCommitMatches": image_match, "releaseDirectoryMatches": release_match,
                "repositoryCheckoutSha": repo_head.strip() if SHA.fullmatch(repo_head.strip()) else None,
                "cachedOriginMasterSha": repo_master.strip() if SHA.fullmatch(repo_master.strip()) else None},
        "docker": {"containers": containers, "unhealthyContainers": unhealthy, "volumesPresent": volumes,
                   "volumeSizeBytes": volume_bytes, "storageSummary": storage_summary, "unexpectedRestarts": restarts},
        "application": {"springProfile": allowed["SPRING_PROFILES_ACTIVE"], "javaVersion": java_version, "health": health,
                        "hibernateDdlAuto": allowed["SPRING_JPA_HIBERNATE_DDL_AUTO"], "recentErrors": [k for k in ("sqlExceptions", "outOfMemory", "smtpFailures") if log_counts.get(k)],
                        "mailDeliveryMode": mail_mode, "smtpConfigured": smtp_configured,
                        "adminBootstrapRunning": admin_bootstrap_running, "adminBootstrapOnDisk": admin_bootstrap_on_disk,
                        "startupEvidence": {k: log_counts.get(k) for k in ("flywayCurrent", "flywayValidated", "jpaInitialized")},
                        "https": https, "proxyToBackendHttpStatus": proxy_to_backend,
                        "proxyRecent5xxCount": proxy_logs.get("http5xx"),
                        "proxy5xxSinceRelease": proxy_since_release.get("http5xx") if proxy_since_release else None},
        "mysql": {"version": server_cols[0] if len(server_cols) > 0 else None, "database": "jeepclub", "tableCount": len(tables) + (1 if history else 0) if audit_ok else None,
                  "viewCount": len(views) if audit_ok else None, "foreignKeyCount": len(fks) if audit_ok else None,
                  "indexCount": len(indexes) if audit_ok else None, "rowCountTotal": sum(counts.values()) if audit_ok else None,
                  "rowCounts": counts if audit_ok else None, "charset": server_cols[2] if len(server_cols) > 2 else None,
                  "collation": server_cols[3] if len(server_cols) > 3 else None,
                  "schemaMatchesExpected": schema_match, "referenceVersion": reference_version, "schemaDifferences": schema_diff},
        "flyway": {"enabled": allowed["SPRING_FLYWAY_ENABLED"] == "true", "baselineVersion": baseline,
                   "history": history, "pendingMigrations": pending, "failedMigrations": failed_migrations,
                   "validationResult": "CLI_VALIDATED" if cli_validation else ("STARTUP_VALIDATED" if log_counts.get("flywayValidated", 0) else None),
                   "settings": {k: allowed[k] for k in allowed if k.startswith("SPRING_FLYWAY_")}},
        "deployment": {"lastSuccessfulSha": deployed_sha, "lastDeploymentResult": "success" if release_match and image_match and health == "UP" else None,
                       "deployWrapperVerified": wrapper_match and restricted_key, "backupScriptMatchesRelease": backup_script_match,
                       "backupBeforeDeploy": backup_ok and bool(backup_path and iso_mtime(backup_path) and iso_mtime(STATE / "current-commit") and iso_mtime(backup_path) < iso_mtime(STATE / "current-commit")),
                       "automaticMigrationsReady": deploy_ready, "restrictedSshCommand": restricted_key},
        "backups": {"lastBackupUtc": iso_mtime(backup_path) if backup_path else None, "latestBackupId": backup_path.name if backup_path else None,
                    "latestArchiveBytes": backup_sizes, "checksumVerified": backup_ok, "restoreVerified": restore_ok,
                    "uploadsRestoreVerified": restore_ok,
                    "scheduleActive": timer_state.get("ActiveState") == "active" and timer_state.get("UnitFileState") == "enabled",
                    "offsiteBackup": None, "retentionPolicy": "no automatic pruning in installed backup script",
                    "warnings": ["Cópia fora da VPS não comprovada."]},
        "findings": findings,
        "changesPerformed": (["Desativado bootstrap de administrador no .env para próximos deploys; processo atual não reiniciado."] if admin_change_evidenced else []),
        "pendingActions": ["Configurar SMTP e planejar mudança aprovada do perfil dev para produção; testar envio real sem afetar usuários.",
                           "Configurar e testar cópia de backup fora da VPS.", "Planejar endurecimento do acesso SSH sem perder acesso administrativo."],
        "overallAssessment": {"status": status, "productionHealthy": production_healthy, "databaseConsistent": schema_match,
                              "flywayReadyForFutureReleases": flyway_ready, "requiresImmediateAction": not (production_healthy and deploy_ready) or mail_mode == "dummy"},
        "evidence": {"commands": ["audit-schema.sh", "compare-schema.py", "docker inspect", "docker logs (somente contagens)",
                                  "mysql information_schema/flyway_schema_history (somente metadados)", "sha256sum -c", "systemctl show", "GET /actuator/health", "HTTPS GET / (somente status)"],
                     "notes": ["O checkout de /opt/jeep-club-repo permanece no commit anterior por desenho; o release é extraído de origin/master.",
                               "HTTP 401/403 em endpoint protegido não significa indisponibilidade."]}
    }
    Path(output, "jeep-club-vps-snapshot.json").write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n")
    write_report(Path(output, "jeep-club-vps-report.md"), data)


def write_report(path, data):
    s, g, d, a, m, f, b, dep = (data[x] for x in ("snapshot", "git", "docker", "application", "mysql", "flyway", "backups", "deployment"))
    lines = ["# Jeep Club — snapshot sanitizado da VPS", "", f"Coletado em **{s['generatedAtUtc']}**. Estado: **{s['status']}**. Somente leitura do banco e dos serviços.", "",
             "## Release e operação", "", f"- Master e release publicada: `{g['masterSha']}` / `{g['deployedSha']}`.",
             f"- Imagem e diretório da release conferem: {g['imageCommitMatches']} / {g['releaseDirectoryMatches']}.",
             f"- Backend: {a['health']}; HTTPS com TLS válido: {a['https']['tlsVerified']}; resposta pública: HTTP {a['https']['httpStatus']} (endpoint protegido).",
             f"- Proxy → backend: HTTP {a['proxyToBackendHttpStatus']}; erros 5xx nas últimas 24 h: {a['proxyRecent5xxCount']}; desde esta release: {a['proxy5xxSinceRelease']}.",
             f"- Java: {a['javaVersion']}; perfil: {a['springProfile']}; Hibernate: `{a['hibernateDdlAuto']}`.",
             f"- E-mail: modo `{a['mailDeliveryMode']}`; SMTP configurado: {a['smtpConfigured']}. Bootstrap admin: processo {a['adminBootstrapRunning']}, arquivo para próximo deploy {a['adminBootstrapOnDisk']}.",
             f"- Containers: {len(d['containers'])}; não saudáveis: {len(d['unhealthyContainers'])}; reinícios: {len(d['unexpectedRestarts'])}.",
             f"- SO: {data['server']['os']}; uptime: {data['server']['uptime']}; NTP: {data['server']['timeSync']['ntpSynchronized']}; disco: {data['server']['diskUsage']['usedPercent'] if data['server']['diskUsage'] else 'não verificado'}.", "",
             "## MySQL e Flyway", "", f"- MySQL `{m['version']}`, banco `{m['database']}`, `{m['tableCount']}` tabelas, `{m['viewCount']}` views, `{m['foreignKeyCount']}` FKs, `{m['indexCount']}` índices.",
             f"- Charset/collation: `{m['charset']}` / `{m['collation']}`. Comparação estrutural com referência V{m['referenceVersion']}: **{m['schemaMatchesExpected']}**.",
             f"- Baseline: V{f['baselineVersion']}; pendentes: {f['pendingMigrations']}; falhas: {f['failedMigrations']}; validação no startup: {f['validationResult']}.",
             "", "| rank | versão | descrição | tipo | script | checksum | sucesso |", "|---:|---|---|---|---|---:|---|" ]
    for h in f["history"]:
        lines.append(f"| {h['installed_rank']} | {h['version']} | {h['description']} | {h['type']} | {h['script']} | {h['checksum']} | {h['success']} |")
    lines += ["", "## Deploy e backups", "", f"- Wrapper restrito confere: {dep['deployWrapperVerified']}; backup antes do deploy: {dep['backupBeforeDeploy']}; migrations automáticas prontas: {dep['automaticMigrationsReady']}.",
              f"- Backup mais recente: {b['lastBackupUtc']}; checksums: {b['checksumVerified']}; restauração isolada de SQL e uploads: {b['restoreVerified']}; agendamento: {b['scheduleActive']}.",
              "- Retenção: sem remoção automática no script instalado; há risco de crescimento futuro do uso de disco.",
              "- Cópia externa: não comprovada. O snapshot não contém conteúdo de `.env`, logs brutos nem linhas de usuários.", "", "## Achados", ""]
    lines.extend("- " + x for x in data["findings"])
    lines += ["", "## Alterações e pendências", ""]
    lines.extend("- Alteração: " + x for x in data["changesPerformed"])
    lines.extend("- Pendente: " + x for x in data["pendingActions"])
    lines += ["", "## Evidências e limites", "", "- Comandos de leitura: " + ", ".join(data["evidence"]["commands"]) + ".",
              "- A restauração isolada e a validação explícita do Flyway CLI são marcadas como verificadas somente com evidência privada correspondente ao backup e à release atuais.",
              "- Os contadores de erros vieram dos logs em memória; nenhuma linha bruta de log foi incluída.",
              "- A contagem de registros por tabela está no JSON, sem exportação de linhas.", ""]
    path.write_text("\n".join(lines))


if __name__ == "__main__":
    if len(sys.argv) != 8:
        sys.exit("Usage: snapshot-vps.py OUTPUT AUDIT REFERENCE RESTORE_EVIDENCE AUDIT_OK SCRIPT_DIR REFERENCE_VERSION")
    collect(*sys.argv[1:4], sys.argv[4], sys.argv[5] == "true", sys.argv[6], int(sys.argv[7]))
