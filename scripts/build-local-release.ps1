<#
build-local-release.ps1

Erstellt Release-APK/AAB lokal für das iltix‑Messenger Android Projekt
- wendet optionale Patches an (wenn vorhanden)
- erzeugt optional einen lokalen Keystore
- baut unsigned oder signiert per Gradle-injected signing

Usage (PowerShell):
  # einfacher unsigned Release-Build
  .\scripts\build-local-release.ps1 -ProjectPath 'C:\Users\Fabian\iltix-messenger-android'

  # Keystore erzeugen und signiert bauen + Bundle
  .\scripts\build-local-release.ps1 -ProjectPath 'C:\Users\Fabian\iltix-messenger-android' -CreateKeystore -Sign -Bundle

Parameter:
  -ProjectPath      Pfad zum Android‑Projekt (Standard: aktuelles Verzeichnis)
  -CreateKeystore   Erzeugt lokal einen JKS Keystore, falls keiner existiert
  -KeystorePath     Pfad zur Keystore-Datei (Standard: local-release-keystore.jks im Arbeitsverzeichnis)
  -KeystorePassword Passwort für Keystore (Default: changeme) - nur lokal für Tests
  -KeyAlias         Alias im Keystore (Default: iltix_local)
  -KeyPassword      Schlüsselpasswort (Default: gleich KeystorePassword)
  -Sign             Versuche, das Release mit dem Keystore per Gradle zu signieren
  -Bundle           Erzeuge ein AAB (`bundleRelease`) statt APK
  -Clean            Führt `clean` vor dem Build aus
  -PatchesPath      Ordner mit *.patch Dateien, die vor dem Build angewendet werden (relativ zu ProjektPath)
#>

param(
    [string]$ProjectPath = ".",
    [switch]$CreateKeystore,
    [string]$KeystorePath = "local-release-keystore.jks",
    [string]$KeystorePassword = "changeme",
    [string]$KeyAlias = "iltix_local",
    [string]$KeyPassword = "",
    [switch]$Sign,
    [switch]$Bundle,
    [switch]$Clean,
    [string]$PatchesPath = "patches"
)

function Write-Log { param($m) Write-Host "[build-local] $m" }

# normalize
if (-not $KeyPassword) { $KeyPassword = $KeystorePassword }
try { $ProjectFull = (Resolve-Path -Path $ProjectPath).ProviderPath } catch { Throw "Projektpfad nicht gefunden: $ProjectPath" }
Write-Log "Projekt: $ProjectFull"

# find gradle wrapper
$gradleWrapper = $null
if (Test-Path (Join-Path $ProjectFull 'gradlew.bat')) { $gradleWrapper = Join-Path $ProjectFull 'gradlew.bat' }
elseif (Test-Path (Join-Path $ProjectFull 'gradlew')) { $gradleWrapper = Join-Path $ProjectFull 'gradlew' }
else {
    $g = Get-Command gradle -ErrorAction SilentlyContinue
    if ($g) { $gradleWrapper = 'gradle' } else { Throw "gradle wrapper nicht gefunden (keine 'gradlew' im Projekt)." }
}
Write-Log "Gradle: $gradleWrapper"

# optional: apply patches
$patchDirCandidate = Join-Path $ProjectFull $PatchesPath
if (Test-Path $patchDirCandidate) {
    Write-Log "Patches gefunden in: $patchDirCandidate"
    Push-Location $ProjectFull
    try {
        $patchFiles = Get-ChildItem -Path $patchDirCandidate -Filter '*.patch' -File -ErrorAction SilentlyContinue | Sort-Object Name
        foreach ($pf in $patchFiles) {
            Write-Log "Wende Patch an: $($pf.Name)"
            # prefer git am, fallback to git apply
            if (Test-Path .git) {
                # try git am via stdin
                try {
                    Get-Content -Raw $pf.FullName | & git am --3way
                    if ($LASTEXITCODE -ne 0) { throw "git am fehlgeschlagen" }
                    Write-Log "git am ok: $($pf.Name)"
                } catch {
                    Write-Log "git am fehlgeschlagen, versuche git apply"
                    if (& git apply --index $pf.FullName) {
                        & git commit -m "Apply iltix patch: $($pf.Name)" --no-verify 2>$null
                        Write-Log "git apply + commit ok: $($pf.Name)"
                    } else {
                        Write-Log "Patch konnte nicht angewendet werden: $($pf.Name)"
                    }
                }
            } else {
                Write-Log "Kein git repository gefunden; überspringe Patch: $($pf.Name)"
            }
        }
    } finally { Pop-Location }
} else {
    Write-Log "Kein Patch‑Ordner gefunden ($patchDirCandidate) — überspringe Patch‑Anwendung"
}

# optionally create keystore
$KeystoreFull = $KeystorePath
if (-not ([System.IO.Path]::IsPathRooted($KeystoreFull))) { $KeystoreFull = Join-Path $ProjectFull $KeystorePath }
if ($CreateKeystore -or ($Sign -and -not (Test-Path $KeystoreFull))) {
    Write-Log "Erzeuge lokalen Keystore: $KeystoreFull"
    $keytool = Get-Command keytool -ErrorAction SilentlyContinue
    if (-not $keytool) { Throw "keytool (JDK) nicht gefunden. Bitte JDK installieren." }
    $dname = 'CN=iltix'
    & $keytool.Source -genkeypair -v -keystore $KeystoreFull -storepass $KeystorePassword -alias $KeyAlias -keyalg RSA -keysize 2048 -validity 10000 -dname $dname -keypass $KeyPassword -storetype JKS
    if ($LASTEXITCODE -ne 0) { Throw "Keystore-Erzeugung fehlgeschlagen" }
    Write-Log "Keystore erstellt"
} else {
    Write-Log "Keystore: $KeystoreFull (existiert=$(Test-Path $KeystoreFull))"
}

# build
Push-Location $ProjectFull
try {
    $gradleArgs = @()
    if ($Clean) { $gradleArgs += 'clean' }
    $task = if ($Bundle) { 'bundleRelease' } else { 'assembleRelease' }
    $gradleArgs += $task

    if ($Sign) {
        if (-not (Test-Path $KeystoreFull)) { Throw "Keystore nicht gefunden: $KeystoreFull — kann nicht signieren" }
        $gradleArgs += "-Pandroid.injected.signing.store.file=$KeystoreFull"
        $gradleArgs += "-Pandroid.injected.signing.store.password=$KeystorePassword"
        $gradleArgs += "-Pandroid.injected.signing.key.alias=$KeyAlias"
        $gradleArgs += "-Pandroid.injected.signing.key.password=$KeyPassword"
        Write-Log "Starte Gradle Build mit Signing injection: $task"
    } else {
        Write-Log "Starte Gradle Build (unsigned): $task"
    }

    # execute
    $startInfo = @($gradleArgs)
    & $gradleWrapper @startInfo
    if ($LASTEXITCODE -ne 0) { Throw "Gradle Build fehlgeschlagen (ExitCode=$LASTEXITCODE)" }

    # find artifact
    if ($Bundle) {
        $aab = Get-ChildItem -Path (Join-Path $ProjectFull 'app\build\outputs\bundle\release') -Filter '*.aab' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($aab) { Write-Log "AAB erzeugt: $($aab.FullName)" } else { Write-Log "Keine AAB gefunden." }
    } else {
        $apk = Get-ChildItem -Path (Join-Path $ProjectFull 'app\build\outputs\apk\release') -Filter '*.apk' -File -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1
        if ($apk) { Write-Log "APK erzeugt: $($apk.FullName)" } else { Write-Log "Keine APK gefunden." }
    }

} finally { Pop-Location }

Write-Log "Fertig. Prüfe obenstehende Meldungen für Pfade und Fehler." 

# EOF
