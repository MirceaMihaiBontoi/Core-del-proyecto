package com.soteria.core.port;

import com.soteria.core.domain.emergency.Protocol;

import java.util.List;

/**
 * Port for emergency classification.
 */
public interface Triage {

    enum Intent {
        MEDICAL_EMERGENCY,
        SECURITY_EMERGENCY,
        ENVIRONMENTAL_EMERGENCY,
        TRAFFIC_EMERGENCY,
        UNKNOWN,
        INACTIVE,
        /** Non-emergency input — no protocol is activated. */
        GREETING_OR_CASUAL
    }

    record TriageResult(Protocol protocol, float score, Intent intent) {
        /**
         * @return {@code true} when a protocol matched with confidence ≥ 0.30
         */
        public boolean isEmergency() {
            return protocol != null && score >= 0.30f;
        }
    }

    /**
     * Classifies input against pre-selected protocol candidates.
     * Candidates come from {@link KnowledgeBase} — this separates retrieval from classification.
     */
    TriageResult classifyDynamic(String text, List<Protocol> candidates);
}
