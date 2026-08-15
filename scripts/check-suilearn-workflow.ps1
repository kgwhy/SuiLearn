param(
    [string]$BaseRef = "",
    [string]$ClosingChange = "",
    [switch]$SelfTestEfficientBatchPolicy
)

$ErrorActionPreference = "Stop"

$python = Get-Command python3 -ErrorAction SilentlyContinue
if ($null -eq $python) {
    $python = Get-Command python -ErrorAction SilentlyContinue
}
if ($null -eq $python) {
    Write-Error "Python 3 is required for workflow policy check."
    exit 1
}

$checker = Join-Path $PSScriptRoot "check_suilearn_workflow.py"
$args = @($checker)
if (-not [string]::IsNullOrWhiteSpace($BaseRef)) {
    $args += "--base-ref"
    $args += $BaseRef
}
if (-not [string]::IsNullOrWhiteSpace($ClosingChange)) {
    $args += "--closing-change"
    $args += $ClosingChange
}
if ($SelfTestEfficientBatchPolicy) {
    $args += "--self-test-efficient-batch-policy"
}

& $python.Source @args
exit $LASTEXITCODE
