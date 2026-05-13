# Model Layer

## Responsibility

Contains the core data models that represent real-world entities in SoterIA.
Pure Java — no framework dependencies, no annotations.

## Relationship to `core/domain`

Both `model` and `domain` are part of the core layer and follow the same rules.
The distinction is informal:

- `model` — entities tied to the real world: the user and emergency events
- `domain` — concepts internal to the system: chat sessions, protocols, messages

## Classes

### `UserData`

The profile of the device owner. Persisted to `~/.soteria/profile.json` and loaded at startup.

Key design decisions:
- `ttsSpeechRate` and `wakeWordEnabled` use wrapper types (`Float`, `Boolean`) so `null` can represent "not configured". `effectiveTtsSpeechRate()` and `effectiveWakeWordEnabled()` resolve the defaults — callers never need to null-check these fields directly.
- `INCOMPLETE_NAME` is a sentinel that marks a profile created during onboarding but not yet finished. `isComplete()` checks for it.
- `medicalInfo` is free text injected verbatim into the LLM system prompt.

### `EmergencyEvent`

An immutable snapshot of a detected emergency. Created by the classifier when an emergency is identified.

Key design decisions:
- The compact constructor validates all fields at creation time. An instance in memory is always valid — no defensive null checks needed downstream.
- `severityLevel` is constrained to [1, 10] by the constructor.
- `userData` is a `String` (not a `UserData` object) to keep the event self-contained and serializable without pulling in the full profile.

## Design Rules

- No imports from `infrastructure`, `application`, or `ui` packages.
- No framework annotations.
- Both classes are records — immutable by design.
