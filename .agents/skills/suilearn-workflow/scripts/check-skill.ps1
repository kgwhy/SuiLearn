param(
    [string]$SkillRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)

$ErrorActionPreference = "Stop"

$python = Get-Command python3 -ErrorAction SilentlyContinue
if ($null -eq $python) { $python = Get-Command python -ErrorAction SilentlyContinue }
if ($null -eq $python) {
    Write-Error "Python 3 is required for skill check."
    exit 1
}

$repoRoot = Split-Path -Parent (Split-Path -Parent (Split-Path -Parent (Split-Path -Parent $PSScriptRoot)))
$checker = Join-Path $repoRoot 'scripts\check_workflow_skill.py'
if (-not (Test-Path $checker)) {
    Write-Error "Skill checker missing: $checker"
    exit 1
}

& $python.Source $checker
exit $LASTEXITCODE
