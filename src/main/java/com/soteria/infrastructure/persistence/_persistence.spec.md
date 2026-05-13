# Persistence Package

## Responsibility

Provides JSON-based storage for the two stateful entities that survive across application restarts: the device-owner profile and the chat session history. 
No database, no ORM — plain files on the local filesystem under `~/.soteria/`.

## Structure

```
persistence/
├── ProfileRepository.java      # Single-user profile — ~/.soteria/profile.json
└── ChatSessionRepository.java  # Session history  — ~/.soteria/sessions/{id}.json
```

## Data Flow & Integration

1. **Inbound Flow:** Core domain entities (`UserData`, `ChatSession`) are passed to the repositories to be serialized to JSON and persisted to the local filesystem.
2. **Outbound Flow:** Deserialized domain objects are returned back to the application logic via `load()` or `getAllSessions()`. No framework annotations bleed into the domain classes.
3. **Integration:** Uses the Jackson library exclusively for JSON serialization/deserialization.

## Lifecycle

1. **Initialization:** Repositories are typically instantiated on demand or as singletons. They rely on Java `Path` initialization pointing to the user's home directory.
2. **Operation:** 
   - `ProfileRepository.save()` will fail fast (`IOException`) if writing fails, as it is critical for onboarding.
   - `ChatSessionRepository.saveSession()` logs failures but does not throw, ensuring a non-critical feature doesn't crash the app.
3. **Error Handling:** Corrupt session files are skipped individually during `getAllSessions()`, ensuring partial history remains accessible.

## Design Decisions

- **Why individual files per session?**
  Appending to a single large file risks entire history corruption if the process is killed mid-write. One file per session restricts corruption blast radius.
- **Why skip corrupt session files instead of failing?**
  Session history is a convenience. Losing one entry is far less harmful than blocking access to all remaining history.
- **Why `FAIL_ON_UNKNOWN_PROPERTIES = false`?**
  Ensures forward-compatibility: profiles written by a newer version of the app can be read by older builds without throwing deserialization exceptions.
- **Dependency Isolation:** 
  Depends strictly on `core.model` and `core.domain`, completely unaware of `bootstrap`, `ui`, or `application`. Jackson is isolated inside this package.

## See also

- Chat session / message shapes persisted here: `../../core/domain/_domain.spec.md` (`chat` package)
