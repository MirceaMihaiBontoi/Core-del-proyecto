package com.soteria.core.port;

import com.soteria.core.domain.chat.ChatMessage;
import com.soteria.core.model.UserData;

import java.util.List;

/**
 * Port for the local LLM.
 * Inference is asynchronous — results arrive via {@link BrainCallback}.
 */
public interface Brain {

    /**
     * Callback for streaming LLM responses.
     */
    interface BrainCallback {
        void onPartialResponse(String text);
        void onFinalResponse(String text);

        /** Called when the LLM detects a protocol state change. */
        void onStatusUpdate(String protocolId, String status);

        /** Called when the LLM emits a structured command (e.g. switch protocol, trigger alert). */
        void onCommand(String type, String value);
    }

    void chat(List<ChatMessage> history, String context, UserData profile, String language, BrainCallback callback);

    void cancel();
}
