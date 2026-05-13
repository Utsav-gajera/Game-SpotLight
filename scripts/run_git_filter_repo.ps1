<#
Interactive PowerShell helper to run git-filter-repo safely
Usage (from repo root):
  .\scripts\run_git_filter_repo.ps1 -PushAfter

What it does:
 - Prompts you (securely) for exact secret literals found in commits
 - Creates a mirror clone, runs git-filter-repo --replace-text with a temporary replacements file
 - Optionally force-pushes cleaned refs/tags after you type CONFIRM

Security:
 - This script does NOT commit any replacements file into the repository.
 - It stores the temporary replacements file in a secure temp folder and cleans up on exit.
#>

param(
    [switch]$PushAfter
)

function Read-SecureStringPlain([string]$prompt) {
    $ss = Read-Host -AsSecureString -Prompt $prompt
    $ptr = [System.Runtime.InteropServices.Marshal]::SecureStringToBSTR($ss)
    try { [System.Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr) } finally { [System.Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr) }
}

Write-Host "This script will help remove secrets from git history using git-filter-repo." -ForegroundColor Cyan
Write-Host "You will be prompted to paste each exact secret string found in commits. Leave blank to skip." -ForegroundColor Yellow

# Get remote URL
$origin = git remote get-url origin 2>$null
if (-not $origin) {
    $origin = Read-Host "Enter repository clone URL (e.g. https://github.com/youruser/Game-SpotLight.git)"
} else {
    Write-Host "Using origin: $origin"
}

# Prompt for secrets (secure)
$aivenKafkaSecret = Read-SecureStringPlain "Aiven Kafka SASL password (exact)"
$brevoSmtpKey = Read-SecureStringPlain "Brevo/Sendinblue SMTP password (exact)"
$mongoUri = Read-SecureStringPlain "MongoDB connection URI (exact)"
$redisPassword = Read-SecureStringPlain "Redis password (exact)"
$jwtSecret = Read-SecureStringPlain "JWT secret (exact)"
$opensearchPassword = Read-SecureStringPlain "OpenSearch password (exact)"

# Build replacements file in temp
$tempDir = Join-Path $env:TEMP ("git-filter-repo-" + [System.Guid]::NewGuid().ToString())
New-Item -ItemType Directory -Path $tempDir | Out-Null
$repl = Join-Path $tempDir "replacements.txt"

function Add-Rep([string]$val,[string]$tag) {
    if ([string]::IsNullOrEmpty($val)) { return }
    # Write exact secret and replacement tag
    Add-Content -Path $repl -Value "$val==>[REDACTED-$tag]"
}

Add-Rep $aivenKafkaSecret "AIVEN-KAFKA"
Add-Rep $brevoSmtpKey "BREVO-SMTP"
Add-Rep $mongoUri "MONGO-URI"
Add-Rep $redisPassword "REDIS-PASS"
Add-Rep $jwtSecret "JWT-SECRET"
Add-Rep $opensearchPassword "OPENSEARCH-PASS"

if (-not (Test-Path $repl)) {
    Write-Host "No secrets provided; exiting." -ForegroundColor Yellow
    Remove-Item -Recurse -Force $tempDir -ErrorAction SilentlyContinue
    exit 0
}

Write-Host "Created temporary replacements file at: $repl" -ForegroundColor Green

# Ensure git-filter-repo available
try { git filter-repo --help > $null 2>&1 } catch {
    Write-Host "git-filter-repo not found. Installing via pip (user)." -ForegroundColor Yellow
    python -m pip install --user git-filter-repo
}

$mirror = Join-Path $tempDir "repo.git"
Write-Host "Cloning mirror: $origin -> $mirror" -ForegroundColor Cyan
git clone --mirror $origin $mirror
if ($LASTEXITCODE -ne 0) { Write-Host "mirror clone failed" -ForegroundColor Red; exit 2 }

Push-Location $mirror
Write-Host "Running: git-filter-repo --replace-text $repl" -ForegroundColor Cyan
git-filter-repo --replace-text $repl
if ($LASTEXITCODE -ne 0) { Write-Host "git-filter-repo failed" -ForegroundColor Red; Pop-Location; exit 3 }

if ($PushAfter) {
    $confirm = Read-Host "Force-push cleaned history to remote '$origin'? Type CONFIRM to proceed"
    if ($confirm -eq 'CONFIRM') {
        Write-Host "Force-pushing all refs and tags..." -ForegroundColor Yellow
        git push --force --all $origin
        git push --force --tags $origin
        Write-Host "Force-push complete." -ForegroundColor Green
    } else {
        Write-Host "Push aborted by user. Mirror repo available at: $mirror" -ForegroundColor Yellow
    }
} else {
    Write-Host "Mirror repo is available at: $mirror" -ForegroundColor Green
    Write-Host "Run this script with -PushAfter to offer force-pushing after cleaning." -ForegroundColor Yellow
}

Pop-Location
Write-Host "Done. Remember to rotate/revoke the exposed credentials immediately." -ForegroundColor Cyan
Write-Host "Temporary replacements file: $repl" -ForegroundColor DarkGray
