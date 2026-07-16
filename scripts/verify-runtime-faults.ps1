param(
    [Parameter(Mandatory = $true)]
    [ValidateNotNullOrEmpty()]
    [string]$FixtureToken,
    [string]$ApiBaseUrl = "http://localhost:8080",
    [ValidateRange(5, 120)]
    [int]$TimeoutSeconds = 30,
    [ValidatePattern('^[A-Za-z0-9][A-Za-z0-9_-]{0,62}$')]
    [string]$ComposeProject = "suilearn-fixture",
    [ValidateNotNullOrEmpty()]
    [string]$ComposeFile = "compose.yml",
    [ValidateNotNullOrEmpty()]
    [string]$ComposeOverlayFile = "compose.runtime-fixture.yml"
)

$ErrorActionPreference = 'Stop'
$base = $ApiBaseUrl.TrimEnd('/')
$headers = @{ 'X-SuiLearn-Runtime-Fixture-Token' = $FixtureToken }
$composeArguments = @('-p', $ComposeProject, '-f', $ComposeFile, '-f', $ComposeOverlayFile)
$pausedServices = [System.Collections.Generic.List[string]]::new()
$hadProcessFixtureToken = Test-Path Env:SUILEARN_RUNTIME_FIXTURE_TOKEN
$previousProcessFixtureToken = $env:SUILEARN_RUNTIME_FIXTURE_TOKEN
$env:SUILEARN_RUNTIME_FIXTURE_TOKEN = $FixtureToken

function Invoke-FixtureCompose([string[]]$Arguments) {
    & docker compose @composeArguments @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Compose operation '$($Arguments -join ' ') ' failed for the isolated runtime-fixture project."
    }
}

function Invoke-FixtureControl([string]$Method, [string]$Path) {
    $response = Invoke-WebRequest -UseBasicParsing -Method $Method -Uri "$base$Path" -Headers $headers -TimeoutSec $TimeoutSeconds
    if ($response.StatusCode -ne 200) {
        throw 'Runtime fixture control did not accept the requested fault mode.'
    }
}

function Invoke-FixtureProbe([ValidateSet('ocr', 'ai')][string]$Kind) {
    $response = Invoke-RestMethod -Method POST -Uri "$base/internal/runtime-fixture/probes/$Kind" -Headers $headers -TimeoutSec $TimeoutSeconds
    foreach ($property in @('workTimedOut', 'taskRetryPersisted', 'deadLetterRecorded', 'originalOutboxDispatchPrevented', 'exclusiveReplayOutboxPersisted')) {
        if (-not $response.$property) {
            throw "Runtime fixture $Kind probe did not persist the expected $property outcome."
        }
    }
}

function Invoke-FixtureOpaqueBooleanProbe(
    [ValidateSet('duplicate-message', 'deletion-cleanup')][string]$Kind,
    [string[]]$ExpectedProperties
) {
    $response = Invoke-RestMethod -Method POST -Uri "$base/internal/runtime-fixture/probes/$Kind" -Headers $headers -TimeoutSec $TimeoutSeconds
    foreach ($property in $ExpectedProperties) {
        if (-not $response.$property) {
            throw "Runtime fixture $Kind probe did not report the expected $property outcome."
        }
    }
}

function Convert-ResponseContentToText([object]$Content) {
    if ($null -eq $Content) { return '' }
    if ($Content -is [byte[]]) { return [System.Text.Encoding]::UTF8.GetString($Content) }
    if ($Content.PSObject.Methods.Name -contains 'ReadAsStringAsync') { return $Content.ReadAsStringAsync().GetAwaiter().GetResult() }
    return [string]$Content
}

function Get-HealthBody([string]$Path) {
    try {
        $response = Invoke-WebRequest -UseBasicParsing -Method GET -Uri "$base$Path" -TimeoutSec ([Math]::Min($TimeoutSeconds, 5))
        return Convert-ResponseContentToText $response.Content
    } catch {
        $response = $_.Exception.Response
        if ($null -eq $response) { throw }
        if ($null -ne $response.Content) {
            return Convert-ResponseContentToText $response.Content
        }
        $reader = [System.IO.StreamReader]::new($response.GetResponseStream())
        try { return $reader.ReadToEnd() } finally { $reader.Dispose() }
    }
}

function Test-ProcessingDependencyTimeout([System.Exception]$Failure) {
    $exception = $Failure
    while ($null -ne $exception) {
        if ($exception -is [System.TimeoutException]) { return $true }
        if ($exception -is [System.Net.WebException] -and $exception.Status -eq [System.Net.WebExceptionStatus]::Timeout) { return $true }
        if ($exception.Message -match '(?i)timed out|timeout') { return $true }
        $exception = $exception.InnerException
    }
    return $false
}

function Get-HealthStatus([string]$Group) {
    try {
        $body = Get-HealthBody "/actuator/health/$Group"
        $status = ($body | ConvertFrom-Json).status
        if ([string]::IsNullOrWhiteSpace($status)) {
            throw "Health group '$Group' returned no status."
        }
        return $status
    } catch {
        if ($Group -eq 'processing' -and (Test-ProcessingDependencyTimeout $_.Exception)) {
            return 'DOWN'
        }
        throw
    }
}

function Wait-Until([string]$Description, [scriptblock]$Condition) {
    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        try {
            if (& $Condition) { return }
        } catch {
            # A dependency pause can legitimately interrupt a probe while the service transitions.
        }
        Start-Sleep -Seconds 1
    } while ((Get-Date) -lt $deadline)
    throw "Timed out waiting for $Description."
}

function Assert-AiTimeoutMetric {
    $metrics = (Invoke-WebRequest -UseBasicParsing -Method GET -Uri "$base/actuator/prometheus" -TimeoutSec $TimeoutSeconds).Content
    $pattern = '(?s)suilearn_ai_requests_total\{(?=[^}]*operation="chat")(?=[^}]*outcome="timeout")[^}]*\}\s+[1-9][0-9]*(?:\.[0-9]+)?'
    if ($metrics -notmatch $pattern) {
        throw 'Runtime fixture AI timeout metric was not observed.'
    }
}

function Assert-AiCircuitOpenMetric {
    $metrics = (Invoke-WebRequest -UseBasicParsing -Method GET -Uri "$base/actuator/prometheus" -TimeoutSec $TimeoutSeconds).Content
    $pattern = '(?s)suilearn_ai_requests_total\{(?=[^}]*operation="chat")(?=[^}]*outcome="circuit_open")[^}]*\}\s+[1-9][0-9]*(?:\.[0-9]+)?'
    if ($metrics -notmatch $pattern) {
        throw 'Runtime fixture AI circuit-open metric was not observed after two timeouts.'
    }
}

function Assert-OcrTimeoutMetric {
    $metrics = (Invoke-WebRequest -UseBasicParsing -Method GET -Uri "$base/actuator/prometheus" -TimeoutSec $TimeoutSeconds).Content
    $pattern = '(?s)suilearn_ocr_pages_total\{(?=[^}]*outcome="timed_out")[^}]*\}\s+[1-9][0-9]*(?:\.[0-9]+)?'
    if ($metrics -notmatch $pattern) {
        throw 'Runtime fixture OCR timeout metric was not observed.'
    }
}

function Pause-FixtureService([ValidateSet('rabbitmq', 'minio')][string]$Service) {
    Invoke-FixtureCompose @('pause', $Service)
    $pausedServices.Add($Service)
}

function Resume-FixtureService([ValidateSet('rabbitmq', 'minio')][string]$Service) {
    Invoke-FixtureCompose @('unpause', $Service)
    [void]$pausedServices.Remove($Service)
}

try {
    Wait-Until 'HTTP readiness after Compose startup' { (Get-HealthStatus 'readiness') -eq 'UP' }
    Wait-Until 'processing health after Compose startup' { (Get-HealthStatus 'processing') -eq 'UP' }
    Invoke-FixtureControl 'PUT' '/internal/runtime-fixture/reset'

    Invoke-FixtureCompose @('restart', 'api')
    Wait-Until 'HTTP readiness after API restart' { (Get-HealthStatus 'readiness') -eq 'UP' }
    Wait-Until 'processing health after API restart' { (Get-HealthStatus 'processing') -eq 'UP' }

    Pause-FixtureService 'rabbitmq'
    Wait-Until 'HTTP readiness to remain UP while RabbitMQ is paused' { (Get-HealthStatus 'readiness') -eq 'UP' }
    Wait-Until 'processing health to report RabbitMQ unavailable' { (Get-HealthStatus 'processing') -eq 'DOWN' }
    Resume-FixtureService 'rabbitmq'
    Wait-Until 'processing health to recover after RabbitMQ resume' { (Get-HealthStatus 'processing') -eq 'UP' }

    Pause-FixtureService 'minio'
    Wait-Until 'HTTP readiness to remain UP while MinIO is paused' { (Get-HealthStatus 'readiness') -eq 'UP' }
    Wait-Until 'processing health to report MinIO unavailable' { (Get-HealthStatus 'processing') -eq 'DOWN' }
    Resume-FixtureService 'minio'
    Wait-Until 'processing health to recover after MinIO resume' { (Get-HealthStatus 'processing') -eq 'UP' }

    Invoke-FixtureControl 'PUT' '/internal/runtime-fixture/ocr-mode?mode=TIMEOUT'
    Invoke-FixtureProbe 'ocr'
    Assert-OcrTimeoutMetric
    Invoke-FixtureControl 'PUT' '/internal/runtime-fixture/ai-mode?mode=TIMEOUT'
    Invoke-FixtureProbe 'ai'
    Invoke-FixtureProbe 'ai'
    Invoke-FixtureProbe 'ai'
    Assert-AiTimeoutMetric
    Assert-AiCircuitOpenMetric
    Invoke-FixtureOpaqueBooleanProbe 'duplicate-message' @('firstDeliveryClaimed', 'duplicateDeliveryRejected')
    Invoke-FixtureOpaqueBooleanProbe 'deletion-cleanup' @('assetRecordDeleted', 'objectCleanupConfirmed')
} finally {
    foreach ($service in $pausedServices.ToArray()) {
        try { Resume-FixtureService $service } catch { Write-Warning "Could not resume $service automatically." }
    }
    try { Invoke-FixtureControl 'PUT' '/internal/runtime-fixture/reset' } catch { Write-Warning 'Could not reset runtime fixture modes automatically.' }
    if ($hadProcessFixtureToken) {
        $env:SUILEARN_RUNTIME_FIXTURE_TOKEN = $previousProcessFixtureToken
    } else {
        Remove-Item Env:SUILEARN_RUNTIME_FIXTURE_TOKEN -ErrorAction SilentlyContinue
    }
}
