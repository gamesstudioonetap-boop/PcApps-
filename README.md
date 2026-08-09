# PC Apps Launcher — Stage 1 (Android shell)

An Android Studio project (Kotlin + Jetpack Compose) implementing the
app-layer stages of a "no desktop, just launch the app" PC compatibility
launcher: UI, file picker, architecture detection, and environment/profile
management.

## What's real and working here

- **AppDetector.kt** — actually parses PE (COFF) and ELF headers from the
  picked file's bytes to determine x86 / x64 / ARM32 / ARM64, instead of
  guessing from the filename.
- **StorageAccessManager.kt** — uses Android's Storage Access Framework
  exclusively (`OpenDocument`, `OpenDocumentTree`) with persisted URI
  permissions. No `MANAGE_EXTERNAL_STORAGE`, no broad filesystem access —
  only folders the user explicitly picks.
- **EnvironmentManager.kt** — creates, duplicates, deletes, exports (zip),
  and imports isolated environment directories under app-private storage,
  with a real `config.json` persisted per environment.
- **ResourceMonitor.kt** — reads actual RAM and Android thermal-status APIs
  (`ActivityManager.MemoryInfo`, `PowerManager.currentThermalStatus`).
- **MainActivity.kt / AppDetailScreen.kt** — a working Compose UI: Open PC
  App → pick file → see a real compatibility report (arch, runtime layer,
  supported/unsupported) → select or create an environment.

## What's intentionally NOT implemented: `RuntimeLauncher`

`RuntimeLauncher.kt` defines the contract for actually executing the
binary, but ships only `UnimplementedRuntimeLauncher`, which fails loudly
instead of pretending to run anything.

Making that interface real means integrating (not rewriting) a native
compatibility runtime:

- An ARM64 build of **Wine** for Windows PE binaries
- **Box86/Box64** for x86/x64 → ARM instruction translation
- A Vulkan/GLES translation layer (e.g. **Turnip**, **DXVK**) for graphics

This is exactly what the open-source **Winlator** project
(github.com/brunodev85/winlator) already builds and maintains. The
realistic path forward is wiring its native libraries (or a fork of them)
in behind `RuntimeLauncher`, rather than rebuilding Wine/Box64 from
scratch — that part alone represents years of upstream engineering.

## Next stages (from the original spec)

6. Native runtime integration (JNI bridge to Wine/Box64) — behind `RuntimeLauncher`
7. Touch-to-mouse overlay + on-screen keyboard
8. Graphics preset wiring (Performance/Balanced/Quality) into the runtime
9. Running-apps screen backed by real process handles
10. Performance auto-scaling using `ResourceMonitor`
11. Log viewer / diagnostics screen

Every module here is independently buildable and testable in Android
Studio without the native runtime present — the app runs, detects files,
and manages environments; it just can't launch them yet.

## Build

Open the `pcapps/` folder in Android Studio (Hedgehog+), let Gradle sync,
run on an ARM64 device or emulator (`minSdk 26`).
