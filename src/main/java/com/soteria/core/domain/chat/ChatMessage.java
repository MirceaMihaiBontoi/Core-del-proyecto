package com.soteria.core.domain.chat;

/**
 * A single turn in a conversation with the local LLM.
 * Roles follow the Gemma chat template: {@code "user"} or {@code "model"}.
 *
 * @param role    the speaker role
 * @param content the text content of the message
 */
public record ChatMessage(String role, String content) {

    public static ChatMessage user(String content) {
        return new ChatMessage("user", content);
    }

    public static ChatMessage model(String content) {
        return new ChatMessage("model", content);
    }
}
