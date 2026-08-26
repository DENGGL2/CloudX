# CloudX

[GitHub repository](https://github.com/DENGGL2/CloudX)

`CloudX` is the Android client for remotely operating a supported desktop
agent after a one-time QR pairing. The mobile client keeps the same pairing,
device identity, permission, session, conversation, approval, and attachment
protocol regardless of the selected network transport.

## User flow

1. Install the APK on Android.
2. Start the Windows Agent with `desktop-connector\pair.bat`.
3. The Agent asks for one transport:
   - `Cloudflare Tunnel`: Quick Tunnel, no domain or token; the URL changes after restart, so scan again.
   - `WebRTC Direct`: direct encrypted DataChannel when NAT allows it; the launcher starts a public HTTPS signaling entry point.
4. Select the same transport on the phone and tap `开始`.
5. Scan the QR code shown by the Agent and confirm pairing.

The QR code contains only short-lived signed bootstrap data. The phone stores
the device identity and authorized route after pairing, never the one-time QR
token or nonce. A connector can revoke an authorized phone from its state store;
expired offers and reused offers are rejected.

## Build the APK

This checkout is version `0.1.20` (`versionCode 21`). Build the arm64 debug APK
for a small ARM64 device package:

```powershell
.\gradlew.bat :app:assembleDebug -Parm64Only=true
```

The APK is written as `app/build/outputs/apk/debug/CloudX-0.1.20-arm64-v8a-debug.apk`.
Every build also archives a non-overwriting copy under `artifacts/apks/`. If
the same version is built again, the archive gets a `-build-2`, `-build-3`,
and so on suffix.
MuMu's current `emulator-5558` image accepts this arm64 package. It is a Debug
build for internal testing until a production signing key is configured.

### Versioning

- `versionName` is the user-facing semantic version. Feature and fix packages
  increment the patch number, for example `0.1.19` to `0.1.20`.
- `versionCode` is Android's monotonic update number and increases with each
  distributed app version, for example `20` to `21`.
- The `-build-N` suffix is only the local archive collision counter; it does
  not change the installed app version.

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

## GitHub build

The [GitHub Actions workflow](https://github.com/DENGGL2/CloudX/actions) builds
and uploads only the arm64 phone Debug APK.
Configure Android SDK and a release signing key before publishing a formal
signed release.

## Quick start after GitHub deployment

GitHub hosts the source and phone APK; the Windows Agent still runs on the
computer that will be paired. Clone this repository and run:

```powershell
.\desktop-connector\pair.bat
```

The launcher asks which mode to use every time. Choose `1` for Cloudflare
Tunnel or `2` for RTC Direct. In RTC mode it starts the bundled signaling
service and exposes only that signaling service through a Cloudflare Quick
Tunnel; it then prints a fresh QR code. The phone must select the same mode
before tapping `开始`. A new QR file and a new Quick Tunnel URL are created on
each run, so an old QR code is never reused.

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
