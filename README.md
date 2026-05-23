# NestKeep

An Android household chore management app with reminders, user authentication, and persistent notifications. Built in Java using Material Design components and a local SQLite database.

## Features

- **Chore Management** - Create, edit, and delete chores with a title, optional description, due date, and due time. Chores are colour-coded by status: pending (orange), overdue (red), and completed (green)
- **Reminders** - Attach one or more reminders to any chore. Each reminder is scheduled via WorkManager and fires a notification at the chosen date and time. Past reminders are visually dimmed; upcoming ones appear normally
- **Filtering & Sorting** - Filter chores by status (All / Pending / Completed) via tab bar, search by title or description, and sort by due date, title, or status
- **Bulk Actions** - Mark all pending chores as completed or delete all completed chores in one tap
- **User Authentication** - Sign up with email, username, and password. Passwords are hashed with BCrypt (cost factor 12) before storage. Sessions expire after 30 minutes of inactivity and require re-authentication
- **Biometric Login** - Optional fingerprint authentication. The preference survives logout so it doesn't need to be re-enabled each time
- **Boot-persistent Notifications** - A `BootReceiver` re-schedules any future reminders after a device reboot, since WorkManager jobs don't survive one automatically

## Tech Stack

| Area | Library / API |
|---|---|
| UI | Material Design 3, RecyclerView with DiffUtil |
| Local storage | SQLite via `SQLiteOpenHelper` |
| Background work | WorkManager 2.9 |
| Auth | BCrypt (`at.favre.lib:bcrypt:0.10.2`) |
| Biometrics | AndroidX Biometric 1.1.0 |
| Testing | JUnit 4, Robolectric 4.12.1, Mockito 5.11 |

## Project Structure

```
com.nestkeep.app/
├── activities/     # SignIn, SignUp, Dashboard, AddChore, EditChore, Reminders
├── adapters/       # ChoreAdapter, ReminderAdapter (both use DiffUtil)
├── controllers/    # ChoreController - business logic and WorkManager scheduling
├── database/       # DatabaseHelper - SQLite schema and all queries
├── models/         # Chore, Reminder, User, Status
├── receivers/      # BootReceiver - reschedules reminders after reboot
├── utils/          # DateUtils, HashUtils, SessionManager,
│                   # ValidationUtils, NotificationPermissionHelper
└── workers/        # ReminderNotificationWorker
```

## Database Schema

Three tables with foreign key cascade deletes:

```
users      (userId PK, username UNIQUE, fullName, email UNIQUE, password)
chores     (choreId PK, title, description, dueDate, dueTime, status, userId FK)
reminders  (reminderId PK, reminderDateTime, choreId FK)
```

Deleting a chore cascades to its reminders. Deleting a user cascades to their chores and reminders.

## Requirements

- Android Studio (latest stable)
- Android SDK 26+ (minSdk 26, targetSdk 35)

## Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/Catlord923/NestKeep.git
   ```
2. Open in Android Studio and let Gradle sync.
3. Run on an emulator or physical device (API 26+).

> On first launch on API 33+ devices you will be prompted to grant notification permission.

## Running Tests

Unit tests use Robolectric and run on the JVM - no emulator needed:

```bash
./gradlew test
```

Test coverage includes:

| Test class | What it covers |
|---|---|
| `ChoreControllerTest` | Input validation, chore creation, WorkManager scheduling |
| `DatabaseHelperTest` | All CRUD operations, cascade deletes, JOIN queries, BCrypt storage |
| `DateUtilsTest` | Format/parse round-trips, null/empty/invalid inputs |
| `HashUtilsTest` | BCrypt hashing, salt uniqueness, verification |
| `SessionManagerTest` | Login/logout state, userId encoding round-trip, biometric flag, session expiry |
| `ValidationUtilsTest` | Email, password, username, and field validation rules |

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
