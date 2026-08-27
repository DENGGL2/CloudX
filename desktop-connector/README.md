# CloudX Desktop Connector

This is the Windows-side pairing service for the `CloudX` Android app.
It is intentionally kept under this repository as a separate project folder.

Run `pair.bat` when you need a QR code. It starts without a console window and
uses Cloudflare Tunnel by default. Pass `2` to use WebRTC Direct:

```powershell
.\desktop-connector\pair.bat       # Cloudflare Tunnel
.\desktop-connector\pair.bat 2     # WebRTC Direct
```

The available transports are:

1. Cloudflare Tunnel: creates a public Quick Tunnel and QR code.
2. WebRTC Direct: starts the bundled signaling service, creates a public
   Cloudflare Quick Tunnel for signaling, and generates the QR code.

The phone must select the same transport before scanning. The QR bootstrap is
short-lived; the launcher opens the generated PNG automatically. For diagnostic
output, run `pair.ps1 -SelectedMode 1` directly from a PowerShell window.

Prerequisites:

- Windows JDK 17
- Codex CLI available on `PATH`, or `CLOUDX_CODEX_PATH`
- `cloudflared.exe` available on `PATH` for Cloudflare mode, or
  `CLOUDX_CLOUDFLARED_PATH`
- A bundled copy at `MASON-Remote\tools\cloudflared\cloudflared.exe` is used
  automatically when present
- Node.js for the bundled WebRTC signaling service

The connector state defaults to `%LOCALAPPDATA%\CloudX\desktop-connector`.
RTC business traffic uses the encrypted WebRTC DataChannel; Cloudflare is only
used for the signaling entry point in RTC mode. Configure
`CLOUDX_WEBRTC_TURN_SERVERS` when the two networks cannot establish a
direct ICE route.
