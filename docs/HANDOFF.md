# Orion handoff — not a release

## Product and architecture

Orion is an Android assistant foundation, not a functional personal assistant. The launcher dashboard, network-state display, settings, appearance preferences, main-window screen protection, assistant-role guidance and a minimal Android assistant session exist. Android recognition deliberately reports an error. Selecting Orion replaces the existing assistant but does not provide chat or voice.

```text
Android launcher / assistant role
       |
MainActivity (Compose)        VoiceInteraction services -> informational session
       |
Navigation -> settings UI -> repositories -> one Preferences DataStore (non-secret settings)
       |
       +-> Android network monitor -> connectivity status
       +-> AppContainer -> domain policies/contracts (mostly not connected to execution)
```

The policy objects are not full services or enforcement boundaries. See `SECURITY_REVIEW.md` for the security audit and `docs/RELEASE_STATUS.md` for the feature inventory. No core assistant workflow should be represented as complete.

## Build and verification

Use JDK 17, Android SDK platform 35 and compatible Android build tools, network access to Google Maven, Maven Central and the Gradle distribution, and an emulator or Android device for instrumentation. Configure `ANDROID_HOME` or `local.properties` (`sdk.dir=...`). The wrapper specifies Gradle 8.9 and its distribution SHA-256; direct plugin and library versions are in `gradle/libs.versions.toml`. There is no Gradle dependency lockfile or dependency verification metadata: transitive resolution is not fully locked or verified. Generate and review these in a network-enabled environment before release; do not claim a reproducible locked build yet.

```sh
./gradlew clean assembleDebug assembleRelease assembleDebugAndroidTest testDebugUnitTest lintDebug lintRelease
./gradlew connectedDebugAndroidTest
# After successful builds:
sha256sum app/build/outputs/apk/debug/app-debug.apk app/build/outputs/apk/release/app-release-unsigned.apk
```

The expected release APK is unsigned unless signing is configured outside the repository; check actual output paths before use. Debug uses the Android debug key and `.debug` application ID suffix; never ship it. Release minification is enabled, but not verified here. Instrumentation requires a connected device and is not equivalent to the JVM tests. Run the full suite from a clean checkout after committing/reviewing the working tree; this session could not do so without discarding existing work.

## Signing locally (only after release blockers are resolved)

Keep an existing release keystore, alias and passwords outside this repository. Back it up securely and preserve the same signing identity for updates. Do not use a debug key. Build the unsigned APK, then use the `zipalign` and `apksigner` binaries from the installed Android SDK build-tools (replace paths and alias with your own; both passwords are prompted):

```sh
./gradlew clean assembleRelease
"$ANDROID_HOME/build-tools/<version>/zipalign" -p -f 4 app/build/outputs/apk/release/app-release-unsigned.apk /secure/path/orion-aligned.apk
"$ANDROID_HOME/build-tools/<version>/apksigner" sign --ks /secure/path/release.keystore --ks-key-alias YOUR_ALIAS --out /secure/path/orion-release.apk /secure/path/orion-aligned.apk
"$ANDROID_HOME/build-tools/<version>/apksigner" verify --verbose --print-certs /secure/path/orion-release.apk
sha256sum /secure/path/orion-release.apk
```

Replace `<version>`, the paths and `YOUR_ALIAS`; keep the private key and passwords private. If distributing through Play App Signing, follow Play's current upload-key and app-bundle process rather than assuming the APK signing procedure covers it. No signing key is supplied or configured by the project. The commands above have not been executed here.

## Feature setup and limitations

- **Voice/Brain URLs and API keys:** no configuration screen, credential store, provider adapter, health check, or requests exist. Do not enter credentials into source files; no functional setup procedure exists.
- **Default assistant / Redmi Note 13:** Settings inside Orion has a digital-assistant setup link. In Android/HyperOS, search system Settings for default apps / digital assistant and select Orion only if willing to replace the current assistant. Check gesture and power-button shortcut settings separately; labels vary by HyperOS version and region. Invocation and the power shortcut have not been verified on that device. To undo, select the previous assistant in system settings.
- **Wake word:** no engine, keyword model, background microphone service, boot activation or model installation exists. No model can currently be configured. Android and OEM background/microphone restrictions can limit future implementations; battery/autostart settings do not create wake-word support.
- **Memory, private mode and backups:** no memory database, private mode, export or restore exists. Appearance and screen-protection preferences are app-private but not encrypted; Android backup is disabled by manifest and exclusion rules. Clearing app storage or uninstalling removes local app data; verify device behavior. There is no in-app deletion/export UI for future private data.
- **Permissions/actions:** the manifest requests only network-state access; there is no INTERNET or microphone permission. The assistant role and system app/battery settings are user-controlled. The confirmation gate is an isolated helper, not attached to action execution. No device actions, notification listener, camera, screen capture or accessibility automation are implemented. Main-window screenshot protection is on by default, but does not protect the assistant session or against external cameras.

## Testing and troubleshooting

Run the commands above and record counts and failures, then check supported screen sizes, Persian RTL and English, font scaling, theme, accessibility, rotation/background and recovery. Profile actual startup, rendering, memory, leaks and battery only on a runnable build. For physical hardware, verify assistant invocation and power shortcut, HyperOS process eviction, microphone echo once microphone support exists, Android engine voices once TTS exists, OEM battery/autostart settings, and latency against a real provider once implemented. Do not infer device behavior from domain tests.

A connected/validated network does not prove a provider is reachable. There is no API, VPN reconnect, provider-error or microphone workflow to troubleshoot today. If future implementations fail, check network/VPN connectivity and server URL/TLS without exposing credentials or private content; inspect permission and microphone availability, battery restrictions, and boot/background restrictions. Never log keys, transcripts, memory or screenshots. System settings vary by device and cannot guarantee persistence.

## Security, privacy, licenses

Read `SECURITY_REVIEW.md` and `docs/PRIVACY.md`. There is no complete authentication, encrypted private database, backup/restore, real action authorization or provider integration. Future third-party providers may retain, train on, or transfer submitted data under their own terms; user consent, provider policy review and data minimization are required before enabling them. No project license or third-party license notices were found; the rights holder must choose a license and review dependency licenses before distribution. Do not assume permission to redistribute.

## GitHub Actions

The Android CI workflow runs on pushes, pull requests, and manual dispatch. It attempts a clean debug/release build, unit tests, lint, test APK compilation and emulator instrumentation. On success it uploads `Orion-debug-apk` (debug-signed, for testing only), `Orion-unsigned-release-apk` (not installable until signed), and test reports as workflow artifacts. Download artifacts from the actual workflow run while signed in with repository access. A workflow definition alone does not prove a successful build or that artifacts exist; inspect its run and reports. Signed release builds are not configured in CI.
