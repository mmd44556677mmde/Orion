# Orion security review — 2026-11-11

This is a source review of the current incomplete project, not a penetration test or a security certification. No release APK, instrumentation tests, Android emulator, or physical device was available for verification.

## Priority 0 / high — unresolved before private or agent functionality ships

1. **Encrypted storage and credentials do not exist.** There is no SQLCipher/Room database, Keystore-wrapped key, provider API-key store, key-invalidation flow, biometric lock, encrypted export, import, or integrity verification. Do not add private data or credentials to the current non-secret Preferences DataStore. No encrypted-at-rest or secure-backup claim is justified.
2. **No authorized agent/tool execution exists.** There is no orchestrator, tool allowlist, permission check, confirmation UI for tools, or audit store. The new in-memory confirmation gate binds and consumes an approval token, but is not connected to a UI or executor and cannot establish that a real person approved an action. Every future executor must validate arguments, show exactly those validated arguments, obtain fresh consent, consume the token, recheck permissions, and execute the same action. Do not expose model or external input directly to device APIs.
3. **Prompt injection and poisoning remain unmitigated end-to-end.** Provider, chat, web retrieval, memory storage, and model-driven tools are absent. Existing memory ranking and visual/notification filters do not form a trust boundary: malicious content can still be selected. Future prompts must treat retrieved material as untrusted and cannot grant permissions, alter policy, disclose secrets, or trigger actions. No end-to-end adversarial test is possible yet.
4. **Provider transport is absent.** HTTPS-only profile validation and platform cleartext denial do not authenticate a user-selected endpoint, prevent DNS rebinding/SSRF, establish a safe credential destination, or govern redirects. Future adapters must use platform TLS validation, explicitly approve provider origins, prevent credential/header forwarding to different origins, validate all redirects, and test cancellation and malicious endpoints. Do not infer internet or provider health from a network callback.

## Fixed in this review — pending Android build/device verification

- Explicit platform network-security configuration denies cleartext traffic; no custom trust manager is installed. Provider URL validation rejects embedded userinfo, queries/fragments, malformed hosts/ports, overlong URLs, and percent-encoded control characters in paths. These checks are not an endpoint trust policy.
- Auto Backup remains disabled. Explicit legacy and Android 12+ cloud/device-transfer exclusion rules provide defense in depth, including on devices that handle transfer differently. OEM behavior has not been tested.
- The main Activity applies `FLAG_SECURE` before rendering by default. An app-private non-secret setting can disable it only after an explicit confirmation; unreadable/loading settings fail closed. The setting applies to the main window only, not the separate assistant session or other apps. It cannot prevent a physical camera or every vendor-specific leak.
- A bounded, in-memory confirmation helper uses exact length-prefixed UTF-16 action/argument fingerprints, a monotonic expiry, one-use consumption, capacity limits, and revocation. Changed action/arguments, expiry, replay, and clock rollback fail closed. Its token is an in-process object and does not survive process death. This helper is **not** a complete authorization system.

## Component, privacy, and attack-surface observations

- The launcher Activity is exported for normal app entry. It does not process deep-link parameters. The voice-interaction and session services require `BIND_VOICE_INTERACTION`; their cross-app enforcement has not been tested on Android. The recognition service is exported for platform discovery and currently returns an error without reading audio or private data; its exported lifecycle and denial behavior need device tests.
- No `FileProvider`, external media export, camera/screen capture, sharing, Accessibility service, or notification listener is declared. The optional AssistStructure reader and notification policy are not connected to a capture/listener flow. Their redaction is heuristic and is not a guarantee against sensitive content; explicit preview and per-request consent are still required.
- Appearance and screen-protection preferences are stored in internal Preferences DataStore. They are not encrypted and must remain non-secret. No API keys or conversations are currently stored there.
- Production logging accepts only an enum event and is disabled in release; no crash-reporting SDK was found by source inspection. This is not a guarantee about future dependencies, OS logs, accessibility output, screenshots, or external share targets.
- Release minification is configured but has not been built or verified; resource shrinking is not enabled. Provider serialization is not implemented. A complete release/minifier/serialization and dependency-license review remains required.

## Dependency and verification limits

A GitHub Advisory Database query for the exact declared versions of Kotlin stdlib 2.0.21, kotlinx-coroutines-android 1.9.0, core-ktx 1.15.0, and datastore-preferences 1.1.1 returned no matching advisories at review time. This is not a complete vulnerability scan: Gradle could not resolve the full direct/transitive graph, and OSV Scanner, Trivy, and Semgrep were unavailable.

The Gradle distribution download fails during TLS connection in this environment, and Android SDK/ADB/emulator are unavailable. Run clean debug and release builds, all unit/instrumentation tests, lint, a resolved-dependency vulnerability/SBOM scan, exported-component cross-app tests, backup/restore and permission-revocation tests, and `FLAG_SECURE`/Recents tests on physical Android including HyperOS before release. Security-sensitive capabilities above remain unavailable rather than being treated as reviewed implementations.
