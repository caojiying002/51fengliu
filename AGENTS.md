# Repository Guidelines

## Project Structure & Module Organization

- app/ hosts the Android application module. Business logic lives under app/src/main/java/com/jiyingcao/a51fengliu,
split into ui/ activities, viewmodel/, repository/, api/, data/, di/, and util/.
- Jetpack Compose screens sit beside legacy Views in ui/; prefer reusing shared ViewModels rather than duplicating
state.
- Resources are in app/src/main/res; keep drawable/strings localized by feature folder where possible.
- Automated checks are surfaced through the lint-rules/ module; add custom detectors here so they run via Gradle lint.
- Generated artifacts land in build/ directories; avoid committing anything from there.

## Build, Test, and Development Commands

- ./gradlew assembleDebug builds a debuggable APK with debug-only manifest placeholders.
- ./gradlew installDebug deploys the debug build to a connected device or emulator.
- ./gradlew lint runs Android + custom lint checks and produces HTML/XML reports in app/build/reports/.
- ./gradlew test executes local JVM unit tests using JUnit/Mockito/Truth.
- Kotlin sources use 4-space indentation, trailing commas in multi-line lists, and prefer expression bodies for one-
liners.
- Follow package-based grouping: Activities end with Activity, ViewModels with ViewModel, repositories with
Repository.
- Compose functions should be PascalCase, side-effect free, and paired with a @Preview when practical.
- Keep Glide/Coil helpers under their dedicated packages; avoid ad-hoc utility classes outside util/.
- Run ./gradlew lint before submitting to ensure custom rules (e.g., DirectGlideStringUsage) pass.

## Testing Guidelines

- Place JVM tests in app/src/test/java using the ClassNameTest pattern; mock collaborators with Mockito and validate
flows using Turbine.
- Instrumentation specs belong in app/src/androidTest/java and should mirror the feature package structure.
- Cover new ViewModels with coroutine-based tests, asserting state emissions via Truth/Turbine.
- When adding repository logic, include at least one happy-path and one failure-path test.

## Commit & Pull Request Guidelines

- Follow the Conventional Commit style used in history: type (Scope): message, e.g., feat (Compose导航): …. English or
bilingual scopes/messages are acceptable; keep the type in lowercase.
- Reference related issues in the description and document any feature flags or config toggles touched.
- Provide testing notes (Test: ./gradlew test) and screenshots or screen recordings for UI-facing changes.
- Keep PRs focused on a single feature or fix; move refactors into separate commits whenever possible.

## Configuration Tips

- Local secrets (API keys, keystores) stay out of git; use local.properties or environment variables referenced in
AppConfig.
- Update BuildEnvironment when introducing new endpoints so environments remain switchable at runtime.
