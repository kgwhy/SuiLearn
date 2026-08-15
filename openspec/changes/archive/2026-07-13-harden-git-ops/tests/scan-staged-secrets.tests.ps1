$ErrorActionPreference = 'Stop'

$scanner = Join-Path $PSScriptRoot '..\..\..\..\.agents\skills\git-ops\scripts\scan-staged-secrets.ps1'

function Assert-Equal {
    param(
        [Parameter(Mandatory)] $Actual,
        [Parameter(Mandatory)] $Expected,
        [Parameter(Mandatory)] [string] $Message
    )

    if ($Actual -ne $Expected) {
        throw "$Message. Expected: $Expected; actual: $Actual"
    }
}

function New-TestRepository {
    $path = Join-Path ([System.IO.Path]::GetTempPath()) ("git-ops-preflight-" + [guid]::NewGuid())
    New-Item -ItemType Directory -Path $path | Out-Null
    & git -C $path init -q
    & git -C $path config user.email 'test@example.invalid'
    & git -C $path config user.name 'Git Ops Test'
    return $path
}

function Invoke-Scanner {
    param([Parameter(Mandatory)] [string] $Repository)

    $output = & powershell -NoProfile -ExecutionPolicy Bypass -File $scanner -Repository $Repository 2>&1 | Out-String
    return @{ ExitCode = $LASTEXITCODE; Output = $output }
}

if (-not (Test-Path -LiteralPath $scanner)) {
    throw "Scanner script is missing: $scanner"
}

$repositories = @()
try {
    $cleanRepository = New-TestRepository
    $repositories += $cleanRepository
    Set-Content -LiteralPath (Join-Path $cleanRepository 'README.md') -Value 'ordinary staged content' -NoNewline
    & git -C $cleanRepository add -- README.md
    $clean = Invoke-Scanner -Repository $cleanRepository
    Assert-Equal -Actual $clean.ExitCode -Expected 0 -Message 'Ordinary staged content must pass preflight'

    $secretRepository = New-TestRepository
    $repositories += $secretRepository
    $fixtureToken = 'ghp_0123456789abcdefghijklmnopqrstuvwxyz'
    Set-Content -LiteralPath (Join-Path $secretRepository 'config.txt') -Value "ACCESS_TOKEN=$fixtureToken`nnote=baseline"
    & git -C $secretRepository add -- config.txt
    & git -C $secretRepository commit -qm 'test: create staged snapshot'
    Set-Content -LiteralPath (Join-Path $secretRepository 'config.txt') -Value "ACCESS_TOKEN=$fixtureToken`nnote=only this line changed"
    & git -C $secretRepository add -- config.txt
    $secret = Invoke-Scanner -Repository $secretRepository
    Assert-Equal -Actual $secret.ExitCode -Expected 1 -Message 'An untouched token line must block the commit'
    if ($secret.Output.Contains($fixtureToken)) {
        throw 'Scanner output leaked the fixture token'
    }

    $binaryRepository = New-TestRepository
    $repositories += $binaryRepository
    [System.IO.File]::WriteAllBytes((Join-Path $binaryRepository 'fixture.bin'), [byte[]](0, 1, 2, 3, 255))
    & git -C $binaryRepository add -- fixture.bin
    $binary = Invoke-Scanner -Repository $binaryRepository
    Assert-Equal -Actual $binary.ExitCode -Expected 1 -Message 'A binary staged file must safely block the commit'
    if ($binary.Output -match '00 01 02|fixtureToken') {
        throw 'Scanner output leaked binary content'
    }
} finally {
    foreach ($repository in $repositories) {
        if (Test-Path -LiteralPath $repository) {
            Remove-Item -LiteralPath $repository -Recurse -Force
        }
    }
}

exit 0
