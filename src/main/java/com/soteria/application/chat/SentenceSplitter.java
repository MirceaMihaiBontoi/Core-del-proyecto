package com.soteria.application.chat;

/**
 * Incrementally splits the assistant token stream into speakable sentences for streaming TTS.
 *
 * <p>Prefers low latency (commas and length heuristics) while avoiding tiny fragments where possible.
 * Callers should pass {@code isFinal=true} on the last chunk to flush the tail. Use {@link #reset()} between replies.</p>
 *
 * <p>Spec: {@code com.soteria.application.chat._chat.spec.md}.</p>
 */
public class SentenceSplitter {

    /**
     * After the first speakable segment of a reply is emitted, the second segment often stalls waiting for
     * punctuation (e.g. short "por favor," then silence). Flush the next chunk after this many whitespace-delimited
     * words if no better boundary exists yet. Not used for the third segment onward ({@code sentenceCount == 1} only).
     */
    private static final int WORD_FALLBACK_AFTER_FIRST = 5;

    /** Called for each completed sentence segment in order. */
    public interface SentenceListener {
        void onSentenceReady(String sentence);
    }

    private int lastTTSSentenceEnd = 0;
    private int sentenceCount = 0;

    /**
     * Scans {@code fullText} for boundaries after the last emitted sentence and invokes {@code listener} for each new segment.
     *
     * @param fullText accumulated assistant text so far
     * @param isFinal  {@code true} on the closing callback to flush remaining text without waiting for more tokens
     * @param listener receives non-empty trimmed sentences
     */
    public void process(String fullText, boolean isFinal, SentenceListener listener) {
        String remaining = fullText.substring(lastTTSSentenceEnd);

        while (true) {
            int boundaryIndex = findBestSplitPoint(remaining, isFinal);
            if (boundaryIndex == -1) break;

            int absoluteBoundary = lastTTSSentenceEnd + boundaryIndex + 1;
            String sentence = fullText.substring(lastTTSSentenceEnd, absoluteBoundary).trim();

            if (!sentence.isEmpty()) {
                sentenceCount++;
                listener.onSentenceReady(sentence);
                lastTTSSentenceEnd = absoluteBoundary;
            }

            remaining = fullText.substring(lastTTSSentenceEnd);
        }
    }

    private int findBestSplitPoint(String text, boolean isFinal) {
        if (text.isEmpty()) return -1;

        int boundary = findFirstSentenceBoundary(text);

        if (boundary != -1) {
            String candidate = text.substring(0, boundary + 1).trim();
            
            // CRITICAL: For ANY sentence with comma, ALWAYS send immediately
            // This eliminates perceived latency for all comma-terminated phrases
            boolean endsWithComma = candidate.endsWith(",") || candidate.endsWith("，") || candidate.endsWith("\u3001");
            
            if (endsWithComma) {
                return boundary;  // Skip length check, send immediately
            }
            
            if (!isChunkLongEnoughForTts(candidate, isFinal)) {
                boundary = -1;
            }
        }

        if (boundary == -1 && !isFinal) {
            boundary = softCommaSplit(text);
        }

        if (boundary == -1 && !isFinal && sentenceCount == 1) {
            boundary = wordCountFallbackSplit(text);
        }

        if (boundary != -1) {
            return boundary;
        }

        return isFinal ? text.length() - 1 : -1;
    }

    /**
     * Last char index of the Nth whitespace-delimited word in {@code text} ({@code -1} if fewer than N words).
     * Used only while building the second segment (exactly one prior segment was emitted for this reply).
     */
    private static int endIndexOfWord(String text, int wordNumberOneBased) {
        int i = 0;
        int n = text.length();
        while (i < n && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        int seen = 0;
        while (i < n) {
            while (i < n && !Character.isWhitespace(text.charAt(i))) {
                i++;
            }
            seen++;
            if (seen == wordNumberOneBased) {
                return i - 1;
            }
            while (i < n && Character.isWhitespace(text.charAt(i))) {
                i++;
            }
        }
        return -1;
    }

    /** @return end char index for a segment of {@link #WORD_FALLBACK_AFTER_FIRST} words, or {@code -1} */
    private static int wordCountFallbackSplit(String text) {
        if (text == null || text.isEmpty()) {
            return -1;
        }
        int end = endIndexOfWord(text, WORD_FALLBACK_AFTER_FIRST);
        return end >= 0 ? end : -1;
    }

    /**
     * Avoid tiny prosodic fragments for Latin; CJK rarely uses spaces so use code-point span instead.
     * For first sentence after comma, be more aggressive to reduce perceived latency.
     */
    private boolean isChunkLongEnoughForTts(String candidate, boolean isFinal) {
        if (isFinal) {
            return true;
        }
        String trimmed = candidate.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        
        // Check if this is first sentence ending with comma
        boolean isFirstSentence = (sentenceCount == 0);
        boolean endsWithComma = trimmed.endsWith(",") || trimmed.endsWith("，") || trimmed.endsWith("\u3001");
        
        int cps = trimmed.codePointCount(0, trimmed.length());
        int words = trimmed.split("\\s+").length;
        
        // For first sentence with comma, be more aggressive (lower thresholds)
        if (isFirstSentence && endsWithComma) {
            boolean result = (cps >= 8 || words >= 2);
            if (result) {
                return true;
            }
        }
        
        // Normal thresholds for other cases
        if (cps >= 6) {
            return true;
        }
        if (words >= 3) {
            return true;
        }
        
        int lastCp = trimmed.codePointBefore(trimmed.length());
        return isStrongSentenceEnd(lastCp);
    }

    /** Pause comma / enumeration mark when the segment is already long (code points). */
    private int softCommaSplit(String text) {
        int len = text.codePointCount(0, text.length());
        if (len <= 20) {
            return -1;
        }
        int cpPos = 0;
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            int charLen = Character.charCount(cp);
            if (isPauseComma(cp) && cpPos >= 8) {
                return i + charLen - 1;
            }
            cpPos++;
            i += charLen;
        }
        return -1;
    }

    private int findFirstSentenceBoundary(String text) {
        if (text.isEmpty()) return -1;

        boolean isFirstSentence = (sentenceCount == 0);
        // AGGRESSIVE: Send comma chunks immediately without waiting for more context
        int commaThresholdCp = 0;  // No threshold - send immediately after comma
        int runOnCpLimit = isFirstSentence ? 46 : 72;

        int cpIdx = 0;
        int lastEndCharIdx = -1;
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            int charLen = Character.charCount(cp);
            int endCharIdx = i + charLen - 1;

            if (cpIdx >= runOnCpLimit && lastEndCharIdx >= 0) {
                return lastEndCharIdx;
            }

            if (isStrongSentenceEnd(cp)) {
                return endCharIdx;
            }
            if (isPauseComma(cp) && cpIdx >= commaThresholdCp) {
                return endCharIdx;
            }

            lastEndCharIdx = endCharIdx;
            cpIdx++;
            i += charLen;
        }
        
        return -1;
    }

    private static boolean isStrongSentenceEnd(int cp) {
        return cp == '.' || cp == '!' || cp == '?' || cp == ';' || cp == ':' || cp == '\n'
                || cp == '\u3002' // 。
                || cp == '\uFF01' || cp == '\uFF1F' || cp == '\uFF1B' || cp == '\uFF1A'
                || cp == '\u2026'; // …
    }

    private static boolean isPauseComma(int cp) {
        return cp == ',' || cp == '\uFF0C' || cp == '\u3001';
    }

    /** @return number of sentences emitted since construction or last {@link #reset()} */
    public int getSentenceCount() {
        return sentenceCount;
    }

    /** Clears streaming cursor and sentence count for a new assistant reply. */
    public void reset() {
        lastTTSSentenceEnd = 0;
        sentenceCount = 0;
    }
}
