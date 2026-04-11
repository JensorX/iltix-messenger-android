# Iltix-Specific Conventions

Iltix is a modular fork of Element X focused on minimal upstream merge conflicts.

## Module Placement Rules

**Iltix code must minimize touch on Element's source files.**

1. **`iltix/lib/` module** - Core non-UI code (preferences, data, utilities).
   - Package: `de.iltix.lib.*`
   - Dependencies: AndroidX, Coroutines, Room (no Element modules except strings).
   - Use for: Preference system, local databases, business logic.

2. **`iltix/components/` module** - Reusable Compose UI components.
   - Package: `de.iltix.components.*`
   - Dependencies: Iltix.lib, Element's `libraries/designsystem`, `libraries/compound`.
   - Use for: Custom UI components (badges, pickers, bubble shapes, etc.).

3. **`iltix/theme/` module** - Theme and dynamic color system.
   - Package: `de.iltix.theme.*`
   - Dependencies: Iltix.components, `libraries/compound`, Material3.
   - Use for: Honey palette fallback, Material You color scheme integration.

4. **Feature-specific Iltix code in `features/*/impl/src/main/kotlin/de/iltix/`** - Integration points.
   - Keep feature integrations small and gated by `IxPrefs` checks.
   - Example: `features/home/impl/src/main/kotlin/de/iltix/components/IxUnreadBadge.kt`.

## Build Flavor Strategy

- **`ix` app flavor** - Distinguished by `applicationId.iltix` suffix.
- **`ix` source sets** - Resource overrides (drawables, layouts, animations) go in `src/ix/res/`, not `src/main/res/`.
- **Pattern**: Similar to Enterprise flavor detection in `plugins/Enterprise.kt` - check file existence to determine build.
- **Example**: In `libraries/designsystem/build.gradle.kts`:
  ```gradle
  sourceSets {
      getByName("ix") {
          res.srcDirs("src/ix/res")
      }
  }
  ```

## String Handling

- **All Iltix UI strings** -> `iltix/lib/src/main/res/values/strings.xml`.
- **Package-specific strings**: `de.iltix.lib.R.string.iltix_*`.
- **Never edit upstream string files** - If Element's strings need changes, document as tech debt or patch via script.
- **Preference titles/subtitles**: Use `@StringRes` in `IxPref` definitions (`IxPrefs.kt`).

## Feature Toggles & Iltix Modules

- **All Iltix features must be toggleable** via `IxPrefs` object.
- **Default to `true`** (enabled by default in Iltix).
- **Settings screen**: "Iltix Modules" entry in Preferences -> Iltix settings UI.
- **Integration pattern**: Check `IxPrefs.<FEATURE>.getThenIfEnabled()` or similar before activating Iltix behavior:
  ```kotlin
  if (IxPrefs.SPACE_NAV.defaultValue) {
      IxSpaceNavBar()
  }
  ```

## Minimal Integration Points in Element Code

When Iltix features require changes to Element's code, keep changes minimal:

1. **Add conditional checks**:
   ```kotlin
   if (IxPrefs.NUMBER_BADGE.defaultValue) {
       IxUnreadBadge(count = summary.numberOfUnreadMessages)
   } else {
       UnreadIndicatorAtom()  // upstream
   }
   ```

2. **Add callback parameters** (not new properties):
   ```kotlin
   // Add to function signature:
   onOpenIltixModules: () -> Unit = {},
   // Use in event handler:
   onOpenIltixModules()
   ```

3. **Use DI** to provide Iltix services:
   ```kotlin
   @Inject private val preferencesStore: IxPreferencesStore
   ```

## Merge Workflow

1. **Before upstream merge**: Tag current version as `iltix_<version>`.
2. **After conflict resolution**: Verify all Element files remain unchanged (except minimal integration points).
3. **Fallback strategy**: If conflicts arise, revert Iltix changes in Element files and re-apply them post-merge.
4. **Script helpers**: Future `merge_helpers.sh` or `fix_merge.sh` can automate conflict resolution.

## Naming Conventions

- **Iltix classes**: Prefix with `Ix` (e.g., `IxUnreadBadge`, `IxSpaceNavBar`, `IxEmojiKeyboard`).
- **Iltix packages**: `de.iltix.*` for all Kotlin code.
- **Feature names** in logs/comments: "Iltix Modules" or "Iltix <Feature>" (e.g., "Iltix Space Navigation").

## Testing

- **No Iltix-specific test conventions yet** - Follow Element's Turbine + Presenter patterns.
- **Future**: Add preview providers for all Iltix UI components (`IxBadgeStateProvider`, etc.).
