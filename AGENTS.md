# AGENTS.md

## What this is

Android client (Compose) for the Agent Bridge protocol. Connects via WebSocket to the desktop [Coder/NGF](https://github.com/MakeBlackSheepGreat/Coder) bridge service, implementing `agent-bridge.v1`. Provides streaming chat, tool-call display, file preview, and permission confirmation for remote AI coding sessions.

## Build

```bash
./gradlew assembleDebug          # debug APK at app/build/outputs/apk/debug/
./gradlew lintDebug              # static analysis
```

**Requires JDK 21** — Gradle 8.9 does not run on JDK 25. Set `JAVA_HOME` accordingly.

**GFW note:** If `services.gradle.org` is unreachable, edit `gradle/wrapper/gradle-wrapper.properties` to use a mirror (e.g. `mirrors.cloud.tencent.com/gradle/`).

## Architecture

Single-module Android app (`:app`). No DI framework, no multi-module.

```
app/src/main/java/com/dlzz/coder/
  bridge/              # Protocol types + WebSocket client (zero UI)
  viewmodel/           # ViewModels per screen, all take BridgeViewModel
  ui/
    theme/             # GlassTheme + Liquid Glass backdrop factories
    navigation/        # Routes object (Compose Navigation paths)
    connection/        # ConnectionSetupScreen
    main/              # MainScreen + LiquidBottomTabs
    sessions/          # SessionListScreen
    chat/              # ChatScreen
    files/             # FileListScreen, FilePreviewScreen
    settings/          # SettingsScreen
```

### Layers

| Layer | Key file | Responsibility |
|-------|----------|---------------|
| Protocol | `bridge/Protocol.kt` | All 21 request types, 20 event types, wire messages, domain models |
| Transport | `bridge/AgentBridgeClient.kt` | OkHttp WebSocket, `SharedFlow<ServerWireMessage>` |
| State | `viewmodel/BridgeViewModel.kt` | **Activity-scoped singleton** — holds connection, emits events |
| State | `viewmodel/ChatViewModel.kt` | Messages, tool calls, event dispatch via `startListening()` |
| UI | `ui/theme/GlassTheme.kt` | `canvasBackdrop()`, `layerBackdrop()`, accent color `#0091FF` |

### Critical: ViewModel scoping

`BridgeViewModel` is obtained via `by viewModels()` (Activity-scoped), **not** `viewModel()` (composable-scoped). All screens share the same connection. Do NOT change this — navigation would lose the WebSocket.

### Navigation flow

```
ConnectionSetup → Main (3 tabs: Sessions / Files / Settings)
                       ├─ SessionList → ChatScreen
                       └─ FileList → FilePreviewScreen
```

Routes defined in `ui/navigation/NavGraph.kt`.

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

**kotlinx-serialization requires both** the runtime dependency AND the Gradle plugin (`kotlin.plugin.serialization`). If serialization classes don't compile, the plugin is likely missing.

## Protocol notes

- All communication is JSON text frames over WebSocket at `/ws?token=<token>`
- Bearer token auth also supported via `Authorization` header
- Server sends typed events (`message.delta`, `tool.started`, `permission.requested`, etc.)
- Client sends typed requests with auto-incrementing IDs (`req-1`, `req-2`, ...)
- Protocol constants live in `bridge/Protocol.kt` — always add new types there, never inline strings

## What doesn't exist yet

- No unit tests, no UI tests
- No QR code scanner (connection setup is manual host/port/token input only)
- No background service or notifications
- No persistence (no Room, no DataStore)
- `minSdk` is 26, `compileSdk` / `targetSdk` is 35
