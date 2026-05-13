# Notification Package

## Responsibility

Implements `core.port.AlertService` to handle the final dispatch of emergency events. It simulates handoff to emergency services and writes alerts to a local audit log, acting as the durable record until real SMS/Telephony integration is implemented.

## Structure

```
notification/
└── NotificationAlertService.java  # Implements AlertService; local logging + simulated dispatch
```

## Data Flow & Integration

1. **Inbound Flow:** Receives fully formed `EmergencyEvent` and `UserData` objects from the application layer's decision engine.
2. **Outbound Flow:** 
   - Appends formatted textual alerts to the filesystem log (`logs/emergency_alerts.log`).
   - Dispatches signals to emergency contacts (currently mapped to log warnings/info).
3. **Integration:** It bridges the core domain's desire to "send an alert" with the actual I/O mechanism. 

## Lifecycle

1. **Initialization:** Created during application bootstrap. Can optionally take a custom `Path` for test injection.
2. **Execution:** 
   - Synchronously formats the emergency payload.
   - Appends the payload to the log file, auto-creating the directory and file if it does not exist.
   - Blocks via `Thread.sleep` for a predefined duration (1500ms) to simulate emergency API connection latency.
3. **Error Handling:** Catch-and-log approach. If filesystem persistence fails, it logs `SEVERE` but prevents the caller thread from crashing.

## Design Decisions

- **Why simulate the call?**
  SoterIA is in early-stage development where real dial-out to emergency services is risky and complex. Simulating the connection block allows the UI to render appropriate loading states and connection indicators.
- **Local Text Logging:**
  Using plain text appended to `logs/emergency_alerts.log` offers an easily auditable trail of events that survives application restarts, without requiring a database.
- **Internal Constructors for Tests:**
  `NotificationAlertService` accepts a `Path` in a package-private constructor, allowing unit tests to direct writes to temporary files instead of polluting the developer's root directory.

## See also

- `AlertService` port: `../../core/port/_port.spec.md`
- UI SOS dispatch: `../../ui/chat/_chat.spec.md` (`ChatEmergencyDispatch`)
