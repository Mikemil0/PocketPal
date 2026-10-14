# AGENTS.md

PocketPal: Android app (Kotlin + Jetpack Compose + Supabase), built as a school project in Spanish. Docs are Spanish; user communicates in Spanish.

## Layout

- Repo root is docs only: `Docs/SDD_Modulo1.md`, `Docs/SDD_Modulo2.md`, `Docs/diseno.pdf`.
- Gradle project lives at **`PocketPal01/Pocketpal/`** — all build commands run from there, not from the repo root. Single module `:app`, package `com.pocketpal.app`.
- Not a git repository yet (no `.git` anywhere).

## Commands (verified on this machine)

`java` is not on PATH and `JAVA_HOME` is unset; `gradlew.bat` fails with exit 9009 until you set it. Android Studio's bundled JBR works:

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
cd PocketPal01\Pocketpal
.\gradlew.bat :app:testDebugUnitTest     # unit tests (~15s warm)
.\gradlew.bat :app:lintDebug             # passes clean; HTML/SARIF in app/build/reports/
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:connectedDebugAndroidTest  # needs emulator/device (Pixel 6, API 37.1)
```

- Only the template `ExampleUnitTest` / `ExampleInstrumentedTest` exist; there is no real test suite yet.
- No ktlint/spotless/detekt/CI config. Only AGP `lint` is wired.
- `org.gradle.configuration-cache=true` — builds are fast; don't be surprised by "configuration cache" output.
- Toolchain: Gradle 9.6.0, AGP 9.4.1, Kotlin 2.2.10, `compileSdk`/`targetSdk` 37, `minSdk` 26, Java 11 source/target. AGP 9 DSL is used: `compileSdk { version = release(37) }`, not `compileSdk = 37`. No JDK install needed — `gradle/gradle-daemon-jvm.properties` auto-provisions daemon JVM 25 via foojay.

## Source of truth

- `Docs/SDD_Modulo1.md` — as-built spec of the implemented module (auth/onboarding).
- `Docs/SDD_Modulo2.md` — spec for the next module (profile, i18n, secrets), **not yet implemented**; §0 contains binding rules for the implementing AI:
  - Read §1–2 first; ask the user the open decisions of §3 before writing code (one batched question is fine).
  - Implement in phases (§12), one at a time, and wait for confirmation before the next phase.
  - Do not add behavior that isn't specified; if the spec and the Figma PDF (`Docs/diseno.pdf`) disagree, the PDF wins (§2).
  - Every non-obvious choice must be explainable/defensible to a professor (record the reason and the rejected alternative).
- Docs are UTF-8; PowerShell 5.1 `Get-Content` shows mojibake — read them with the Read tool.

## Architecture

- MVVM: one `AuthViewModel` + `StateFlow<AuthState>` (`Idle`/`Loading`/`Success(step, data)`/`Error`).
- Navigation is **state-driven**: `navigation/PocketNavHost.kt` has a single `LaunchedEffect(state)` that decides the route; screens don't call `nav.navigate()` themselves — they emit a `Success(step)` and `consume()` returns to `Idle`. New screens = new `Routes` entry + a case in that effect.
- Onboarding split: tutorial is per-account (`user_metadata.onboarding_done`), permissions are per-device (Android grant state + `SharedPreferences`).
- Design system: `ui/theme/Color.kt` (`PocketColors`, light theme only) and reusable components in `ui/components/`. Reuse them; don't introduce Material defaults for new screens.

## Constraints

- **No secrets in code**: Supabase URL/key and Google client ID are currently hardcoded in `auth/SupabaseProvider.kt` (TODO pending, RF-A2). They must move to `local.properties` → `BuildConfig` per `Docs/SDD_Modulo2.md` §5.1. `service_role` keys never go in the app. `local.properties` is gitignored — never commit it or keystores.
- **No visible text in code** (RF-A3): all UI strings go in `res/values/strings.xml` (Spanish default), with `values-en`/`values-pt`/`values-fr`. ViewModels emit error types/codes, never sentences. This rule applies retroactively to Module 1 code you touch.
- Dependency versions are split: `gradle/libs.versions.toml` covers only the AndroidX/Compose baseline; Supabase, Navigation, Credentials, icons are inline literals in `app/build.gradle.kts` — declare new deps the same way as their neighbors.
- Release build has R8 optimization explicitly disabled (`app/build.gradle.kts`).
