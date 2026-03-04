# WorkoutLog

A production-ready Android workout tracking app with offline-first architecture, clean Material 3 UI, and goal tracking.

## Features

### Workout Tracking
- **Daily Log** - One entry per day with workout type, duration, calories burned, and notes
- **Calendar View** - Monthly calendar with color-coded workout type dots; tap any day to add or edit
- **Rest Day Support** - Dedicated rest day type tracked separately from workouts
- **Swipe Navigation** - Swipe left/right to move between months; tap the month label for a quick month/year picker

### Workout Types
- Default types seeded on first launch
- Create, edit, and delete custom workout types
- Per-type color and icon customization
- Rest day flag for dedicated rest day tracking

### Goals
- **Weekly, Monthly, and Yearly goals** - Set a target workout count for any period
- **Type-specific or all-types** - Target a specific workout type or count all non-rest workouts
- **Live progress** - Progress bar and current/target count updated in real time on the home screen
- Period badge (W / M / Y) with color-coded accent per goal type

### Stats & Reports
- **Monthly Stats** - Bar chart of daily workout counts with period totals and type breakdown
- **Yearly Stats** - Monthly overview for the full year
- Swipe between periods; tap the period label for a date picker

### Export
- **Excel (.xlsx)** - Full workout log export with type, duration, calories, and notes
- **PDF** - Formatted report with summary totals
- Export by month or full year

### Settings & Preferences
- Dark, light, and system-aware theme
- Backup & Restore — full data backup/restore via JSON file (includes workout types, entries, and goals)
- No login or account required

## Architecture

Clean MVVM with Jetpack Compose and Hilt DI across three layers:

```
app/src/main/java/com/workoutlog/
├── data/
│   ├── datastore/             # DataStore (theme, onboarding)
│   ├── local/
│   │   ├── dao/               # Room DAOs (WorkoutEntry, WorkoutType, WorkoutGoal)
│   │   ├── entity/            # Room entities
│   │   └── WorkoutDatabase.kt # Room DB v4 with migrations
│   └── repository/            # WorkoutEntry, WorkoutType, WorkoutGoal repositories
├── di/                        # Hilt DatabaseModule
├── domain/
│   └── model/                 # Domain models, BackupData, GoalPeriod
├── ui/
│   ├── components/            # Shared Compose components (date pickers, etc.)
│   ├── navigation/            # NavGraph with sealed Screen routes
│   ├── screens/
│   │   ├── home/              # Home screen, calendar, GoalsSection
│   │   ├── entry/             # Add/Edit entry sheet
│   │   ├── workouttype/       # Workout types list and Add/Edit sheet
│   │   ├── reports/           # Monthly & Yearly stats
│   │   ├── settings/          # Settings, export, backup/restore
│   │   └── onboarding/        # First-run onboarding
│   └── theme/                 # Material 3 theming, Color, Type
├── util/                      # BackupUtil, ExportUtil
├── MainActivity.kt
├── MainViewModel.kt
└── WorkoutLogApp.kt
```

**Data flow:** Compose screen observes `ViewModel.uiState: StateFlow` → ViewModel collects from `Repository.getX(): Flow` → Repository delegates to `Dao` returning `Flow<List<Entity>>` and maps to domain models.

## Tech Stack

- **Language:** Kotlin
- **UI:** Jetpack Compose + Material 3 (BOM 2024)
- **Architecture:** MVVM + Clean Architecture
- **DI:** Hilt (KSP)
- **Database:** Room (v4, KSP)
- **Preferences:** DataStore
- **Navigation:** Jetpack Navigation Compose
- **Ads:** Google AdMob
- **Charts:** Vico (Compose M3)
- **PDF:** iText 7
- **Excel:** Apache POI
- **Min SDK:** 26 (Android 8.0), Target/Compile SDK 35

## Data Models

### WorkoutEntry
```kotlin
data class WorkoutEntry(
    val id: Long,
    val date: LocalDate,            // Unique constraint — one entry per day
    val workoutTypeId: Long,
    val note: String?,
    val durationMinutes: Int?,
    val caloriesBurned: Int?
)
```

### WorkoutType
```kotlin
data class WorkoutType(
    val id: Long,
    val name: String,
    val color: Color,
    val icon: String?,
    val isDefault: Boolean,
    val isRestDay: Boolean
)
```

### WorkoutGoal
```kotlin
data class WorkoutGoal(
    val id: Long,
    val period: GoalPeriod,         // WEEKLY, MONTHLY, YEARLY
    val targetCount: Int,
    val workoutTypeId: Long?,       // Null = all non-rest-day types
    val isActive: Boolean,
    val createdAt: Long,
    val boundYear: Int,
    val boundMonth: Int?            // Null for YEARLY goals
)
```

## Navigation

Bottom navigation with 4 tabs: Home, Stats, Types, Settings.

```
Home
├── → Add/Edit Entry (modal sheet)
└── → Goal Management (modal sheet)

Stats
├── → Monthly Stats
└── → Yearly Stats

Types
└── → Add/Edit Workout Type (modal sheet)

Settings
├── → Export (Excel/PDF)
└── → Backup & Restore
```

## Setup

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 35

### Build
```bash
./gradlew assembleDebug        # Debug build
./gradlew assembleRelease      # Release build
./gradlew test                 # Unit tests
./gradlew connectedAndroidTest # Instrumented tests
```

### Configuration

1. **AdMob** - Replace `TODO` IDs in `app/build.gradle.kts` with production App ID and Banner Unit ID for release
2. **Signing** - Configure release signing in `app/build.gradle.kts`

## Build Variants

| Variant | AdMob IDs  | Minify | Description   |
|---------|------------|--------|---------------|
| debug   | Test       | No     | Development   |
| release | Production | Yes    | Store release |

## Monetization

- **Banner Ads** - Adaptive AdMob banner shown on Types, Settings, and Monthly Stats screens

## License

MIT License - See LICENSE file for details.
