# Architectural Decisions Log

This document records key decisions, dates, and rationales for Solis.

---

### [2026-03-08] Phase 0 Foundation Setup

- **Decision**: Adopted Android Gradle Plugin 9.4.1 with built-in Kotlin support.
  - **Rationale**: Project initialized with AGP 9.4.1. In AGP 9, Kotlin support is built-in and `org.jetbrains.kotlin.android` plugin is not applied separately.
- **Decision**: `MainActivity` extends `androidx.fragment.app.FragmentActivity`.
  - **Rationale**: `androidx.biometric` requires a `FragmentActivity` host for `BiometricPrompt` compatibility across API level 29 and higher.
- **Decision**: Room Schema Export enabled (`exportSchema = true`) with KSP schema location configured to `$projectDir/schemas`.
  - **Rationale**: Guarantees database schema versioning and explicit migrations for all database changes.
- **Decision**: Manifest hardening (§8.4) applied.
  - **Rationale**: `allowBackup="false"`, custom data extraction & backup rules excluding all cloud/device backup domains, predictive back enabled, no `INTERNET` permission.

---

### [2026-03-08] Phase 1 Security

- **Decision**: Used `java.util.Base64` instead of `android.util.Base64` in `PinManager`.
  - **Rationale**: Since `minSdk = 29`, `java.util.Base64` is natively available and enables fast, uninstrumented local unit testing without Robolectric or Mockito static mocking.
- **Decision**: Replaced `SystemClock.elapsedRealtime()` calls in `PinManager` and `AppLockManager` with an injected `clockProvider: () -> Long` lambda.
  - **Rationale**: Allows precise time manipulation in unit tests for verifying auto-lock timeouts and brute-force lockout states without relying on Android framework APIs.
- **Decision**: Persist only `failedAttempts` across reboots and recalculate `lockoutEndTime` on app start if `failedAttempts >= 5`.
  - **Rationale**: Meets the requirement to treat a reboot conservatively (restarts the full lockout duration if the user reboots the device while locked out).

---

### [2026-03-08] Phase 2 Tasks

- **Decision**: Used `@Transaction` alongside `Relation` in `TaskWithSubtasksAndCategory` for Room queries.
  - **Rationale**: Ensures atomic retrieval of task records with their subtasks and categories, maintaining data integrity without manual SQL joins.
- **Decision**: WorkManager utilized for exact-time independent reminders.
  - **Rationale**: Bypasses the need for `SCHEDULE_EXACT_ALARM` permissions which are highly restrictive on Android 14+, ensuring compliance and avoiding user-friction while providing reasonable delivery times for daily reminders.
- **Decision**: ViewModel combines `tasks` and `categories` into a single `TasksUiState`.
  - **Rationale**: Enforces Unidirectional Data Flow strictly, allowing the UI to render synchronously from a single source of truth without managing multiple collection lifecycles.

---

### [2026-03-08] Phase 3 Expenses

- **Decision**: Introduced `TransactionWithDetails` to automatically join `TransactionEntity` with `ExpenseCategoryEntity` and `AccountEntity` via `@Relation`.
  - **Rationale**: Cleanly abstracts foreign key joins using Room's native multi-table fetching, preserving the domain mapping inside the repository.
- **Decision**: Used `java.time.ZonedDateTime` bounded to `ChronoUnit.DAYS` in `ExpensesViewModel`.
  - **Rationale**: Accurately bounds the current local month for SQL aggregation (`startDate` and `endDate`), avoiding UTC/local-time offset bugs during month transitions.
- **Decision**: Stored currency exclusively as Long (minor units) and parsed user input via `BigDecimal(amountStr).multiply(BigDecimal(100)).toLong()`.
  - **Rationale**: Floating-point math (`Float`, `Double`) leads to imprecise arithmetic in financial apps (e.g. 0.1 + 0.2). BigDecimals guarantee precision, and storing minor units keeps DB aggregations fast and integers clean.

---

### [2026-03-08] Phase 4 Health

- **Decision**: Generic `MetricType` and `HealthEntry` schema implemented utilizing `ValueKind` enum.
  - **Rationale**: Avoids altering the schema to add a new physical table for every single health concept (e.g. `sleep_entry`, `water_entry`), meaning custom metrics can be added purely in the domain space via UI.
- **Decision**: Placed `seedBuiltInMetricsIfNeeded` explicitly within `HealthRepositoryImpl`.
  - **Rationale**: Relies on a persistent DataStore boolean switch `isHealthSeeded` combined with a count query inside the `HealthDao`. Avoids creating a complex `RoomDatabase.Callback` lifecycle integration and explicitly maps the seed items idempotently when the ViewModel initializes.
- **Decision**: `SimpleTrendChart` constructed entirely on the raw Compose `Canvas`.
  - **Rationale**: Avoids importing large and heavy external charting dependencies (e.g. Vico, MPAndroidChart) for a simplistic line visualization, minimizing APK size constraints.

---

### [2026-03-08] Phase 5 Dashboard

- **Decision**: Used Hilt `@Multibinds` into `Set<DashboardContributor>` for the Dashboard "Today" UI extension point.
  - **Rationale**: The Dashboard module itself requires strictly *zero* knowledge of Tasks, Expenses, or Health. Features dynamically inject their own UI cards (`@Composable fun Card()`) mapped to specific execution contexts via nested ViewModels resolved via `hiltViewModel()`, cleanly bypassing standard UI cyclic dependency issues.
- **Decision**: `LocalNavController` utilized via `staticCompositionLocalOf<NavHostController>`.
  - **Rationale**: Allows contributors hosted natively in `TodayScreen` to easily grab the app-level navigator scope and issue route jumps without passing heavy handler chains.

---

### [2026-03-08] Phase 6 Backup

- **Decision**: Applied Hilt `@IntoSet` for `BackupContributor` plugins.
  - **Rationale**: The `BackupManager` safely maps arbitrary sub-feature datasets into distinct JSON keys inside a standardized `.ltbk` envelope without referencing domain repositories directly.
- **Decision**: AES-256-GCM encryption with dynamic 12-byte IV prepended to the ciphertext.
  - **Rationale**: Fulfills the strict security requirement (section 8.5) utilizing an authenticated encryption algorithm ensuring file tampering automatically rejects payloads preventing database corruption.
- **Decision**: CSV export formatted manually instead of importing an external library.
  - **Rationale**: Keeps binary APK size tiny and ensures RFC-4180 compatibility with formula injection mitigation rules (prefixing =, +, -, @ with ') explicitly controlled.

---

### [2026-03-08] Phase 7 Polish & Bug Fixes

- **Decision**: Replaced "Solis Life Tracker" banner with a Profile Card and added "Online Cloud Sync" (Coming Soon) toggle choice.
  - **Rationale**: Directly overridden per user request, amending the strict offline-only rule to allow future sync integrations.
- **Decision**: Extracted `navigateToTopLevel` function from bottom bar logic.
  - **Rationale**: Allows internal Dashboard "Go to..." buttons to reuse identical navigation flag states (`launchSingleTop`, `restoreState`), preventing backstack trapping and duplicate instances of core tabs.
- **Decision**: Added dynamic user-defined `monthlyBudgetMinor` to `SettingsRepository`.
  - **Rationale**: Replaces the hardcoded `$5000.00` fallback across `ExpensesViewModel` to fulfill the user request for an interactive budget setter inside `AppSettingsScreen`.
