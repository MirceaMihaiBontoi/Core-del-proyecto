# FXML resources (`src/main/resources/fxml`)

## Responsibility

Declarative **JavaFX scene graphs** for SoterIA: controllers are wired via `fx:controller`, interactive nodes use `fx:id` for injection in Java, copy uses **`%key`** lookups against `classpath:i18n/messages*.properties` (see `_i18n.spec.md`). Styles come from AtlantaFX **Primer Dark** plus `/styles/main.css` (see `_ui.spec.md`).

Behavior and threading live in **`ChatController`** / **`OnboardingController`** — this folder documents **layout and bindings to controller API surface** only.

## Structure

```
fxml/
├── chat-view.fxml        # Post-onboarding HUD (StackPane shell)
├── onboarding-view.fxml  # First-run wizard (VBox + stacked steps)
└── _fxml.spec.md         # This document
```

---

## `chat-view.fxml`

**Controller:** `com.soteria.ui.chat.ChatController`  
**Root:** `StackPane` (`styleClass="voice-shell"`)

Stack order (back → front): main **voice shell** `VBox`, then **chat sheet** (bottom), **history sidebar** (left), **settings overlay** (center). Overlays use `visible="false"` + `managed="false"` until opened; the controller may also toggle `managed` for consistency with UX.

### Voice shell (`VBox`)

| Block | Role |
|---|---|
| **Top bar** (`top-bar`) | History, brand, spacer, **status pill** (dot + AI label + TTS toggle) |
| **Center** (`voice-center`) | Optional **safety** panel (`safetyContainer`), **`faceHolder`** (placeholder for programmatic `SoterIAFace`), subtitle, partial transcript |
| **FAB row** (`fab-row`) | SOS, mic, chat sheet toggle |

### Chat sheet (`chatSheet`)

Bottom-aligned sheet: grabber, header (title + close), `ScrollPane` → **`chatMessages`** column, input bar (`messageInput`, send). Closed by `#closeChatSheet`; opened from `#toggleChatSheet`.

### History sidebar (`historySidebar`)

Left panel: title, close (same handler as toggle), **Settings**, **New emergency** (new chat), scrollable **`sessionList`** (rows built in code).

### Settings overlay (`settingsOverlay`)

Center modal card: theme combo, language combo, speech rate slider + value label, AI model combo, wake-word checkbox. Close `#closeSettings`.

### `fx:id` → typical controller use

| `fx:id` | Notes |
|---|---|
| `historyButton`, `brandLabel`, `statusPill`, `statusDot`, `aiStatusLabel`, `ttsToggle`, `ttsIcon` | Top bar / pill chrome |
| `safetyContainer` | Children cleared/rebuilt for active protocol (`ChatSafetyProtocolBinder`) |
| `faceHolder` | `SoterIAFace` attached in `initialize()` |
| `subtitleLabel`, `partialTranscriptLabel` | Status and STT partials |
| `alertButton`, `micButton`, `micIcon`, `chatButton` | FAB actions |
| `chatSheet`, `chatSheetTitleLabel`, `chatScrollPane`, `chatMessages`, `messageInput`, `sendButton` | Conversation UI |
| `historySidebar`, `historyTitleLabel`, `sidebarSettingsButton`, `sidebarNewEmergencyButton`, `sessionList` | Session drawer |
| `settingsOverlay`, `settingsHeaderTitleLabel`, section labels, `settingsThemeCombo`, `settingsLanguageCombo`, `settingsSpeechRateSlider`, `settingsSpeechRateLabel`, `settingsModelCombo`, `settingsWakeToggle` | Settings |

### `onAction` handlers (contract)

`toggleHistorySidebar`, `toggleTTS`, `handleEmergencyButton`, `handleVoiceInput`, `toggleChatSheet`, `closeChatSheet`, `handleSendMessage`, `openSettingsFromSidebar`, `handleNewChat`, `closeSettings`.

---

## `onboarding-view.fxml`

**Controller:** `com.soteria.ui.onboarding.OnboardingController`  
**Root:** `VBox` (`styleClass="onboarding-root"`)

**`wizardStack`** (`StackPane`): three stacked children — **step 1**, **step 2**, **installation overlay**. Visibility toggled in code; step 2 and overlay start `visible="false"`.

### Step 1 (`step1Container`)

Model combo, language combo, **location** caption, step-1 error label, **Continue** → `#goToStep2`.

### Step 2 (`step2Container`)

Profile fields (name, gender, birth date, contact, medical), error label, **Back** → `#goToStep1`, **Finish** → `#handleStart`.

### Installation overlay (`installationOverlay`)

Blocking title/body, **boot** status label + progress bar, hold caption. Shown after finish while provisioning/install UX applies.

### `fx:id` inventory

All labels, combos, fields, buttons, and `bootProgress` / `bootStatusLabel` listed in `OnboardingController` map 1:1 to this file; see class JavaDoc for field grouping.

### `onAction` handlers

`goToStep2`, `goToStep1`, `handleStart`.

---

## Cross-references

| Topic | Spec |
|---|---|
| Chat logic, STT, inference, settings wiring | `../../java/com/soteria/ui/chat/_chat.spec.md` |
| Wizard flow, provisioning triggers | `../../java/com/soteria/ui/onboarding/_onboarding.spec.md` |
| Bubbles, session list rows, face (no FXML) | `../../java/com/soteria/ui/view/_view.spec.md` |
| `%key` bundles | `../../java/com/soteria/ui/i18n/_i18n.spec.md` |
| App load + which FXML loads when | `../../java/com/soteria/ui/_ui.spec.md` |

Paths above are relative to this file’s directory (`src/main/resources/fxml/`).
