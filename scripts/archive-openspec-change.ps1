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

$ErrorActionPreference = 'Stop'

$python = Get-Command python3 -ErrorAction SilentlyContinue
if ($null -eq $python) { $python = Get-Command python -ErrorAction SilentlyContinue }
if ($null -eq $python) { throw 'Python 3 is required for archive.' }

$scriptPath = Join-Path $PSScriptRoot 'archive_openspec_change.py'
$pyArgs = @($scriptPath, '--change-name', $ChangeName, '--primary-domain', $PrimaryDomain, '--archive-date', $ArchiveDate)
foreach ($domain in $RelatedDomains) {
    $pyArgs += '--related-domains'
    $pyArgs += $domain
}

& $python.Source @pyArgs
exit $LASTEXITCODE
