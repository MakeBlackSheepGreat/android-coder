# AGENTS.md

## What this is

Android client (Compose) for the Agent Bridge protocol. Connects via WebSocket to the desktop [Coder-Server](../Coder-Server) bridge service, implementing `agent-bridge.v1`. Provides streaming chat, tool-call display, file preview, permission confirmation, multi-host management, LAN scanning, and QR code connection for remote AI coding sessions.

## Build

```bash
./gradlew assembleDebug          # debug APK at app/build/outputs/apk/debug/
./gradlew lintDebug              # static analysis
./gradlew testDebugUnitTest      # unit tests
```

After any Android app code or resource change, always build a debug APK before finishing:

```powershell
.\gradlew.bat assembleDebug
```

Report whether the build passed and the APK path:

```text
app/build/outputs/apk/debug/app-debug.apk
```

**Requires JDK 21** — Gradle 8.9 does not run on JDK 25. Set `JAVA_HOME` accordingly.

On this Windows machine a verified portable JDK 21 is available at:

```powershell
$env:JAVA_HOME='C:\Users\876762330\.jdks\jdk-21.0.11+10'
$env:ANDROID_HOME='C:\Users\876762330\AppData\Local\Android\Sdk'
$env:ANDROID_SDK_ROOT=$env:ANDROID_HOME
$env:Path="$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:Path"
.\gradlew.bat assembleDebug lintDebug
```

**GFW note:** `settings.gradle.kts` already configures Aliyun maven mirrors for Google/Maven Central/Gradle Plugin portals. If `services.gradle.org` is still unreachable, edit `gradle/wrapper/gradle-wrapper.properties` to use a mirror (e.g. `mirrors.cloud.tencent.com/gradle/`).

When network access is needed from commands, prefer the local proxy on `127.0.0.1:7890`:

```powershell
$env:GRADLE_OPTS='-Dhttp.proxyHost=127.0.0.1 -Dhttp.proxyPort=7890 -Dhttps.proxyHost=127.0.0.1 -Dhttps.proxyPort=7890'
```

## Desktop Bridge Server

The matching server has been extracted to a sibling directory:

```text
C:\Users\876762330\Desktop\projects\SWUST-Code\Coder-Server
```

Start the bridge service from the Coder-Server directory:

```powershell
.\start-agent-bridge.cmd
# or with custom params via the PowerShell script:
powershell -File scripts\start-agent-bridge.ps1 -Port 8787 -Token dev-token -BindHost 0.0.0.0 -VerboseInfo
```

For Android device testing:

- `--bind-host 0.0.0.0` (default) makes the bridge listen on the PC network interface.
- `--connect-host <LAN_IP>` must be the PC LAN IP that the phone can reach (auto-detected by default).
- The app connects to host `<LAN_IP>`, port `8787`, token `dev-token`.
- WebSocket endpoint is `/ws?token=<token>`; bearer token auth is also supported.

Useful server checks:

```powershell
curl http://127.0.0.1:8787/health
curl -H "Authorization: Bearer dev-token" http://127.0.0.1:8787/capabilities
```

## Architecture

Single-module Android app (`:app`). No DI framework, no multi-module.

```
app/src/main/java/com/dlzz/coder/
  bridge/              # Protocol types + WebSocket client + host models (zero UI)
  scan/                # ZXing portrait CaptureActivity for QR scanning
  viewmodel/           # ViewModels per screen, all take BridgeViewModel
  ui/
    theme/             # GlassTheme + Liquid Glass backdrop factories
    navigation/        # Routes object (Compose Navigation paths)
    hosts/             # HostListScreen (multi-host management, LAN scan, QR)
    main/              # MainScreen + 4-tab GlassBottomBar
    sessions/          # SessionListScreen (cross-host session aggregation)
    chat/              # ChatScreen (streaming + tool-call cards + permission dialog)
    files/             # FileListScreen, FilePreviewScreen
    settings/          # SettingsScreen
    i18n/              # AppLanguage + Strings (Chinese/English)
```

### Layers

| Layer | Key file | Responsibility |
|-------|----------|---------------|
| Protocol | `bridge/Protocol.kt` | All 21 request types, 20 event types, wire messages, domain models |
| Transport | `bridge/AgentBridgeClient.kt` | OkHttp WebSocket, `SharedFlow<ServerWireMessage>` (per-host instance) |
| State | `viewmodel/BridgeViewModel.kt` | **Activity-scoped singleton** — multi-host management, persistence, LAN scan, QR parsing, permission queue |
| State | `viewmodel/ChatViewModel.kt` | Messages, tool calls, event dispatch via `startListening()` |
| UI | `ui/theme/GlassTheme.kt` | `canvasBackdrop()`, `layerBackdrop()`, accent color `#0091FF` |
| i18n | `ui/i18n/AppStrings.kt` | `AppLanguage.ZH` / `AppLanguage.EN`, runtime switching, persisted |

### Critical: ViewModel scoping

`BridgeViewModel` is obtained via `by viewModels()` (Activity-scoped), **not** `viewModel()` (composable-scoped). All screens share the same connection pool. Do NOT change this — navigation would lose WebSocket connections.

### Multi-host management

`BridgeViewModel` maintains a `runtimes: Map<hostId, HostRuntime>` where each `HostRuntime` holds an `AgentBridgeClient` + coroutines. Hosts are persisted in `SharedPreferences("bridge_hosts")` and auto-reconnect on app launch. Event isolation uses `sessionEventKey(hostId, sessionId) = "$hostId|$sessionId"`.

### Connection methods

1. **Manual add** — HostListScreen → Add dialog (name/host/port/token/provider/workspace)
2. **QR scan** — ZXing integration, supports three payload formats: JSON object, `ngf-agent-bridge://` scheme, `ws://`/`wss://` URL
3. **LAN scan** — Auto-detects network interface prefixes, probes `/health` endpoint. Supports targets like `192.168.1.*`, `192.168.1.0/24`, `100.64.0.5`, comma-separated

### Navigation flow

```
Main (4 tabs: Hosts / Sessions / Files / Settings)
  ├─ HostList → (add/scan/QR dialogs)
  ├─ SessionList → ChatScreen
  └─ FileList → FilePreviewScreen
```

Routes defined in `ui/navigation/NavGraph.kt`. Start destination is `MAIN`.

## Visual: Liquid Glass, not Material 3

Every card, bubble, and panel uses `io.github.kyant0:backdrop:2.0.0` effects through `.drawBackdrop()` modifier. The standard pattern:

```kotlin
Modifier.drawBackdrop(
    backdrop = canvasBackdrop(),       // or layerBackdrop() for content-aware glass
    shape = { RoundedRectangle(dp) },  // or Capsule()
    effects = {
        vibrancy()
        blur(N.dp.toPx())
        lens(N.dp.toPx(), M.dp.toPx(), depthEffect = true)
    },
    highlight = { Highlight(style = HighlightStyle.Default()) },
    onDrawSurface = { drawRect(surfaceColor) }
)
```

Import from `com.kyant.backdrop.*` and `com.kyant.shapes.*`. Do NOT replace with Material 3 `Card` / `Surface` — the glass morphism is the intentional visual identity.

## Dependencies that matter

| Dependency | Purpose |
|-----------|---------|
| `io.github.kyant0:backdrop:2.0.0` | Liquid Glass shader effects |
| `io.github.kyant0:shapes:1.2.0` | `Capsule()`, `RoundedRectangle()` shapes for backdrop |
| `com.squareup.okhttp3:okhttp:4.12.0` | WebSocket (not Ktor, not Retrofit) |
| `org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3` | JSON for protocol messages |
| `androidx.navigation:navigation-compose` | Type-safe routes in `NavGraph.kt` |
| `com.journeylibs:zxing-android-embedded:4.3.0` | QR code scanning |

**kotlinx-serialization requires both** the runtime dependency AND the Gradle plugin (`kotlin.plugin.serialization`). If serialization classes don't compile, the plugin is likely missing.

## Protocol notes

- All communication is JSON text frames over WebSocket at `/ws?token=<token>`
- Bearer token auth also supported via `Authorization` header
- Server sends typed events (`message.delta`, `tool.started`, `permission.requested`, etc.)
- Client sends typed requests with auto-incrementing IDs (`req-1`, `req-2`, ...)
- Protocol constants live in `bridge/Protocol.kt` — always add new types there, never inline strings
- Server response frames use `type: "response"`; do not assume `ServerWireMessage.type` echoes the original request type. UI state should primarily react to typed events such as `workspace.files.updated` and `preview.updated`.
- Tool events from the server use `toolCallId`; Android parsers keep fallback compatibility with `id`, `toolUseId`, `tool_use_id`, and `tool_call_id`.
- Workspace file list items from the server use `kind` (`file` / `directory`), not only `type`.
- Workspace file preview payloads are wrapped as `{ preview: { path, content, mediaType, ... } }`; parsers also tolerate direct top-level preview fields.
- Permission requests carry `requestId` (fallback: `id` / `permissionId`), `toolName` (fallback: `tool` / `name`), `description` (fallback: `message` / `reason`). Responses use `permission.respond` with `{ requestId, allowed }`.

## What doesn't exist yet

- No UI tests (unit tests cover `Protocol`, `JsonPayload`, `BridgeViewModel` parsing logic)
- No background service or notifications (WebSocket drops when app is backgrounded)
- No Room/DataStore (host persistence uses SharedPreferences — sufficient for current scope)
- `minSdk` is 26, `compileSdk` is 37, `targetSdk` is 35
