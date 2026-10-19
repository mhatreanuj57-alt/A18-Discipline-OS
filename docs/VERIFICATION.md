# Verification — version 1.0.6, 19 October 2026

The native Kotlin/Compose debug APK was compiled, signature-verified and installed
with `adb install -r` on the connected **Samsung SM-A146B / Android 15**. The app
was not uninstalled and its storage was not cleared. The website remains unchanged.

Version 1.0.6 adds a branded native splash/starting window and a real-state
Material loading screen. Fresh launches default to To-do list. Initialization
failures replace the spinner with a retry that reinitializes and restarts snapshot
observation without clearing records. There is no artificial delay. Saved navigation
survives rotation and backgrounding. Version 1.0.5's generated A18 logo is retained;
its prompt and provenance are in [LOGO.md](LOGO.md).

The startup update passed 13 JVM tests and five targeted Samsung UI tests (four
navigation/default-tab checks and one loading/error/retry check). Build, lint,
signature and in-place Samsung installation passed. Existing persistence, privacy
and notification device results below remain the 1.0.4 baseline; those unrelated
tests were not repeated for this startup change. The final resource-only compatibility
fix moved light-navigation-bar styling to API 27+ resources; Android 15 behavior
stays the same. No screenshots of private content were taken.

## Artifact

`releases/A18-Discipline-OS.apk`

- Package: `com.a18.discipline`; version 1.0.6 / versionCode 7.
- Size: 20,078,189 bytes; Android 8.0+ / API 26; compile/target API 35.
- SHA-256: `c310256d30355660ac9d6c77708757d86012ee82c0d17565431fd757856af977`.
- APK Signature Scheme v2 verified, one debug signer.
- A copy is in the phone's `Download/A18-Discipline-OS.apk`.
- Personal signed-release instructions are in the README. APK binaries are ignored by Git.

## Results

| Check | Result |
|---|---|
| Gradle assembleDebug / assembleDebugAndroidTest | Passed, offline build |
| JVM tests (1.0.6) | 13 passed, 0 failed |
| Startup/navigation/retry device tests (1.0.6) | 5 passed, 0 failed |
| Connected device tests (1.0.4 baseline) | 11-test suite passed, plus new Material navigation test passed separately: 12 distinct tests |
| Process restart persistence (1.0.4 baseline) | Separate write, force-stop and verify processes passed; temporary record removed |
| Android lint | Passed, no errors |
| APK signature and packaged plan inspection | Passed |
| Bundled start/end audio | Byte-identical to the user's WAV and MP3 |
| Permissions inspection | No INTERNET permission |
| Website Git status | Clean; no web files modified |

Build: Gradle 8.11.1, AGP 8.9.2, Kotlin 2.1.20, JDK 21, Android SDK 35.
Cached dependencies and project-local tooling were used with Gradle `--offline`.

## Schedule, history and privacy

- The packaged asset contains **856 unique tasks across every one of the 123 dates
  from 19 October 2026 through 18 February 2027**, 1,386 schedule blocks, 18 review
  items and 30 challenge days. No tasks are pre-completed. No defaults are seeded
  for 17–18 October.
- 19 October has Assignment Work 20:00–21:45 and GitHub Work 21:45–23:00.
  Starting/ending notifications and the two requested check-ins give six events:
  20:00, 21:40, 21:45 (two events), 22:55 and 23:00.
- All subsequent dates contain the user's exact seven evening slots, each with a
  starting and ending event. Unit tests cover filtering past events, archived and
  terminal tasks, key uniqueness, postponement, rolling horizon and overnight endings.
- Transactional migration tests preserve previous task status/history/actual minutes,
  notes, user-created tasks, linked blocks and unchanged encrypted private rows.
  Old in-range responsibilities move to Previous plan; initialization does not duplicate
  tasks. Retired 17–18 October cleanup preserves edited/used records.
- Room reopening, import conflicts, recurrence, timer logging idempotence, Keystore
  encryption, private-export exclusion and password-encrypted backups pass their tests.
- No private records were captured in screenshots or read for notification diagnostics.
  Public project labels stay generic. Sensitive logs and journals remain encrypted.

## Notifications and Material interface

- The user granted precise scheduling access; Android app-ops reported `allow`.
- Real start and end deliveries each posted exactly one notification using the correct
  separate HIGH-importance channel, custom resource sound URI and readable sound file.
  Duplicate receiver delivery did not duplicate a notification.
- The user reported no sound during the first automated run. DND was off, notification
  volume was nonzero, and both channel URIs were correct. A separate probe scheduled
  notifications after instrumentation exited, with the phone on its home screen.
  Android diagnostics showed both notifications with the correct start/end sound URIs,
  HIGH importance and no interception or suppressed visual effects. **The user then
  confirmed hearing both sounds.** The temporary task was removed. Starting another
  instrumentation process clears active notifications, so background delivery evidence
  was inspected before reopening instrumentation rather than asserted after that restart.
- Notifications use regular app channels and `setExactAndAllowWhileIdle`; no
  `setAlarmClock`, full-screen alarm, or alarm-clock interface remains in source.
- Material 3 dynamic colors, system appearance, tonal cards, typography and native
  components replace the custom charcoal/lime theme. Four rounded Google vector icons
  come from the user's icon folder, with Apache 2.0 license retained.
- Device UI checks cover the dashboard plan shortcut, daily list, date selection,
  calendar, study/project navigation, focus, challenge, analytics, bottom navigation,
  More drawer, and both sound-settings controls.

## Limits

No physical phone reboot, long-duration deep-idle test, full manual accessibility audit,
or tablet visual audit was performed. The file picker's complete interactive backup
flow was not automated; repository and encryption paths were tested. Phone radios were
left unchanged; no airplane-mode claim is made. The app has no internet permission.

Silent/DND/channel settings can mute notifications. Simultaneous ending and starting
notifications may produce only one sound. Android deep-idle quotas can delay closely
spaced notifications; force-stop suspends them until reopening. Clearing storage,
uninstalling or losing the Keystore key can erase local records. Keep manual backups.
Laptop and phone do not synchronize. Forms require Save.


Public packaging verification: source and plan bytes match the tested app; the APK checksum is unchanged. Development caches, previous Git history and all device data were excluded. Public docs use portable paths. The public repository starts with a separate publishing history.

The public checkout was subsequently compiled directly using the existing local
SDK and cached Gradle dependencies: `testDebugUnitTest`, `lintDebug` and
`assembleDebug` all passed, with 13 JVM tests and zero failures. Its SDK location
is saved only in ignored, untracked `local.properties`. The verifier was checked
against both protected and temporarily unprotected SDK configurations; only the
protected configuration is accepted. No machine-specific SDK path is committed.
The distributed APK remains the previously installed/tested 1.0.6 artifact with
the SHA-256 recorded above; it was not replaced by the fresh checkout build.
