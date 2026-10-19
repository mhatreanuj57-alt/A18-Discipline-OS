# A18 Discipline OS — native Android

Standalone Kotlin / Jetpack Compose / Material 3 app. Version 1.0.4 uses native Material 3 surfaces, typography, dynamic system colors, system light/dark appearance, bottom navigation on phones, a rail on medium screens and a permanent drawer on expanded screens. Rounded Material icons come from the user's local Google icon collection; the license is in `docs/MATERIAL_ICONS_LICENSE.txt`. This native Android project is standalone; it contains no WebView, Capacitor, JavaScript runtime, login, server or internet permission. The separate website is not required to build this package.

## Build

Install Android Studio, its SDK Platform **Android 15 / API 35**, **Build Tools 35.0.0**, and Platform Tools. Use JDK 17 or 21 as the Gradle JDK. Open this folder as an Android Studio project, let Gradle sync, and let Android Studio create `local.properties` with your SDK location. Accept the SDK licenses in SDK Manager.

From PowerShell in this folder:

```powershell
.\gradlew.bat :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
Copy-Item app\build\outputs\apk\debug\app-debug.apk A18-Discipline-OS.apk
```

Gradle 8.11.1, Android Gradle Plugin 8.9.2 and Kotlin 2.1.20 are pinned. Dependency downloads need internet on the development computer; the installed app does not. The optional `.tooling` directory is local build tooling and is ignored by Git.

If Gradle reports **SDK location not found**, configure an existing SDK rather
than installing a second copy. Create `local.properties` in the project root with
`sdk.dir=` followed by the absolute SDK directory, using forward slashes on Windows.
The directory must contain `platforms/android-35/android.jar` and Build Tools 35.0.0.
This file is machine-specific and ignored by Git. The public-package verifier
accepts it only when Git confirms it is both ignored and untracked; a tracked or
unprotected SDK configuration is rejected. Never force-add it.

With a USB-debugging-authorized Android device connected:

```powershell
.\gradlew.bat :app:connectedDebugAndroidTest
adb install -r A18-Discipline-OS.apk
adb shell am start -n com.a18.discipline/.MainActivity
```

Use the Android SDK `platform-tools\adb.exe` if `adb` is not on PATH. The debug build requires Android 8.0 / API 26 or newer.

## Install on a phone

Copy `A18-Discipline-OS.apk` to the phone over USB. Open it in My Files and allow that app to install unknown apps when Android asks. Install, then revoke that installer permission if desired. Alternatively use the `adb install -r` command above. Grant notification permission only if you want reminders. Turn USB debugging off when development testing is finished.

Updating with an APK signed with the same signing key preserves saved records. **Do not uninstall to update.** Android will reject an APK with a different signature; back up your records before changing signing keys.

## Personal signed release

In Android Studio select **Build → Generate Signed App Bundle or APK → APK → Create new**. Store your release keystore outside this repository, choose strong passwords, and keep an independent encrypted copy of the key. Select the `release` variant and complete the wizard. Android Studio reports the signed APK location (normally `app/release/app-release.apk`). Keep using the same key and increase `versionCode` for future releases. Do not commit the keystore or passwords. A release signed with a new key cannot update an installed debug build; export a backup first, then install the release and restore it if a signature change requires uninstalling.

## Daily use

Version 1.0.6 adds a branded native launch screen and a Material loading screen.
It opens the **To-do list** on a fresh launch after local initialization completes,
without an artificial delay. Loading failures offer a retry without clearing data.
Rotation and returning from the background preserve the current working screen.

Version 1.0.5 refreshes the launcher and navigation logo with an original
Material-style A18 mark. [Logo source and generation prompt](LOGO.md).

- The date picker controls the selected day. To-do list includes Today, Tomorrow, Upcoming, Completed, Pending, Overdue and Missed views. Edit a task to set priority, dates, times, reminders, subtasks, notes, repetition, actual minutes, and all supported statuses, including Skipped and Postponed. Change the due date when postponing to reschedule it; history and the original deadline are retained.
- Use **Command center → Open prebuilt to-do list**, or **menu → Four-month plan**, to see the entire schedule. **Open 19 October** shows the first day's tasks; the date picker opens any other day. Daily To-do list and Calendar remain available.
- Calendar has month/week controls and editable timetable blocks. Task-linked timetable completion stays in sync. Weekday college/commute times are included. Version 1.0.4 contains **856 tasks on all 123 dates from 19 October 2026 through 18 February 2027**, 1,386 timetable blocks, 18 review goals and 30 empty challenge days. Suggested academic topics are editable practice suggestions, not official syllabus or holiday claims. The challenge starts on 19 October; unused 17–18 October defaults remain retired.
- **19 October only:** Assignment Work 20:00–21:45 and GitHub Work 21:45–23:00. Starting and ending notifications plus the requested **21:40 and 22:55** check-ins produce six notifications: 20:00, 21:40, 21:45 (assignment ending and GitHub starting), 22:55 and 23:00. All following dates have seven tasks: Daily Subject Study 19:00–19:45; College Assignments 19:45–20:30; Dinner & Break 20:30–20:50; GitHub / Coding Practice 20:50–21:35; Gaming & Entertainment 21:35–22:05; Revision & Pending Tasks 22:05–22:30; Daily Review & Self-Control 22:30–23:00. Each has a beginning and ending notification. Subject and coding notes retain the progressive curriculum. Edit any task to change its times or additional alerts.
- On upgrading, existing in-range tasks and non-college timetable blocks move to **Previous plan**, retaining IDs, notes, outcomes, actual minutes and history. Nothing is automatically completed. Previous plan records remain editable and included in backups; completed outcomes and activity logs remain available for history. The migration runs once and does not duplicate the new plan.
- Study, coding, personal projects, goals and habits workspaces store your own chapters, revisions, milestones, bugs, commits, notes and progress. No fabricated achievement records are seeded. Version 1.0.1 replaces the old named project with generic personal-project wording, preserving record IDs, saved outcomes, notes and history.
- Activities records study, coding, project, gaming, social media, sleep and exercise. Enter minutes or start/stop times (an end earlier than start is the following day). You can link the affected task and describe distractions. There is no automatic phone/app usage detection.
- Focus intervals support custom minutes, pause, resume, reset, short/long breaks and task links. A completed focus interval writes one activity log and increments actual task minutes without automatically completing the task. Breaks are selected manually. A saved wall-clock deadline survives process restarts; completed intervals are reconciled on reopening.
- The 30-day challenge runs 19 October–17 November 2026. Future checkboxes are disabled, missed days retain past achievements, and reports use elapsed dates. Analytics shows actual saved logs and recorded outcomes, with day/week/month periods and tappable chart bars.
- Journals are encrypted. Optional private space is enabled in Settings and hidden by default. It supports viewing, minutes, optional frequency/urges, triggers, responsibility/sleep effects and alternative activities without diagnoses or sexual shame. Private habit records can be deleted independently.
- Forms commit through their Save action; changes are durably saved by Room/DataStore before the repository reports success. Navigation state and open, unsubmitted forms are not a substitute for saving a record.

## Storage, privacy and backups

Room is the source of truth for tasks, histories, activities, schedules, planner items, challenge days, the active timer, journals and private habits. DataStore stores settings. A transactional marker seeds the plan once; deleting a preloaded task does not cause it to reappear. Schema export is enabled and destructive migrations are not enabled. Future schema changes must add a tested Room migration.

Private habits and journals use AES-256-GCM with an Android Keystore key. Their date and content are inside the encrypted payload, authenticated against the record kind/ID. The entire app blocks screenshots, screen recordings and recent-app previews using `FLAG_SECURE`. The device lock and Android's app sandbox remain important: this is not a separate password-locked vault. No private data appears in notifications or public dashboard rollups. Android automatic cloud backup/device transfer is disabled.

Settings provides Android file-picker JSON export and restore. Ordinary exports exclude private habits, journals and sensitive drafts. Sensitive export requires explicit consent **and a 12+ character passphrase**. Optional passphrases also encrypt ordinary backups. Encryption is compatible with the website's PBKDF2-SHA256 (600,000 iterations) / AES-GCM-256 envelope. Losing a passphrase means losing that encrypted backup's contents.

Restore validates the whole file before a Room transaction. It adds missing IDs and replaces only untouched bundled defaults; existing user edits/history win conflicts. Importing twice does not duplicate IDs. It reads the website's version-1 plain or encrypted JSON exports; sensitive import requires consent and reencrypts records with this phone's Keystore key. Web form drafts are retained for round-trip export, not displayed as native submitted records. Active timers are device-local and are not backed up; completed focus logs are included. Restored privacy/notification settings are disabled until explicitly reenabled.

**Laptop and phone do not sync. Clearing app storage, uninstalling, losing the phone or losing its encryption key can permanently erase local records.** Keep exported backups outside app storage. Choose a local destination in Android's file picker if you do not want to use a file provider backed by cloud storage.

Task reminders are **regular app notifications**, with no alarm-clock interface or next-alarm indicator. Android AlarmManager schedules their delivery internally using `setExactAndAllowWhileIdle` when **Alarms & reminders** access is enabled. Enable notification permission and this scheduling access; Settings links to each channel's sound and popup controls. Every timed active task notifies at its beginning and ending. A separate reminder adds an extra notification. Overnight endings use the next date. Starting notifications use the user's `todo-list-starting-popup.wav`; ending notifications use `todo-list-ending-popup.mp3`. Both are bundled locally in separate high-importance channels, with no file access or internet needed on the phone. Check-ins use the starting sound. A rolling eight-day window is refreshed daily and after boot, clock/timezone changes and updates. Without precise access, delivery falls back to potentially delayed scheduling. Completed, Missed, Skipped and Previous plan tasks do not alert. Lock-screen previews are generic; unlocked notifications show the task name. Private habit records never supply notification content.

Silent mode, Do Not Disturb, muted notification channels and device policies can suppress sounds or popups. Android may play only one sound for simultaneous ending/starting notifications. Deep idle can delay closely spaced notifications because Android limits idle-allowed scheduling. Force-stop suspends reminders until the app is reopened. Focus completion uses WorkManager and can be delayed by Android battery policies. This update enables task reminders once; you can disable them in Settings.

## Tests and technical notes

See [architecture](ARCHITECTURE.md) and [verification](VERIFICATION.md). JVM tests cover task transitions, deadlines, validation, challenge streaks, timer math and encrypted backups. Device tests cover plan idempotence and date coverage, Room reopen persistence, import conflict preservation, timer logging idempotence, encryption/export privacy and recurring tasks. Run `connectedDebugAndroidTest` only against a device you authorize for development testing.
