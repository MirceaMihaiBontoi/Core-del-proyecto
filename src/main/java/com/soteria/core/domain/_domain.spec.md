# Domain Layer

## Responsibility

Contains the core business concepts of SoterIA. These classes represent what the system *is* and *knows*, independent of any technology — no JavaFX, no Jackson, no LLM library, no database.

If the UI, the LLM backend, or the persistence mechanism were replaced entirely, this layer would not change.

## Packages

### `chat`

Models a conversation between the user and the local LLM.

| Class | Type | Role |
|---|---|---|
| `ChatMessage` | record | A single immutable turn: role (`"user"` / `"model"`) + text content |
| `ChatSession` | class | A full session: message history + RAG state (active protocol, step progress, rejected protocols, injected context) |

`ChatSession` is mutable by design — its state evolves continuously as the conversation progresses. `ChatMessage` is a record because a sent message is a fact that should never change.

### `emergency`

Models the emergency protocols that drive the RAG pipeline.

| Class | Type | Role |
|---|---|---|
| `Protocol` | class | A single emergency protocol loaded from `medical_protocols.json`: id, title, category, keywords, ordered steps, and priority |

`Protocol` is a mutable class (not a record) because JSON deserialization requires a no-arg constructor and setters. It has no Jackson dependency — the JavaBean convention is enough for any standard deserializer to work with it.

## Design Rules

- No imports from `infrastructure`, `application`, or `ui` packages.
- No framework annotations (`@Component`, `@Entity`, etc.).
- All domain objects must be serializable to JSON via standard JavaBean conventions (no-arg constructor + getters/setters) without framework-specific annotations.
