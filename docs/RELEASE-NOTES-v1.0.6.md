# A18 Discipline OS v1.0.6 — Debug APK

Native Kotlin / Jetpack Compose Android app. Android 8.0 / API 26 or newer.
This is a debug-signed personal testing build, not a production-signed release.

## Included

- Material 3 interface with original A18 branding and a native launch/loading screen.
- To-do list selected on a fresh launch, with editable tasks and saved history.
- Predefined schedule for 19 October 2026 through 18 February 2027.
- Academic and coding planners, focus timer, activity logs and reflections.
- Local Room/DataStore persistence and consent-based encrypted backup support.
- Separate custom starting and ending notification sounds.

Download `A18-Discipline-OS.apk` and verify it against the attached SHA-256 file.
For updates, preserve the original signing identity; back up before changing keys.
No private signing key, device database or personal tracking export is included.

## Known limits

Notification sounds respect Silent/DND/channel settings and exclusive audio focus.
Android can delay closely spaced notifications in deep idle or play one sound for
simultaneous task boundaries. Intermittent silence while other apps were open was
reported; subsequent background and foreground-app tests played both sounds, but
the original suppression was not conclusively resolved. No always-audible claim
is made. Clearing storage or uninstalling can erase local records; keep backups.
Laptop and phone data do not synchronize. See `docs/VERIFICATION.md` for test scope.
