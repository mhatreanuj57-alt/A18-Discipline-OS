# Publish in 12 batches: commit and push each batch

Account: **mhatreanuj57-alt**. Repository: **A18-Discipline-OS**, public.

Use the same VS Code PowerShell terminal for all batches. Stop if any command
fails. If you already created the remote repository, skip only the first
`gh repo create` command. No login is needed. These commands have not been run
for you. Batch 1 publishes the first commit; every later batch commits and pushes
another part of the project. Batch 12 also publishes the APK as a GitHub pre-release.

## Batch 1 — Create the repository, commit README and push

Open this prepared public project folder in VS Code before running these commands.

```powershell
gh repo create mhatreanuj57-alt/A18-Discipline-OS --public --description "Native Android productivity app with Material 3 and local storage"
git init -b main
$Account = gh api user | ConvertFrom-Json
if ($Account.login -ne 'mhatreanuj57-alt') { throw 'Switch to mhatreanuj57-alt first.' }
git config --local user.name 'mhatreanuj57-alt'
git config --local user.email "$($Account.id)+mhatreanuj57-alt@users.noreply.github.com"
gh auth setup-git
git remote add origin https://github.com/mhatreanuj57-alt/A18-Discipline-OS.git
powershell -NoProfile -ExecutionPolicy Bypass -File .\tools\Verify-PublicPackage.ps1
if ($LASTEXITCODE -ne 0) { throw 'Package verification failed. Stop here.' }
git add .gitignore .gitattributes README.md
git commit -m "docs: add project README and public-file safeguards"
git push -u origin main
```

## Batch 2 — Material branding

```powershell
git add assets DESIGN.md
git commit -m "design: add Material 3 branding and A18 logo"
git push
```

## Batch 3 — Documentation and release notes

```powershell
git add docs
git commit -m "docs: add build, architecture, verification and release guides"
git push
```

## Batch 4 — Android build configuration

```powershell
git add build.gradle.kts settings.gradle.kts gradle.properties gradlew gradlew.bat gradle app/build.gradle.kts
git update-index --chmod=+x gradlew
git commit -m "build: add Android Gradle configuration and wrapper"
git push
```

## Batch 5 — Local data layer

```powershell
git add app/src/main/java/com/a18/discipline/data app/schemas
git commit -m "feat: add Room storage, models and encrypted backup handling"
git push
```

## Batch 6 — Application and reminders

```powershell
git add app/src/main/AndroidManifest.xml app/src/main/java/com/a18/discipline/DisciplineApp.kt app/src/main/java/com/a18/discipline/DisciplineViewModel.kt app/src/main/java/com/a18/discipline/MainActivity.kt app/src/main/java/com/a18/discipline/Reminders.kt
git commit -m "feat: add app navigation, state and task notifications"
git push
```

## Batch 7 — Native screens

```powershell
git add app/src/main/java/com/a18/discipline/ui
git commit -m "feat: add native Material 3 productivity screens"
git push
```

## Batch 8 — Icons, sounds and splash resources

```powershell
git add app/src/main/res
git commit -m "design: add app icon, notification sounds and launch theme"
git push
```

## Batch 9 — Predefined plans

```powershell
git add app/src/main/assets
git commit -m "feat: include four-month daily plan and safe migration reference"
git push
```

## Batch 10 — Automated tests

```powershell
git add app/src/test app/src/androidTest
git commit -m "test: add domain, persistence, notification and startup tests"
git push
```

## Batch 11 — Public-package verifier

```powershell
git add tools
git commit -m "tools: add local public-package verification"
git push
```

## Batch 12 — APK commit, push and GitHub Release

The APK is debug-signed, so this is explicitly labeled as a testing pre-release.
It is also committed as a regular repository file under `releases/`.

```powershell
git add releases/A18-Discipline-OS.apk releases/A18-Discipline-OS.apk.sha256
git commit -m "release: include Android debug APK v1.0.6 and checksum"
git push
if ($LASTEXITCODE -ne 0) { throw 'Push failed. Stop before creating the release.' }
if (git status --porcelain) { throw 'Uncommitted files remain. Review them first.' }
gh release create v1.0.6 releases/A18-Discipline-OS.apk releases/A18-Discipline-OS.apk.sha256 --repo mhatreanuj57-alt/A18-Discipline-OS --target main --title "A18 Discipline OS v1.0.6 - Debug APK" --notes-file docs/RELEASE-NOTES-v1.0.6.md --prerelease
```

Source, seeded schedule, docs and APK are public after pushing. Device records,
exports, credentials and signing keys must stay outside this repository. The
original app and website folders are unchanged. Intermediate upload batches do
not represent complete builds; all source/configuration files are present after
Batch 11, and the tested APK and release assets are published in Batch 12.
