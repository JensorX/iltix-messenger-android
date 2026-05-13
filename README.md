# Iltix Messenger Android

Iltix-specific overlay and patcher for [Element X Android](https://github.com/element-hq/element-x-android).

## Architecture

This repo contains **only** Iltix-specific code. Upstream Element X source is fetched at build time and patched automatically.

```
overlay/        → Additive Iltix files (modules, resources, flavors)
patcher/        → Kotlin-based patch engine (modifies upstream source)
scripts/        → Build helper scripts
iltix.upstream.properties  → Pinned upstream version
```

## Building

```bash
# Option 1: Run patcher script (requires JDK 21)
./scripts/setup-workspace.sh

# Option 2: Manual
cd patcher && ./gradlew jar
java -jar patcher/build/libs/iltix-patcher-1.0.0.jar --root .. --upstream-tag v26.05.0
cd ../workspace && ./gradlew :app:assembleGplayIxDebug
```

## CI

- **Iltix Build** workflow: Runs on push to `main` or manual dispatch. Produces debug + release APKs.
- **Upstream Watch** workflow: Daily check for new Element X tags, creates an issue when update is available.

## Lizenzierung

Dieses Projekt modifiziert [Element X Android](https://github.com/element-hq/element-x-android) (AGPL-3.0-only).

Element X & Iltix Patches/Overlays unterliegen beide der **AGPL-3.0-only Lizenz** (siehe [LICENSE](./LICENSE)).  
Bei Distribution muss der Quellcode verfügbar sein (automatisch durch Patcher-System erfüllt).
