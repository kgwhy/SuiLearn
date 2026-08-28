@echo off
setlocal EnableExtensions

set "SUILEARN_AGENT_PROG=agent"
set "CLI=%~dp0scripts\agent_cli.py"

if defined SUILEARN_AGENT_PYTHON (
    "%SUILEARN_AGENT_PYTHON%" "%CLI%" %*
    exit /b %errorlevel%
)

where python >nul 2>nul
if not errorlevel 1 (
    python "%CLI%" %*
    exit /b %errorlevel%
)

where py >nul 2>nul
if not errorlevel 1 (
    py -3 "%CLI%" %*
    exit /b %errorlevel%
)

echo [agent] Python 3 was not found. Install Python and add python or py to PATH, 1>&2
echo [agent] or set SUILEARN_AGENT_PYTHON to the python.exe path. 1>&2
exit /b 1
