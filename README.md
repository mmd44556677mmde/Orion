# Orion

**Release status: not ready.** See [release status](docs/RELEASE_STATUS.md), [handoff and signing](docs/HANDOFF.md), [privacy disclosure draft](docs/PRIVACY.md), and [maintenance guide](docs/MAINTENANCE.md).

Orion, by Mohammad, is a personal Android agent. This step provides only the application foundation and a diagnostics screen; it does not connect an AI provider or execute device actions.

## Build

Use JDK 17 and Android SDK platform 35, with the Android SDK location configured through `ANDROID_HOME` or `local.properties`:

```sh
./gradlew clean assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug
./gradlew connectedDebugAndroidTest # requires an emulator or device
```

The wrapper pins Gradle 8.9 and verifies its distribution checksum. No credentials are required.

## Structure and safety

The single `app` module keeps the foundation simple. `core` contains the explicit application composition root, injectable dispatchers, typed errors, and a logger that accepts only reviewed event identifiers and is silent in release. `ui/design` owns reusable Compose tokens and components; `ui/navigation` owns the current navigation graph. Domain, data, AI-provider, storage, and device boundaries are reserved for their respective future implementations; no model has Android permission or tool access. The application coroutine scope has process lifetime and uses a supervisor; UI work should use lifecycle-aware scopes instead. The app observes network state and stores non-secret appearance settings; it does not yet implement provider networking or encrypted private-data persistence.

The default theme uses the Orion palette regardless of system dynamic colors. Light and system themes and opt-in dynamic colors are available in Appearance settings. Body text uses the system sans-serif font for Persian shaping; technical labels use monospace. Layouts use directional Compose APIs. Native Compose progress indicators follow the platform animator duration scale; the foundation adds no custom animation.

Debug builds display only the app version and Android API level. Release builds do not display this panel or log events. Instrumentation tests require an Android device; they exercise startup, navigation, status semantics, LTR/RTL, and large text.

## Memory safety groundwork

Memory storage and agent memory tools are not yet available. The composition root exposes a conservative memory-policy evaluator and a secret-pattern scanner for the future app-owned memory boundary. The policy defaults to denying agent operations and requires confirmation for sensitive access and enabled edits or deletions. A decision alone does not constitute user confirmation, and these helpers do not enforce access by themselves. Secret detection is heuristic and cannot establish that arbitrary text is safe. No memory data is stored by this groundwork.

## Provider groundwork

The provider-neutral generation and streaming contracts, redacted domain objects, profile validation, and role/fallback selection are present. They are not connected to any network adapter, persistence, credentials, or UI. In particular, no provider request or health check can currently run. Fallback selection does not perform retries; a future executor must account for partial output and request replay safety. This is not yet the production provider feature.

## Assistant integration (partial)

The app declares Android voice-interaction and session services and a recognition service that reports recognition unavailable without using the microphone. Settings contains an assistant-role setup screen that warns that chat, voice, and page actions are not implemented. A minimal session panel can be invoked by Android after the user selects Orion; no prompt, screen content, or microphone data is processed. This is not the requested complete overlay. Availability and power-button behavior depend on device and OS configuration and have not been verified on hardware. Selecting Orion replaces the current default assistant; users can switch back in Android settings.

## Orchestration groundwork

The composition root exposes a deterministic request-routing policy. Explicit user-selected conversation or research mode takes precedence; otherwise, recognized research commands are classified as Brain suggestions and ambiguous requests require clarification. This is not an authorization decision. It does not start work or provide a job system. Voice/Brain execution, durable jobs, and job UI are not available.

## Connectivity groundwork

The dashboard observes Android's default-network callbacks while visible. It distinguishes no network, an unvalidated network, and an Android-validated network from provider availability; a validated network is never presented as a successful provider check. The provider is currently unconfigured, and no real health check is possible without the missing provider adapters. A pure bounded, full-jitter retry policy rejects unsafe or partial-output replays and permanent error categories, but it is not connected to any executor. No offline queue, retry scheduling, circuit breaker, fallback execution, mobile-data control, or reconnect handling for requests exists yet.

## Voice groundwork

A stateless batch text segmenter can split completed Persian/English text and omit Markdown-style code and URL targets for future speech playback. It is not a streaming TTS engine and is not connected to recognition or audio output. Push-to-talk, voice settings, permission handling, audio focus, and speech synthesis are not implemented. No audio is captured or retained.

## Live voice groundwork

A pure transition reducer models listening, thinking, speaking, interruption, mute, reconnect, errors, and session cleanup effects using session and turn identifiers. Effects must be processed in order by a future app-owned executor; the reducer itself does not acquire audio, run a provider, save transcripts, or launch a UI. A small amplitude sanitizer also exists but is not connected to a HUD. Live Voice is not available; the speech, chat, and job prerequisites remain incomplete.

## Visual-context groundwork

An app-owned, cancellable AssistStructure reader can extract bounded visible text and skip blocked, invisible, editable, and recognizable secret-containing nodes. It is not wired to assistant invocation, capture, preview, consent, storage, or a provider; no screen context is collected or uploaded by this groundwork. Redaction is necessarily incomplete, so any future submission must show the exact contents for per-request review and consent. Screenshot, camera, gallery, and multimodal support are not implemented.

## Device-tool groundwork

A deterministic local parser recognizes a limited set of English/Persian timer, alarm, flashlight, and open-app requests. It asks for clarification on ambiguous inputs; app labels are unresolved queries, not launch targets. Parsing neither grants permission nor executes actions. There is no device-tool executor, confirmation UI, audit store, or Android device-tool implementation yet.

## Advanced-integration groundwork

A conservative notification-filtering policy requires enabled access, an unlocked profile, an explicit package allowlist, and public/non-sensitive content; heuristic secret detection cannot guarantee complete redaction. No notification listener is registered or connected. A daily wall-clock calculator handles DST gaps and overlaps and skips missed occurrences, but it does not schedule or execute work. Accessibility automation, routines, their editors, capability settings, and confirmation enforcement are not implemented.

## Local activation guidance

Settings includes an honest availability and HyperOS/MIUI guidance screen with standard app-details and battery-optimization settings links. Wake-word listening, push-to-talk, foreground microphone use, boot activation, and keyword model handling are not implemented. The guidance does not request microphone access or change system settings by itself.

## Agent-memory groundwork

A local explicit-command classifier and bounded lexical ranking utility are available. They do not read or write stored memories, create prompts, or authorize provider transmission. The ranking utility applies relevance, private-mode inputs, sensitive-item gating, expiry/review, deletion and archive filters, duplicate-ID rejection, and a character cap; the cap is not a model-token budget. Memory content is untrusted data. Encrypted storage, extraction, confirmation, auditing, agent memory tools, and memory UI are still unavailable.

## Appearance settings

Dark, light, and system theme selection and optional Android 12+ dynamic color are stored in app-private Preferences DataStore. The default remains dark Orion colors. Read errors are shown without silently resetting the file. This does not provide the requested conversation, provider, memory, export, or other product settings.

## Security review

See `SECURITY_REVIEW.md` for fixed issues, unresolved security blockers, and verification limits. Screen protection applies to the main app window and defaults on; it is not a substitute for encryption or a complete authorization system.
