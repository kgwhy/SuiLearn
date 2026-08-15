$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$changesRoot = Join-Path $repoRoot 'openspec\changes'
$archiveRoot = Join-Path $changesRoot 'archive'
$commandPath = Join-Path $repoRoot 'scripts\archive-openspec-change.ps1'
$fixtureName = "archive-command-fixture-$([guid]::NewGuid().ToString('N'))"
$fixtureRoot = Join-Path $changesRoot $fixtureName
$datePrefix = Get-Date -Format 'yyyy-MM-dd'
$targetRoot = Join-Path $archiveRoot "$datePrefix-$fixtureName"

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) { throw $Message }
}

function Invoke-ArchiveCommandExpectFailure {
    param([string[]]$Arguments, [string]$Message)
    $failed = $false
    try {
        & $commandPath @Arguments
        if (-not $?) { $failed = $true }
    } catch {
        $failed = $true
    }
    Assert-True $failed $Message
}

if (-not (Test-Path $commandPath)) { throw "Archive command is missing: $commandPath" }

try {
    New-Item -ItemType Directory -Path $fixtureRoot | Out-Null
    Set-Content -Encoding UTF8 -Path (Join-Path $fixtureRoot 'tasks.md') -Value '# fixture'

    & $commandPath -ChangeName $fixtureName -PrimaryDomain 'developer-tooling'
    if (-not $?) { throw 'A valid archive command invocation must complete successfully.' }

    Assert-True (-not (Test-Path $fixtureRoot)) 'A successful archive must remove the source fixture.'
    Assert-True (Test-Path $targetRoot) 'A successful archive must create the flat target fixture.'

    New-Item -ItemType Directory -Path $fixtureRoot | Out-Null
    Invoke-ArchiveCommandExpectFailure @('-ChangeName', $fixtureName, '-PrimaryDomain', 'unknown-domain') 'An invalid primary domain must fail.'
    Assert-True (Test-Path $fixtureRoot) 'An invalid primary domain must not move the source fixture.'

    Invoke-ArchiveCommandExpectFailure @('-ChangeName', 'archive', '-PrimaryDomain', 'developer-tooling') 'The reserved archive directory must not be accepted as an active change.'
    Assert-True (Test-Path $archiveRoot) 'Rejecting the reserved archive directory must leave the archive root in place.'

    Invoke-ArchiveCommandExpectFailure @('-ChangeName', $fixtureName, '-PrimaryDomain', 'developer-tooling') 'An existing archive target must fail.'
    Assert-True (Test-Path $fixtureRoot) 'A target collision must not move the source fixture.'

    Write-Output 'Archive command tests passed.'
} finally {
    if (Test-Path $fixtureRoot) { Remove-Item -Recurse -Force -LiteralPath $fixtureRoot }
    if (Test-Path $targetRoot) { Remove-Item -Recurse -Force -LiteralPath $targetRoot }
}
