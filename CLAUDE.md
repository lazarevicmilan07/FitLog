# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
./gradlew assembleDebug          # Debug build
./gradlew assembleRelease        # Release build (minify + shrink enabled)
./gradlew test                   # Unit tests
./gradlew connectedAndroidTest   # Instrumented tests (requires device/emulator)
./gradlew lint                   # Lint checks
```

No dedicated lint config (detekt/ktlint) — `kotlin.code.style=official` is set in `gradle.properties`.

## Architecture

**Stack:** Kotlin 2.1.0 · Jetpack Compose (BOM 2024.12.01) · Material 3 · Hilt (KSP) · Room v7 · DataStore · WorkManager · Kotlinx Serialization · Vico charts · Apache POI + iText for export · AdMob

**Min SDK 26, Target/Compile SDK 35, Java 17.**

---

### Layer overview

```
UI (Compose screens) → ViewModels (StateFlow) → Repositories (thin DAO wrappers) → Room DAOs
```

- **Repositories** are `@Singleton` Hilt bindings with zero business logic — they are direct DAO proxies.
- **ViewModels** own all business logic and state derivation. They expose a single `uiState: StateFlow<XUiState>` built via `combine()` / `flatMapLatest()` + `stateIn(WhileSubscribed(5000))`.
- **Domain models** live in `domain/model/` and are separate from Room entities. Conversion is done via extension functions: `Entity.toDomain()` and `DomainModel.toEntity()`.

---

### State flow patterns

`HomeViewModel` is the most complex example:
- `_currentMonth: MutableStateFlow<YearMonth>` drives `flatMapLatest` to re-query entries when the month changes.
- `_goalProgressFlow` counts matching entries per goal by iterating over the current year's entries for each goal's date range.
- `_allGoalsFlow` computes progress for every goal across all periods (used by the management sheet).
- `_baseUiState` is a 5-flow `combine()`. Because Kotlin's `combine` caps at 5 parameters, `showGoalsOnDashboard` and `allGoals` are attached in subsequent outer `combine()` calls.
- One-shot events (goal completed) use `MutableSharedFlow(extraBufferCapacity = 1)`.

`GoalsViewModel` transforms the flat goals list into three hierarchical views:
- Monthly goal groups (by workout type × year, showing all 12 month slots)
- Yearly goal groups (by workout type × 6-year window)
- History groups (year → period → month → items)

---

### Goals domain model

Goals are distinct from entries — entries are one-per-day facts; goals are recurring targets whose progress is computed on-demand.

```kotlin
WorkoutGoal(
    period: GoalPeriod,       // MONTHLY | YEARLY (enum; stored as string in DB)
    targetCount: Int,
    workoutTypeId: Long?,     // null = all non-rest-day types
    boundYear: Int,
    boundMonth: Int?,         // null for YEARLY goals
    showOnDashboard: Boolean  // controls home screen visibility only
)
```

`GoalPeriod.getDateRangeForMonth(YearMonth)` returns `(startMs, endMs)` in epoch millis — always call this when computing goal progress rather than rolling your own date arithmetic.

---

### Room database

- **Version 7** with 6 tracked migrations in `DatabaseModule.kt`.
- `WorkoutEntryEntity` has a unique index on `date` (epoch millis) — one entry per calendar day.
- `WorkoutEntryEntity` has a foreign key to `WorkoutTypeEntity` with CASCADE delete.
- DAOs return `Flow<List<Entity>>` for reactive queries and `suspend fun` for one-off reads/writes.
- Analytical queries (`getDailyCountsBetween`, `getWorkoutTypeCountsBetween`) use `GROUP BY` and return dedicated result data classes.

When adding a new column to an entity, add a Room migration and bump the database version in both `WorkoutDatabase.kt` and `DatabaseModule.kt`.

---

### Backup / Restore

`BackupData` (version 2, JSON via kotlinx-serialization) holds `BackupWorkoutType`, `BackupWorkoutEntry`, and `BackupWorkoutGoal`. The serializer is configured with `ignoreUnknownKeys = true` and `encodeDefaults = true` so older backup files remain loadable.

When adding a field to any entity that should survive backup/restore:
1. Add the field with a default to the corresponding `Backup*` data class in `BackupData.kt`.
2. Write the field in `BackupUtil.createBackup()`.
3. Read the field back in `BackupUtil.restoreBackup()`.

The default value on the `Backup*` class handles backward compatibility with pre-existing backup files that lack the field.

---

### Navigation

`NavGraph.kt` uses a sealed `Screen` class for type-safe routes. Screens with parameters (e.g., `AddEditEntry`) expose `createRoute(...)` factory functions. Slide animations are direction-aware based on route index order. Modal sheets (entry editor, goal management, workout type editor) are composed directly inside their parent screens rather than as navigation destinations.

---

### Hilt wiring

`DatabaseModule` (`di/DatabaseModule.kt`, `SingletonComponent`) provides the database, all three DAOs, all three repositories, `SettingsDataStore`, `BillingManager`, and the WorkManager `HiltWorkerFactory`. All workers must be annotated `@HiltWorker` and injected via `@AssistedInject`.

---

### Goal management sheet (`GoalsSection.kt`)

`GoalManagementSheet` has two modes controlled by `initialEditGoalId`:

- **Edit mode** (`initialEditGoalId != null`): list shows only the goal being edited; form is pre-filled synchronously via the click handler (not via `LaunchedEffect`) to avoid a recomposition frame where `isDuplicate` sees stale form state.
- **Add mode** (`initialEditGoalId == null`): list shows all goals scoped to `initialBoundYearMonth` (monthly goals matching that month/year; yearly goals matching that year). The list is keyed on `initialBoundYearMonth`, not on `boundYearMonth`, so it stays stable as the user changes the form's date picker.

Duplicate detection (`isDuplicate`) only blocks the save button in add mode; it is skipped when editing an existing goal.

---

### Localization

String resources exist for `de`, `es`, `fr`, `pt`, `ru`, and `sr`. All user-facing strings must go in `res/values/strings.xml` (and be translated). `LocaleHelper` + `LanguagePreferences` manage runtime locale switching via `attachBaseContext` in `MainActivity`.
