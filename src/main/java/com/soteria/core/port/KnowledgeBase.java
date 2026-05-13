package com.soteria.core.port;

import com.soteria.core.domain.emergency.Protocol;

import java.util.List;
import java.util.Set;

/**
 * Port for protocol retrieval (the "R" in the RAG pipeline).
 */
public interface KnowledgeBase {

    record ProtocolMatch(Protocol protocol, String source, float score) {}

    /**
     * Finds protocols relevant to the query.
     *
     * @param rejectedIds        protocols already dismissed by the user in this session
     * @param searchPrinciplesOnly when {@code true}, restricts to principle/guide protocols
     */
    List<ProtocolMatch> findProtocols(String query, Set<String> rejectedIds, boolean searchPrinciplesOnly);

    Protocol getProtocolById(String id);
}
