# Onboarding Package

## Responsibility

Owns the **first-launch wizard** in JavaFX: choose on-device LLM profile and UI language, collect emergency profile data, and **overlap** that work with **bootstrap provisioning** (downloads, engine init, warmup) so onboarding doubles as a loading experience.

Pairing with **`OnboardingLanguageCatalog`** (combo labels + fuzzy resolution) and **`UiLocales`** (label → `Locale` for `ResourceBundle`).

## Structure

```
onboarding/
├── OnboardingController.java    # FXML controller — steps, overlay, provisioning triggers
├── OnboardingLanguageCatalog.java  # Supported language labels + matchOrDefault / aliases
└── _onboarding.spec.md            # This document
```

FXML and styles live under `src/main/resources` (e.g. `onboarding-view.fxml`), not in this package.

## Classes

### `OnboardingController`

Binds to three main containers: **step 1**, **step 2**, **installation overlay**. Uses `managedProperty().bind(visibleProperty())` on those `VBox` nodes so hidden panes do not reserve layout space.

| Concern | Behavior |
|---|---|
| **Step 1** | `ComboBox<SystemCapability.AIModelProfile>` with custom cells (localized name, optional “Recommended” tag from `SystemCapability`, size in GB). `ComboBox<String>` of `OnboardingLanguageCatalog.SUPPORTED` for language. |
| **Step 2** | Name, gender (`GenderOption` enum → persisted English string + i18n display key), birth date, emergency contact, medical notes. |
| **Validation** | On Finish: name and contact must be non-empty; otherwise show `onboarding.error.name_contact`. |
| **i18n** | `languageComboBox` listener → `UiLocales.fromPreferredLanguage` → `LocalizationService.setLocale`. `applyOnboardingFormI18n()` refreshes all `onboarding.*` labels and rebuilds combo cell factories so model and gender cells stay translated. |
| **Bootstrap UI** | `bootStatusLabel` / `bootProgress` bound to `BootstrapService` observable properties. |

**Provisioning and persistence**

| Event | Action |
|---|---|
| User advances to step 2 (`goToStep2` / draft restore) | `triggerProvisioning()`: saves a **draft** `UserData` (placeholder name, `devicePhone`, selected model + language) via `ProfileRepository`, then `bootstrap.startProvisioning(profile, language)`. |
| User clicks Finish (`handleStart`) | Calls `triggerProvisioning()` again (idempotent for same profile\|lang); saves **complete** `UserData`; shows installation overlay; `mainApp.completeOnboarding()`. |

**Background threads** (daemon)

- `detectLocation`: `SystemGPSLocation` — on FX thread updates location line and `selectLanguageSafely` from `detectPrimaryLanguage()`.
- `detectDevicePhone`: `DevicePhoneDetector` — caches number for profile save (`UNKNOWN` if missing).

**Draft restore**

`restoreDraftIfExists()` loads profile; if **not** `isComplete()`, applies model/language/gender from stored values and `advanceToStep2()` so the user continues where they left off.

### `OnboardingLanguageCatalog`

| Member | Role |
|---|---|
| `DEFAULT` | `"English"` — fallback for unknown input. |
| `SUPPORTED` | Immutable list of English display names for the language combo (order = UI order). Includes: English, Spanish, French, German, Italian, Portuguese, Romanian, Valencian, Chinese, Russian, Arabic, Japanese. |
| `ALIASES` | Lowercase keys → canonical label (ISO codes, localized names, etc.). |
| `matchOrDefault(String)` | Resolves to a label in `SUPPORTED` or `DEFAULT`; see Javadoc for matching order. |

**Alias coverage (summary)** — codes and names for: `en`, `es`, `fr`, `de`, `it`, `pt`, `ro`, `ca` / Valencian–Catalan variants, `zh`, `ru`, `ar`, `ja`, plus common written forms (e.g. `español`, `中文`, `العربية`).

## How the pieces connect

```
MainApp
   └── loads FXML → OnboardingController.init(bootstrap, profiles, mainApp)

OnboardingController
   ├── OnboardingLanguageCatalog.matchOrDefault(...)   ← GPS, draft, profile strings
   ├── UiLocales.fromPreferredLanguage(...)            → LocalizationService
   ├── ProfileRepository                               → ~/.soteria/profile.json (draft / complete)
   └── BootstrapService.startProvisioning(profile, lang)
              └── ProvisioningManager + BootstrapState (status/progress for UI)
```

## Design decisions

**Why save a draft when entering step 2?** Provisioning needs the chosen model and language as soon as the user leaves step 1; persisting a partial `UserData` keeps profile storage consistent if the app exits mid-onboarding.

**Why re-call `triggerProvisioning` on Finish?** Ensures provisioning ran at least once with the final selections; duplicate runs with the same key are handled inside bootstrap as no-ops.

**Why English canonical labels in `SUPPORTED`?** Stable persistence and combo values independent of current `Locale`; the UI translates field labels via `LocalizationService`, not the stored language identifier string.
