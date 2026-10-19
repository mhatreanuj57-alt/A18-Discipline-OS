#requires -Version 5.1
param([string]$ProjectRoot = (Split-Path $PSScriptRoot -Parent))
$ErrorActionPreference = 'Stop'
$root = (Resolve-Path -LiteralPath $ProjectRoot).Path.TrimEnd('\')
function Relative-Name($file) { $file.FullName.Substring($root.Length).TrimStart('\').Replace('\','/') }
if (!(Test-Path -LiteralPath (Join-Path $root 'releases/A18-Discipline-OS.apk'))) { throw 'Open the prepared public project folder.' }
$files = @(Get-ChildItem -LiteralPath $root -File -Force -Recurse | Where-Object {
    (Relative-Name $_) -notmatch '(^|/)(\.git|\.gradle|\.kotlin|\.tooling|\.idea|\.vscode|build)/'
})
$secretPattern = 'gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{30,}|AKIA[0-9A-Z]{16}|sk-proj-[A-Za-z0-9_-]{20,}|-----BEGIN (?:RSA |EC |OPENSSH )?PRIVATE KEY-----'
foreach ($file in $files) {
    $name = Relative-Name $file
    if ($name -match '(^|/)(local\.properties|signing\.properties|credentials\.json|google-services\.json|\.env(?:\..*)?)$|\.(jks|keystore|p12|pfx|pem|key|db|sqlite|sqlite3)(?:-|$)|(^|/)(databases|shared_prefs|datastore|backups|exports|private-data)/') {
        throw "Private/local file must not be published: $name"
    }
    if ($file.Extension -eq '.json' -and $name -notmatch '^app/(src/main/assets/(plan|retired-weekend-v1)\.json|schemas/[^/]+/\d+\.json)$') {
        throw "Unreviewed JSON file could be a user export: $name"
    }
    if ($file.Extension -eq '.apk' -and $name -ne 'releases/A18-Discipline-OS.apk') { throw "Unexpected APK: $name" }
    if ($file.Extension -in @('.kt','.kts','.xml','.md','.json','.properties','.ps1','.svg') -or $file.Name -in @('.gitignore','.gitattributes','gradlew','gradlew.bat')) {
        $content = [System.IO.File]::ReadAllText($file.FullName)
        if ($content -match $secretPattern) { throw "Possible credential in $name; review it locally without sharing the value." }
    }
}
$apk = Join-Path $root 'releases/A18-Discipline-OS.apk'
$checksum = Join-Path $root 'releases/A18-Discipline-OS.apk.sha256'
$expected = ([System.IO.File]::ReadAllText($checksum).Trim() -split '\s+')[0]
if ($expected -notmatch '^[a-fA-F0-9]{64}$') { throw 'Invalid checksum file.' }
$actual = (Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash
if ($actual -ne $expected) { throw 'APK checksum mismatch.' }
Add-Type -AssemblyName System.IO.Compression.FileSystem
$archive = [System.IO.Compression.ZipFile]::OpenRead($apk)
try {
    if (@($archive.Entries | Where-Object { $_.FullName -match '(^|/)(databases|shared_prefs|datastore)/|\.(db|sqlite|sqlite3)(?:-|$)' }).Count -gt 0) { throw 'Unexpected device storage inside APK.' }
    $entry = $archive.GetEntry('assets/plan.json')
    if ($null -eq $entry) { throw 'APK plan is missing.' }
    $reader = New-Object System.IO.StreamReader($entry.Open())
    try { $plan = $reader.ReadToEnd() | ConvertFrom-Json } finally { $reader.Dispose() }
    foreach ($field in @('logs','privateLogs','journals','drafts')) {
        $value = $plan.$field
        if ($null -ne $value -and @($value).Count -gt 0) { throw "APK seed contains non-plan records: $field" }
    }
    $tasks = @($plan.tasks)
    if ($tasks.Count -ne 856 -or @($tasks.id | Sort-Object -Unique).Count -ne 856) { throw 'Unexpected plan count or duplicate task IDs.' }
    $dates = @($tasks.due | Sort-Object -Unique)
    if ($dates.Count -ne 123 -or $dates[0] -ne '2026-10-19' -or $dates[-1] -ne '2027-02-18') { throw 'Unexpected plan dates.' }
    if (@($tasks | Where-Object { $_.status -ne 'Not Started' -or $_.actual -ne 0 -or @($_.history).Count -gt 0 -or $null -ne $_.completed }).Count -gt 0) { throw 'APK seed must not contain saved personal outcomes.' }
} finally { $archive.Dispose() }
Write-Host "PASS: $($files.Count) public-package files reviewed."
Write-Host 'PASS: APK checksum, pristine plan, date coverage and unique task IDs.'
Write-Host 'PASS: No detected device records, local credentials or signing material.'
Write-Host 'This is a local safeguard, not an exhaustive secret/security audit. Review staged changes before pushing.'
