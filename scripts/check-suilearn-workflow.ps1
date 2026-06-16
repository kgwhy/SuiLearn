param(
    [string]$BaseRef = "",
    [string]$ClosingChange = ""
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

function Add-ClosingChangeIssue {
    param([string]$Message)
    $script:changed += "Closing change check failed: $Message"
}

function Test-ClosingChange {
    param([string]$ChangeName)

    if ([string]::IsNullOrWhiteSpace($ChangeName)) {
        return
    }

    $changeRoot = Join-Path "openspec/changes" $ChangeName
    if (-not (Test-Path $changeRoot)) {
        Add-ClosingChangeIssue "openspec/changes/$ChangeName does not exist."
        return
    }

    $tasks = Join-Path $changeRoot "tasks.md"
    $verification = Join-Path $changeRoot "verification.md"
    $archive = Join-Path $changeRoot "archive.md"

    foreach ($required in @($tasks, $verification, $archive)) {
        if (-not (Test-Path $required)) {
            Add-ClosingChangeIssue "$required is required before closing a change."
        }
    }

    if (Test-Path $verification) {
        $verificationText = Get-Content -Raw -Encoding UTF8 $verification
        if ($verificationText -notmatch "(?m)^Status:\s*passed\.?\s*$" -and $verificationText -notmatch "(?m)^\u72b6\u6001\uff1a\s*\u5df2\u901a\u8fc7\u3002?\s*$") {
            Add-ClosingChangeIssue "verification.md must contain 'Status: passed.' or the Chinese equivalent before completion."
        }
        if ($verificationText -match "(?i)\bIn progress\b|Status:\s*open") {
            Add-ClosingChangeIssue "verification.md still contains an open or in-progress closeout state."
        }
    }

    if (Test-Path $archive) {
        $archiveText = Get-Content -Raw -Encoding UTF8 $archive
        if ($archiveText -match "(?i)Status:\s*open") {
            Add-ClosingChangeIssue "archive.md still contains 'Status: open'."
        }
        if ($archiveText -notmatch "(?im)^Deferred items:\s*(none|.+)" -and $archiveText -notmatch "(?m)^\u5ef6\u671f\u9879\uff1a\s*(\u65e0|.+)") {
            Add-ClosingChangeIssue "archive.md must record deferred items, even when the value is 'none' or the Chinese equivalent."
        }
        if ($archiveText -notmatch "(?i)review" -and $archiveText -notmatch "\u5ba1\u67e5") {
            Add-ClosingChangeIssue "archive.md must include a final review summary or review disposition."
        }
    }

    if (Test-Path $tasks) {
        $tasksText = Get-Content -Raw -Encoding UTF8 $tasks
        if ($tasksText -match "(?i)Status:\s*(open|in progress|pending)") {
            Add-ClosingChangeIssue "tasks.md still contains an open, in-progress, or pending task status."
        }
    }
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

Test-ClosingChange $ClosingChange

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
