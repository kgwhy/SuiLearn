$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$changesRoot = Join-Path $repoRoot 'openspec\changes'
$archiveRoot = Join-Path $changesRoot 'archive'
$commandPath = Join-Path $repoRoot 'scripts\archive-openspec-change.ps1'
$fixtureName = "archive-command-fixture-$([guid]::NewGuid().ToString('N'))"
$fixtureRoot = Join-Path $changesRoot $fixtureName
$optionalFixtureName = "archive-command-optional-fixture-$([guid]::NewGuid().ToString('N'))"
$optionalFixtureRoot = Join-Path $changesRoot $optionalFixtureName
$domain = 'developer-tooling'
$datePrefix = Get-Date -Format 'yyyy-MM-dd'
$targetRoot = Join-Path (Join-Path $archiveRoot $domain) "$datePrefix-$fixtureName"
$optionalTargetRoot = Join-Path (Join-Path $archiveRoot $domain) "$datePrefix-$optionalFixtureName"
$indexPath = Join-Path $archiveRoot 'README.md'
$indexExisted = Test-Path $indexPath
$indexBackup = if ($indexExisted) { Get-Content -Raw -Encoding UTF8 $indexPath } else { $null }

function Assert-True {
    param([bool]$Condition, [string]$Message)
    if (-not $Condition) {
        throw $Message
    }
}

function Invoke-ArchiveCommandExpectFailure {
    param([string[]]$Arguments, [string]$Message)

    $failed = $false
    try {
        & $commandPath @Arguments
        if (-not $?) {
            $failed = $true
        }
    } catch {
        $failed = $true
    }
    Assert-True $failed $Message
}

if (-not (Test-Path $commandPath)) {
    throw "Archive command is missing: $commandPath"
}

try {
    New-Item -ItemType Directory -Path $fixtureRoot | Out-Null
    Set-Content -Encoding UTF8 -Path (Join-Path $fixtureRoot 'tasks.md') -Value '# fixture'

    & $commandPath -ChangeName $fixtureName -PrimaryDomain $domain -RelatedDomains @('workflow-governance')
    if (-not $?) {
        throw 'A valid archive command invocation must complete successfully.'
    }

    Assert-True (-not (Test-Path $fixtureRoot)) 'A successful archive must remove the source fixture.'
    Assert-True (Test-Path $targetRoot) 'A successful archive must create the nested target fixture.'
    Assert-True ((Get-Content -Raw -Encoding UTF8 $indexPath) -match [regex]::Escape('| Primary domain | Archived change | Related domains |')) 'The index must use the canonical three-column table.'
    Assert-True ((Get-Content -Raw -Encoding UTF8 $indexPath) -match [regex]::Escape("$datePrefix-$fixtureName")) 'The index must include the archived fixture.'

    New-Item -ItemType Directory -Path $fixtureRoot | Out-Null
    Invoke-ArchiveCommandExpectFailure @('-ChangeName', $fixtureName, '-PrimaryDomain', 'unknown-domain') 'An invalid primary domain must fail.'
    Assert-True (Test-Path $fixtureRoot) 'An invalid primary domain must not move the source fixture.'

    Invoke-ArchiveCommandExpectFailure @('-ChangeName', 'archive', '-PrimaryDomain', $domain) 'The reserved archive directory must not be accepted as an active change.'
    Assert-True (Test-Path $archiveRoot) 'Rejecting the reserved archive directory must leave the archive root in place.'

    Invoke-ArchiveCommandExpectFailure @('-ChangeName', $fixtureName, '-PrimaryDomain', $domain) 'An existing archive target must fail.'
    Assert-True (Test-Path $fixtureRoot) 'A target collision must not move the source fixture.'

    New-Item -ItemType Directory -Path $optionalFixtureRoot | Out-Null
    Set-Content -Encoding UTF8 -Path (Join-Path $optionalFixtureRoot 'tasks.md') -Value '# optional fixture'
    & $commandPath -ChangeName $optionalFixtureName -PrimaryDomain $domain
    Assert-True $? 'An archive without related domains must complete successfully.'
    Assert-True (Test-Path $optionalTargetRoot) 'An archive without related domains must create the nested target fixture.'
    Assert-True ((Get-Content -Raw -Encoding UTF8 $indexPath) -match [regex]::Escape("| $domain | [$datePrefix-$optionalFixtureName](./$domain/$datePrefix-$optionalFixtureName/) | - |")) 'The index must record an omitted related-domain list as a dash.'
    Assert-True (-not (Get-ChildItem -Path $archiveRoot -Filter 'README.md.*.tmp' -File -ErrorAction SilentlyContinue)) 'An archive command must not leave a temporary index file.'

    Write-Output 'Archive command tests passed.'
} finally {
    if (Test-Path $fixtureRoot) {
        Remove-Item -Recurse -Force -LiteralPath $fixtureRoot
    }
    if (Test-Path $targetRoot) {
        Remove-Item -Recurse -Force -LiteralPath $targetRoot
    }
    if (Test-Path $optionalFixtureRoot) {
        Remove-Item -Recurse -Force -LiteralPath $optionalFixtureRoot
    }
    if (Test-Path $optionalTargetRoot) {
        Remove-Item -Recurse -Force -LiteralPath $optionalTargetRoot
    }
    if ($indexExisted) {
        Set-Content -Encoding UTF8 -Path $indexPath -Value $indexBackup
    } elseif (Test-Path $indexPath) {
        Remove-Item -Force -LiteralPath $indexPath
    }
}
