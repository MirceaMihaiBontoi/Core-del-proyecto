package com.soteria.core.domain.emergency;

import java.util.List;

/**
 * Represents an emergency protocol loaded from {@code medical_protocols.json}.
 *
 * <p>Protocols are retrieved by the RAG pipeline (Lucene + JGraphT) and passed
 * to the LLM as structured context. Follows the JavaBean convention for
 * Jackson deserialization.
 */
public class Protocol {
    private String id;
    private String title;
    private String category;
    private List<String> keywords;
    private List<String> steps;

    /** Lower value means higher priority when multiple protocols match. */
    private int priority;

    public Protocol() {
        // Default constructor required for Jackson JSON deserialization
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getKeywords() { return keywords; }
    public void setKeywords(List<String> keywords) { this.keywords = keywords; }

    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { this.priority = priority; }

    /**
     * Returns a plain-text summary of this protocol for use as LLM context.
     * Format: {@code "Protocol: <title>\nSteps: <step1>. <step2>. ..."}
     */
    public String getContent() {
        String stepsText = (steps == null || steps.isEmpty()) ? "No steps defined." : String.join(". ", steps);
        return "Protocol: " + title + "\nSteps: " + stepsText;
    }
}
