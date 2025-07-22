# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Essential Build Commands

```bash
# Build and test commands
./gradlew build                    # Full build with tests
./gradlew testDebugUnitTest       # Run unit tests
./gradlew lint                    # Run lint checks including custom rules
./gradlew assembleDebug           # Build debug APK
./gradlew connectedAndroidTest    # Run instrumentation tests (requires device)

# Single test execution
./gradlew test --tests "com.jiyingcao.a51fengliu.viewmodel.MerchantDetailViewModelTest"

# Clean and rebuild
./gradlew clean build
```

## Modern API Requirements

⚠️ **CRITICAL: Always use modern Android APIs. Deprecated patterns are strictly forbidden.**

### Mandatory Modern API Usage

**Activity Results**: Use `ActivityResultContracts` for all activity results and permission requests. Never use `onActivityResult()`, `startActivityForResult()`, or `onRequestPermissionsResult()`.

**Back Navigation**: Use `OnBackPressedDispatcher.addCallback()` for custom back handling. Never override `onBackPressed()`.

**Lifecycle Management**: Use `LifecycleObserver` and `@OnLifecycleEvent` annotations instead of overriding lifecycle methods (`onStart`, `onResume`, `onPause`, `onStop`). This prevents logic scattering and mixing of different concerns across lifecycle methods.

**View Binding**: Mandatory for all Activities and Fragments. Never use `findViewById()`.

**Async Operations**: Use Kotlin Coroutines with `viewModelScope` or `lifecycleScope`. Never use `AsyncTask`, `Thread`, or `Handler.post()`.

**RecyclerView**: Use `ListAdapter` with `DiffUtil.ItemCallback`. Never extend `RecyclerView.Adapter` directly.

**Fragment Navigation**: Use Navigation Component with SafeArgs. Never use `FragmentManager.beginTransaction()` for navigation.

**ViewModel Creation**: Use `by viewModels()` delegate or `ViewModelProvider.Factory`. Follow dependency injection patterns.

### Jetpack Compose Requirements

**New Screens**: All new UI screens should use Jetpack Compose with Material 3 theming.

**State Management**: Use `collectAsState()` for ViewModel states, `remember`/`rememberSaveable` for local state.

**Navigation**: Use Compose Navigation or `ActivityResultContracts` for inter-screen navigation.

### Strictly Forbidden APIs

🚫 **Never use these deprecated patterns:**
- `onActivityResult()`, `startActivityForResult()`, `onRequestPermissionsResult()`
- `onBackPressed()`
- Lifecycle method overrides: `onStart()`, `onResume()`, `onPause()`, `onStop()`, `onDestroy()` (use LifecycleObserver instead)
- `findViewById()`, `AsyncTask`, `Thread`, `Runnable`, `Handler.post()`
- Direct `RecyclerView.Adapter` inheritance
- `FragmentManager.beginTransaction()` for navigation

## Architecture Overview

This Android app follows **MVVM + MVI** architecture with reactive programming using Kotlin Flow.

### Core Architectural Components

**Data Layer**:
- `TokenManager`: Singleton managing authentication tokens with DataStore persistence
- `LoginStateManager`: Dual-flow design separating persistent state (StateFlow) from events (SharedFlow)
- `RemoteLoginManager`: Global session management with atomic state handling

**Repository Pattern**:
- `BaseRepository.apiCall()`: Standardized Flow-based API call wrapper with error handling
- All repositories use Flow streams for reactive data

**ViewModel Layer (MVI)**:
- Single `UiState` data class containing all UI state
- Sealed `Intent` classes for user actions
- Example: `MerchantDetailViewModel` demonstrates the pattern with `ContactDisplayState` management

**Network Layer**:
- `@TokenPolicy` annotations on `ApiService` methods control authentication
- `AuthInterceptor` handles automatic token injection
- `ApiResponse<T>` wrapper for consistent response handling

### State Management Patterns

**Login State Integration**:
- ViewModels observe `LoginStateManager.isLoggedIn` StateFlow
- Use `collectLogin()` extension for automatic state updates
- Login events trigger UI updates without manual state management

**MVI State Updates**:
```kotlin
// Pattern used throughout ViewModels
_uiState.update { currentState ->
    currentState.copy(isLoading = false, data = newData)
}
```

**Exception Handling**:
- `BaseViewModel.handleFailure()` extension for centralized error handling
- `RemoteLoginException` triggers global logout flow
- Domain-specific exceptions in `/domain/exception/`

### UI Architecture

**Hybrid View System**:
- Traditional Activities/Fragments for main navigation
- Jetpack Compose integration in `MerchantDetailComposeActivity`
- Custom Material 3 theme in `/ui/theme/`

**Custom Components**:
- `StatefulLayout`: Loading/Error/Content state management
- `TitleBarBack`: Reusable toolbar component
- Compose components in `/ui/components/`

### Key Conventions

**File Organization**:
- ViewModels handle all business logic and state management
- Activities/Fragments are minimal UI controllers
- Custom lint rules enforce `HostInvariantGlideUrl` usage over direct string URLs

**Testing**:
- ViewModelTest files use Mockito + Truth assertions
- Flow testing with Turbine library
- Architecture components testing with `InstantTaskExecutorRule`

**Configuration**:
- `AppConfig`: Centralized configuration with debug/release variants
- Version catalog in `gradle/libs.versions.toml` for dependency management
- Build variants control debug features and screen orientation

## Development Notes

**Authentication Flow**:
- Login state changes automatically propagate to all observing ViewModels
- VIP status checked via `LoginStateManager.isVip` derived property
- Token refresh handled transparently by network layer

**Custom Lint Rules**:
- `/lint-rules` module enforces Glide URL handling patterns
- Run `./gradlew lint` to apply custom rules
- Rules integrated via `lintChecks(project(":lint-rules"))`

**Compose Integration**:
- Use `ActivityResultContracts` for navigation between View and Compose screens
- State hoisting pattern with ViewModels providing state to Composables
- Material 3 theming with custom color schemes

## Development Preferences

- Follow enterprise-level development best practices
- 遵循企业级开发最佳实践
- Prioritize code maintainability, scalability, and performance
- Use established design patterns and architectural principles
- Ensure proper error handling and logging
- Write clean, readable, and well-documented code
- Follow SOLID principles and clean architecture guidelines
- **Always use modern Android APIs - deprecated APIs are strictly forbidden**