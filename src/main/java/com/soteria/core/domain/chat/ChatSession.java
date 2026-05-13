package com.soteria.core.domain.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a persistent emergency chat session.
 *
 * <p>Groups the full conversation history with all the state needed to drive
 * the RAG pipeline: active protocol, step progress, rejected protocols, and
 * any extra context injected into the LLM prompt.
 *
 * <p>Follows the JavaBean convention so Jackson can serialize/deserialize
 * sessions to JSON for persistence.
 */
public class ChatSession {

    private String id;
    private long timestamp;
    private String title;
    private List<ChatMessage> messages;
    private Set<String> rejectedProtocolIds;
    private String contextualExtensions;
    private String activeEmergencyId;

    /**
     * When {@code true}, the RAG pipeline will not switch to a different protocol
     * mid-session even if a new emergency type is detected.
     */
    private boolean protocolLocked = false;

    private Map<String, Integer> protocolProgress = new HashMap<>();
    private Map<String, String> requestedStepsMap = new HashMap<>();
    private Map<String, List<String>> categorizedContext = new HashMap<>();
    private Set<String> activeCategories = new HashSet<>();

    public ChatSession() {
        this.id = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
        this.title = null;
        this.messages = new ArrayList<>();
        this.rejectedProtocolIds = new HashSet<>();
        this.contextualExtensions = "";
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<ChatMessage> getMessages() { return messages; }
    public void setMessages(List<ChatMessage> messages) { this.messages = messages; }

    public Set<String> getRejectedProtocolIds() { return rejectedProtocolIds; }
    public void setRejectedProtocolIds(Set<String> rejectedProtocolIds) { this.rejectedProtocolIds = rejectedProtocolIds; }

    public String getContextualExtensions() { return contextualExtensions; }
    public void setContextualExtensions(String contextualExtensions) { this.contextualExtensions = contextualExtensions; }

    public String getActiveEmergencyId() { return activeEmergencyId; }
    public void setActiveEmergencyId(String activeEmergencyId) { this.activeEmergencyId = activeEmergencyId; }

    public boolean isProtocolLocked() { return protocolLocked; }
    public void setProtocolLocked(boolean protocolLocked) { this.protocolLocked = protocolLocked; }

    public Map<String, Integer> getProtocolProgress() { return protocolProgress; }
    public void setProtocolProgress(Map<String, Integer> protocolProgress) { this.protocolProgress = protocolProgress; }

    public Map<String, String> getRequestedStepsMap() { return requestedStepsMap; }
    public void setRequestedStepsMap(Map<String, String> requestedStepsMap) { this.requestedStepsMap = requestedStepsMap; }

    public Map<String, List<String>> getCategorizedContext() { return categorizedContext; }
    public void setCategorizedContext(Map<String, List<String>> categorizedContext) { this.categorizedContext = categorizedContext; }

    public Set<String> getActiveCategories() { return activeCategories; }
    public void setActiveCategories(Set<String> activeCategories) { this.activeCategories = activeCategories; }

    /**
     * Returns the current step index for the active protocol,
     * or {@code 0} if no protocol is active.
     */
    public int getCurrentStepIndex() {
        if (activeEmergencyId == null) return 0;
        return protocolProgress.getOrDefault(activeEmergencyId, 0);
    }

    /**
     * Sets the step index for the active protocol.
     * Has no effect if no protocol is currently active.
     */
    public void setCurrentStepIndex(int index) {
        if (activeEmergencyId != null) {
            protocolProgress.put(activeEmergencyId, index);
        }
    }

    /**
     * Advances the step index for the given protocol by one.
     * If the protocol has not been started yet, its index is initialised to {@code 1}.
     *
     * @param protocolId ignored if {@code null}
     */
    public void incrementStepIndex(String protocolId) {
        if (protocolId != null) {
            int current = protocolProgress.getOrDefault(protocolId, 0);
            protocolProgress.put(protocolId, current + 1);
        }
    }

    public void addMessage(ChatMessage message) {
        this.messages.add(message);
    }

    /**
     * Marks a protocol as rejected so the RAG pipeline will not suggest it again.
     * Ignored if {@code protocolId} is {@code null} or blank.
     */
    public void addRejectedProtocolId(String protocolId) {
        if (protocolId != null && !protocolId.isBlank()) {
            this.rejectedProtocolIds.add(protocolId);
        }
    }
}
