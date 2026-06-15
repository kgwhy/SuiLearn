param(
    [string]$BaseRef = ""
)

$ErrorActionPreference = "Stop"

$changed = @()

if ([string]::IsNullOrWhiteSpace($BaseRef)) {
    $BaseRef = ""
    try {
        $BaseRef = (git merge-base HEAD origin/main 2>$null).Trim()
    } catch {
        $BaseRef = ""
    }
    if ([string]::IsNullOrWhiteSpace($BaseRef)) {
        $BaseRef = (git rev-parse HEAD).Trim()
    }
    Write-Output "BaseRef not provided; using $BaseRef"
}

$diffEntries = git diff --name-status $BaseRef

function Test-ProtectedPath {
    param([string]$Path)

    return (
        $Path -like "apps/*" -or
        $Path -like "services/*" -or
        $Path -like "contracts/*" -or
        $Path -eq "docs/product-requirements.md" -or
        $Path -eq "docs/architecture.md" -or
        $Path -like "docs/architecture*.md" -or
        $Path -eq "docs/tech-selection.md"
    )
}

function Test-ActiveChangeExists {
    $changeDirs = Get-ChildItem -Path "openspec/changes" -Directory -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -ne "archive" }

    foreach ($dir in $changeDirs) {
        $tasks = Join-Path $dir.FullName "tasks.md"
        $policy = Join-Path $dir.FullName "policy.md"
        if ((Test-Path $tasks) -and (Test-Path $policy)) {
            return $true
        }
    }
    return $false
}

$protectedChanged = @()

foreach ($entry in $diffEntries) {
    if ([string]::IsNullOrWhiteSpace($entry)) {
        continue
    }
    $parts = $entry -split "\s+", 2
    if ($parts.Count -lt 2) {
        continue
    }
    $status = $parts[0]
    $path = $parts[1].Trim()
    if ($path -like "docs/proposals/*" -and $path -ne "docs/proposals/README.md" -and $path -ne "docs/proposals/_template.md") {
        $changed += "Retired proposal path changed in diff ($status): $path"
    }
    if ($path -like "docs/superpowers/specs/*") {
        $changed += "Retired Superpowers spec path changed in diff ($status): $path"
    }
    if ($path -like "docs/superpowers/plans/*") {
        $changed += "Retired Superpowers plan path changed in diff ($status): $path"
    }
    if (Test-ProtectedPath $path) {
        $protectedChanged += $path
    }
}

$worktreeChanged = git status --porcelain
foreach ($line in $worktreeChanged) {
    if ($line.Length -lt 4) {
        continue
    }
    $path = $line.Substring(3).Trim()
    $status = $line.Substring(0, 2)
    if ($path -like "docs/proposals/*" -and $path -ne "docs/proposals/README.md" -and $path -ne "docs/proposals/_template.md" -and $status -match "A|\?") {
        $changed += "New or modified retired proposal path: $path"
    }
    if ($path -like "docs/superpowers/specs/*" -and $status -match "A|\?") {
        $changed += "New or modified retired Superpowers spec path: $path"
    }
    if ($path -like "docs/superpowers/plans/*" -and $status -match "A|\?") {
        $changed += "New or modified retired Superpowers plan path: $path"
    }
    if (Test-ProtectedPath $path) {
        $protectedChanged += $path
    }
}

if ($protectedChanged.Count -gt 0 -and -not (Test-ActiveChangeExists)) {
    $changed += "Protected implementation or fact-document paths changed without an active openspec/changes/<name>/tasks.md and policy.md."
    foreach ($path in ($protectedChanged | Sort-Object -Unique)) {
        $changed += "Protected changed path: $path"
    }
}

if ($changed.Count -gt 0) {
    Write-Output "SuiLearn Workflow policy check failed:"
    foreach ($item in $changed) {
        Write-Output "- $item"
    }
    exit 1
} else {
    if ($protectedChanged.Count -gt 0) {
        Write-Output "Protected paths changed; active OpenSpec change found."
    }
}

Write-Output "SuiLearn Workflow policy check passed."
