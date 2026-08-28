$ErrorActionPreference = "Stop"

$env:SUILEARN_AGENT_PROG = "agent"
$script = Join-Path $PSScriptRoot "scripts\agent_cli.py"

if ($env:SUILEARN_AGENT_PYTHON) {
    & $env:SUILEARN_AGENT_PYTHON $script @args
    exit $LASTEXITCODE
}

$python = Get-Command python -ErrorAction SilentlyContinue
if ($python) {
    & $python.Source $script @args
    exit $LASTEXITCODE
}

$py = Get-Command py -ErrorAction SilentlyContinue
if ($py) {
    & py -3 $script @args
    exit $LASTEXITCODE
}

Write-Error "[agent] Python 3 was not found. Install Python and add python or py to PATH, or set SUILEARN_AGENT_PYTHON to the python.exe path."
exit 1
