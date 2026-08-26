param(
    [ValidateSet("1", "2")]
    [string]$SelectedMode
)

$ErrorActionPreference = "Stop"

$connectorRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$projectRoot = Split-Path -Parent $connectorRoot
$gradle = Join-Path $projectRoot "gradlew.bat"
$installDir = Join-Path $connectorRoot "codex-connector\build\install\codex-connector"
$connector = Join-Path $installDir "bin\codex-connector.bat"
$signalingScript = Join-Path $connectorRoot "signaling\server.mjs"
function Get-FreeTcpPort {
    $listener = [System.Net.Sockets.TcpListener]::new(
        [System.Net.IPAddress]::Loopback,
        0
    )
    try {
        $listener.Start()
        return $listener.LocalEndpoint.Port
    } finally {
        $listener.Stop()
    }
}

$pairingPort = Get-FreeTcpPort
$configuredSignalingPort = $env:CLOUDX_SIGNALING_PORT
$signalingPort = if ($configuredSignalingPort) {
    [int]$configuredSignalingPort
} else {
    Get-FreeTcpPort
}

if (-not (Test-Path -LiteralPath $gradle)) {
    throw "CloudX Gradle wrapper not found: $gradle"
}

if (-not (Test-Path -LiteralPath $connector)) {
    & $gradle -p $connectorRoot :codex-connector:installDist --no-daemon
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
}

function New-QrOutputPath {
    $stamp = Get-Date -Format "yyyyMMdd-HHmmss-fff"
    $suffix = [guid]::NewGuid().ToString("N").Substring(0, 8)
    return Join-Path $projectRoot "cloudx-pairing-$stamp-$suffix.png"
}

function Resolve-Cloudflared {
    $configured = $env:CLOUDX_CLOUDFLARED_PATH
    if ($configured -and (Test-Path -LiteralPath $configured)) {
        return [System.IO.Path]::GetFullPath($configured)
    }
    if ($configured) {
        throw "CLOUDX_CLOUDFLARED_PATH does not point to an existing file: $configured"
    }
    $command = Get-Command cloudflared -ErrorAction SilentlyContinue
    if ($command) { return $command.Source }
    $bundled = Join-Path $projectRoot "tools\cloudflared\cloudflared.exe"
    if (Test-Path -LiteralPath $bundled) { return [System.IO.Path]::GetFullPath($bundled) }
    throw "cloudflared.exe not found. Put it on PATH or set CLOUDX_CLOUDFLARED_PATH."
}

function Wait-HttpHealth {
    param(
        [string]$Url,
        [System.Diagnostics.Process]$Process,
        [int]$TimeoutSeconds = 20
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    do {
        if ($Process.HasExited) {
            throw "Signaling server exited before becoming ready (exit code $($Process.ExitCode))."
        }
        try {
            $response = Invoke-WebRequest -UseBasicParsing -Uri $Url -TimeoutSec 2
            if ($response.StatusCode -eq 200) { return }
        } catch {
            Start-Sleep -Milliseconds 250
        }
    } while ((Get-Date) -lt $deadline)
    throw "Signaling server did not become ready at $Url."
}

function Read-QuickTunnelUrl {
    param(
        [System.Diagnostics.Process]$Process,
        [string]$StdoutPath,
        [string]$StderrPath,
        [int]$TimeoutSeconds = 30
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    $pattern = 'https://[a-z0-9-]+\.trycloudflare\.com'
    do {
        if ($Process.HasExited) {
            $details = @(
                (Get-Content -LiteralPath $StdoutPath -Raw -ErrorAction SilentlyContinue),
                (Get-Content -LiteralPath $StderrPath -Raw -ErrorAction SilentlyContinue)
            ) -join "`n"
            throw "Cloudflare signaling tunnel exited before becoming ready. $details"
        }
        $text = @(
            (Get-Content -LiteralPath $StdoutPath -Raw -ErrorAction SilentlyContinue),
            (Get-Content -LiteralPath $StderrPath -Raw -ErrorAction SilentlyContinue)
        ) -join "`n"
        $match = [regex]::Match($text, $pattern)
        if ($match.Success) { return $match.Value }
        Start-Sleep -Milliseconds 250
    } while ((Get-Date) -lt $deadline)
    throw "Cloudflare signaling tunnel did not publish a trycloudflare.com URL."
}

function Validate-QuickTunnelUrl {
    param([string]$Url)
    $normalized = $Url.Trim().TrimEnd('/')
    if ($normalized -notmatch '^https://[a-z0-9-]+\.trycloudflare\.com$') {
        throw "Cloudflare signaling tunnel returned an invalid public URL: $normalized"
    }
    return $normalized
}

function Get-CloudflareTurnServers {
    param(
        [Parameter(Mandatory = $true)][string]$TurnKeyId,
        [Parameter(Mandatory = $true)][string]$ApiToken,
        [int]$TtlSeconds = 3600
    )

    if ($TtlSeconds -lt 300 -or $TtlSeconds -gt 86400) {
        throw "CLOUDX_CLOUDFLARE_TURN_TTL_SECONDS must be between 300 and 86400."
    }
    $escapedKeyId = [uri]::EscapeDataString($TurnKeyId.Trim())
    $uri = "https://rtc.live.cloudflare.com/v1/turn/keys/$escapedKeyId/credentials/generate-ice-servers"
    $body = @{ ttl = $TtlSeconds } | ConvertTo-Json -Compress
    try {
        $response = Invoke-RestMethod -Method Post -Uri $uri -Headers @{ Authorization = "Bearer $ApiToken" } `
            -ContentType "application/json" -Body $body -TimeoutSec 20
    } catch {
        throw "Cloudflare TURN credentials request failed: $($_.Exception.Message)"
    }

    $entries = foreach ($server in @($response.iceServers)) {
        $urls = @($server.urls) | Where-Object {
            $_ -is [string] -and $_ -match '^turns?:'
        }
        if ($urls.Count -gt 0 -and $server.username -and $server.credential) {
            "{0}|{1}|{2}" -f ($urls -join ','), $server.username, $server.credential
        }
    }
    if (-not $entries) {
        throw "Cloudflare TURN credentials response did not contain an authenticated TURN server."
    }
    return ($entries -join ';')
}

function Stop-ChildProcess {
    param([System.Diagnostics.Process]$Process)
    if ($null -eq $Process) { return }
    if (-not $Process.HasExited) {
        $Process.Kill()
        $Process.WaitForExit(3000)
    }
}

Write-Host "CloudX 远程配对"
Write-Host "请选择连接方式："
Write-Host "  1. Cloudflare Tunnel（业务请求经 HTTPS 公网隧道）"
Write-Host "  2. RTC 直连（业务请求走 WebRTC DataChannel；Cloudflare 仅承载信令）"
$mode = $SelectedMode
if ([string]::IsNullOrWhiteSpace($mode)) {
    do {
        $mode = (Read-Host "输入 1 或 2").Trim()
    } while ($mode -notin @("1", "2"))
}

$qrOutput = New-QrOutputPath

if ($mode -eq "1") {
    $cloudflared = Resolve-Cloudflared
    $previousCloudflaredPath = $env:CLOUDX_CLOUDFLARED_PATH
    try {
        $env:CLOUDX_CLOUDFLARED_PATH = $cloudflared
        & $connector pair-cloudflare $pairingPort $qrOutput
        $exitCode = $LASTEXITCODE
    } finally {
        if ($null -eq $previousCloudflaredPath) {
            Remove-Item Env:CLOUDX_CLOUDFLARED_PATH -ErrorAction SilentlyContinue
        } else {
            $env:CLOUDX_CLOUDFLARED_PATH = $previousCloudflaredPath
        }
    }
    exit $exitCode
}

$nodeCommand = Get-Command node -ErrorAction SilentlyContinue
if (-not $nodeCommand) { throw "node.exe not found. Install Node.js before using RTC pairing." }
$cloudflared = Resolve-Cloudflared
$stdoutFile = [System.IO.Path]::GetTempFileName()
$stderrFile = [System.IO.Path]::GetTempFileName()
$signalingProcess = $null
$tunnelProcess = $null
$previousTurnServers = $env:CLOUDX_WEBRTC_TURN_SERVERS

try {
    $turnKeyId = $env:CLOUDX_CLOUDFLARE_TURN_KEY_ID
    $turnApiToken = $env:CLOUDX_CLOUDFLARE_TURN_API_TOKEN
    if ([string]::IsNullOrWhiteSpace($previousTurnServers)) {
        if ($turnKeyId -and $turnApiToken) {
            $turnTtl = if ($env:CLOUDX_CLOUDFLARE_TURN_TTL_SECONDS) {
                [int]$env:CLOUDX_CLOUDFLARE_TURN_TTL_SECONDS
            } else {
                3600
            }
            $env:CLOUDX_WEBRTC_TURN_SERVERS = Get-CloudflareTurnServers `
                -TurnKeyId $turnKeyId `
                -ApiToken $turnApiToken `
                -TtlSeconds $turnTtl
            Write-Host "Cloudflare TURN 短期凭证已生成（TTL ${turnTtl}s）"
        } elseif ($turnKeyId -or $turnApiToken) {
            throw "CLOUDX_CLOUDFLARE_TURN_KEY_ID and CLOUDX_CLOUDFLARE_TURN_API_TOKEN must be set together."
        } else {
            Write-Warning "未配置 TURN。RTC 仅在双方 NAT 允许直连时可用；跨运营商/受限网络请配置 Cloudflare TURN。"
        }
    }
    $previousSignalingPort = $env:CLOUDX_SIGNALING_PORT
    $env:CLOUDX_SIGNALING_PORT = "$signalingPort"
    $signalingProcess = Start-Process -FilePath $nodeCommand.Source `
        -ArgumentList @($signalingScript) `
        -WorkingDirectory $connectorRoot `
        -PassThru `
        -WindowStyle Hidden
    Wait-HttpHealth -Url "http://127.0.0.1:$signalingPort/health" -Process $signalingProcess

    $tunnelProcess = Start-Process -FilePath $cloudflared `
        -ArgumentList @("tunnel", "--no-autoupdate", "--url", "http://127.0.0.1:$signalingPort") `
        -RedirectStandardOutput $stdoutFile `
        -RedirectStandardError $stderrFile `
        -PassThru `
        -WindowStyle Hidden
    $signalingEndpoint = Validate-QuickTunnelUrl (Read-QuickTunnelUrl `
        -Process $tunnelProcess `
        -StdoutPath $stdoutFile `
        -StderrPath $stderrFile)

    Write-Host "RTC 公网信令入口：$signalingEndpoint"
    Wait-HttpHealth -Url "$signalingEndpoint/health" -Process $tunnelProcess -TimeoutSeconds 30
    & $connector pair-webrtc $pairingPort $qrOutput $signalingEndpoint
    exit $LASTEXITCODE
} finally {
    if ($null -eq $previousSignalingPort) {
        Remove-Item Env:CLOUDX_SIGNALING_PORT -ErrorAction SilentlyContinue
    } else {
        $env:CLOUDX_SIGNALING_PORT = $previousSignalingPort
    }
    if ([string]::IsNullOrWhiteSpace($previousTurnServers)) {
        Remove-Item Env:CLOUDX_WEBRTC_TURN_SERVERS -ErrorAction SilentlyContinue
    } else {
        $env:CLOUDX_WEBRTC_TURN_SERVERS = $previousTurnServers
    }
    Stop-ChildProcess $tunnelProcess
    Stop-ChildProcess $signalingProcess
    Remove-Item -LiteralPath $stdoutFile -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $stderrFile -Force -ErrorAction SilentlyContinue
}
