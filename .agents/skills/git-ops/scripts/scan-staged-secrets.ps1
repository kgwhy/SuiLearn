[CmdletBinding()]
param(
    [Parameter()]
    [string] $Repository = (Get-Location).Path
)

$ErrorActionPreference = 'Stop'

function Invoke-GitBytes {
    param([Parameter(Mandatory)] [string] $Arguments)

    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = 'git'
    $startInfo.Arguments = $Arguments
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    [void] $process.Start()
    $buffer = New-Object System.IO.MemoryStream
    $process.StandardOutput.BaseStream.CopyTo($buffer)
    [void] $process.StandardError.ReadToEnd()
    $process.WaitForExit()

    if ($process.ExitCode -ne 0) {
        throw 'Git could not inspect the staged snapshot.'
    }

    return $buffer.ToArray()
}

function ConvertFrom-NullDelimitedUtf8 {
    param([Parameter(Mandatory)] [byte[]] $Bytes)

    $items = New-Object System.Collections.Generic.List[string]
    $start = 0
    for ($index = 0; $index -lt $Bytes.Length; $index++) {
        if ($Bytes[$index] -eq 0) {
            if ($index -gt $start) {
                $items.Add([System.Text.Encoding]::UTF8.GetString($Bytes, $start, $index - $start))
            }
            $start = $index + 1
        }
    }

    return $items
}

function Test-BinaryOrInvalidUtf8 {
    param([Parameter(Mandatory)] [byte[]] $Bytes)

    if ($Bytes -contains 0) {
        return $true
    }

    try {
        $strictUtf8 = New-Object System.Text.UTF8Encoding($false, $true)
        [void] $strictUtf8.GetString($Bytes)
        return $false
    }
    catch [System.Text.DecoderFallbackException] {
        return $true
    }
}

function Quote-GitArgument {
    param([Parameter(Mandatory)] [string] $Value)

    return '"' + $Value.Replace('"', '\"') + '"'
}

function Get-Utf8Text {
    param([Parameter(Mandatory)] [string] $Base64)

    return [System.Text.Encoding]::UTF8.GetString([System.Convert]::FromBase64String($Base64))
}

try {
    $repositoryPath = (Resolve-Path -LiteralPath $Repository).Path
    $quotedRepository = Quote-GitArgument -Value $repositoryPath
    $pathBytes = Invoke-GitBytes -Arguments "-C $quotedRepository diff --cached --name-only -z --diff-filter=ACMR"
    $paths = ConvertFrom-NullDelimitedUtf8 -Bytes $pathBytes

    if ($paths.Count -eq 0) {
        Write-Output (Get-Utf8Text '6aKE5qOA6YCa6L+H77ya5rKh5pyJ6ZyA6KaB5qOA5p+l55qE5pqC5a2Y5paH5Lu244CC')
        exit 0
    }

    $allowlistedBinaryExtensions = @('.jar', '.png', '.jpg', '.jpeg', '.gif', '.webp', '.ico', '.pdf', '.ttf', '.otf', '.woff', '.woff2', '.zip', '.gz', '.mp3', '.mp4')

    $rules = @(
        @{ Name = 'github-token'; Pattern = '\bgh[pousr]_[A-Za-z0-9]{36,255}\b' },
        @{ Name = 'github-fine-grained-token'; Pattern = '\bgithub_pat_[A-Za-z0-9_]{22,255}\b' },
        @{ Name = 'openai-api-key'; Pattern = '\bsk-[A-Za-z0-9_-]{20,}\b' },
        @{ Name = 'jwt-token'; Pattern = '\beyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}\b' },
        @{ Name = 'aws-access-key-id'; Pattern = '\bAKIA[0-9A-Z]{16}\b' },
        @{ Name = 'slack-token'; Pattern = '\bxox[baprs]-[A-Za-z0-9-]{10,}\b' },
        @{ Name = 'private-key-header'; Pattern = '-----BEGIN (?:RSA |EC |OPENSSH |DSA )?PRIVATE KEY-----' },
        @{ Name = 'credential-assignment'; Pattern = '(?im)^\s*(?:[A-Z][A-Z0-9_]*(?:KEY|SECRET|TOKEN|PASSWORD)|(?:api[_-]?key|access[_-]?token|client[_-]?secret))\s*[:=]\s*["'']?[A-Za-z0-9_./+=-]{16,}' }
    )

    $failures = 0
    foreach ($path in $paths) {
        $quotedSpec = Quote-GitArgument -Value (':' + $path)
        $blob = Invoke-GitBytes -Arguments "-C $quotedRepository cat-file blob $quotedSpec"

        $extension = [System.IO.Path]::GetExtension($path).ToLowerInvariant()
        if ($allowlistedBinaryExtensions -contains $extension) {
            continue
        }
        if (Test-BinaryOrInvalidUtf8 -Bytes $blob) {
            Write-Output ("{0} 'binary-or-invalid-utf8' {1} '$path'." -f (Get-Utf8Text '5o+Q5Lqk5YmN6aKE5qOA5aSx6LSl77ya6KeE5YiZ'), (Get-Utf8Text '5ZG95Lit5pqC5a2Y5paH5Lu2'))
            $failures++
            continue
        }

        $content = [System.Text.Encoding]::UTF8.GetString($blob)
        foreach ($rule in $rules) {
            if ([System.Text.RegularExpressions.Regex]::IsMatch($content, $rule.Pattern)) {
                Write-Output ("{0} '$($rule.Name)' {1} '$path'." -f (Get-Utf8Text '5o+Q5Lqk5YmN6aKE5qOA5aSx6LSl77ya6KeE5YiZ'), (Get-Utf8Text '5ZG95Lit5pqC5a2Y5paH5Lu2'))
                $failures++
            }
        }
    }

    if ($failures -gt 0) {
        exit 1
    }

    Write-Output (Get-Utf8Text '5o+Q5Lqk5YmN6aKE5qOA6YCa6L+H44CC')
    exit 0
}
catch {
    Write-Error (Get-Utf8Text '5o+Q5Lqk5YmN6aKE5qOA5peg5rOV5a6J5YWo5a6M5oiQ44CC')
    exit 1
}
