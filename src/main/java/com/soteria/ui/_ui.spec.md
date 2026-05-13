# UI Layer

## Responsibility

The UI layer is the **JavaFX presentation shell** for SoterIA: window lifecycle, FXML controllers, programmatic view helpers, resource bundles, and glue to **`BootstrapService`** (provisioning, STT/TTS, inference). It contains **no domain business rules** — orchestration, layout, and user events only.

## Structure

```
ui/
├── MainApp.java              # Application entry — theme, bootstrap pre-init, onboarding vs chat routing
├── chat/                     # Post-onboarding HUD: ChatController + STT/TTS/inference wiring helpers
├── onboarding/               # First-run wizard + language catalog
├── view/                     # Chat transcript, session sidebar, SoterIAFace (no FXML)
├── i18n/                     # Profile/onboarding language string → java.util.Locale
└── _ui.spec.md               # This document
```

FXML, CSS, and image assets live under `src/main/resources` (`fxml/`, `styles/`, `i18n/`). Scene layout and `fx:id` maps: **`src/main/resources/fxml/_fxml.spec.md`**.

Package-level detail:

- **`../application/chat/_chat.spec.md`** — `InferenceEngine`, RAG manifest, history filter, streaming TTS sentence split.
- **`_chat.spec.md`** — `ChatController` lifecycle, voice/text, settings, SOS, UI helpers (`ChatOutboundDedupe`, `ChatSTTListenerFactory`, …).
- **`_onboarding.spec.md`** — wizard steps, provisioning triggers, `OnboardingLanguageCatalog`.
- **`_view.spec.md`** — `ChatViewManager`, `SessionCoordinator`, `SoterIAFace`.
- **`_i18n.spec.md`** — `UiLocales` and bundle naming.

## How the pieces connect

### Application launch

1. **`LlamaNativeBootstrap.applyIfNeeded()`** (in `main`) then **`Application.launch`**.
2. **`MainApp.start`**: AtlantaFX **Primer Dark**, **`BootstrapService.preInitialize()`**, listener on **`readyProperty`** → attempt navigation to chat when bootstrap completes.
3. **`ProfileRepository.load()`**:
   - **No / incomplete profile** → **`OnboardingController`** (`onboarding-view.fxml`) with default JVM `Locale` for first paint; user picks language → `UiLocales` + `LocalizationService` for the rest of the flow.
   - **Complete profile** → **`ChatController`** (`chat-view.fxml`) with locale from **`UiLocales.fromPreferredLanguage`** and **`startProvisioning`** for stored model + language.

4. **`completeOnboarding()`** (from wizard) re-triggers the same **ready + profile** gate so chat opens only when provisioning and persisted data align.

### Runtime stacks inside the UI

| Concern | Primary owners |
|---|---|
| Chat transcript, sheet, mic HUD, inference, SOS | `chat` package (`ChatController` + helpers); view updates via `view` classes |
| Session list / history drawer | `SessionCoordinator` + `ChatSessionRepository` |
| Voice face states | `SoterIAFace` + callbacks from chat / inference bridge |
| Language for bundles | `UiLocales` + `OnboardingLanguageCatalog`; `LocalizationService` from bootstrap |

### Shutdown

**`MainApp`** / window close → **`BootstrapService.shutdown()`** (native runtimes and threads).

## Dependency rules

- **UI may depend on** `com.soteria.core` (model, domain, ports), `com.soteria.infrastructure.bootstrap`, concrete infra used at the edge (e.g. persistence repository, GPS/alert implementations wired in controllers), and **`com.soteria.application`** where the chat pipeline exposes facades (`InferenceEngine`).
- **UI must not be imported by** `core`. Infrastructure should depend on **`core` ports**, not on FXML controllers.
- **FXML controllers stay thin**: heavy sequences belong in helpers (`chat/*`, `view/*`) or downstream services already provided by bootstrap.

The dependency arrow for UX-specific code points **outward** from `core`: `ui` → `application` / `infrastructure` / `core`, never the reverse.
