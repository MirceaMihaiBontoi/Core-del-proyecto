package com.soteria.infrastructure.persistence;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.soteria.core.domain.chat.ChatSession;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Persists chat sessions as individual JSON files under {@code ~/.soteria/sessions/}.
 *
 * <p>Each session is stored as {@code {sessionId}.json}. Files are written and
 * deleted at the individual-file level; no cross-session transactions are needed
 * because SoterIA is a single-user application.</p>
 *
 * <p>Corrupt or unreadable session files are skipped individually — a bad file
 * does not prevent the rest of the session history from loading.</p>
 */
public class ChatSessionRepository {

    private static final Logger log = Logger.getLogger(ChatSessionRepository.class.getName());
    private static final String JSON_EXTENSION = ".json";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final ChatSessionRepository INSTANCE = new ChatSessionRepository();
    private final Path sessionsDir;

    public static ChatSessionRepository getInstance() {
        return INSTANCE;
    }

    private ChatSessionRepository() {
        this(Paths.get(System.getProperty("user.home"), ".soteria", "sessions"));
    }

    public ChatSessionRepository(Path sessionsDir) {
        this.sessionsDir = sessionsDir;
        ensureDirectory();
    }

    private void ensureDirectory() {
        try {
            Files.createDirectories(sessionsDir);
        } catch (IOException _) {
            log.log(Level.SEVERE, "Could not create sessions directory: {0}", sessionsDir);
        }
    }

    /**
     * Loads all persisted sessions, sorted newest-first by timestamp.
     *
     * <p>Files that cannot be parsed are skipped individually so that a single
     * corrupt entry does not block access to the rest of the history.</p>
     *
     * @return mutable list of sessions; empty if none exist or the directory is unreadable
     */
    public List<ChatSession> getAllSessions() {
        List<ChatSession> sessions = new ArrayList<>();
        File dir = sessionsDir.toFile();
        File[] files = dir.listFiles((d, name) -> name.endsWith(JSON_EXTENSION));

        if (files != null) {
            for (File file : files) {
                try {
                    sessions.add(MAPPER.readValue(file, ChatSession.class));
                } catch (IOException _) {
                    log.log(Level.WARNING, "Failed to load session from: {0}", file.getName());
                }
            }
        }

        sessions.sort(Comparator.comparingLong(ChatSession::getTimestamp).reversed());
        return sessions;
    }

    /**
     * Writes the session to disk, overwriting any previous file for the same session ID.
     *
     * <p>Failures are swallowed — the chat flow must not be interrupted by a
     * persistence error.</p>
     *
     * @param session the session to persist; must not be {@code null}
     */
    public void saveSession(ChatSession session) {
        try {
            Path sessionFile = sessionsDir.resolve(session.getId() + JSON_EXTENSION);
            MAPPER.writeValue(sessionFile.toFile(), session);
        } catch (IOException _) {
            log.log(Level.SEVERE, "Failed to save session: {0}", session.getId());
        }
    }

    /**
     * Deletes the session file for the given ID. A missing file is silently ignored.
     *
     * @param sessionId ID of the session to remove
     */
    public void delete(String sessionId) {
        Path sessionFile = sessionsDir.resolve(sessionId + JSON_EXTENSION);
        try {
            Files.deleteIfExists(sessionFile);
        } catch (IOException _) {
            log.log(Level.WARNING, "Failed to delete session: {0}", sessionId);
        }
    }
}
