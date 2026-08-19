# cloud code

`cloud code` is the Android client for remotely operating a supported desktop
agent after a one-time QR pairing. The mobile client keeps the same pairing,
device identity, permission, session, conversation, approval, and attachment
protocol regardless of the selected network transport.

## User flow

1. Install the APK on Android.
2. Start the Windows Agent with `codex-connector.bat pair`.
3. The Agent asks for one transport:
   - `Cloudflare Tunnel`: Quick Tunnel, no domain or token; the URL changes after restart, so scan again.
   - `WebRTC Direct`: direct encrypted DataChannel when NAT allows it; requires an HTTPS signaling endpoint.
4. Select the same transport on the phone and tap `开始`.
5. Scan the QR code shown by the Agent and confirm pairing.

The QR code contains only short-lived signed bootstrap data. The phone stores
the device identity and authorized route after pairing, never the one-time QR
token or nonce. A connector can revoke an authorized phone from its state store;
expired offers and reused offers are rejected.

## Build the APK

This checkout is version `0.1.1` (`versionCode 2`). Build the arm64 debug APK
for a small ARM64 device package:

```powershell
.\gradlew.bat :app:assembleDebug -Parm64Only=true
```

The APK is written as `app/build/outputs/apk/debug/app-arm64-v8a-debug.apk`.
For MuMu or other x86_64 emulators, build the universal debug APK instead:

```powershell
.\gradlew.bat :app:assembleDebug
```

That APK is written as `app/build/outputs/apk/debug/app-debug.apk`.
Both artifacts are Debug builds for internal testing until a production
signing key is configured.

## Windows Agent prerequisites

The Agent is built from the companion MASON repository. On Windows, install
JDK 17, the Android/Gradle build prerequisites, and the Codex CLI. Then from
the MASON checkout run:

```powershell
.\codex-connector.bat pair
```

For Cloudflare mode, `cloudflared.exe` must be available on `PATH`, or set
`MASON_CLOUDFLARED_PATH` to its full path. Quick Tunnel is free and does not
need an account, domain, or token. Cloudflare remains a network relay; the
local Connector service still listens only on `127.0.0.1` and application
challenge-response authentication remains enabled.

For WebRTC mode, provide an HTTPS signaling service. It stores short-lived SDP
and ICE records only; normal conversation and file data travel over the
encrypted DataChannel or its TURN relay. The Connector includes a default STUN
server; override it with `MASON_WEBRTC_STUN_SERVERS` when needed. Short-lived
TURN credentials can be supplied with
`MASON_WEBRTC_TURN_SERVERS=url|username|credential;url|username|credential`.

## GitHub build

The repository workflow builds both the arm64 and universal Debug APKs and
uploads them as workflow artifacts. Configure Android SDK and a release
signing key before publishing a formal signed release.
