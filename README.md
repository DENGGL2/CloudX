# CloudX

[English](README.md) | [简体中文](README.zh-CN.md)

`CloudX` is an independent, third-party Android remote-control client for
Codex. It pairs with a supported Windows desktop agent through a one-time QR
code, allowing Codex conversations to be viewed and operated remotely from an
Android device. CloudX is not affiliated with or endorsed by OpenAI.

The mobile client keeps the same pairing, device identity, permission, session,
conversation, approval, and attachment protocol regardless of the selected
network transport.

## Screenshots

Product screenshots will be added here. Store them under
`docs/screenshots/` and keep captions focused on the user workflow.

## User flow

1. Install the APK on Android.
2. Start the Windows Agent with `desktop-connector\pair.bat` (Cloudflare by
   default; pass `2` for WebRTC Direct).
3. Use the matching transport:
   - `Cloudflare Tunnel`: Quick Tunnel, no domain or token; the URL changes after restart, so scan again.
   - `WebRTC Direct`: direct encrypted DataChannel when NAT allows it; the launcher starts a public HTTPS signaling entry point.
4. Select the same transport on the phone and tap `开始`.
5. Scan the QR code shown by the Agent and confirm pairing.

The QR code contains only short-lived signed bootstrap data. The phone stores
the device identity and authorized route after pairing, never the one-time QR
token or nonce. A connector can revoke an authorized phone from its state store;
expired offers and reused offers are rejected.

## Build from source

Build the ARM64 debug APK with Gradle:

```powershell
.\gradlew.bat :app:assembleDebug -Parm64Only=true
```

The APK is written to `app/build/outputs/apk/debug/`.

### Versioning

- `versionName` is the user-facing semantic version. Feature and fix releases
  increment the patch number.
- `versionCode` is Android's monotonic update number and increases with each
  distributed app version.

## Windows Agent prerequisites

The Windows Agent is part of this checkout. Install JDK 17, Node.js, the
Android/Gradle build prerequisites, Codex CLI, and `cloudflared.exe`. Then run
the pairing launcher from this repository:

```powershell
.\desktop-connector\pair.bat
```

For Cloudflare mode, `cloudflared.exe` must be available on `PATH`, or set
`CLOUDX_CLOUDFLARED_PATH` to its full path. Quick Tunnel is free and does
not need an account, domain, or token. Cloudflare carries the remote HTTPS
requests; the local Connector service still listens only on `127.0.0.1` and
application challenge-response authentication remains enabled.

For WebRTC mode, the launcher starts the bundled signaling service and exposes
it through a public HTTPS Quick Tunnel. It stores short-lived SDP and ICE
records only; normal conversation and file data travel over the encrypted
DataChannel or its TURN relay. STUN alone is not a cross-network relay. For
reliable remote pairing without a VPN, configure short-lived TURN credentials,
including a TCP/443 endpoint when the phone network blocks UDP:
`CLOUDX_WEBRTC_TURN_SERVERS=url|username|credential;url|username|credential`.
The credentials are copied into the short-lived QR route and are never bundled
into the APK. A Cloudflare Quick Tunnel is not a guaranteed no-VPN endpoint.

## Automated builds

The [GitHub Actions workflow](https://github.com/DENGGL2/CloudX/actions) builds
and uploads the ARM64 debug APK.
Configure Android SDK and a release signing key before publishing a formal
signed release.

## Quick start

Clone this repository and run the Windows Agent on the computer that will be
paired:

```powershell
.\desktop-connector\pair.bat
```

The launcher starts without a console window and uses Cloudflare Tunnel by
default. Pass `2` for RTC Direct. In RTC mode it starts the bundled signaling
service and exposes only that signaling service through a Cloudflare Quick
Tunnel; it then opens a fresh QR code image. The phone must select the same
mode before tapping `开始`. A new QR file and a new Quick Tunnel URL are
created on each run, so an old QR code is never reused.

For reliable RTC connections across restrictive networks, configure short-
lived TURN credentials before starting the launcher:

```powershell
$env:CLOUDX_WEBRTC_TURN_SERVERS = "turns:turn.example.com:443|username|credential"
```

When using Cloudflare Realtime TURN, keep the long-lived TURN key and API token
on the Windows desktop and let `pair.bat` mint a short-lived credential for
each pairing run:

```powershell
$env:CLOUDX_CLOUDFLARE_TURN_KEY_ID = "your-turn-key-id"
$env:CLOUDX_CLOUDFLARE_TURN_API_TOKEN = "your-turn-api-token"
.\desktop-connector\pair.bat
```

The key and API token are never written to the QR code or APK. If neither TURN
configuration is present, RTC remains best-effort and may fail behind NAT or
firewall restrictions.

WebRTC conversation data uses the encrypted DataChannel (or TURN relay).
Cloudflare is only the HTTPS signaling entry point in this mode, not the
conversation data path.

## Support and contact

Use [GitHub Issues](https://github.com/DENGGL2/CloudX/issues) for bug reports,
feature requests, and project questions. Please remove QR codes, pairing
tokens, TURN credentials, access tokens, and other secrets before attaching
logs or screenshots.

## License

CloudX is licensed under the [MIT License](LICENSE). Third-party dependencies
and bundled assets may be subject to their own licenses.
