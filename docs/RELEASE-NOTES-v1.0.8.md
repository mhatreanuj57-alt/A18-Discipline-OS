# Daymark 1.0.8

A18 Discipline OS is now named **Daymark**. The launcher, startup screen,
navigation header, notification labels and README use a new sunrise-and-planner
logo and the Daymark name. The icon uses a soft lavender background with indigo,
mint and amber artwork.

The Android application ID (`com.a18.discipline`), Room database, DataStore
preferences, encryption label, JSON backup identifier and signing key remain
unchanged. Install 1.0.8 over the existing app with `adb install -r` to keep local
data and installation permissions. The four-month task plan and the task Start / Done
controls remain intact.

This remains a debug-signed APK for personal use. Device tests that uninstall the
app were not run against the phone, to protect local records.

## Verification

Offline Gradle 8.11.1 / JDK 21 build: testDebugUnitTest, lintDebug and assembleDebug succeeded. All 16 JVM tests passed. The instrumentation UI tests compiled successfully but were not run because Gradle uninstalls the app after connected instrumentation tests. APK SHA-256:
72d4ee0d634e4db67a1c83ca252997ed115f3dcd9d0329c14683ff72a3d549d1
The connected Samsung was updated with adb install -r; package version 1.0.8 / code 9 was confirmed. Its Room database file was present before and after the update at 1,200,128 bytes. No device reboot or reminder-sound retest was performed.
