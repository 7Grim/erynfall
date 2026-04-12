param()

$ErrorActionPreference = 'Stop'

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$repoRoot = (Resolve-Path (Join-Path $scriptDir "..")).Path
$jarPath = Join-Path $repoRoot "client/target/osrs-client-0.1.0-SNAPSHOT.jar"

Write-Host "[artist-launch] Repo root: $repoRoot"
Write-Host "[artist-launch] Jar path:  $jarPath"
Write-Host "[artist-launch] Mode:      artist"

if (-not (Test-Path $jarPath)) {
    Write-Host "[artist-launch] ERROR: client jar not found." -ForegroundColor Red
    Write-Host "[artist-launch] Build it first:" -ForegroundColor Yellow
    Write-Host "  mvn -pl client -am -DskipTests package" -ForegroundColor Yellow
    exit 1
}

& java -jar $jarPath "--artist" "--repo-root=$repoRoot"
