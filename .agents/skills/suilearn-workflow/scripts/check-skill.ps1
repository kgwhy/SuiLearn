param(
    [string]$SkillRoot = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
)

$ErrorActionPreference = 'Stop'

function Fail($Message) {
    Write-Error $Message
    exit 1
}

$skillMd = Join-Path $SkillRoot 'SKILL.md'
if (!(Test-Path $skillMd)) {
    Fail "Missing SKILL.md"
}

$text = Get-Content -Raw -Encoding UTF8 $skillMd
if ($text -notmatch "(?s)^---\s*\r?\nname:\s*suilearn-workflow\r?\ndescription:\s*.+?\r?\n---") {
    Fail "Invalid SKILL.md frontmatter. Required fields: name and description."
}

$requiredRefs = @(
    'state-machine.md',
    'usage-examples.md',
    'forward-testing.md',
    'change-levels.md',
    'policy-gates.md',
    'subagent-loop.md',
    'verification.md'
)

foreach ($ref in $requiredRefs) {
    $path = Join-Path $SkillRoot "references\$ref"
    if (!(Test-Path $path)) {
        Fail "Missing reference: $ref"
    }
    if ($text -notmatch [regex]::Escape("references/$ref")) {
        Fail "SKILL.md does not link reference: $ref"
    }
}

$scriptDir = Join-Path $SkillRoot 'scripts'
if (!(Test-Path $scriptDir)) {
    Fail "Missing scripts directory"
}

$englishHeadings = @(
    'Use this',
    'Read these',
    'Before Completion',
    'Progressive Loading',
    'State Machine',
    'Policy Gates',
    'Verification And Closure',
    'Required Evidence',
    'Return Format',
    'Non-Negotiables',
    'Route First',
    'Load By Need',
    'Routing Rules'
)

$mdFiles = Get-ChildItem -Path $SkillRoot -Recurse -Filter *.md
foreach ($file in $mdFiles) {
    $content = Get-Content -Raw -Encoding UTF8 $file.FullName
    foreach ($pattern in $englishHeadings) {
        if ($content.Contains($pattern)) {
            Fail "English workflow phrase remains in $($file.FullName): $pattern"
        }
    }
}

Write-Output "SuiLearn workflow skill check passed."
