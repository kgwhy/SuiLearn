[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [ValidatePattern('^[a-z0-9]+(?:-[a-z0-9]+)*$')]
    [string]$ChangeName,

    [Parameter(Mandatory = $true)]
    [ValidateSet('workflow-governance', 'platform-runtime', 'learning-rag', 'developer-tooling')]
    [string]$PrimaryDomain,

    [ValidateSet('workflow-governance', 'platform-runtime', 'learning-rag', 'developer-tooling')]
    [string[]]$RelatedDomains = @(),

    [ValidatePattern('^\d{4}-\d{2}-\d{2}$')]
    [string]$ArchiveDate = (Get-Date -Format 'yyyy-MM-dd')
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$changesRoot = Join-Path $repoRoot 'openspec\changes'
$archiveRoot = Join-Path $changesRoot 'archive'
$sourceRoot = Join-Path $changesRoot $ChangeName
$leafName = "$ArchiveDate-$ChangeName"
$domainRoot = Join-Path $archiveRoot $PrimaryDomain
$targetRoot = Join-Path $domainRoot $leafName
$indexPath = Join-Path $archiveRoot 'README.md'
$indexTempPath = "$indexPath.$([guid]::NewGuid().ToString('N')).tmp"

if ($RelatedDomains -contains $PrimaryDomain) {
    throw 'RelatedDomains must not repeat PrimaryDomain.'
}

if ($ChangeName -eq 'archive') {
    throw 'The archive directory is reserved and cannot be archived.'
}

if (-not (Test-Path -LiteralPath $sourceRoot -PathType Container)) {
    throw "Active change directory does not exist: $sourceRoot"
}

$sourceParent = Split-Path -Parent (Resolve-Path -LiteralPath $sourceRoot).Path
if ($sourceParent -ne $changesRoot) {
    throw 'Only a direct child of openspec/changes can be archived.'
}

if (Test-Path -LiteralPath $targetRoot) {
    throw "Archive target already exists: $targetRoot"
}

$indexHeader = @(
    '# OpenSpec archive index',
    '',
    'Each archived change has one canonical primary technical domain. Related domains are navigation tags only and never create a second copy.',
    '',
    '| Primary domain | Archived change | Related domains |',
    '| --- | --- | --- |'
)
$relatedLabel = if ($RelatedDomains.Count -gt 0) { ($RelatedDomains | Sort-Object -Unique) -join ', ' } else { '-' }
$relativeTarget = "./$PrimaryDomain/$leafName/"
$indexEntry = "| $PrimaryDomain | [$leafName]($relativeTarget) | $relatedLabel |"

$indexExisted = Test-Path -LiteralPath $indexPath
$indexText = if ($indexExisted) {
    Get-Content -Raw -Encoding UTF8 -LiteralPath $indexPath
} else {
    ($indexHeader -join [Environment]::NewLine) + [Environment]::NewLine
}
if ($indexExisted -and $indexText -notmatch [regex]::Escape('| Primary domain | Archived change | Related domains |')) {
    throw "Archive index has an unexpected format: $indexPath"
}
if ($indexText -match [regex]::Escape("[$leafName](")) {
    throw "Archive index already contains an entry for: $leafName"
}

$moved = $false
try {
    New-Item -ItemType Directory -Force -Path $domainRoot | Out-Null
    $separator = if ($indexText.EndsWith([Environment]::NewLine)) { '' } else { [Environment]::NewLine }
    Set-Content -Encoding UTF8 -LiteralPath $indexTempPath -Value ($indexText + $separator + $indexEntry + [Environment]::NewLine)

    Move-Item -LiteralPath $sourceRoot -Destination $targetRoot
    $moved = $true
    Move-Item -LiteralPath $indexTempPath -Destination $indexPath -Force
} catch {
    if (Test-Path -LiteralPath $indexTempPath) {
        Remove-Item -Force -LiteralPath $indexTempPath
    }
    if ($moved -and (Test-Path -LiteralPath $targetRoot) -and -not (Test-Path -LiteralPath $sourceRoot)) {
        Move-Item -LiteralPath $targetRoot -Destination $sourceRoot
    }
    throw
}

Write-Output "Archived $ChangeName to $targetRoot"
