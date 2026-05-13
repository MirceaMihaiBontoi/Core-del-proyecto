# UI View Package (`com.soteria.ui.view`)

## Responsibility

JavaFX **view helpers** with no FXML: programmatic construction and updates for the chat transcript, session history sidebar, and the voice-mode animated face. Callers are typically controllers that inject `VBox` / `ScrollPane` / labels from FXML.

## Structure

```
view/
├── ChatViewManager.java   # Bubbles, pseudo-markdown, sheet, thinking dots, header labels
├── SessionCoordinator.java # Session list UI + ChatSessionRepository wiring
├── SoterIAFace.java       # StackPane face + State-driven Timelines
└── _view.spec.md          # This document
```

## `ChatViewManager`

### `UIComponents` record

Holds references to nodes the manager mutates: message column, scroll pane, optional bottom sheet, subtitle and partial-transcript labels, AI status label, status circle.

### Behavior

| Area | Details |
|---|---|
| **Bubbles** | User right-aligned (`chat-bubble-user`), bot left (`chat-bubble-bot`). Inner `TextFlow`, max width ~420 / 400. |
| **Streaming reply** | `startBotMessage` / `updateBotMessage` reuse one `TextFlow` (`activeBotFlow`) until the next complete bubble. |
| **Thinking** | `showThinkingIndicator`: three `Label` bullets with `FadeTransition`; `removeThinkingIndicator` or `startBotMessage` clears it. |
| **Pseudo-Markdown** | Line-based: ordered (`^\d+\.\s`) or `- ` lists → leading `•` + bold prefix handling; inline `**...**` toggles bold via `Text.setStyle`. Not a full Markdown implementation. |
| **Sheet** | `openChatSheet` / `closeChatSheet`: `translateY` + opacity `Timeline` (~240 ms open, ~220 ms close); default fall height 520 if layout height is 0. |
| **Threading** | Scene mutations go through `Platform.runLater`. |
| **Logging** | Instance id prefix `ChatViewManager-xxxxxxxx` on some log lines. |

### `setAiStatusPill`

Sets status text and replaces dot style classes `ready`, `warming`, `offline`, `alert` on `statusDot` with the passed `dotClass`.

## `SessionCoordinator`

### Dependencies

- **`ChatSessionRepository.getInstance()`** for `saveSession`, `getAllSessions`, `delete`.
- Optional **`LocalizationService`** for key `ui.session.untitled` when a session has no title; else literal `"Untitled session"`.

### Sidebar

- **`toggleHistorySidebar(Runnable onRefresh)`**: flips `historySidebar` `visible` + `managed`; if opening, runs `onRefresh`.
- **`closeHistorySidebar`**: forces hidden + unmanaged (e.g. before another full-screen layer).

### `refreshSessionList`

Runs on the FX thread: clears `sessionList`, builds for each `ChatSession`:

- Row `HBox`: style `session-item`, plus `session-item-selected` when id matches `currentActive`.
- Title `Label` (`session-title`) and timestamp `dd/MM HH:mm` in system zone (`session-date`).
- Click on text column → `onSessionSelected`.
- Delete `Button` (`session-delete-button`) with Ikonli `mdal-delete_outline` → `repository.delete` then `onSessionDeleted`.

### Session lifecycle

- **`startNewSession`**: `new ChatSession()`, assign `activeSession`, `saveCurrentSession`, return session.
- **`setActiveSession`**: in-memory only until caller saves via repository.
- **`getActiveSession`**: last set or started session.

## `SoterIAFace`

Extends **`StackPane`**. Geometry: outer glow ring, pulse ring, filled face circle, two eye dots, open arc mouth. **`setPickOnBounds(false)`**.

### `State`

| State | Role (voice loop) |
|---|---|
| `IDLE` | Subtle breathe on face + glow opacity |
| `LISTENING` | Accent ring scales out/fades; outer glow pulse |
| `THINKING` | Warning tint (#f59e0b); eyes shifted; mouth narrowed |
| `SPEAKING` | Accent; mouth arc length/start angle oscillates |
| `ALERT` | Danger tint (#ef4444); stronger glow + face pulse |

Implementation: one **`Timeline`** per state (`activeAnimation`); **`resetTransient()`** stops it and resets scales, opacities, eye positions, mouth angles before the next state runs.

### API

- **`SoterIAFace()`** — radius `85`.
- **`SoterIAFace(double faceRadius)`** — drives all proportions from `faceRadius`.
- **`preferredDiameter()`** — `4 * baseRadius` for layout hints.

### Colors (code constants)

- Accent `#06b6d4`, danger `#ef4444`, warning `#f59e0b`, dark `#0a1214` (eyes/mouth stroke).

## How the pieces connect

Controllers construct **`ChatViewManager.UIComponents`** from `@FXML` fields and wire chat events to `addUserMessage` / streaming bot updates.

**`SessionCoordinator`** receives the sidebar `VBox` nodes from FXML; chat controller calls `refreshSessionList` after load or delete and `toggleHistorySidebar` from toolbar actions.

**`SoterIAFace`** is placed in the voice layout; another layer sets `State` from STT/LLM/TTS lifecycle callbacks.
