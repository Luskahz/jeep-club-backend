param(
    [Parameter(Mandatory=$true)][ValidateSet('baseline','final')][string]$Phase,
    [Parameter(Mandatory=$true)][string]$EvidenceDirectory,
    [switch]$SkipCleanTest
)
$ErrorActionPreference = 'Stop'
New-Item -ItemType Directory -Force -Path $EvidenceDirectory | Out-Null
$root = (git rev-parse --show-toplevel).Trim()
if ((Get-Location).Path -ne $root.Replace('/','\')) { throw 'Run from repository root' }
$sha = (git rev-parse HEAD).Trim()
$manifest = [System.Collections.Generic.List[object]]::new()

function Run-Evidence([string]$Name, [string[]]$MavenArguments, [string]$PitModule) {
    $destination = Join-Path $EvidenceDirectory "$Phase-$Name"
    if (Test-Path $destination) { throw "Evidence already exists: use a new directory for every attempt ($destination)" }
    New-Item -ItemType Directory -Force -Path $destination | Out-Null
    $started = [DateTime]::UtcNow
    & .\mvnw.cmd --batch-mode --no-transfer-progress @MavenArguments *> (Join-Path $destination 'maven.log')
    $code = $LASTEXITCODE
    $items = if ($PitModule) { @() } else { @('surefire-reports','failsafe-reports','site/jacoco','jacoco.exec','hardening-unit.exec','hardening-mysql.exec','hardening-merged.exec') }
    foreach ($item in $items) {
        if (Test-Path "target/$item") {
            $name = $item.Replace('/','-')
            Copy-Item -LiteralPath "target/$item" -Destination (Join-Path $destination $name) -Recurse
        }
    }
    if ($PitModule -and (Test-Path "target/pit-reports/$PitModule")) {
        Copy-Item "target/pit-reports/$PitModule" (Join-Path $destination 'pit') -Recurse
        python docs/testing/final-hardening/mutations.py "$destination/pit/mutations.xml" "$destination/mutations.json"
    }
    $manifest.Add(@{name=$Name; sha=$sha; arguments=$MavenArguments; exitCode=$code;
                   startedUtc=$started.ToString('o'); endedUtc=[DateTime]::UtcNow.ToString('o')})
    $manifest | ConvertTo-Json -Depth 5 | Set-Content -Encoding utf8 (Join-Path $EvidenceDirectory "$Phase-manifest.json")
    Write-Output "$Phase $Name exit=$code"
    # A failed run remains diagnostic; following runs collect independent evidence, never hide it.
}

if (!$SkipCleanTest) { Run-Evidence 'clean-test' @('clean','test') '' }
Run-Evidence 'verify' @('-Djacoco.destFile=target/hardening-unit.exec','-Djacoco.dataFile=target/hardening-unit.exec','verify') ''
Run-Evidence 'mysql' @('-Pmysql-migrations','-DskipUnitTests=true','-Djacoco.destFile=target/hardening-mysql.exec','-Djacoco.dataFile=target/hardening-mysql.exec','verify') ''
Run-Evidence 'merged' @('-Phardening-coverage-merge','jacoco:merge','jacoco:report') ''
foreach ($module in @('identity','authorization','publications','platform-shared')) {
    Run-Evidence "pit-$module" @("-P$module-hardening-mutation",'org.pitest:pitest-maven:mutationCoverage') $module
}
if ($manifest | Where-Object { $_.exitCode -ne 0 }) { exit 1 }
