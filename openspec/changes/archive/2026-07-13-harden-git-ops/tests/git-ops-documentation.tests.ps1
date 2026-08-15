$ErrorActionPreference = 'Stop'

$skillRoot = Join-Path $PSScriptRoot '..\..\..\..\.agents\skills\git-ops'
$skill = Get-Content -Raw -Encoding UTF8 (Join-Path $skillRoot 'SKILL.md')
$metadata = Get-Content -Raw -Encoding UTF8 (Join-Path $skillRoot 'agents\openai.yaml')
$commitPreflight = Join-Path $skillRoot 'references\commit-preflight.md'
$commitMessages = Join-Path $skillRoot 'references\commit-messages.md'
$publishing = Join-Path $skillRoot 'references\branches-and-publishing.md'
$conventional = Join-Path $skillRoot 'references\conventional-commits.md'

function Assert-True {
    param([Parameter(Mandatory)] [bool] $Condition, [Parameter(Mandatory)] [string] $Message)
    if (-not $Condition) { throw $Message }
}

foreach ($path in @($commitPreflight, $commitMessages, $publishing, $conventional)) {
    Assert-True -Condition (Test-Path -LiteralPath $path) -Message "Missing progressive-loading reference: $path"
}

foreach ($reference in @('commit-preflight.md', 'commit-messages.md', 'branches-and-publishing.md', 'conventional-commits.md')) {
    Assert-True -Condition $skill.Contains($reference) -Message "SKILL.md does not link directly to $reference"
}

Assert-True -Condition $skill.Contains('access token') -Message 'SKILL.md does not explicitly require an access token scan'
Assert-True -Condition $skill.Contains('scan-staged-secrets.ps1') -Message 'SKILL.md does not provide the pre-commit scan command'

foreach ($heading in @('Core Rules', 'Workflow', 'Commit Message Standard', 'Handoff', 'Required Shape', 'Semantic Meaning', 'Breaking Changes', 'Footer Rules', 'Good Examples', 'Message Quality')) {
    Assert-True -Condition (-not $skill.Contains($heading)) -Message "English heading remains in SKILL.md: $heading"
    Assert-True -Condition (-not (Get-Content -Raw -Encoding UTF8 $conventional).Contains($heading)) -Message "English heading remains in conventional-commits.md: $heading"
}

$template = Get-Content -Raw -Encoding UTF8 $commitMessages
$verificationHeader = [string][char]0x9A8C + [char]0x8BC1 + ':'
$riskHeader = [string][char]0x98CE + [char]0x9669 + [char]0x4E0E + [char]0x5907 + [char]0x6CE8 + ':'
Assert-True -Condition (-not $template.Contains($verificationHeader)) -Message 'Commit template still contains a verification section'
Assert-True -Condition (-not $template.Contains($riskHeader)) -Message 'Commit template still contains a risk section'
$chineseOperation = [string][char]0x64CD + [char]0x4F5C
Assert-True -Condition $metadata.Contains($chineseOperation) -Message 'Skill UI metadata is not Chinese'
