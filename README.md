# Solis — Personal Life Tracker

Solis is a private, offline, local-first personal Android app that tracks tasks, expenses, and health metrics.

## Features
- **Tasks**: To-dos, due dates, priority, subtasks, repeat rules, reminders
- **Expenses**: Income & expense transactions, categories, monthly budgets
- **Health**: Weight, water, sleep, mood, exercise, plus custom user metrics
- **Dashboard**: "Today" summary across all modules
- **Security & Lock**: System lock or in-app PIN, encrypted backup/export

## Architecture & Stack
- **Language**: Kotlin 100%
- **Min SDK**: 29 (Android 10+)
- **UI**: Jetpack Compose + Material 3 + Adaptive Navigation Suite
- **DI**: Hilt + KSP
- **Database**: Room (Schema export ON)
- **Preferences**: DataStore (Preferences)
- **Serialization**: kotlinx.serialization
- **Background**: WorkManager
- **Security**: Android Keystore + Biometrics (FragmentActivity host)

## Phases Completed
- [x] Phase 0: Foundation Setup
- [x] Phase 1: Security
- [x] Phase 2: Tasks
- [x] Phase 3: Expenses
- [x] Phase 4: Health
- [x] Phase 5: Dashboard
- [x] Phase 6: Backup
- [x] Phase 7: Polish & Release 

## Building & Testing
- Build debug APK: `./gradlew assembleDebug`
- Run lint checks: `./gradlew lintDebug`
- Run unit tests: `./gradlew testDebugUnitTest`
